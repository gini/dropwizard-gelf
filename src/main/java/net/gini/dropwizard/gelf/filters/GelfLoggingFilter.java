package net.gini.dropwizard.gelf.filters;

import com.google.common.base.Stopwatch;
import com.google.common.io.CountingOutputStream;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        final Stopwatch stopwatch = new Stopwatch();

        // It's quite safe to assume that we only receive HTTP requests
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        String address = httpRequest.getHeader(HttpHeaders.X_FORWARDED_FOR);
        if (address == null) {
            address = request.getRemoteAddr();
        }

        MDC.put("remoteAddress", address);
        MDC.put("httpMethod", httpRequest.getMethod());
        MDC.put("protocol", httpRequest.getProtocol());
        MDC.put("requestUri", httpRequest.getRequestURI());
        MDC.put("requestLength", String.valueOf(httpRequest.getContentLength()));
        MDC.put("requestContentType", httpRequest.getContentType());
        MDC.put("requestEncoding", httpRequest.getCharacterEncoding());

        final String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null) {
            MDC.put("userAgent", userAgent);
        }

        final String authType = httpRequest.getAuthType();
        if (authType != null) {
            MDC.put("requestAuth", authType);
            MDC.put("userPrincipal", httpRequest.getUserPrincipal().getName());
        }

        final CountingHttpServletResponseWrapper responseWrapper = new CountingHttpServletResponseWrapper(httpResponse);

        stopwatch.start();
        chain.doFilter(request, responseWrapper);
        stopwatch.stop();

        MDC.put("responseStatus", String.valueOf(httpResponse.getStatus()));
        MDC.put("responseContentType", httpResponse.getContentType());
        MDC.put("responseEncoding", httpResponse.getCharacterEncoding());
        MDC.put("responseTimeNanos", String.valueOf(stopwatch.elapsed(TimeUnit.NANOSECONDS)));
        MDC.put("responseLength", String.valueOf(responseWrapper.getCount()));

        LOG.info("{} {} {}", httpRequest.getMethod(), httpRequest.getRequestURI(), httpRequest.getProtocol());

        // This should be safe since the request has been processed completely
        MDC.clear();
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
}
