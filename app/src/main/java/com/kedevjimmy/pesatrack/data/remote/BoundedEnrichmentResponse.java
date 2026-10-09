package com.kedevjimmy.pesatrack.data.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/** Byte and elapsed-time limits for optional entity enrichment responses. */
public final class BoundedEnrichmentResponse {
    private BoundedEnrichmentResponse() {}

    public static String read(InputStream input, int maxBytes, long deadlineNanos) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while (true) {
            requireBeforeDeadline(deadlineNanos);
            // Read at most one byte beyond the cap to detect excess without collecting it.
            int count = input.read(buffer, 0, Math.min(buffer.length, maxBytes - output.size() + 1));
            requireBeforeDeadline(deadlineNanos);
            if (count == -1) break;
            if (count > maxBytes - output.size()) throw new IOException("Enrichment response is too large");
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    public static void requireBeforeDeadline(long deadlineNanos) throws SocketTimeoutException {
        if (System.nanoTime() - deadlineNanos >= 0) throw new SocketTimeoutException("Enrichment deadline exceeded");
    }
}
