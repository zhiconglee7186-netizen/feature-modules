package demo.subscription.domain;

public record GatewayWebhookEvent(
        GatewayEventType type,
        Long userId,
        String planCode,
        String externalSubscriptionRef,
        String externalPaymentRef,
        Integer amountInMinorUnits,
        String currency
) {
}
