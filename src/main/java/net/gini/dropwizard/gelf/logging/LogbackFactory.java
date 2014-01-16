package net.gini.dropwizard.gelf.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.FilterAttachable;
import me.moocar.logbackgelf.GelfAppender;
import net.gini.dropwizard.gelf.config.GelfConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory class to create valid Logback {@link Appender} instances.
 */
public final class LogbackFactory {
    private LogbackFactory() { /* singleton */ }

    /**
     * Build a {@link GelfAppender} from the supplied {@link GelfConfiguration}.
     *
     * @param gelf     The configuration for the {@link GelfAppender}
     * @param context  The logger context
     * @param facility The name to use as log facility
     * @return A fully configured and started {@link Appender}
     */
    public static GelfAppender buildGelfAppender(GelfConfiguration gelf, LoggerContext context, String facility) {
        checkNotNull(context);
        checkNotNull(facility);

        final GelfAppender appender = new GelfAppender();

        appender.setContext(context);
        appender.setFacility(facility);
        appender.setGraylog2ServerHost(gelf.getHost());
        appender.setGraylog2ServerPort(gelf.getPort());
        appender.setGraylog2ServerVersion(gelf.getServerVersion());
        appender.setHostName(gelf.getHostName());
        appender.setMessagePattern(gelf.getMessagePattern());
        appender.setShortMessagePattern(gelf.getShortMessagePattern());
        appender.setUseLoggerName(gelf.isUseLoggerName());
        appender.setUseThreadName(gelf.isUseThreadName());
        appender.setChunkThreshold(gelf.getChunkThreshold());
        appender.setAdditionalFields(gelf.getAdditionalFields());
        appender.setStaticAdditionalFields(gelf.getStaticFields());
        appender.setIncludeFullMDC(gelf.isIncludeFullMDC());
        appender.setUseMarker(gelf.isUseMarker());

        addThresholdFilter(appender, gelf.getThreshold());
        appender.start();

        return appender;
    }

    /**
     * Build a {@link GelfAppender} from the supplied {@link GelfConfiguration}.
     *
     * @param gelf    The configuration for the {@link GelfAppender}
     * @param context The logger context
     * @return A fully configured and started {@link Appender}
     */
    public static GelfAppender buildGelfAppender(GelfConfiguration gelf, LoggerContext context) {
        return buildGelfAppender(gelf, context, gelf.getFacility());
    }

    /**
     * Add a {@link ThresholdFilter} to the supplied appender.
     *
     * @param appender  The appender to add the filter to
     * @param threshold The filter threshold
     */
    private static void addThresholdFilter(FilterAttachable<ILoggingEvent> appender, Level threshold) {
        final ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel(threshold.toString());
        filter.start();
        appender.addFilter(filter);
    }
}
