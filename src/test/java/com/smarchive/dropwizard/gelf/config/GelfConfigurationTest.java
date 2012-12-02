package com.smarchive.dropwizard.gelf.config;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link GelfConfiguration}.
 */
public class GelfConfigurationTest {

    @Test
    public void hasValidDefaults() {
        final GelfConfiguration config = new GelfConfiguration();

        assertThat("default hostname is localhost", config.getHost(), is("localhost"));
        assertThat("default port is Graylog2 default", config.getPort(), is(12201));
        assertThat("default server version is 0.9.6", config.getServerVersion(), is("0.9.6"));
        assertThat("default facility is GELF", config.getFacility(), is("GELF"));
        assertThat("default chunk threshold is 1000", config.getChunkThreshold(), is(1000));
        assertThat("default additional fields are empty", config.getAdditionalFields().isEmpty(), is(true));
    }
}
