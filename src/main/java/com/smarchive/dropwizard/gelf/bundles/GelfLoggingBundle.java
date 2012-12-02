package com.smarchive.dropwizard.gelf.bundles;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.FilterAttachable;
import com.smarchive.dropwizard.gelf.config.GelfConfiguration;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.logging.AsyncAppender;
import me.moocar.logbackgelf.GelfAppender;
import org.slf4j.LoggerFactory;

/**
 */
public abstract class GelfLoggingBundle<T extends Configuration> implements ConfiguredBundle<T> {
    /**
     * Initializes the environment.
     *
     * @param configuration the {@link Configuration} object
     * @param environment   the service's {@link Environment}
     * @throws Exception if something goes wrong
     */
    @Override
    public void run(T configuration, Environment environment) throws Exception {

        final GelfConfiguration config = getConfiguration(configuration);

        if (config.isEnabled()) {
            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.addAppender(AsyncAppender.wrap(buildGelfAppender(config, root.getLoggerContext())));
        }
    }

    private Appender<ILoggingEvent> buildGelfAppender(GelfConfiguration configuration, LoggerContext context) {

        final GelfAppender<ILoggingEvent> appender = new GelfAppender<ILoggingEvent>();

        appender.setContext(context);
        appender.setFacility(configuration.getFacility());
        appender.setGraylog2ServerHost(configuration.getHost());
        appender.setGraylog2ServerPort(configuration.getPort());
        appender.setGraylog2ServerVersion(configuration.getServerVersion());
        appender.setMessagePattern(configuration.getMessagePattern());
        appender.setUseLoggerName(configuration.isUseLoggerName());
        appender.setUseThreadName(configuration.isUseThreadName());
        appender.setChunkThreshold(configuration.getChunkThreshold());
        appender.setAdditionalFields(configuration.getAdditionalFields());
        addThresholdFilter(appender, configuration.getThreshold());
        appender.start();

        return appender;
    }

    private static void addThresholdFilter(FilterAttachable<ILoggingEvent> appender, Level threshold) {
        final ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel(threshold.toString());
        filter.start();
        appender.addFilter(filter);
    }

    /**
     * Initializes the service bootstrap.
     *
     * @param bootstrap the service bootstrap
     */
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // Do nothing
    }

    public abstract GelfConfiguration getConfiguration(T configuration);
}
