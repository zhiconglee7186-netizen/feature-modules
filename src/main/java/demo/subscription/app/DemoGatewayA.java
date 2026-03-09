package demo.subscription.app;

import demo.subscription.domain.CheckoutResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DemoGatewayA implements PaymentGateway {

    @Override
    public String channel() {
        return "gateway-a";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public CheckoutResponse createSubscriptionCheckout(String planCode, Long userId) {
        String sessionId = "sess_" + UUID.randomUUID();
        String externalSubscriptionRef = "sub_" + UUID.randomUUID();
        return new CheckoutResponse(channel(), sessionId, externalSubscriptionRef, "/demo/redirect/" + sessionId);
    }
}
