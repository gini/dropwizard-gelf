package net.gini.dropwizard.gelf.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.helpers.NOPAppender;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.ThresholdLevelFilterFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GelfAppenderFactoryTest {

    private static final String APPLICATION_NAME = "applicationName";

    @Test
    public void hasValidDefaults() throws IOException, ConfigurationException {
        final GelfAppenderFactory factory = new GelfAppenderFactory();

        assertThat("default host is 'localhost'", factory.getHost(), is("localhost"));
        assertThat("default port is 12201", factory.getPort(), is(12201));
        assertThat("default origin host is absent", factory.getOriginHost(), is(Optional.<String>empty()));
        assertThat("default facility is absent", factory.getFacility().isPresent(), is(false));
        assertThat("default additional fields are empty", factory.getAdditionalFields().isEmpty(), is(true));
        assertThat("default field types are empty", factory.getAdditionalFieldTypes().isEmpty(), is(true));
        assertThat("default MDC fields are empty", factory.getMdcFields().isEmpty(), is(true));
        assertThat("default dynamic MDC fields are empty", factory.getDynamicMdcFields().isEmpty(), is(true));
        assertThat("default maximum message size is 8192", factory.getMaximumMessageSize(), is(8192));
        assertThat("default timestamp pattern is \"yyyy-MM-dd HH:mm:ss,SSSS\"", factory.getTimestampPattern(), is("yyyy-MM-dd HH:mm:ss,SSSS"));
    }

    @Test
    public void buildGelfAppenderShouldWorkWithValidConfiguration() {
        final GelfAppenderFactory gelf = new GelfAppenderFactory();

        final Appender<ILoggingEvent> appender = gelf.build(new LoggerContext(), APPLICATION_NAME, null, new ThresholdLevelFilterFactory(), new AsyncLoggingEventAppenderFactory());

        assertThat(appender, instanceOf(AsyncAppender.class));
    }

    @Test
    public void buildGelfAppenderReturnsNOPAppenderIfDisabled() {
        final GelfAppenderFactory gelf = new GelfAppenderFactory();
        gelf.setEnabled(false);

        final Appender<ILoggingEvent> appender = gelf.build(new LoggerContext(), APPLICATION_NAME, null, new ThresholdLevelFilterFactory(), new AsyncLoggingEventAppenderFactory());

        assertThat(appender, instanceOf(NOPAppender.class));
    }
}
