import com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustPolicy;
import java.util.*;

public class NotificationSourceTrustCheck {
    public static void main(String[] args) {
        Set<String> approved = new HashSet<>();
        String self = "com.kedevjimmy.pesatrack";
        check(!NotificationSourceTrustPolicy.isApproved("ke.example.forwarder", self, approved), "unknown package");
        approved.add("ke.example.bank");
        check(NotificationSourceTrustPolicy.isApproved("ke.example.bank", self, approved), "approved package");
        check(!NotificationSourceTrustPolicy.isApproved("ke.example.forwarder", self, approved), "bank title cannot approve another package");
        approved.add(self);
        check(!NotificationSourceTrustPolicy.isApproved(self, self, approved), "self cannot be approved");
        approved.add("");
        check(!NotificationSourceTrustPolicy.isApproved("", self, approved), "empty source");
        check(!NotificationSourceTrustPolicy.isApproved(null, self, approved), "missing source");
        check(!NotificationSourceTrustPolicy.isApproved(" ke.example.bank ", self, approved), "malformed source");
        check(!NotificationSourceTrustPolicy.isApproved("KE.EXAMPLE.BANK", self, approved), "exact package only");
        approved.remove("ke.example.bank");
        check(!NotificationSourceTrustPolicy.isApproved("ke.example.bank", self, approved), "revoked package");
        System.out.println("Notification source trust checks passed (9 cases).");
    }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
