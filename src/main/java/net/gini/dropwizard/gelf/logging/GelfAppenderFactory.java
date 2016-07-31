package net.gini.dropwizard.gelf.logging;

import biz.paluch.logging.gelf.intern.GelfMessage;
import biz.paluch.logging.gelf.logback.GelfLogbackAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.helpers.NOPAppender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import io.dropwizard.validation.PortRange;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@JsonTypeName("gelf")
public class GelfAppenderFactory extends AbstractAppenderFactory<ILoggingEvent> {

    @JsonProperty
        private boolean enabled = true;

    @JsonProperty
    @NotNull
    private Level threshold = Level.ALL;

    @JsonProperty
    private Optional<String> facility = Optional.empty();

    @JsonProperty
    @NotEmpty
    private String host = "localhost";

    @JsonProperty
    @PortRange
    private int port = 12201;

    @JsonProperty
    private Optional<String> originHost = Optional.empty();

    @JsonProperty
    @NotNull
    private ImmutableMap<String, String> additionalFields = ImmutableMap.of();

    @JsonProperty
    @NotNull
    private ImmutableMap<String, String> additionalFieldTypes = ImmutableMap.of();

    @JsonProperty
    private boolean includeFullMDC = false;

    @JsonProperty
    @NotNull
    private Collection<String> mdcFields = ImmutableList.of();

    @JsonProperty
    @NotNull
    private Collection<String> dynamicMdcFields = ImmutableList.of();

    @JsonProperty
    private boolean mdcProfiling = false;

    @JsonProperty
    private boolean extractStackTrace = false;

    @JsonProperty
    private boolean filterStackTrace = false;

    @JsonProperty
    @Min(0)
    private int maximumMessageSize = 8192;

    @JsonProperty
    @NotNull
    private String timestampPattern = "yyyy-MM-dd HH:mm:ss,SSSS";

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

    public Optional<String> getOriginHost() {
        return originHost;
    }

    public void setOriginHost(Optional<String> originHost) {
        this.originHost = originHost;
    }

    public ImmutableMap<String, String> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(ImmutableMap<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }

    public ImmutableMap<String, String> getAdditionalFieldTypes() {
        return additionalFieldTypes;
    }

    public void setAdditionalFieldTypes(ImmutableMap<String, String> additionalFieldTypes) {
        this.additionalFieldTypes = additionalFieldTypes;
    }

    public boolean isIncludeFullMDC() {
        return includeFullMDC;
    }

    public void setIncludeFullMDC(boolean includeFullMDC) {
        this.includeFullMDC = includeFullMDC;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public Collection<String> getMdcFields() {
        return mdcFields;
    }

    public void setMdcFields(Collection<String> mdcFields) {
        this.mdcFields = mdcFields;
    }

    public Collection<String> getDynamicMdcFields() {
        return dynamicMdcFields;
    }

    public void setDynamicMdcFields(Collection<String> dynamicMdcFields) {
        this.dynamicMdcFields = dynamicMdcFields;
    }

    public boolean isMdcProfiling() {
        return mdcProfiling;
    }

    public void setMdcProfiling(boolean mdcProfiling) {
        this.mdcProfiling = mdcProfiling;
    }

    public boolean isExtractStackTrace() {
        return extractStackTrace;
    }

    public void setExtractStackTrace(boolean extractStackTrace) {
        this.extractStackTrace = extractStackTrace;
    }

    public boolean isFilterStackTrace() {
        return filterStackTrace;
    }

    public void setFilterStackTrace(boolean filterStackTrace) {
        this.filterStackTrace = filterStackTrace;
    }

    public int getMaximumMessageSize() {
        return maximumMessageSize;
    }

    public void setMaximumMessageSize(int maximumMessageSize) {
        this.maximumMessageSize = maximumMessageSize;
    }

    public String getTimestampPattern() {
        return timestampPattern;
    }

    public void setTimestampPattern(String timestampPattern) {
        this.timestampPattern = timestampPattern;
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context,
                                         String applicationName,
                                         LayoutFactory<ILoggingEvent> layoutFactory,
                                         LevelFilterFactory<ILoggingEvent> levelFilterFactory,
                                         AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory) {
        if (!enabled) {
            final Appender<ILoggingEvent> appender = new NOPAppender<>();
            appender.start();
            return appender;
        }

        final GelfLogbackAppender appender = new GelfLogbackAppender();

        appender.setContext(context);
        appender.setName("dropwizard-gelf");
        appender.setFacility(facility.orElse(applicationName));
        appender.setGraylogHost(host);
        appender.setGraylogPort(port);
        appender.setVersion(GelfMessage.GELF_VERSION_1_1);
        appender.setAdditionalFields(buildFieldsSpec(additionalFields));
        appender.setAdditionalFieldTypes(buildFieldsSpec(additionalFieldTypes));
        appender.setMdcFields(buildMdcFieldsSpec(mdcFields));
        appender.setDynamicMdcFields(buildMdcFieldsSpec(dynamicMdcFields));
        appender.setIncludeFullMdc(true);
        appender.setMdcProfiling(mdcProfiling);
        appender.setExtractStackTrace(extractStackTrace);
        appender.setFilterStackTrace(filterStackTrace);
        appender.setMaximumMessageSize(maximumMessageSize);
        appender.setTimestampPattern(timestampPattern);

        if (originHost.isPresent()) {
            appender.setOriginHost(originHost.get());
        }

        appender.addFilter(levelFilterFactory.build(threshold));
        getFilterFactories().stream().forEach(f -> appender.addFilter(f.build()));
        appender.start();

        return wrapAsync(appender, asyncAppenderFactory);
    }

    private String buildMdcFieldsSpec(@NotNull Collection<String> fields) {
        return Joiner.on(',').skipNulls().join(fields);
    }

    private String buildFieldsSpec(@NotNull Map<String, String> fields) {
        return Joiner.on(',').withKeyValueSeparator("=").useForNull("null").join(fields);
    }
}
