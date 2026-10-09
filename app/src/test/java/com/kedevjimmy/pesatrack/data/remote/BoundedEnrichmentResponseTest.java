package com.kedevjimmy.pesatrack.data.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class BoundedEnrichmentResponseTest {
    @Test public void exactByteLimitAcceptsUtf8() throws Exception {
        byte[] bytes = "éé".getBytes(StandardCharsets.UTF_8);
        assertEquals("éé", BoundedEnrichmentResponse.read(new ByteArrayInputStream(bytes), 4, future()));
    }
    @Test(expected = IOException.class) public void rejectsOversizedStreamWithoutContentLength() throws Exception {
        BoundedEnrichmentResponse.read(new ByteArrayInputStream(new byte[65537]), 65536, future());
    }
    @Test(expected = java.net.SocketTimeoutException.class) public void rejectsAlreadyExpiredDeadline() throws Exception {
        BoundedEnrichmentResponse.read(new ByteArrayInputStream(new byte[0]), 65536, System.nanoTime() - 1);
    }
    @Test(expected = java.net.SocketTimeoutException.class) public void rejectsSlowTricklingBody() throws Exception {
        InputStream delayed = new ByteArrayInputStream(new byte[] {65}) {
            @Override public synchronized int read(byte[] b, int off, int len) {
                try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return super.read(b, off, len);
            }
        };
        BoundedEnrichmentResponse.read(delayed, 65536, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(10));
    }
    private static long future() { return System.nanoTime() + TimeUnit.SECONDS.toNanos(1); }
}
