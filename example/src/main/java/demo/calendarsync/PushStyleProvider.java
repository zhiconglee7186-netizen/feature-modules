package demo.calendarsync;

import java.time.LocalDateTime;
import java.util.List;

public class PushStyleProvider implements CalendarProvider {
    @Override
    public ProviderKind kind() {
        return ProviderKind.PUSH;
    }

    @Override
    public List<String> listCalendars(SyncCredential credential) {
        return List.of("team-calendar", "personal-calendar");
    }

    @Override
    public void registerChangeSignal(SyncCredential credential, String calendarId) {
        System.out.println("Registered webhook-style watch for " + calendarId);
    }

    @Override
    public SyncBatch fullSync(SyncCredential credential, String calendarId) {
        return sampleBatch(credential, calendarId, "push-token-1");
    }

    @Override
    public SyncBatch incrementalSync(SyncCredential credential, String calendarId, String previousToken) {
        return sampleBatch(credential, calendarId, previousToken + "-next");
    }

    private SyncBatch sampleBatch(SyncCredential credential, String calendarId, String nextToken) {
        NormalizedEvent event = new NormalizedEvent(
                credential.userId(),
                kind(),
                calendarId,
                "evt-1",
                "Planning Session",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                "Room A",
                "Sample event from a push-style provider",
                "confirmed"
        );
        return new SyncBatch(List.of(event), List.of(), nextToken);
    }
}
