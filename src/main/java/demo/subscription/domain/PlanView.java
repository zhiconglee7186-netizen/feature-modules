package demo.subscription.domain;

public record PlanView(
        String planCode,
        String displayName,
        String billingCycle,
        boolean gatewayEnabled
) {
}
