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
 * Dropwizard {@link ConfiguredBundle} which configures and adds {@link GelfAppender} to Logback.
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

    /**
     * Build the {@link GelfAppender} from the supplied {@link GelfConfiguration}.
     *
     * @param configuration The configuration for the {@link GelfAppender}
     * @param context       The logger context
     * @return A fully configured and started {@link Appender}
     */
    private Appender<ILoggingEvent> buildGelfAppender(GelfConfiguration configuration, LoggerContext context) {

        final GelfAppender appender = new GelfAppender();

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

    /**
     * Initializes the service bootstrap. Does nothing for this implementation.
     *
     * @param bootstrap the service bootstrap
     */
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // Do nothing
    }

    /**
     * Get the {@link GelfConfiguration} object from the supplied {@link Configuration}.
     *
     * @param configuration The {@link Configuration} of the service
     * @return The {@link GelfConfiguration} of the service
     */
    public abstract GelfConfiguration getConfiguration(T configuration);
}
