package net.gini.dropwizard.gelf.filters;

import com.google.common.base.Charsets;

import com.google.common.collect.Queues;
import net.gini.dropwizard.gelf.testing.ExpectedLogEntry;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import ch.qos.logback.classic.spi.ILoggingEvent;
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
    @Rule
    public ExpectedLogEntry expectedLogEntry = new ExpectedLogEntry();

    private static WebTarget target;

    @BeforeClass
    public static void setupTarget() {
        target = ClientBuilder.newClient().target("http://127.0.0.1:" + APP.getLocalPort());
    }

    @Test
    public void testMdcIsFilled() throws InterruptedException {
        expectedLogEntry.mdcKeyAndValue(REQ_URI, "/hello");
        final Response response = target.path("/hello").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = expectedLogEntry.getEntry();
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
    public void testResponseTime() throws InterruptedException {
        expectedLogEntry.mdcKeyAndValue(REQ_URI, "/slow");
        final Response response = target.path("/slow").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = expectedLogEntry.getEntry();
        verifyResponseTimeLongerThan(logEntry, SLEEP_TIME_IN_MS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testResponseSize() throws InterruptedException {
        expectedLogEntry.mdcKeyAndValue(REQ_URI, "/large");
        final Response response = target.path("/large").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        final ILoggingEvent logEntry = expectedLogEntry.getEntry();
        verifyLength(logEntry, LARGE_ITERATIONS * HELLO_WORLD_BYTES.length);
    }

    @Test
    public void testAsyncResponse() throws InterruptedException {
        expectedLogEntry.mdcKeyAndValue(REQ_URI, "/async");
        final Response response = target.path("/async").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = expectedLogEntry.getEntry();
        final Map<String, String> mdc = logEntry.getMDCPropertyMap();
        assertThat(mdc.get(PROTOCOL)).isEqualTo("HTTP/1.1");
        assertThat(mdc.get(RESP_STATUS)).isEqualTo("200");
        assertThat(mdc.get(RESP_CONTENT_TYPE)).isEqualTo(TEXT_PLAIN);
        verifyLength(logEntry, HELLO_WORLD_BYTES.length);
        verifyResponseTimeLongerThan(logEntry, SLEEP_TIME_IN_MS, TimeUnit.MILLISECONDS);
    }


    /**
     * this test verifies that the byte counter works as expected on short response entities.
     *
     * requests with such a short message will probably not be transferred using chunks (depending on the implementation)
     * this way, the Content-Length header might be set
     */
    @Test
    public void testStreamingResponse() throws InterruptedException {
        expectedLogEntry.mdcKeyAndValue(REQ_URI, "/streaming");
        final Response response = target.path("/streaming").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(HELLO_WORLD);
        final ILoggingEvent logEntry = expectedLogEntry.getEntry();
        final Map<String, String> mdc = logEntry.getMDCPropertyMap();
        assertThat(mdc.get(PROTOCOL)).isEqualTo("HTTP/1.1");
        assertThat(mdc.get(RESP_STATUS)).isEqualTo("200");
        assertThat(mdc.get(RESP_CONTENT_TYPE)).isEqualTo(TEXT_PLAIN);
        verifyLength(logEntry, HELLO_WORLD_BYTES.length);
    }

    /**
     * this test verifies that the byte counter works as expected on larger response entities.
     *
     * requests with an entity stream larger than the buffer used in the output generator will actually be
     * transferred in chunks, so the Content-Lenght header will not be set, since the server starts sending data
     * before the end of the stream
     */
    @Test
    public void testStreamingLargeResponse() throws InterruptedException {
        expectedLogEntry.mdcKeyAndValue(REQ_URI, "/largeStream/100000");
        final Response response = target.path("/largeStream/100000").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        String s = response.readEntity(String.class);
        assertThat(s.length()).isGreaterThan(100000);
        final ILoggingEvent logEntry = expectedLogEntry.getEntry();
        final Map<String, String> mdc = logEntry.getMDCPropertyMap();
        assertThat(mdc.get(PROTOCOL)).isEqualTo("HTTP/1.1");
        assertThat(mdc.get(RESP_STATUS)).isEqualTo("200");
        assertThat(mdc.get(RESP_CONTENT_TYPE)).isEqualTo(TEXT_PLAIN);
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

        @Path("/streaming")
        @GET
        public void streamingHelloWorld(@Suspended final AsyncResponse asyncResponse,
                                        @Context HttpServletResponse servletResponse,
                                        @Context HttpServletRequest servletRequest) throws IOException {

            ArrayDeque<String> strings = Queues.newArrayDeque(Arrays.asList("Hello", " wörld!"));
            final Supplier<Optional<String>> supplier = () -> {
                if(strings.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(strings.pop());
            };

            streaming(supplier, asyncResponse, servletResponse, servletRequest);
        }


        @Path("/largeStream/{count}")
        @GET
        public void streamingLargeData(@Suspended final AsyncResponse asyncResponse,
                                       @Context HttpServletResponse servletResponse,
                                       @Context HttpServletRequest servletRequest,
                                       @PathParam("count") long count) throws IOException {

            AtomicLong atomicLong = new AtomicLong(count);

            final Supplier<Optional<String>> supplier = () -> {
                if(atomicLong.get() == 0) {
                    return Optional.empty();
                }

                String s = Double.toHexString(ThreadLocalRandom.current().nextDouble());
                atomicLong.decrementAndGet();
                return Optional.of(s);
            };

            streaming(supplier, asyncResponse, servletResponse, servletRequest);
        }


        private void streaming(Supplier<Optional<String>> supplier, final AsyncResponse asyncResponse,
                              HttpServletResponse servletResponse,
                              HttpServletRequest servletRequest) throws IOException {

            // this must be called before the async context is requested
            servletResponse.setHeader("Content-Type", "text/plain");

            // set a timeout, so the request won't run forever
            servletRequest.getAsyncContext().setTimeout(1000L);

            final AsyncContext asyncContext = servletRequest.getAsyncContext();
            final ServletOutputStream servletOutputStream = asyncContext.getResponse().getOutputStream();

            servletOutputStream.setWriteListener(new WriteListener() {
                private boolean done;

                @Override
                public void onWritePossible() throws IOException {
                    while (servletOutputStream.isReady()) {
                        if(done) {
                            asyncContext.complete();
                            break;
                        } else {
                            // here you would usually get the data from some async source, e.g. a Subscriber
                            Optional<String> s = supplier.get();
                            if(!s.isPresent()) {
                                done = true;
                            } else {
                                servletOutputStream.write(s.get().getBytes());
                            }
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    // NOP
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
