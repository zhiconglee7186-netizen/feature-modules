package demo.calendarsync;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CalendarSyncOrchestrator {
    private final Map<ProviderKind, CalendarProvider> providers;
    private final InMemoryEventRepository eventRepository;
    private final InMemorySyncStateRepository syncStateRepository;

    public CalendarSyncOrchestrator(
            List<CalendarProvider> providers,
            InMemoryEventRepository eventRepository,
            InMemorySyncStateRepository syncStateRepository
    ) {
        this.providers = providers.stream().collect(java.util.stream.Collectors.toMap(CalendarProvider::kind, p -> p));
        this.eventRepository = eventRepository;
        this.syncStateRepository = syncStateRepository;
    }

    public void bootstrapAfterAuthorization(SyncCredential credential) {
        CalendarProvider provider = providerFor(credential.providerKind());
        for (String calendarId : provider.listCalendars(credential)) {
            apply(provider.fullSync(credential, calendarId), credential, calendarId);
            provider.registerChangeSignal(credential, calendarId);
        }
    }

    public void syncChangedCalendar(SyncCredential credential, String calendarId) {
        CalendarProvider provider = providerFor(credential.providerKind());
        String token = syncStateRepository.getToken(credential.userId(), credential.providerKind(), calendarId);
        SyncBatch batch;

        if (token == null) {
            batch = provider.fullSync(credential, calendarId);
        } else {
            batch = provider.incrementalSync(credential, calendarId, token);
        }

        apply(batch, credential, calendarId);
    }

    private void apply(SyncBatch batch, SyncCredential credential, String calendarId) {
        if (batch == null) {
            return;
        }

        for (NormalizedEvent event : batch.upserts()) {
            eventRepository.upsert(event);
        }

        for (String remoteEventId : batch.deletedRemoteEventIds()) {
            eventRepository.delete(credential.userId(), credential.providerKind(), calendarId, remoteEventId);
        }

        syncStateRepository.saveToken(credential.userId(), credential.providerKind(), calendarId, batch.nextToken());
    }

    private CalendarProvider providerFor(ProviderKind providerKind) {
        CalendarProvider provider = providers.get(providerKind);
        return Objects.requireNonNull(provider, "No provider adapter for " + providerKind);
    }
}
