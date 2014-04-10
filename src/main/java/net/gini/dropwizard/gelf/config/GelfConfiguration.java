package net.gini.dropwizard.gelf.config;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.validation.PortRange;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.TimeZone;

/**
 * Configuration class for settings related to {@link net.gini.dropwizard.gelf.bundles.GelfLoggingBundle}.
 */
public class GelfConfiguration {
    static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @JsonProperty
    private boolean enabled = false;

    @JsonProperty
    private boolean requestLogEnabled = false;

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
    @PortRange(min = 1)
    private int port = 12201;

    @JsonProperty
    @NotNull
    private Optional<String> hostName = Optional.absent();

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
    private String shortMessagePattern = null;

    @JsonProperty
    @NotNull
    private ImmutableMap<String, String> additionalFields = ImmutableMap.of();

    @JsonProperty
    @NotNull
    private ImmutableMap<String, String> staticFields = ImmutableMap.of();

    @JsonProperty
    private boolean includeFullMDC = false;

    @JsonProperty
    private boolean useMarker = false;

    @JsonProperty
    @NotNull
    private TimeZone timeZone = UTC;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequestLogEnabled() {
        return requestLogEnabled;
    }

    public Level getThreshold() {
        return threshold;
    }

    public void setThreshold(Level threshold) {
        this.threshold = threshold;
    }

    public String getFacility() {
        return facility;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Optional<String> getHostName() {
        return hostName;
    }

    public void setHostName(final Optional<String> hostName)
    {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public String getShortMessagePattern() {
        return shortMessagePattern;
    }

    public ImmutableMap<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public ImmutableMap<String, String> getStaticFields() {
        return staticFields;
    }

    public boolean isIncludeFullMDC() {
        return includeFullMDC;
    }

    public void setIncludeFullMDC(boolean includeFullMDC) {
        this.includeFullMDC = includeFullMDC;
    }

    public boolean isUseMarker() {
        return useMarker;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }
}