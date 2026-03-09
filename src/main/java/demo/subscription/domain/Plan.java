package demo.subscription.domain;

public record Plan(
        String planCode,
        String displayName,
        String billingCycle,
        boolean active
) {
}
