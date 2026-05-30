package checkout.boundedcontext.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link PaymentIntentId} value object.
 * These tests will fail to compile until the source class is created.
 */
class PaymentIntentIdTest {

    @Test
    @DisplayName("Should create with positive value via constructor")
    void shouldCreateWithPositiveValue() {
        PaymentIntentId id = new PaymentIntentId(1L);

        assertEquals(1L, id.value());
    }

    @Test
    @DisplayName("Should create with positive value via factory")
    void shouldCreateWithPositiveValueViaFactory() {
        PaymentIntentId id = PaymentIntentId.of(123L);

        assertEquals(123L, id.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentIntentId(null));
    }

    @Test
    @DisplayName("Should reject zero value")
    void shouldRejectZeroValue() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentIntentId(0L));
    }

    @ParameterizedTest
    @DisplayName("Should reject negative values")
    @ValueSource(longs = {-1L, -100L, Long.MIN_VALUE})
    void shouldRejectNegativeValues(long value) {
        assertThrows(IllegalArgumentException.class, () -> new PaymentIntentId(value));
    }

    @Test
    @DisplayName("Should reject null via factory")
    void shouldRejectNullViaFactory() {
        assertThrows(IllegalArgumentException.class, () -> PaymentIntentId.of(null));
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        PaymentIntentId id1 = PaymentIntentId.of(42L);
        PaymentIntentId id2 = PaymentIntentId.of(42L);
        PaymentIntentId id3 = PaymentIntentId.of(99L);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    @DisplayName("Should not equal null or different type")
    void shouldNotEqualNullOrDifferentType() {
        PaymentIntentId id = PaymentIntentId.of(1L);

        assertNotEquals(null, id);
        assertNotEquals("1", id);
    }

    @Test
    @DisplayName("Should include value in toString")
    void shouldIncludeValueInToString() {
        PaymentIntentId id = PaymentIntentId.of(777L);
        String str = id.toString();

        assertTrue(str.contains("777"));
    }

    @Test
    @DisplayName("Should handle max long value")
    void shouldHandleMaxLongValue() {
        PaymentIntentId id = PaymentIntentId.of(Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, id.value());
    }
}
