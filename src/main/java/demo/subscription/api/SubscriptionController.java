package demo.subscription.api;

import demo.subscription.app.SubscriptionService;
import demo.subscription.domain.CheckoutRequest;
import demo.subscription.domain.CheckoutResponse;
import demo.subscription.domain.PlanView;
import demo.subscription.domain.SubscriptionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/demo/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public List<PlanView> getPlans() {
        return subscriptionService.getPlans();
    }

    @PostMapping("/{userId}/checkout")
    public CheckoutResponse createCheckout(@PathVariable Long userId, @RequestBody CheckoutRequest request) {
        return subscriptionService.createCheckout(userId, request);
    }

    @GetMapping("/{userId}")
    public SubscriptionView getSubscription(@PathVariable Long userId) {
        return subscriptionService.getSubscription(userId);
    }

    @PostMapping("/{userId}/cancel")
    public void cancel(@PathVariable Long userId) {
        subscriptionService.cancelAtPeriodEnd(userId);
    }

    @PostMapping("/{userId}/resume")
    public void resume(@PathVariable Long userId) {
        subscriptionService.resume(userId);
    }
}
