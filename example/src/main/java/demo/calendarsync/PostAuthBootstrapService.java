package demo.calendarsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PostAuthBootstrapService {
    private final CalendarSyncOrchestrator orchestrator;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public PostAuthBootstrapService(CalendarSyncOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public void handleAuthorizationSuccess(SyncCredential credential) {
        executor.submit(() -> orchestrator.bootstrapAfterAuthorization(credential));
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
