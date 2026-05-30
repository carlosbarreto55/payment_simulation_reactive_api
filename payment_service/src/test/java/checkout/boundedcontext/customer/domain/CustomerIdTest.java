package checkout.boundedcontext.customer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link CustomerId} value object.
 * These tests will fail to compile until the source class is created.
 */
class CustomerIdTest {

    @Test
    @DisplayName("Should create with positive value via constructor")
    void shouldCreateWithPositiveValue() {
        CustomerId id = new CustomerId(1L);

        assertEquals(1L, id.value());
    }

    @Test
    @DisplayName("Should create with positive value via factory")
    void shouldCreateWithPositiveValueViaFactory() {
        CustomerId id = CustomerId.of(456L);

        assertEquals(456L, id.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerId(null));
    }

    @Test
    @DisplayName("Should reject zero value")
    void shouldRejectZeroValue() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerId(0L));
    }

    @ParameterizedTest
    @DisplayName("Should reject negative values")
    @ValueSource(longs = {-1L, -100L, Long.MIN_VALUE})
    void shouldRejectNegativeValues(long value) {
        assertThrows(IllegalArgumentException.class, () -> new CustomerId(value));
    }

    @Test
    @DisplayName("Should reject null via factory")
    void shouldRejectNullViaFactory() {
        assertThrows(IllegalArgumentException.class, () -> CustomerId.of(null));
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        CustomerId id1 = CustomerId.of(42L);
        CustomerId id2 = CustomerId.of(42L);
        CustomerId id3 = CustomerId.of(99L);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    @DisplayName("Should not equal null or different type")
    void shouldNotEqualNullOrDifferentType() {
        CustomerId id = CustomerId.of(1L);

        assertNotEquals(null, id);
        assertNotEquals("1", id);
    }

    @Test
    @DisplayName("Should include value in toString")
    void shouldIncludeValueInToString() {
        CustomerId id = CustomerId.of(777L);
        String str = id.toString();

        assertTrue(str.contains("777"));
    }

    @Test
    @DisplayName("Should handle max long value")
    void shouldHandleMaxLongValue() {
        CustomerId id = CustomerId.of(Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, id.value());
    }
}
