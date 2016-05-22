package net.gini.dropwizard.gelf.logging;

import com.google.common.base.Optional;

import org.junit.Test;

import java.io.IOException;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.helpers.NOPAppender;
import io.dropwizard.configuration.ConfigurationException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class GelfAppenderFactoryTest {

    private static final String APPLICATION_NAME = "applicationName";

    @Test
    public void hasValidDefaults() throws IOException, ConfigurationException {
        final GelfAppenderFactory factory = new GelfAppenderFactory();

        assertThat("default Graylog2 host is 'localhost'", factory.getHost(), is("localhost"));
        assertThat("default Graylog2 port is 12201", factory.getPort(), is(12201));
        assertThat("default hostname is absent", factory.getHostName(), is(Optional.<String>absent()));
        assertThat("default server version is 0.9.6", factory.getServerVersion(), is("0.9.6"));
        assertThat("default facility is absent", factory.getFacility().isPresent(), is(false));
        assertThat("default chunk threshold is 1000", factory.getChunkThreshold(), is(1000));
        assertThat("default message pattern is %m%rEx", factory.getMessagePattern(), is("%m%rEx"));
        assertThat("default short message pattern is unset", factory.getShortMessagePattern(), nullValue());
        assertThat("default additional fields are empty", factory.getAdditionalFields().isEmpty(), is(true));
        assertThat("default static additional fields are empty", factory.getStaticFields().isEmpty(), is(true));
        assertThat("default field types are empty", factory.getFieldTypes().isEmpty(), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void buildGelfAppenderShouldFailWithNullContext() {
        new GelfAppenderFactory().build(null, "", null);
    }

    @Test
    public void buildGelfAppenderShouldWorkWithValidConfiguration() {
        final GelfAppenderFactory gelf = new GelfAppenderFactory();

        final Appender<ILoggingEvent> appender = gelf.build(new LoggerContext(), APPLICATION_NAME, null);

        assertThat(appender, instanceOf(AsyncAppender.class));
    }

    @Test
    public void buildGelfAppenderReturnsNOPAppenderIfDisabled() {
        final GelfAppenderFactory gelf = new GelfAppenderFactory();
        gelf.setEnabled(false);

        final Appender<ILoggingEvent> appender = gelf.build(new LoggerContext(), APPLICATION_NAME, null);

        assertThat(appender, instanceOf(NOPAppender.class));
    }
}
