package demo.calendarsync;

import java.time.LocalDateTime;

public record NormalizedEvent(
        long userId,
        ProviderKind providerKind,
        String calendarId,
        String remoteEventId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        String description,
        String status
) {
}
