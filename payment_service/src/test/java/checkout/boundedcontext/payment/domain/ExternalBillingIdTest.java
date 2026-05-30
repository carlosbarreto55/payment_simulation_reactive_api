package checkout.boundedcontext.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link ExternalBillingId} value object.
 * These tests will fail to compile until the source class is created.
 */
class ExternalBillingIdTest {

    @Test
    @DisplayName("Should create with non-blank value via constructor")
    void shouldCreateWithNonBlankValue() {
        ExternalBillingId id = new ExternalBillingId("billing-123");

        assertEquals("billing-123", id.value());
    }

    @Test
    @DisplayName("Should create with non-blank value via factory")
    void shouldCreateWithNonBlankValueViaFactory() {
        ExternalBillingId id = ExternalBillingId.of("ext-abc-999");

        assertEquals("ext-abc-999", id.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalBillingId(null));
    }

    @Test
    @DisplayName("Should reject blank value")
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalBillingId(""));
    }

    @Test
    @DisplayName("Should reject whitespace-only value")
    void shouldRejectWhitespaceOnlyValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalBillingId("   "));
    }

    @Test
    @DisplayName("Should reject null via factory")
    void shouldRejectNullViaFactory() {
        assertThrows(IllegalArgumentException.class, () -> ExternalBillingId.of(null));
    }

    @Test
    @DisplayName("Should reject blank via factory")
    void shouldRejectBlankViaFactory() {
        assertThrows(IllegalArgumentException.class, () -> ExternalBillingId.of(""));
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        ExternalBillingId id1 = ExternalBillingId.of("same-id");
        ExternalBillingId id2 = ExternalBillingId.of("same-id");
        ExternalBillingId id3 = ExternalBillingId.of("different-id");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    @DisplayName("Should not equal null or different type")
    void shouldNotEqualNullOrDifferentType() {
        ExternalBillingId id = ExternalBillingId.of("test");

        assertNotEquals(null, id);
        assertNotEquals("test", id);
    }

    @Test
    @DisplayName("Should include value in toString")
    void shouldIncludeValueInToString() {
        ExternalBillingId id = ExternalBillingId.of("my-billing-id");
        String str = id.toString();

        assertTrue(str.contains("my-billing-id"));
    }

    @Test
    @DisplayName("Should reject external IDs longer than 255 characters")
    void shouldRejectTooLongExternalIds() {
        String longId = "a".repeat(256);
        assertThrows(IllegalArgumentException.class, () -> ExternalBillingId.of(longId));
    }

    @Test
    @DisplayName("Should handle UUID-like values")
    void shouldHandleUuidLikeValues() {
        String uuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        ExternalBillingId id = ExternalBillingId.of(uuid);

        assertEquals(uuid, id.value());
    }
}
