package demo.calendarsync;

import java.time.LocalDateTime;
import java.util.List;

public class SubscriptionStyleProvider implements CalendarProvider {
    @Override
    public ProviderKind kind() {
        return ProviderKind.SUBSCRIPTION;
    }

    @Override
    public List<String> listCalendars(SyncCredential credential) {
        return List.of("work-calendar");
    }

    @Override
    public void registerChangeSignal(SyncCredential credential, String calendarId) {
        System.out.println("Created renewable subscription for " + calendarId);
    }

    @Override
    public SyncBatch fullSync(SyncCredential credential, String calendarId) {
        NormalizedEvent event = new NormalizedEvent(
                credential.userId(),
                kind(),
                calendarId,
                "evt-sub-1",
                "Quarterly Review",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(2),
                "Virtual",
                "Sample event from a subscription-style provider",
                "confirmed"
        );
        return new SyncBatch(List.of(event), List.of(), "sub-token-1");
    }

    @Override
    public SyncBatch incrementalSync(SyncCredential credential, String calendarId, String previousToken) {
        return new SyncBatch(List.of(), List.of("evt-sub-1"), previousToken + "-next");
    }
}
