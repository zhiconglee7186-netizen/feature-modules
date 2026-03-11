package demo.calendarsync;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebhookWindowCoordinator {
    private final Map<String, Long> lockExpiry = new ConcurrentHashMap<>();
    private final Map<String, Boolean> pending = new ConcurrentHashMap<>();
    private final long windowMillis;

    public WebhookWindowCoordinator(Duration window) {
        this.windowMillis = window.toMillis();
    }

    public boolean shouldRunNow(long userId, String calendarId) {
        String key = key(userId, calendarId);
        long now = System.currentTimeMillis();
        Long expiresAt = lockExpiry.get(key);

        if (expiresAt != null && expiresAt > now) {
            pending.put(key, true);
            return false;
        }

        lockExpiry.put(key, now + windowMillis);
        return true;
    }

    public boolean consumePending(long userId, String calendarId) {
        return pending.remove(key(userId, calendarId)) != null;
    }

    private String key(long userId, String calendarId) {
        return userId + "|" + calendarId;
    }
}
