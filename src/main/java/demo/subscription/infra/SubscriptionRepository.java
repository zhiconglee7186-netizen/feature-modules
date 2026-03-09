package demo.subscription.infra;

import demo.subscription.domain.SubscriptionStatus;
import demo.subscription.domain.UserSubscription;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class SubscriptionRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<UserSubscription> subscriptions = new ArrayList<>();

    public SubscriptionRepository() {
        subscriptions.add(new UserSubscription(
                idGenerator.getAndIncrement(),
                1L,
                "free",
                "system",
                null,
                SubscriptionStatus.TRIALING,
                null,
                null,
                false));
    }

    public UserSubscription findActiveByUserId(Long userId) {
        return subscriptions.stream()
                .filter(item -> item.userId().equals(userId))
                .filter(item -> item.status() == SubscriptionStatus.ACTIVE || item.status() == SubscriptionStatus.TRIALING)
                .max(Comparator.comparing(item -> item.currentPeriodEnd() == null ? java.time.LocalDateTime.MIN : item.currentPeriodEnd()))
                .orElse(null);
    }

    public UserSubscription findLatestByUserId(Long userId) {
        return subscriptions.stream()
                .filter(item -> item.userId().equals(userId))
                .max(Comparator.comparing(UserSubscription::id))
                .orElse(null);
    }

    public UserSubscription findByExternalSubscriptionRef(String externalSubscriptionRef) {
        if (externalSubscriptionRef == null) {
            return null;
        }
        return subscriptions.stream()
                .filter(item -> externalSubscriptionRef.equals(item.externalSubscriptionRef()))
                .findFirst()
                .orElse(null);
    }

    public UserSubscription save(UserSubscription subscription) {
        if (subscription.id() == null) {
            UserSubscription next = new UserSubscription(
                    idGenerator.getAndIncrement(),
                    subscription.userId(),
                    subscription.planCode(),
                    subscription.channel(),
                    subscription.externalSubscriptionRef(),
                    subscription.status(),
                    subscription.currentPeriodStart(),
                    subscription.currentPeriodEnd(),
                    subscription.cancelAtPeriodEnd());
            subscriptions.add(next);
            return next;
        }

        subscriptions.removeIf(item -> item.id().equals(subscription.id()));
        subscriptions.add(subscription);
        return subscription;
    }
}
