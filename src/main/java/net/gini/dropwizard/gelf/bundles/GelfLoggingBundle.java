package net.gini.dropwizard.gelf.bundles;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.logging.AsyncAppender;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import me.moocar.logbackgelf.GelfAppender;
import net.gini.dropwizard.gelf.config.GelfConfiguration;
import net.gini.dropwizard.gelf.filters.GelfLoggingFilter;
import net.gini.dropwizard.gelf.logging.LogbackFactory;
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

        final GelfConfiguration gelf = getConfiguration(configuration);

        if (gelf.isEnabled()) {
            final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.addAppender(new AsyncAppender(LogbackFactory.buildGelfAppender(gelf, root.getLoggerContext()), 128, Duration.milliseconds(100), false));

            if (gelf.isRequestLogEnabled()) {
                final Logger logger = (Logger) LoggerFactory.getLogger(GelfLoggingFilter.class);
                logger.setAdditive(false);

                final LoggerContext context = logger.getLoggerContext();
                final GelfAppender appender = LogbackFactory.buildGelfAppender(gelf, context, gelf.getFacility() + "-requests");
                logger.addAppender(appender);

                environment.servlets().addFilter("gelf", new GelfLoggingFilter());
            }
        }
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
