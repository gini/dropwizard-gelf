package net.gini.dropwizard.gelf.filters;

import com.google.common.base.Charsets;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static net.gini.dropwizard.gelf.filters.GelfLoggingFilter.AdditionalKeys.PROTOCOL;
import static net.gini.dropwizard.gelf.filters.GelfLoggingFilter.AdditionalKeys.REQ_URI;
import static net.gini.dropwizard.gelf.filters.GelfLoggingFilter.AdditionalKeys.RESP_CONTENT_TYPE;
import static net.gini.dropwizard.gelf.filters.GelfLoggingFilter.AdditionalKeys.RESP_LENGTH;
import static net.gini.dropwizard.gelf.filters.GelfLoggingFilter.AdditionalKeys.RESP_STATUS;
import static net.gini.dropwizard.gelf.filters.GelfLoggingFilter.AdditionalKeys.RESP_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for {@link GelfLoggingFilter}.
 */
public class GelfLoggingFilterTest {

    private static final String HELLO_WORLD = "Hello wörld!";
    private static final byte[] HELLO_WORLD_BYTES = HELLO_WORLD.getBytes(Charsets.UTF_8);
    private static final int LARGE_ITERATIONS = 1024;
    private static final long SLEEP_TIME_IN_MS = TimeUnit.SECONDS.toMillis(1);

    @ClassRule
    public static DropwizardAppRule<Configuration> APP = new DropwizardAppRule<>(TestApp.class);

    private static WebTarget target;
    private static ListAppender<ILoggingEvent> appender;
    private int logStartIndex;

    @BeforeClass
    public static void setupTarget() {
        target = ClientBuilder.newClient().target("http://127.0.0.1:" + APP.getLocalPort());
    }

    @BeforeClass
    public static void setupAppender() {
        appender = new ListAppender<>();
        final Logger logger = (Logger) LoggerFactory.getLogger(GelfLoggingFilter.class);
        logger.addAppender(appender);
        appender.start();
    }

    @Before
    public void setUp() {
        logStartIndex = appender.list.size();
    }

    @Test
    public void testMdcIsFilled() {
        final Response response = target.path("/hello").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = findLogEntry("/hello");
        assertThat(logEntry.getFormattedMessage())
                .isEqualTo("127.0.0.1 - - \"GET /hello HTTP/1.1\" 200 " + HELLO_WORLD_BYTES.length);
        final Map<String, String> mdc = logEntry.getMDCPropertyMap();
        assertThat(mdc.get(RESP_TIME)).isNotEmpty();
        assertThat(mdc.get(PROTOCOL)).isEqualTo("HTTP/1.1");
        assertThat(mdc.get(RESP_STATUS)).isEqualTo("200");
        assertThat(mdc.get(RESP_CONTENT_TYPE)).isEqualTo(TEXT_PLAIN);
        verifyLength(logEntry, HELLO_WORLD_BYTES.length);
    }

    @Test
    public void testResponseTime() {
        final Response response = target.path("/slow").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = findLogEntry("/slow");
        verifyResponseTimeLongerThan(logEntry, SLEEP_TIME_IN_MS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testResponseSize() {
        final Response response = target.path("/large").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final ILoggingEvent logEntry = findLogEntry("/large");
        verifyLength(logEntry, LARGE_ITERATIONS * HELLO_WORLD_BYTES.length);
    }

    @Test
    public void testAsyncResponse() throws InterruptedException {
        final Response response = target.path("/async").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = findLogEntry("/async");
        final Map<String, String> mdc = logEntry.getMDCPropertyMap();
        assertThat(mdc.get(PROTOCOL)).isEqualTo("HTTP/1.1");
        assertThat(mdc.get(RESP_STATUS)).isEqualTo("200");
        assertThat(mdc.get(RESP_CONTENT_TYPE)).isEqualTo(TEXT_PLAIN);
        verifyLength(logEntry, HELLO_WORLD_BYTES.length);
        verifyResponseTimeLongerThan(logEntry, SLEEP_TIME_IN_MS, TimeUnit.MILLISECONDS);
    }

    private void verifyLength(final ILoggingEvent event, final int expectedSize) {
        final Map<String, String> mdc = event.getMDCPropertyMap();
        assertThat(mdc.get(RESP_LENGTH)).isEqualTo(String.valueOf(expectedSize));
    }

    private void verifyResponseTimeLongerThan(final ILoggingEvent event, final long expectedTime, final TimeUnit unit) {
        final Map<String, String> mdc = event.getMDCPropertyMap();
        final String responseTime = mdc.get(RESP_TIME);
        assertThat(responseTime).isNotNull();
        assertThat(Long.parseLong(responseTime)).isGreaterThan(unit.toNanos(expectedTime));

    }

    private ILoggingEvent findLogEntry(final String requestPath) {
        for (ILoggingEvent event : appender.list.subList(logStartIndex, appender.list.size())) {
            final Map<String, String> mdc = event.getMDCPropertyMap();
            if (requestPath.equals(mdc.get(REQ_URI))) {
                return event;
            }
        }
        fail("Log entry for request " + requestPath + " not found!");
        // Not reached as fail throws
        return null;
    }

    public static class TestApp extends Application<Configuration> {

        @Override
        public void run(final Configuration configuration, final Environment environment) throws Exception {
            environment.servlets()
                    .addFilter("request-log", new GelfLoggingFilter())
                    .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
            environment.jersey().register(TestResource.class);
        }
    }

    @Path("/")
    @Produces(TEXT_PLAIN)
    public static class TestResource {

        private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

        @Path("/hello")
        @GET
        public String helloWorld() {
            return HELLO_WORLD;
        }

        @Path("/async")
        @GET
        public void asyncHelloWorld(@Suspended final AsyncResponse response) {
            EXECUTOR_SERVICE.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Thread.sleep(SLEEP_TIME_IN_MS);
                    response.resume(HELLO_WORLD);
                    return null;
                }
            });
        }

        @Path("/slow")
        @GET
        public String slowHelloWorld() throws InterruptedException {
            Thread.sleep(SLEEP_TIME_IN_MS);
            return HELLO_WORLD;
        }

        @Path("/large")
        @GET
        public StreamingOutput largeHelloWorld() {
            return new StreamingOutput() {
                @Override
                public void write(final OutputStream outputStream) throws IOException, WebApplicationException {
                    for (int i = 0; i < LARGE_ITERATIONS; ++i) {
                        outputStream.write(HELLO_WORLD_BYTES);
                    }
                }
            };
        }
    }
}
