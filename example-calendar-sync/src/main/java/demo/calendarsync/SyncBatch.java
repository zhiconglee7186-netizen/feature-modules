package demo.calendarsync;

import java.util.List;

public record SyncBatch(
        List<NormalizedEvent> upserts,
        List<String> deletedRemoteEventIds,
        String nextToken
) {
}
