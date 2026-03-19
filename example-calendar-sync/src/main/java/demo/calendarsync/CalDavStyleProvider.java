package demo.calendarsync;

import java.time.LocalDateTime;
import java.util.List;

public class CalDavStyleProvider implements CalendarProvider {
    @Override
    public ProviderKind kind() {
        return ProviderKind.CALDAV;
    }

    @Override
    public List<String> listCalendars(SyncCredential credential) {
        return List.of("family-calendar");
    }

    @Override
    public void registerChangeSignal(SyncCredential credential, String calendarId) {
        System.out.println("No push watch for " + calendarId + "; relying on sync token flow");
    }

    @Override
    public SyncBatch fullSync(SyncCredential credential, String calendarId) {
        NormalizedEvent event = new NormalizedEvent(
                credential.userId(),
                kind(),
                calendarId,
                "evt-caldav-1",
                "Dinner",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusHours(1),
                "Home",
                "Sample event from a CalDAV-style provider",
                "confirmed"
        );
        return new SyncBatch(List.of(event), List.of(), "caldav-token-1");
    }

    @Override
    public SyncBatch incrementalSync(SyncCredential credential, String calendarId, String previousToken) {
        NormalizedEvent updatedEvent = new NormalizedEvent(
                credential.userId(),
                kind(),
                calendarId,
                "evt-caldav-1",
                "Dinner Updated",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusHours(2),
                "Home",
                "Incremental update from a CalDAV-style provider",
                "confirmed"
        );
        return new SyncBatch(List.of(updatedEvent), List.of(), previousToken + "-next");
    }
}
