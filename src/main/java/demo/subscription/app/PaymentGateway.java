package demo.subscription.app;

import demo.subscription.domain.CheckoutResponse;

public interface PaymentGateway {

    String channel();

    boolean enabled();

    CheckoutResponse createSubscriptionCheckout(String planCode, Long userId);
}
