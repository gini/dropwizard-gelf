package net.gini.dropwizard.gelf.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Layout;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import ch.qos.logback.core.helpers.NOPAppender;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.validation.PortRange;
import me.moocar.logbackgelf.GelfAppender;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonTypeName("gelf")
public class GelfAppenderFactory extends AbstractAppenderFactory {

    @JsonProperty
    private boolean enabled = true;

    @JsonProperty
    @NotNull
    private Level threshold = Level.ALL;

    @JsonProperty
    private Optional<String> facility = Optional.absent();

    @JsonProperty
    @NotEmpty
    private String host = "localhost";

    @JsonProperty
    @PortRange(min = 1)
    private int port = 12201;

    @JsonProperty
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
    @NotNull
    private ImmutableMap<String, String> fieldTypes = ImmutableMap.of();

    @JsonProperty
    private boolean includeFullMDC = false;

    @JsonProperty
    private boolean useMarker = false;

    public Level getThreshold() {
        return threshold;
    }

    public void setThreshold(Level threshold) {
        this.threshold = threshold;
    }

    public Optional<String> getFacility() {
        return facility;
    }

    public void setFacility(Optional<String> facility) {
        this.facility = facility;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Optional<String> getHostName() {
        return hostName;
    }

    public void setHostName(Optional<String> hostName) {
        this.hostName = hostName;
    }

    public boolean isUseLoggerName() {
        return useLoggerName;
    }

    public void setUseLoggerName(boolean useLoggerName) {
        this.useLoggerName = useLoggerName;
    }

    public boolean isUseThreadName() {
        return useThreadName;
    }

    public void setUseThreadName(boolean useThreadName) {
        this.useThreadName = useThreadName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public int getChunkThreshold() {
        return chunkThreshold;
    }

    public void setChunkThreshold(int chunkThreshold) {
        this.chunkThreshold = chunkThreshold;
    }

    public String getMessagePattern() {
        return messagePattern;
    }

    public void setMessagePattern(String messagePattern) {
        this.messagePattern = messagePattern;
    }

    public String getShortMessagePattern() {
        return shortMessagePattern;
    }

    public void setShortMessagePattern(String shortMessagePattern) {
        this.shortMessagePattern = shortMessagePattern;
    }

    public ImmutableMap<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(ImmutableMap<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public ImmutableMap<String, String> getStaticFields() {
        return staticFields;
    }

    public void setStaticFields(ImmutableMap<String, String> staticFields) {
        this.staticFields = staticFields;
    }

    public ImmutableMap<String, String> getFieldTypes() {
        return fieldTypes;
    }

    public void setFieldTypes(ImmutableMap<String, String> fieldTypes) {
        this.fieldTypes = fieldTypes;
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

    public void setUseMarker(boolean useMarker) {
        this.useMarker = useMarker;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context, String applicationName, Layout<ILoggingEvent> layout) {
        checkNotNull(context);

        if (!enabled) {
            final Appender<ILoggingEvent> appender = new NOPAppender<>();
            appender.start();
            return appender;
        }

        final GelfAppender appender = new GelfAppender();

        appender.setContext(context);
        appender.setFacility(facility.or(applicationName));
        appender.setGraylog2ServerHost(host);
        appender.setGraylog2ServerPort(port);
        appender.setGraylog2ServerVersion(serverVersion);
        appender.setMessagePattern(messagePattern);
        appender.setShortMessagePattern(shortMessagePattern);
        appender.setUseLoggerName(useLoggerName);
        appender.setUseThreadName(useThreadName);
        appender.setChunkThreshold(chunkThreshold);
        appender.setAdditionalFields(additionalFields);
        appender.setStaticAdditionalFields(staticFields);
        appender.setFieldTypes(fieldTypes);
        appender.setIncludeFullMDC(includeFullMDC);
        appender.setUseMarker(useMarker);

        if(hostName.isPresent()) {
            appender.setHostName(hostName.get());
        }

        addThresholdFilter(appender, threshold);
        appender.start();

        return wrapAsync(appender);
    }
}
