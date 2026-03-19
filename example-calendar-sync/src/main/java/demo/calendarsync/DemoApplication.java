package demo.calendarsync;

import java.time.Duration;
import java.util.List;

public class DemoApplication {
    public static void main(String[] args) throws Exception {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemorySyncStateRepository syncStateRepository = new InMemorySyncStateRepository();
        CalendarSyncOrchestrator orchestrator = new CalendarSyncOrchestrator(
                List.of(
                        new PushStyleProvider(),
                        new SubscriptionStyleProvider(),
                        new CalDavStyleProvider()
                ),
                eventRepository,
                syncStateRepository
        );

        PostAuthBootstrapService bootstrapService = new PostAuthBootstrapService(orchestrator);
        WebhookWindowCoordinator webhookCoordinator = new WebhookWindowCoordinator(Duration.ofSeconds(5));

        SyncCredential pushCredential = new SyncCredential(
                101L,
                ProviderKind.PUSH,
                "account-a",
                "access-token-placeholder",
                "refresh-token-placeholder",
                "encrypted-secret-placeholder"
        );

        bootstrapService.handleAuthorizationSuccess(pushCredential);
        Thread.sleep(200);

        if (webhookCoordinator.shouldRunNow(pushCredential.userId(), "team-calendar")) {
            orchestrator.syncChangedCalendar(pushCredential, "team-calendar");
        }

        if (!webhookCoordinator.shouldRunNow(pushCredential.userId(), "team-calendar")) {
            System.out.println("Merged a duplicate webhook into the current sync window");
        }

        Thread.sleep(5200);

        if (webhookCoordinator.consumePending(pushCredential.userId(), "team-calendar")) {
            orchestrator.syncChangedCalendar(pushCredential, "team-calendar");
        }

        SyncCredential caldavCredential = new SyncCredential(
                202L,
                ProviderKind.CALDAV,
                "account-b",
                "access-token-placeholder",
                "refresh-token-placeholder",
                "encrypted-secret-placeholder"
        );
        orchestrator.bootstrapAfterAuthorization(caldavCredential);
        orchestrator.syncChangedCalendar(caldavCredential, "family-calendar");

        System.out.println("Normalized events in local storage:");
        for (NormalizedEvent event : eventRepository.findAll()) {
            System.out.println(event);
        }

        bootstrapService.shutdown();
    }
}
