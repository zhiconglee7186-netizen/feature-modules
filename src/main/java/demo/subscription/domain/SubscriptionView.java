package demo.subscription.domain;

import java.time.LocalDateTime;

public record SubscriptionView(
        String planCode,
        String channel,
        String status,
        LocalDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
}
