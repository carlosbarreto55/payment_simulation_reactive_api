package checkout.common.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    public Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency cannot be blank");
        }
        this.amount = amount;
        this.currency = currency.toUpperCase();
    }

    public static Money ofCents(int cents) {
        if (cents == 0) {
            return zero();
        }
        return new Money(BigDecimal.valueOf(cents).movePointLeft(2), "BRL");
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, "BRL");
    }

    public static Money ofBrl(BigDecimal amount) {
        return new Money(amount, "BRL");
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public int toCents() {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).intValueExact();
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }
}
