package net.gini.dropwizard.gelf.testing;

import net.gini.dropwizard.gelf.filters.GelfLoggingFilter;

import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import static org.junit.Assert.fail;

/**
 * A {@link org.junit.rules.TestRule} for expected log entries.
 */
public class ExpectedLogEntry extends ExternalResource {

    /**
     * How long to wait for log entries to be logged before timing out
     */
    private static final long LOG_TIMEOUT_IN_MS = TimeUnit.SECONDS.toMillis(1);

    private LogAppender appender;
    private String key;
    private String expectedValue;
    private ILoggingEvent entry;
    private final CountDownLatch entryFound = new CountDownLatch(1);

    public ExpectedLogEntry() {
        this(null, null);
    }

    public ExpectedLogEntry(final String key, final String expectedValue) {
        this.key = key;
        this.expectedValue = expectedValue;
    }

    public void mdcKeyAndValue(final String key, final String expectedValue) {
        this.key = key;
        this.expectedValue = expectedValue;
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        appender = new LogAppender();
        final Logger logger = (Logger) LoggerFactory.getLogger(GelfLoggingFilter.class);
        logger.addAppender(appender);
        appender.start();
    }

    @Override
    protected void after() {
        super.after();
        final Logger logger = (Logger) LoggerFactory.getLogger(GelfLoggingFilter.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    public ILoggingEvent getEntry() throws InterruptedException {
        if (!entryFound.await(LOG_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)) {
            fail("Expected log entry with MDC value " + key + "=" + expectedValue + ", but got none");
        }
        return entry;
    }

    private class LogAppender extends AppenderBase<ILoggingEvent> {

        @Override
        protected void append(final ILoggingEvent eventObject) {
            if (key != null) {
                final String value = eventObject.getMDCPropertyMap().get(key);
                if (expectedValue.equals(value)) {
                    entry = eventObject;
                    entryFound.countDown();
                }
            }
        }
    }
}
