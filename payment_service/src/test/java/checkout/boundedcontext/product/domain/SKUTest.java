package checkout.boundedcontext.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link SKU} value object.
 * These tests will fail to compile until the source class is created.
 */
class SKUTest {

    @Test
    @DisplayName("Should create with valid uppercase alphanumeric value via constructor")
    void shouldCreateWithValidValue() {
        SKU sku = new SKU("PROD123");

        assertEquals("PROD123", sku.value());
    }

    @Test
    @DisplayName("Should create with valid value via factory")
    void shouldCreateWithValidValueViaFactory() {
        SKU sku = SKU.of("SKU999X");

        assertEquals("SKU999X", sku.value());
    }

    @Test
    @DisplayName("Should reject null value")
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new SKU(null));
    }

    @Test
    @DisplayName("Should reject blank value")
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new SKU(""));
    }

    @Test
    @DisplayName("Should reject whitespace-only value")
    void shouldRejectWhitespaceOnlyValue() {
        assertThrows(IllegalArgumentException.class, () -> new SKU("   "));
    }

    @Test
    @DisplayName("Should reject value shorter than 3 characters")
    void shouldRejectTooShortValue() {
        assertThrows(IllegalArgumentException.class, () -> new SKU("AB"));
        assertThrows(IllegalArgumentException.class, () -> new SKU("A"));
    }

    @Test
    @DisplayName("Should reject value longer than 50 characters")
    void shouldRejectTooLongValue() {
        assertThrows(IllegalArgumentException.class, () -> new SKU("A".repeat(51)));
    }

    @ParameterizedTest
    @DisplayName("Should reject lowercase letters")
    @ValueSource(strings = {"prod123", "Prod123", "pROD123"})
    void shouldRejectLowercaseLetters(String value) {
        assertThrows(IllegalArgumentException.class, () -> new SKU(value));
    }

    @ParameterizedTest
    @DisplayName("Should reject special characters")
    @ValueSource(strings = {"PROD-123", "PROD 123", "PROD@123", "PROD_123", "PROD.123"})
    void shouldRejectSpecialCharacters(String value) {
        assertThrows(IllegalArgumentException.class, () -> new SKU(value));
    }

    @Test
    @DisplayName("Should accept exactly 3 characters")
    void shouldAcceptExactlyThreeCharacters() {
        SKU sku = new SKU("ABC");

        assertEquals("ABC", sku.value());
    }

    @Test
    @DisplayName("Should accept exactly 50 characters")
    void shouldAcceptExactlyFiftyCharacters() {
        String value = "A".repeat(50);
        SKU sku = new SKU(value);

        assertEquals(value, sku.value());
    }

    @Test
    @DisplayName("Should accept digits only")
    void shouldAcceptDigitsOnly() {
        SKU sku = new SKU("123456");

        assertEquals("123456", sku.value());
    }

    @Test
    @DisplayName("Should accept letters only")
    void shouldAcceptLettersOnly() {
        SKU sku = new SKU("ABCDEF");

        assertEquals("ABCDEF", sku.value());
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        SKU sku1 = SKU.of("SAME01");
        SKU sku2 = SKU.of("SAME01");
        SKU sku3 = SKU.of("DIFF01");

        assertEquals(sku1, sku2);
        assertEquals(sku1.hashCode(), sku2.hashCode());
        assertNotEquals(sku1, sku3);
    }

    @Test
    @DisplayName("Should not equal null or different type")
    void shouldNotEqualNullOrDifferentType() {
        SKU sku = SKU.of("TEST01");

        assertNotEquals(null, sku);
        assertNotEquals("TEST01", sku);
    }

    @Test
    @DisplayName("Should include value in toString")
    void shouldIncludeValueInToString() {
        SKU sku = SKU.of("PROD001");
        String str = sku.toString();

        assertTrue(str.contains("PROD001"));
    }
}
