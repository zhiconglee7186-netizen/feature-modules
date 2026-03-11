package demo.calendarsync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySyncStateRepository {
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public String getToken(long userId, ProviderKind providerKind, String calendarId) {
        return tokens.get(key(userId, providerKind, calendarId));
    }

    public void saveToken(long userId, ProviderKind providerKind, String calendarId, String token) {
        if (token == null || token.isBlank()) {
            tokens.remove(key(userId, providerKind, calendarId));
            return;
        }
        tokens.put(key(userId, providerKind, calendarId), token);
    }

    private String key(long userId, ProviderKind providerKind, String calendarId) {
        return userId + "|" + providerKind + "|" + calendarId;
    }
}
