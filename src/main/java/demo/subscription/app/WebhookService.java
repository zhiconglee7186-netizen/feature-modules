package demo.subscription.app;

import demo.subscription.domain.GatewayEventType;
import demo.subscription.domain.GatewayWebhookEvent;
import demo.subscription.domain.PaymentRecord;
import demo.subscription.infra.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WebhookService {

    private final SubscriptionService subscriptionService;
    private final PaymentRepository paymentRepository;

    public WebhookService(SubscriptionService subscriptionService, PaymentRepository paymentRepository) {
        this.subscriptionService = subscriptionService;
        this.paymentRepository = paymentRepository;
    }

    public void handleGatewayA(String signature, GatewayWebhookEvent event) {
        verifySignature(signature);

        if (event == null || event.type() == null) {
            throw new IllegalArgumentException("Event is required.");
        }

        switch (event.type()) {
            case CHECKOUT_COMPLETED -> subscriptionService.activatePaidSubscription(
                    event.userId(),
                    "gateway-a",
                    event.planCode(),
                    event.externalSubscriptionRef());
            case INVOICE_PAID -> storePayment(event);
            case SUBSCRIPTION_CANCELED -> subscriptionService.downgradeToFree(event.externalSubscriptionRef());
            case SUBSCRIPTION_UPDATED -> {
                // Minimal sample: no-op. Real systems would update status and cancel-at-period-end flags.
            }
            default -> throw new IllegalArgumentException("Unsupported event type.");
        }
    }

    private void storePayment(GatewayWebhookEvent event) {
        if (paymentRepository.findByExternalPaymentRef(event.externalPaymentRef()) != null) {
            return;
        }

        PaymentRecord record = new PaymentRecord(
                null,
                event.userId(),
                event.externalSubscriptionRef(),
                event.planCode(),
                "gateway-a",
                event.externalPaymentRef(),
                event.amountInMinorUnits(),
                event.currency(),
                "succeeded",
                LocalDateTime.now());
        paymentRepository.save(record);
    }

    private void verifySignature(String signature) {
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing webhook signature.");
        }
    }
}
