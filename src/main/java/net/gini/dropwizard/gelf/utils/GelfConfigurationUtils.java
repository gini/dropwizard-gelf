package net.gini.dropwizard.gelf.utils;

import com.google.common.base.Optional;
import io.dropwizard.Configuration;
import io.dropwizard.logging.AppenderFactory;
import net.gini.dropwizard.gelf.logging.GelfAppenderFactory;

public class GelfConfigurationUtils {

    public static Optional<GelfAppenderFactory> getGelfAppenderFactory(Configuration configuration) {
        for (AppenderFactory appenderFactory : configuration.getLoggingFactory().getAppenders()) {
            if (appenderFactory instanceof GelfAppenderFactory) {
                return Optional.of((GelfAppenderFactory) appenderFactory);
            }
        }
        return Optional.absent();
    }
}
