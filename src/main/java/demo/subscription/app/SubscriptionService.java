package demo.subscription.app;

import demo.subscription.domain.CheckoutRequest;
import demo.subscription.domain.CheckoutResponse;
import demo.subscription.domain.Plan;
import demo.subscription.domain.PlanView;
import demo.subscription.domain.SubscriptionStatus;
import demo.subscription.domain.SubscriptionView;
import demo.subscription.domain.UserSubscription;
import demo.subscription.infra.PlanRepository;
import demo.subscription.infra.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final Map<String, PaymentGateway> gateways;

    public SubscriptionService(
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            List<PaymentGateway> gateways) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.gateways = gateways.stream().collect(Collectors.toMap(PaymentGateway::channel, Function.identity()));
    }

    public List<PlanView> getPlans() {
        boolean gatewayAEnabled = gateways.containsKey("gateway-a") && gateways.get("gateway-a").enabled();
        return planRepository.findActive().stream()
                .map(plan -> new PlanView(
                        plan.planCode(),
                        plan.displayName(),
                        plan.billingCycle(),
                        gatewayAEnabled))
                .toList();
    }

    public CheckoutResponse createCheckout(Long userId, CheckoutRequest request) {
        UserSubscription active = subscriptionRepository.findActiveByUserId(userId);
        if (active != null && active.isPaidPlan()) {
            throw new IllegalStateException("User already has an active paid subscription.");
        }

        Plan plan = planRepository.findByCode(request.planCode());
        if (plan == null || !plan.active()) {
            throw new IllegalArgumentException("Unknown plan.");
        }

        PaymentGateway gateway = gateways.get(request.channel());
        if (gateway == null || !gateway.enabled()) {
            throw new IllegalArgumentException("Unsupported payment channel.");
        }

        return gateway.createSubscriptionCheckout(plan.planCode(), userId);
    }

    public SubscriptionView getSubscription(Long userId) {
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId);
        if (subscription == null) {
            return new SubscriptionView("free", "system", "active", null, false);
        }
        return new SubscriptionView(
                subscription.planCode(),
                subscription.channel(),
                subscription.status().name().toLowerCase(),
                subscription.currentPeriodEnd(),
                subscription.cancelAtPeriodEnd());
    }

    public void cancelAtPeriodEnd(Long userId) {
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId);
        if (subscription == null || !subscription.isPaidPlan()) {
            throw new IllegalStateException("No paid subscription to cancel.");
        }

        subscriptionRepository.save(subscription.withCancelAtPeriodEnd(true));
    }

    public void resume(Long userId) {
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId);
        if (subscription == null || !subscription.cancelAtPeriodEnd()) {
            throw new IllegalStateException("No canceled subscription to resume.");
        }

        subscriptionRepository.save(subscription.withCancelAtPeriodEnd(false));
    }

    public UserSubscription activatePaidSubscription(Long userId, String channel, String planCode, String externalRef) {
        UserSubscription existing = subscriptionRepository.findLatestByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = existing != null && existing.currentPeriodEnd() != null ? existing.currentPeriodEnd() : now;

        UserSubscription next = new UserSubscription(
                existing != null ? existing.id() : null,
                userId,
                planCode,
                channel,
                externalRef,
                SubscriptionStatus.ACTIVE,
                now,
                base.plusMonths(12),
                false);
        return subscriptionRepository.save(next);
    }

    public void downgradeToFree(String externalRef) {
        UserSubscription existing = subscriptionRepository.findByExternalSubscriptionRef(externalRef);
        if (existing == null) {
            return;
        }

        UserSubscription next = new UserSubscription(
                existing.id(),
                existing.userId(),
                "free",
                "system",
                null,
                SubscriptionStatus.TRIALING,
                existing.currentPeriodStart(),
                existing.currentPeriodEnd(),
                false);
        subscriptionRepository.save(next);
    }
}
