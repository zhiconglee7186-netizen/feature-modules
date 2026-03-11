package demo.calendarsync;

import java.util.List;

public interface CalendarProvider {
    ProviderKind kind();

    List<String> listCalendars(SyncCredential credential);

    void registerChangeSignal(SyncCredential credential, String calendarId);

    SyncBatch fullSync(SyncCredential credential, String calendarId);

    SyncBatch incrementalSync(SyncCredential credential, String calendarId, String previousToken);
}
