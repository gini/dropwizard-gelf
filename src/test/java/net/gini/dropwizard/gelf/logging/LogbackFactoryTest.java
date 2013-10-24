package net.gini.dropwizard.gelf.logging;

import ch.qos.logback.classic.LoggerContext;
import me.moocar.logbackgelf.GelfAppender;
import net.gini.dropwizard.gelf.config.GelfConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link LogbackFactory}.
 */
public class LogbackFactoryTest {

    @Test(expected = NullPointerException.class)
    public void buildGelfAppenderShouldFailWithNullConfiguration() {
        LogbackFactory.buildGelfAppender(null, new LoggerContext(), "");
    }

    @Test(expected = NullPointerException.class)
    public void buildGelfAppenderShouldFailWithNullContext() {
        LogbackFactory.buildGelfAppender(new GelfConfiguration(), null, "");
    }

    @Test(expected = NullPointerException.class)
    public void buildGelfAppenderShouldFailWithNullFacility() {
        LogbackFactory.buildGelfAppender(new GelfConfiguration(), new LoggerContext(), null);
    }

    @Test
    public void buildGelfAppenderShouldWorkWithValidConfiguration() {
        final GelfConfiguration gelf = new GelfConfiguration();
        final String facility = "facility";

        GelfAppender appender = LogbackFactory.buildGelfAppender(gelf, new LoggerContext(), facility);

        assertEquals(facility, appender.getFacility());
        assertEquals(gelf.getHost(), appender.getGraylog2ServerHost());
        assertEquals(gelf.getPort(), appender.getGraylog2ServerPort());
        assertEquals(gelf.getMessagePattern(), appender.getMessagePattern());
    }
}
