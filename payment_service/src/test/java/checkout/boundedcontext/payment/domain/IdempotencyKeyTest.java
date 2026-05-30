package checkout.boundedcontext.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link IdempotencyKey} value object.
 * These tests will fail to compile until the source class is created.
 */
class IdempotencyKeyTest {

    @Test
    @DisplayName("Should create with non-blank value via constructor")
    void shouldCreateWithNonBlankValue() {
        IdempotencyKey key = new IdempotencyKey("key-123");

        assertEquals("key-123", key.value());
    }

    @Test
    @DisplayName("Should create with non-blank value via factory")
    void shouldCreateWithNonBlankValueViaFactory() {
        IdempotencyKey key = IdempotencyKey.of("invoice-abc");

        assertEquals("invoice-abc", key.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey(null));
    }

    @Test
    @DisplayName("Should reject blank value")
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey(""));
    }

    @ParameterizedTest
    @DisplayName("Should reject whitespace-only values")
    @ValueSource(strings = {"   ", "\t", "\n", "  \t  "})
    void shouldRejectWhitespaceOnlyValues(String value) {
        assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey(value));
    }

    @Test
    @DisplayName("Should reject null via factory")
    void shouldRejectNullViaFactory() {
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of(null));
    }

    @Test
    @DisplayName("Should reject blank via factory")
    void shouldRejectBlankViaFactory() {
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of("   "));
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        IdempotencyKey key1 = IdempotencyKey.of("same-key");
        IdempotencyKey key2 = IdempotencyKey.of("same-key");
        IdempotencyKey key3 = IdempotencyKey.of("different-key");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
    }

    @Test
    @DisplayName("Should not equal null or different type")
    void shouldNotEqualNullOrDifferentType() {
        IdempotencyKey key = IdempotencyKey.of("test");

        assertNotEquals(null, key);
        assertNotEquals("test", key);
    }

    @Test
    @DisplayName("Should include value in toString")
    void shouldIncludeValueInToString() {
        IdempotencyKey key = IdempotencyKey.of("my-key");
        String str = key.toString();

        assertTrue(str.contains("my-key"));
    }

    @Test
    @DisplayName("Should handle UUID-like values")
    void shouldHandleUuidLikeValues() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        IdempotencyKey key = IdempotencyKey.of(uuid);

        assertEquals(uuid, key.value());
    }

    @Test
    @DisplayName("Should preserve leading and trailing non-whitespace characters")
    void shouldPreserveValidCharacters() {
        String value = "  key-with-spaces  ";
        IdempotencyKey key = IdempotencyKey.of(value);

        assertEquals(value, key.value());
    }

    @Test
    @DisplayName("Should accept 255-character boundary value")
    void shouldAccept255CharacterBoundaryValue() {
        String value = "a".repeat(255);
        IdempotencyKey key = IdempotencyKey.of(value);

        assertEquals(value, key.value());
    }
}
