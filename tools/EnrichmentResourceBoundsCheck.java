import com.kedevjimmy.pesatrack.data.remote.BoundedEnrichmentResponse;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class EnrichmentResourceBoundsCheck {
    public static void main(String[] args) throws Exception {
        byte[] utf8 = "éé".getBytes(StandardCharsets.UTF_8);
        if (!"éé".equals(BoundedEnrichmentResponse.read(new ByteArrayInputStream(utf8), 4, future()))) throw new AssertionError("exact UTF8 limit");
        expect(IOException.class, () -> BoundedEnrichmentResponse.read(new ByteArrayInputStream(new byte[65537]), 65536, future()));
        expect(SocketTimeoutException.class, () -> BoundedEnrichmentResponse.read(new ByteArrayInputStream(new byte[0]), 65536, System.nanoTime() - 1));
        InputStream slow = new ByteArrayInputStream(new byte[] {65}) {
            public synchronized int read(byte[] b, int off, int len) {
                try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return super.read(b, off, len);
            }
        };
        expect(SocketTimeoutException.class, () -> BoundedEnrichmentResponse.read(slow, 65536, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(10)));
        System.out.println("Android enrichment resource reader checks passed (4 cases).");
    }
    interface Action { void run() throws Exception; }
    static void expect(Class<? extends Exception> type, Action action) throws Exception {
        try { action.run(); } catch (Exception e) { if (type.isInstance(e)) return; throw e; }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    static long future() { return System.nanoTime() + TimeUnit.SECONDS.toNanos(1); }
}
