package net.gini.dropwizard.gelf.filters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.io.CountingOutputStream;
import com.google.common.net.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * A {@link Filter} which logs requests and adds some data about it to the logger's {@link MDC}.
 */
public class GelfLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(GelfLoggingFilter.class);

    /**
     * Called by the web container to indicate to a filter that it is
     * being placed into service.
     * <p/>
     * <p>The servlet container calls the init
     * method exactly once after instantiating the filter. The init
     * method must complete successfully before the filter is asked to do any
     * filtering work.
     * <p/>
     * <p>The web container cannot place the filter into service if the init
     * method either
     * <ol>
     * <li>Throws a ServletException
     * <li>Does not return within a time period defined by the web container
     * </ol>
     *
     * @param filterConfig the {@link FilterChain} for this {@link Filter}
     * @throws ServletException if something goes wrong
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Do nothing
    }

    /**
     * The <code>doFilter</code> method of the Filter is called by the
     * container each time a request/response pair is passed through the
     * chain due to a client request for a resource at the end of the chain.
     * The FilterChain passed in to this method allows the Filter to pass
     * on the request and response to the next entity in the chain.
     * <p/>
     * <p>A typical implementation of this method would follow the following
     * pattern:
     * <ol>
     * <li>Examine the request
     * <li>Optionally wrap the request object with a custom implementation to
     * filter content or headers for input filtering
     * <li>Optionally wrap the response object with a custom implementation to
     * filter content or headers for output filtering
     * <li>
     * <ul>
     * <li><strong>Either</strong> invoke the next entity in the chain
     * using the FilterChain object
     * (<code>chain.doFilter()</code>),
     * <li><strong>or</strong> not pass on the request/response pair to
     * the next entity in the filter chain to
     * block the request processing
     * </ul>
     * <li>Directly set headers on the response after invocation of the
     * next entity in the filter chain.
     * </ol>
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        // It's quite safe to assume that we only receive HTTP requests
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final StringBuilder buf = new StringBuilder(256);

        final Optional<String> address = Optional.fromNullable(httpRequest.getHeader(HttpHeaders.X_FORWARDED_FOR));
        final String clientAddress = address.or(request.getRemoteAddr());

        buf.append(clientAddress);
        buf.append(" - ");

        final String authType = httpRequest.getAuthType();
        if (authType != null) {
            buf.append(httpRequest.getUserPrincipal().getName());
        } else {
            buf.append("-");
        }
        buf.append(" \"");
        buf.append(httpRequest.getMethod());
        buf.append(' ');
        buf.append(httpRequest.getRequestURI());
        buf.append(' ');
        buf.append(request.getProtocol());
        buf.append("\" ");

        final CountingHttpServletResponseWrapper responseWrapper = new CountingHttpServletResponseWrapper(httpResponse);

        final Stopwatch stopwatch = Stopwatch.createUnstarted();
        stopwatch.start();

        try {
            chain.doFilter(request, responseWrapper);
        } finally {
            if (request.isAsyncStarted()) {
                final AsyncListener listener =
                        new LoggingAsyncListener(buf, stopwatch, authType, clientAddress, httpRequest,
                                                 responseWrapper);
                request.getAsyncContext().addListener(listener);
            } else {
                logRequest(buf, stopwatch, authType, clientAddress, httpRequest, responseWrapper);
            }
        }
    }

    private static void logRequest(final StringBuilder buf, final Stopwatch stopwatch, final String authType,
                                   final String clientAddress,
                                   final HttpServletRequest httpRequest,
                                   final CountingHttpServletResponseWrapper responseWrapper) {
        stopwatch.stop();

        buf.append(responseWrapper.getStatus());
        buf.append(" ");
        buf.append(responseWrapper.getCount());

        final String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null) {
            MDC.put(AdditionalKeys.USER_AGENT, userAgent);
        }

        if (authType != null) {
            MDC.put(AdditionalKeys.REQ_AUTH, authType);
            MDC.put(AdditionalKeys.PRINCIPAL, httpRequest.getUserPrincipal().getName());
        }

        MDC.put(AdditionalKeys.REMOTE_ADDRESS, clientAddress);
        MDC.put(AdditionalKeys.HTTP_METHOD, httpRequest.getMethod());
        MDC.put(AdditionalKeys.PROTOCOL, httpRequest.getProtocol());
        MDC.put(AdditionalKeys.REQ_URI, httpRequest.getRequestURI());
        MDC.put(AdditionalKeys.REQ_LENGTH, String.valueOf(httpRequest.getContentLength()));
        MDC.put(AdditionalKeys.REQ_CONTENT_TYPE, httpRequest.getContentType());
        MDC.put(AdditionalKeys.REQ_ENCODING, httpRequest.getCharacterEncoding());
        MDC.put(AdditionalKeys.RESP_STATUS, String.valueOf(responseWrapper.getStatus()));
        MDC.put(AdditionalKeys.RESP_CONTENT_TYPE, responseWrapper.getContentType());
        MDC.put(AdditionalKeys.RESP_ENCODING, responseWrapper.getCharacterEncoding());
        MDC.put(AdditionalKeys.RESP_TIME, String.valueOf(stopwatch.elapsed(TimeUnit.NANOSECONDS)));
        MDC.put(AdditionalKeys.RESP_LENGTH, String.valueOf(responseWrapper.getCount()));

        LOG.info(buf.toString());

        clearMDC();
    }

    /**
     * Called by the web container to indicate to a filter that it is being
     * taken out of service.
     * <p/>
     * <p>This method is only called once all threads within the filter's
     * doFilter method have exited or after a timeout period has passed.
     * After the web container calls this method, it will not call the
     * doFilter method again on this instance of the filter.
     * <p/>
     * <p>This method gives the filter an opportunity to clean up any
     * resources that are being held (for example, memory, file handles,
     * threads) and make sure that any persistent state is synchronized
     * with the filter's current state in memory.
     */
    @Override
    public void destroy() {
        // Do nothing
    }

    private static void clearMDC() {
        MDC.remove(AdditionalKeys.USER_AGENT);
        MDC.remove(AdditionalKeys.REQ_AUTH);
        MDC.remove(AdditionalKeys.PRINCIPAL);
        MDC.remove(AdditionalKeys.REMOTE_ADDRESS);
        MDC.remove(AdditionalKeys.HTTP_METHOD);
        MDC.remove(AdditionalKeys.PROTOCOL);
        MDC.remove(AdditionalKeys.REQ_URI);
        MDC.remove(AdditionalKeys.REQ_LENGTH);
        MDC.remove(AdditionalKeys.REQ_CONTENT_TYPE);
        MDC.remove(AdditionalKeys.REQ_ENCODING);
        MDC.remove(AdditionalKeys.RESP_STATUS);
        MDC.remove(AdditionalKeys.RESP_CONTENT_TYPE);
        MDC.remove(AdditionalKeys.RESP_ENCODING);
        MDC.remove(AdditionalKeys.RESP_TIME);
        MDC.remove(AdditionalKeys.RESP_LENGTH);
    }

    /**
     * An implementation of {@link ServletOutputStream} which counts the bytes being
     * written using a {@link CountingOutputStream}.
     */
    private static final class CountingServletOutputStream extends ServletOutputStream {

        private final CountingOutputStream outputStream;

        private CountingServletOutputStream(ServletOutputStream servletOutputStream) {
            this.outputStream = new CountingOutputStream(servletOutputStream);
        }

        /**
         * Writes the specified byte to this output stream. The general
         * contract for <code>write</code> is that one byte is written
         * to the output stream. The byte to be written is the eight
         * low-order bits of the argument <code>b</code>. The 24
         * high-order bits of <code>b</code> are ignored.
         * <p/>
         * Subclasses of <code>OutputStream</code> must provide an
         * implementation for this method.
         *
         * @param b the <code>byte</code>.
         * @throws java.io.IOException if an I/O error occurs. In particular,
         *                             an <code>IOException</code> may be thrown if the
         *                             output stream has been closed.
         */
        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        public long getCount() {
            return outputStream.getCount();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // NOP
        }
    }

    /**
     * An implementation of {@link HttpServletResponseWrapper} which counts the bytes being written as the response
     * body using a {@link CountingServletOutputStream}.
     */
    private static final class CountingHttpServletResponseWrapper extends HttpServletResponseWrapper {
        private CountingServletOutputStream outputStream;

        private CountingHttpServletResponseWrapper(HttpServletResponse response) throws IOException {
            super(response);
        }

        /**
         * The default behavior of this method is to return getOutputStream()
         * on the wrapped response object.
         */
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStream == null) {
                outputStream = new CountingServletOutputStream(getResponse().getOutputStream());
            }
            return outputStream;
        }

        /**
         * Get the number of bytes written to the response output stream.
         *
         * @return the number of bytes written to the response output stream
         */
        public long getCount() {
            return outputStream == null ? 0L : outputStream.getCount();
        }

        /**
         * The default behavior of this method is to call resetBuffer() on the wrapped response object.
         *
         * @see javax.servlet.http.HttpServletResponseWrapper#resetBuffer()
         */
        @Override
        public void resetBuffer() {
            super.resetBuffer();
            outputStream = null;
        }

        /**
         * The default behavior of this method is to call reset() on the wrapped response object.
         *
         * @see javax.servlet.http.HttpServletResponseWrapper#reset()
         */
        @Override
        public void reset() {
            super.reset();
            outputStream = null;
        }
    }

    private static class LoggingAsyncListener implements AsyncListener {

        private final StringBuilder buf;
        private final Stopwatch stopwatch;
        private final String authType;
        private final String clientAddress;
        private final HttpServletRequest httpRequest;
        private final CountingHttpServletResponseWrapper responseWrapper;

        public LoggingAsyncListener(final StringBuilder buf, final Stopwatch stopwatch, final String authType,
                                    final String clientAddress,
                                    final HttpServletRequest httpRequest,
                                    final CountingHttpServletResponseWrapper responseWrapper) {
            this.buf = buf;
            this.stopwatch = stopwatch;
            this.authType = authType;
            this.clientAddress = clientAddress;
            this.httpRequest = httpRequest;
            this.responseWrapper = responseWrapper;
        }

        @Override
        public void onComplete(final AsyncEvent event) throws IOException {
            logRequest(buf, stopwatch, authType, clientAddress, httpRequest, responseWrapper);
        }

        @Override
        public void onTimeout(final AsyncEvent event) throws IOException {
            // Intentionally empty
        }

        @Override
        public void onError(final AsyncEvent event) throws IOException {
            // Intentionally empty
        }

        @Override
        public void onStartAsync(final AsyncEvent event) throws IOException {
            // Intentionally empty
        }
    }

    @VisibleForTesting
    static final class AdditionalKeys {

        public static final String USER_AGENT = "userAgent";
        public static final String REQ_AUTH = "requestAuth";
        public static final String PRINCIPAL = "userPrincipal";
        public static final String REMOTE_ADDRESS = "remoteAddress";
        public static final String HTTP_METHOD = "httpMethod";
        public static final String PROTOCOL = "protocol";
        public static final String REQ_URI = "requestUri";
        public static final String REQ_LENGTH = "requestLength";
        public static final String REQ_CONTENT_TYPE = "requestContentType";
        public static final String REQ_ENCODING = "requestEncoding";
        public static final String RESP_STATUS = "responseStatus";
        public static final String RESP_CONTENT_TYPE = "responseContentType";
        public static final String RESP_ENCODING = "responseEncoding";
        public static final String RESP_TIME = "responseTimeNanos";
        public static final String RESP_LENGTH = "responseLength";

        private AdditionalKeys() {
        }
    }
}
