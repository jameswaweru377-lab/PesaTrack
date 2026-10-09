package com.kedevjimmy.pesatrack.ingestion;

import java.util.Set;
import java.util.regex.Pattern;

/** Trust the exact Android source package only after explicit local approval. */
public final class NotificationSourceTrustPolicy {
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+");
    private NotificationSourceTrustPolicy() {}
    public static boolean isEligible(String source, String self) {
        return source != null && !source.equals(self) && PACKAGE.matcher(source).matches();
    }
    public static boolean isApproved(String source, String self, Set<String> approved) {
        return isEligible(source, self) && approved.contains(source);
    }
}
