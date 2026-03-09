package demo.subscription.domain;

public record CheckoutRequest(
        String planCode,
        String channel
) {
}
