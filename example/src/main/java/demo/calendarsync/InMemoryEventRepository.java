package demo.calendarsync;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventRepository {
    private final Map<String, NormalizedEvent> storage = new ConcurrentHashMap<>();

    public void upsert(NormalizedEvent event) {
        storage.put(key(event.userId(), event.providerKind(), event.calendarId(), event.remoteEventId()), event);
    }

    public void delete(long userId, ProviderKind providerKind, String calendarId, String remoteEventId) {
        storage.remove(key(userId, providerKind, calendarId, remoteEventId));
    }

    public Collection<NormalizedEvent> findAll() {
        return storage.values();
    }

    private String key(long userId, ProviderKind providerKind, String calendarId, String remoteEventId) {
        return userId + "|" + providerKind + "|" + calendarId + "|" + remoteEventId;
    }
}
