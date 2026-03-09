package demo.subscription.domain;

import java.time.LocalDateTime;

public record UserSubscription(
        Long id,
        Long userId,
        String planCode,
        String channel,
        String externalSubscriptionRef,
        SubscriptionStatus status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
    public boolean isPaidPlan() {
        return "premium".equals(planCode) || "enterprise".equals(planCode);
    }

    public UserSubscription withCancelAtPeriodEnd(boolean value) {
        return new UserSubscription(
                id,
                userId,
                planCode,
                channel,
                externalSubscriptionRef,
                status,
                currentPeriodStart,
                currentPeriodEnd,
                value);
    }
}
