package net.gini.dropwizard.gelf.config;

import com.google.common.base.Optional;
import com.yammer.dropwizard.config.ConfigurationException;
import com.yammer.dropwizard.config.ConfigurationFactory;
import com.yammer.dropwizard.validation.Validator;
import org.junit.Test;

import java.io.IOException;
import java.util.TimeZone;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link GelfConfiguration}.
 */
public class GelfConfigurationTest {

    @Test
    public void hasValidDefaults() throws IOException, ConfigurationException {
        final GelfConfiguration config = ConfigurationFactory.forClass(GelfConfiguration.class, new Validator()).build();

        assertThat("disabled by default", config.isEnabled(), is(false));
        assertThat("request log is disabled by default", config.isRequestLogEnabled(), is(false));
        assertThat("default Graylog2 host is 'localhost'", config.getHost(), is("localhost"));
        assertThat("default Graylog2 port is 12201", config.getPort(), is(12201));
        assertThat("default hostname is absent", config.getHostName(), is(Optional.<String>absent()));
        assertThat("default server version is 0.9.6", config.getServerVersion(), is("0.9.6"));
        assertThat("default facility is GELF", config.getFacility(), is("GELF"));
        assertThat("default chunk threshold is 1000", config.getChunkThreshold(), is(1000));
        assertThat("default message pattern is %m%rEx", config.getMessagePattern(), is("%m%rEx"));
        assertThat("default short message pattern is unset", config.getShortMessagePattern(), nullValue());
        assertThat("default additional fields are empty", config.getAdditionalFields().isEmpty(), is(true));
        assertThat("default static additional fields are empty", config.getStaticFields().isEmpty(), is(true));
        assertThat("default timezone is UTC", config.getTimeZone(), is(TimeZone.getTimeZone("UTC")));
    }
}
