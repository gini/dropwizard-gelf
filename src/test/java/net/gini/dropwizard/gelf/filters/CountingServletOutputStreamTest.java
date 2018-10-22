package net.gini.dropwizard.gelf.filters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CountingServletOutputStream}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CountingServletOutputStreamTest {

    @Mock
    private ServletOutputStream delegate;
    private CountingServletOutputStream outputStream;

    @Before
    public void setUp() {
        outputStream = new CountingServletOutputStream(delegate);
    }

    @Test
    public void testThatCloseDelegates() throws IOException {
        outputStream.close();

        verify(delegate).close();
    }

    @Test
    public void testThatFlushDelegates() throws IOException {
        outputStream.flush();

        verify(delegate).flush();
    }

    @Test
    public void testCountsWrittenBytes() throws IOException {
        final byte[] data = "lorem ipsum".getBytes(StandardCharsets.UTF_8);

        outputStream.write(42);
        outputStream.write(data);
        outputStream.write(data, 1, data.length - 1);

        assertThat(outputStream.getCount()).isEqualTo(2 * data.length);
    }

    @Test
    public void testIsReadyDelegates() {
        when(outputStream.isReady()).thenReturn(true);

        assertThat(outputStream.isReady()).isTrue();
    }

    @Test
    public void testThatSetWriteListenerDelegates() {
        final WriteListener writeListener = new WriteListener() {

            @Override
            public void onWritePossible() {
            }

            @Override
            public void onError(final Throwable t) {
            }
        };

        outputStream.setWriteListener(writeListener);

        verify(delegate).setWriteListener(writeListener);
    }
}
