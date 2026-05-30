package checkout.common.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link Money} value object.
 * These tests will fail to compile until the source class is created.
 */
class MoneyTest {

    @Test
    @DisplayName("Should create Money with valid amount and currency")
    void shouldCreateMoneyWithValidAmountAndCurrency() {
        Money money = new Money(new BigDecimal("99.99"), "BRL");

        assertEquals(new BigDecimal("99.99"), money.amount());
        assertEquals("BRL", money.currency());
    }

    @Test
    @DisplayName("Should normalize currency to uppercase via constructor")
    void shouldNormalizeCurrencyToUppercase() {
        Money money = new Money(BigDecimal.TEN, "brl");

        assertEquals("BRL", money.currency());
    }

    @Test
    @DisplayName("Should reject null amount")
    void shouldRejectNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null, "BRL"));
    }

    @Test
    @DisplayName("Should reject null currency")
    void shouldRejectNullCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, null));
    }

    @Test
    @DisplayName("Should reject blank currency")
    void shouldRejectBlankCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, ""));
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, "   "));
    }

    @Test
    @DisplayName("Should create zero BRL money")
    void shouldCreateZeroBrlMoney() {
        Money zero = Money.zero();

        assertEquals(BigDecimal.ZERO, zero.amount());
        assertEquals("BRL", zero.currency());
    }

    @Test
    @DisplayName("Should create Money from BRL amount")
    void shouldCreateMoneyFromBrlAmount() {
        Money money = Money.ofBrl(new BigDecimal("49.90"));

        assertEquals(new BigDecimal("49.90"), money.amount());
        assertEquals("BRL", money.currency());
    }

    @Test
    @DisplayName("Should create Money from cents with BRL assumed")
    void shouldCreateMoneyFromCents() {
        Money money = Money.ofCents(2990);

        assertEquals(new BigDecimal("29.90"), money.amount());
        assertEquals("BRL", money.currency());
    }

    @Test
    @DisplayName("Should handle zero cents")
    void shouldHandleZeroCents() {
        Money money = Money.ofCents(0);

        assertEquals(BigDecimal.ZERO, money.amount());
        assertEquals("BRL", money.currency());
    }

    @Test
    @DisplayName("Should handle negative cents")
    void shouldHandleNegativeCents() {
        Money money = Money.ofCents(-100);

        assertEquals(new BigDecimal("-1.00"), money.amount());
        assertEquals("BRL", money.currency());
    }

    @Test
    @DisplayName("Should add two monies with same currency")
    void shouldAddSameCurrency() {
        Money m1 = Money.ofBrl(new BigDecimal("10.00"));
        Money m2 = Money.ofBrl(new BigDecimal("5.50"));

        Money result = m1.add(m2);

        assertEquals(new BigDecimal("15.50"), result.amount());
        assertEquals("BRL", result.currency());
    }

    @Test
    @DisplayName("Should reject add when currencies mismatch")
    void shouldRejectAddOnCurrencyMismatch() {
        Money brl = Money.ofBrl(BigDecimal.TEN);
        Money usd = new Money(BigDecimal.TEN, "USD");

        assertThrows(IllegalArgumentException.class, () -> brl.add(usd));
    }

    @Test
    @DisplayName("Should subtract two monies with same currency")
    void shouldSubtractSameCurrency() {
        Money m1 = Money.ofBrl(new BigDecimal("10.00"));
        Money m2 = Money.ofBrl(new BigDecimal("3.25"));

        Money result = m1.subtract(m2);

        assertEquals(new BigDecimal("6.75"), result.amount());
        assertEquals("BRL", result.currency());
    }

    @Test
    @DisplayName("Should reject subtract when currencies mismatch")
    void shouldRejectSubtractOnCurrencyMismatch() {
        Money brl = Money.ofBrl(BigDecimal.TEN);
        Money eur = new Money(BigDecimal.TEN, "EUR");

        assertThrows(IllegalArgumentException.class, () -> brl.subtract(eur));
    }

    @Test
    @DisplayName("Should compare greater than for same currency")
    void shouldCompareGreaterThanSameCurrency() {
        Money larger = Money.ofBrl(new BigDecimal("100.00"));
        Money smaller = Money.ofBrl(new BigDecimal("99.99"));

        assertTrue(larger.isGreaterThan(smaller));
        assertFalse(smaller.isGreaterThan(larger));
    }

    @Test
    @DisplayName("Should reject isGreaterThan when currencies mismatch")
    void shouldRejectIsGreaterThanOnCurrencyMismatch() {
        Money brl = Money.ofBrl(BigDecimal.TEN);
        Money usd = new Money(BigDecimal.ONE, "USD");

        assertThrows(IllegalArgumentException.class, () -> brl.isGreaterThan(usd));
    }

    @Test
    @DisplayName("Should convert to cents exactly")
    void shouldConvertToCentsExactly() {
        Money money = Money.ofBrl(new BigDecimal("123.45"));

        assertEquals(12345, money.toCents());
    }

    @Test
    @DisplayName("Should round to cents with HALF_EVEN")
    void shouldRoundToCentsWithHalfEven() {
        Money money = new Money(new BigDecimal("10.005"), "BRL");

        assertEquals(1000, money.toCents());
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        Money original = Money.ofBrl(new BigDecimal("50.00"));
        Money added = original.add(Money.ofBrl(new BigDecimal("10.00")));

        assertEquals(new BigDecimal("50.00"), original.amount());
        assertEquals(new BigDecimal("60.00"), added.amount());
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        Money m1 = Money.ofBrl(new BigDecimal("25.00"));
        Money m2 = Money.ofBrl(new BigDecimal("25.00"));
        Money m3 = Money.ofBrl(new BigDecimal("30.00"));

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotEquals(m1, m3);
    }

    @Test
    @DisplayName("Should implement informative toString")
    void shouldHaveInformativeToString() {
        Money money = Money.ofBrl(new BigDecimal("99.90"));
        String str = money.toString();

        assertTrue(str.contains("99.90"));
        assertTrue(str.contains("BRL"));
    }

    @ParameterizedTest
    @DisplayName("Should handle edge case amounts")
    @CsvSource({
            "0.01,1",
            "0.99,99",
            "999999.99,99999999",
            "-1.00,-100"
    })
    void shouldHandleEdgeCaseAmounts(String amountStr, int expectedCents) {
        Money money = new Money(new BigDecimal(amountStr), "BRL");
        assertEquals(expectedCents, money.toCents());
    }
}
