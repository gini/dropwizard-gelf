package net.gini.dropwizard.gelf.config;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Configuration class for settings related to {@link net.gini.dropwizard.gelf.bundles.GelfLoggingBundle}.
 */
public class GelfConfiguration {

    @JsonProperty
    private boolean enabled = false;

    @NotNull
    @JsonProperty
    private Level threshold = Level.ALL;

    @JsonProperty
    @NotEmpty
    private String facility = "GELF";

    @JsonProperty
    @NotEmpty
    private String host = "localhost";

    @JsonProperty
    @Min(1)
    @Max(65535)
    private int port = 12201;

    @JsonProperty
    private boolean useLoggerName = true;

    @JsonProperty
    private boolean useThreadName = true;

    @JsonProperty
    @NotEmpty
    @Pattern(regexp = "0\\.9\\.[56]")
    private String serverVersion = "0.9.6";

    @JsonProperty
    @Min(0)
    private int chunkThreshold = 1000;

    @JsonProperty
    private String messagePattern = "%m%rEx";

    @JsonProperty
    @NotNull
    private ImmutableMap<String, String> additionalFields = ImmutableMap.of();

    @JsonProperty
    private boolean includeFullMDC = false;

    public boolean isEnabled() {
        return enabled;
    }

    public Level getThreshold() {
        return threshold;
    }

    public String getFacility() {
        return facility;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isUseLoggerName() {
        return useLoggerName;
    }

    public boolean isUseThreadName() {
        return useThreadName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public int getChunkThreshold() {
        return chunkThreshold;
    }

    public String getMessagePattern() {
        return messagePattern;
    }

    public ImmutableMap<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public boolean isIncludeFullMDC() {
        return includeFullMDC;
    }
}