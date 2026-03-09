package demo.subscription.infra;

import demo.subscription.domain.Plan;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlanRepository {

    private final List<Plan> plans = List.of(
            new Plan("free", "Free", "none", true),
            new Plan("premium", "Premium", "yearly", true),
            new Plan("enterprise", "Enterprise", "yearly", true)
    );

    public List<Plan> findActive() {
        return plans.stream().filter(Plan::active).toList();
    }

    public Plan findByCode(String planCode) {
        return plans.stream()
                .filter(plan -> plan.planCode().equals(planCode))
                .findFirst()
                .orElse(null);
    }
}
