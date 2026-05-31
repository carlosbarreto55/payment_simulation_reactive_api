package checkout.boundedcontext.payment.domain;

import checkout.boundedcontext.customer.domain.CustomerId;
import checkout.boundedcontext.payment.domain.event.PaymentApproved;
import checkout.boundedcontext.payment.domain.event.PaymentDenied;
import checkout.boundedcontext.payment.domain.event.PaymentInitiated;
import checkout.boundedcontext.payment.domain.event.PaymentProcessed;
import checkout.boundedcontext.payment.domain.event.PaymentRefunded;
import checkout.boundedcontext.payment.domain.event.PaymentInitiated.ProductItemSnapshot;
import checkout.common.domain.event.DomainEvent;
import checkout.common.domain.valueobject.Money;
import checkout.common.exception.InvalidPaymentStatusException;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class PaymentIntent {

    private PaymentIntentId id;
    private Money amount;
    private PaymentStatus status;
    private IdempotencyKey idempotencyKey;
    private List<PaymentMethod> methods;
    private CustomerId customerId;
    private List<ProductItem> products;
    private List<PaymentTransaction> transactions;
    private String failureReason;
    private Instant createdAt;
    private final List<DomainEvent> events = new ArrayList<>();

    private PaymentIntent(IdempotencyKey idempotencyKey, Money amount, List<PaymentMethod> methods,
                         CustomerId customerId, List<ProductItem> products) {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(methods, "methods is required");
        Objects.requireNonNull(products, "products is required");

        if (amount.toCents() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.methods = PaymentMethod.validatedList(methods);
        this.customerId = customerId;
        this.products = Collections.unmodifiableList(products);
        this.transactions = new ArrayList<>();
        this.createdAt = Instant.now();
    }

    public static PaymentIntent initiate(IdempotencyKey idempotencyKey, Money amount,
                                         List<PaymentMethod> methods, CustomerId customerId,
                                         List<ProductItem> products) {
        PaymentIntent intent = new PaymentIntent(idempotencyKey, amount, methods, customerId, products);

        List<ProductItemSnapshot> productSnapshots = products.stream()
                .map(p -> new ProductItemSnapshot(p.externalId(), p.name(), p.quantity(), p.priceInCents()))
                .toList();

        String aggregateId = idempotencyKey.value();

        intent.events.add(new PaymentInitiated(
                null, null, aggregateId,
                idempotencyKey.value(), amount.toCents(), amount.currency(),
                methods.stream().map(PaymentMethod::name).toList(),
                customerId != null ? customerId.value() : null,
                productSnapshots
        ));

        return intent;
    }

    public void process(ExternalBillingId externalId, String providerStatus) {
        if (!this.status.canTransitionTo(PaymentStatus.PROCESSING)) {
            throw new InvalidPaymentStatusException(
                    "Cannot transition from " + this.status + " to " + PaymentStatus.PROCESSING);
        }

        PaymentTransaction transaction = PaymentTransaction.record(externalId, providerStatus);
        this.transactions.add(transaction);
        this.status = PaymentStatus.PROCESSING;

        this.events.add(new PaymentProcessed(
                null, null, idempotencyKey.value(),
                id != null ? String.valueOf(id.value()) : "unassigned",
                externalId.value(), providerStatus
        ));
    }

    public void approve() {
        if (!this.status.canTransitionTo(PaymentStatus.APPROVED)) {
            throw new InvalidPaymentStatusException(
                    "Cannot transition from " + this.status + " to " + PaymentStatus.APPROVED);
        }
        this.status = PaymentStatus.APPROVED;

        this.events.add(new PaymentApproved(
                null, null, idempotencyKey.value(),
                id != null ? String.valueOf(id.value()) : "unassigned"
        ));
    }

    public void deny(String reason) {
        if (!this.status.canTransitionTo(PaymentStatus.DENIED)) {
            throw new InvalidPaymentStatusException(
                    "Cannot transition from " + this.status + " to " + PaymentStatus.DENIED);
        }
        this.status = PaymentStatus.DENIED;
        this.failureReason = reason;

        this.events.add(new PaymentDenied(
                null, null, idempotencyKey.value(),
                id != null ? String.valueOf(id.value()) : "unassigned",
                reason
        ));
    }

    public void refund() {
        if (!this.status.canTransitionTo(PaymentStatus.REFUNDED)) {
            throw new InvalidPaymentStatusException(
                    "Cannot transition from " + this.status + " to " + PaymentStatus.REFUNDED);
        }
        this.status = PaymentStatus.REFUNDED;

        this.events.add(new PaymentRefunded(
                null, null, idempotencyKey.value(),
                id != null ? String.valueOf(id.value()) : "unassigned"
        ));
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(events);
    }

    public void clearDomainEvents() {
        events.clear();
    }

    public void assignId(PaymentIntentId id) {
        if (this.id != null) {
            throw new IllegalStateException("PaymentIntent ID is already assigned and cannot be changed");
        }
        this.id = id;
    }

    private String getAggregateIdString() {
        return id != null ? String.valueOf(id.value()) : "unassigned";
    }
}
