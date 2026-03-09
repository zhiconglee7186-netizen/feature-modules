package demo.subscription.domain;

public record CheckoutResponse(
        String channel,
        String sessionId,
        String externalSubscriptionRef,
        String redirectUrl
) {
}
