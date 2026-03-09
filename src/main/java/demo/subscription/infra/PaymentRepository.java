package demo.subscription.infra;

import demo.subscription.domain.PaymentRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class PaymentRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<PaymentRecord> records = new ArrayList<>();

    public PaymentRecord findByExternalPaymentRef(String externalPaymentRef) {
        if (externalPaymentRef == null) {
            return null;
        }
        return records.stream()
                .filter(item -> externalPaymentRef.equals(item.externalPaymentRef()))
                .findFirst()
                .orElse(null);
    }

    public PaymentRecord save(PaymentRecord record) {
        PaymentRecord next = new PaymentRecord(
                idGenerator.getAndIncrement(),
                record.userId(),
                record.externalSubscriptionRef(),
                record.planCode(),
                record.channel(),
                record.externalPaymentRef(),
                record.amountInMinorUnits(),
                record.currency(),
                record.status(),
                record.paidAt());
        records.add(next);
        return next;
    }
}
