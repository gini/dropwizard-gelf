package net.gini.dropwizard.gelf.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Simple factory for a {@link Thread.UncaughtExceptionHandler} which logs the last uncaught exception with a SLF4J {@link
 * Logger} on ERROR level.
 */
public final class UncaughtExceptionHandlers {

    private UncaughtExceptionHandlers() {
    }

    /**
     * Returns a builder for an exception handler that bootstraps a GELF log appender, logs the uncaught exception
     * and then exits the system. This is particularly useful for the main thread, which may start up other,
     * non-daemon threads, but fail to fully initialize the application successfully.
     * <br>
     * Example usage:
     * <pre>public static void main(String[] args) {
     *   Thread.currentThread().setUncaughtExceptionHandler(
     *       UncaughtExceptionHandlers.loggingSystemExitBuilder("some-service", "log.example.com").build());
     *   ...
     * </pre>
     *
     * @param facility The facility to use in the GELF messages
     * @param host     The host of the Graylog server
     * @return builder object for building the exception handler
     */
    public static LoggingSystemExitBuilder loggingSystemExitBuilder(final String facility, final String host) {
        return new LoggingSystemExitBuilder(facility, host);
    }

    /**
     * Returns an exception handler that logs the uncaught exception to {@code System.err} and then exits the system.
     * This is particularly useful for the main thread, which may start up other, non-daemon threads, but fail to fully
     * initialize the application successfully. <br> Example usage:
     * <pre>public static void main(String[] args) {
     *   Thread.currentThread().setUncaughtExceptionHandler(
     *       UncaughtExceptionHandlers.systemExit());
     *   ...
     * </pre>
     *
     * @return exception handler
     */
    public static Thread.UncaughtExceptionHandler systemExit() {
        return new Exiter(Runtime.getRuntime());
    }

    public static final class LoggingSystemExitBuilder {

        private String facility;
        private String host;
        private int port = 12201;
        private boolean cleanRootLogger = false;
        private boolean logToStderr = true;

        LoggingSystemExitBuilder(final String facility, final String host) {
            this.facility = requireNonNull(facility);
            this.host = requireNonNull(host);
        }

        /**
         * Sets the port of the Graylog server.
         *
         * @param port The port of the Graylog server.
         * @return {@link LoggingSystemExitBuilder} instance
         */
        public LoggingSystemExitBuilder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets whether all existing appenders should be detached from the root logger.
         *
         * @param cleanRootLogger If true, detach and stop all other appenders from the root logger
         * @return {@link LoggingSystemExitBuilder} instance
         */
        public LoggingSystemExitBuilder cleanRootLogger(final boolean cleanRootLogger) {
            this.cleanRootLogger = cleanRootLogger;
            return this;
        }

        /**
         * Sets whether the stacktrace of the uncaught exception should be printed to {@code System.err}.
         *
         * @param logToStderr If true, print the stacktrace to {@code System.err}
         * @return {@link LoggingSystemExitBuilder} instance
         */
        public LoggingSystemExitBuilder logToStderr(final boolean logToStderr) {
            this.logToStderr = logToStderr;
            return this;
        }

        public Thread.UncaughtExceptionHandler build() {
            return new LoggingExiter(Runtime.getRuntime(), facility, host, port, cleanRootLogger, logToStderr);
        }
    }

    /**
     * Exception handler that exits the system. Bootstrap a GELF log appender and logs the uncaught exception.
     * Optionally prints the stacktrace to {@code System.err} as well.
     */
    private static final class LoggingExiter implements Thread.UncaughtExceptionHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(Exiter.class);

        private final Runtime runtime;
        private final String name;
        private final String host;
        private final int port;
        private final boolean cleanRootLogger;
        private final boolean logToStderr;

        private LoggingExiter(final Runtime runtime, final String name, final String host, final int port,
                              final boolean cleanRootLogger, final boolean logToStderr) {
            this.runtime = runtime;
            this.name = name;
            this.host = host;
            this.port = port;
            this.cleanRootLogger = cleanRootLogger;
            this.logToStderr = logToStderr;
        }

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            // Re-initialize logging system (as dropwizard likely has stopped it)
            GelfBootstrap.bootstrap(name, host, port, cleanRootLogger);
            // Log the exception
            final String msg = format("Caught an exception in %s.  Shutting down", t);
            LOGGER.error(msg, e);
            if (logToStderr) {
                System.err.print(msg);
                System.err.print("! ");
                e.printStackTrace(System.err);
            }
            // Stop appenders (should flush all existing messages)
            getRootLogger().detachAndStopAllAppenders();
            // Terminate the JVM
            runtime.exit(1);
        }

        private ch.qos.logback.classic.Logger getRootLogger() {
            return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        }
    }

    private static final class Exiter implements Thread.UncaughtExceptionHandler {

        private final Runtime runtime;

        private Exiter(final Runtime runtime) {
            this.runtime = requireNonNull(runtime);
        }

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            System.err.print(format("Caught an exception in %s.  Shutting down! ", t));
            e.printStackTrace(System.err);
            runtime.exit(1);
        }
    }
}