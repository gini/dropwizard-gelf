package net.gini.dropwizard.gelf.filters;

import com.google.common.io.CountingOutputStream;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * An implementation of {@link ServletOutputStream} which counts the bytes being
 * written using a {@link CountingOutputStream}.
 */
final class CountingServletOutputStream extends ServletOutputStream {

    /**
     * The underlying stream that is wrapped by CountingOutputStream.
     */
    private final ServletOutputStream underlyingStream;
    private final CountingOutputStream outputStream;

    CountingServletOutputStream(ServletOutputStream servletOutputStream) {
        this.underlyingStream = servletOutputStream;
        this.outputStream = new CountingOutputStream(servletOutputStream);
    }

    /**
     * Writes the specified byte to this output stream. The general
     * contract for <code>write</code> is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument <code>b</code>. The 24
     * high-order bits of <code>b</code> are ignored.
     * <p>
     * Subclasses of <code>OutputStream</code> must provide an
     * implementation for this method.
     * </p>
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

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        underlyingStream.flush();
    }

    public long getCount() {
        return outputStream.getCount();
    }

    @Override
    public boolean isReady() {
        return underlyingStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        underlyingStream.setWriteListener(writeListener);
    }
}
