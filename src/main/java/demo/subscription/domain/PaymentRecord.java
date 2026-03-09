package demo.subscription.domain;

import java.time.LocalDateTime;

public record PaymentRecord(
        Long id,
        Long userId,
        String externalSubscriptionRef,
        String planCode,
        String channel,
        String externalPaymentRef,
        Integer amountInMinorUnits,
        String currency,
        String status,
        LocalDateTime paidAt
) {
}
