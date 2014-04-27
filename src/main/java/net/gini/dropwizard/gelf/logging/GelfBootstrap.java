package net.gini.dropwizard.gelf.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Optional;
import org.slf4j.LoggerFactory;

/**
 * A class adding a configured {@link me.moocar.logbackgelf.GelfAppender} to the root logger.
 */
public final class GelfBootstrap {
    private GelfBootstrap() { /* No instance methods */ }

    /**
     * Bootstrap the SLF4J root logger with a configured {@link me.moocar.logbackgelf.GelfAppender}.
     *
     * @param name            The facility to use in the GELF messages
     * @param host            The host of the Graylog2 server
     * @param port            The port of the Graylog2 server
     * @param cleanRootLogger If true, detach and stop all other appenders from the root logger
     */
    public static void bootstrap(final String name, String host, int port, boolean cleanRootLogger) {
        bootstrap(name, host, port, Optional.<String>absent(), cleanRootLogger);
    }

    /**
     * Bootstrap the SLF4J root logger with a configured {@link me.moocar.logbackgelf.GelfAppender}.
     *
     * @param name            The facility to use in the GELF messages
     * @param host            The host of the Graylog2 server
     * @param port            The port of the Graylog2 server
     * @param hostName        The (local) hostname used in GELF messages. Defaults to the local hostname.
     * @param cleanRootLogger If true, detach and stop all other appenders from the root logger
     */
    public static void bootstrap(final String name, String host, int port, Optional<String> hostName, boolean cleanRootLogger) {
        // initially configure for WARN+ GELF logging
        final GelfAppenderFactory gelf = new GelfAppenderFactory();
        gelf.setIncludeFullMDC(true);
        gelf.setThreshold(Level.WARN);
        gelf.setHost(host);
        gelf.setPort(port);
        gelf.setHostName(hostName);

        final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        if (cleanRootLogger) {
            root.detachAndStopAllAppenders();
        }

        root.addAppender(gelf.build(root.getLoggerContext(), name, null));
    }
}
