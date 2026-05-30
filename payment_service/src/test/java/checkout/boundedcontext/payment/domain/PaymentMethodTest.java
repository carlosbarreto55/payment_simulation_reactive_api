package checkout.boundedcontext.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link PaymentMethod} enum.
 * These tests will fail to compile until the source enum is created.
 */
class PaymentMethodTest {

    @Test
    @DisplayName("Should map valid AbacatePay strings to enum")
    void shouldMapValidAbacatePayStrings() {
        assertEquals(PaymentMethod.PIX, PaymentMethod.fromAbacatePay("PIX"));
        assertEquals(PaymentMethod.CARD, PaymentMethod.fromAbacatePay("CARD"));
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid AbacatePay strings")
    @ValueSource(strings = {"pix", "card", "BOLETO", "", "   ", "UNKNOWN", "PAYPAL"})
    void shouldRejectInvalidAbacatePayStrings(String value) {
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromAbacatePay(value));
    }

    @Test
    @DisplayName("Should reject null AbacatePay string")
    void shouldRejectNullAbacatePayString() {
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromAbacatePay(null));
    }

    @Test
    @DisplayName("Should convert enum to AbacatePay string correctly")
    void shouldConvertToAbacatePayString() {
        assertEquals("PIX", PaymentMethod.PIX.toAbacatePayString());
        assertEquals("CARD", PaymentMethod.CARD.toAbacatePayString());
    }

    @Test
    @DisplayName("Should round-trip AbacatePay conversion")
    void shouldRoundTripAbacatePayConversion() {
        for (PaymentMethod method : PaymentMethod.values()) {
            String str = method.toAbacatePayString();
            PaymentMethod parsed = PaymentMethod.fromAbacatePay(str);
            assertEquals(method, parsed);
        }
    }

    @Test
    @DisplayName("Should validate list with one element")
    void shouldValidateListWithOneElement() {
        List<PaymentMethod> input = Collections.singletonList(PaymentMethod.PIX);
        List<PaymentMethod> result = PaymentMethod.validatedList(input);

        assertEquals(1, result.size());
        assertEquals(PaymentMethod.PIX, result.get(0));
        assertThrows(UnsupportedOperationException.class, () -> result.add(PaymentMethod.CARD),
                "Result should be unmodifiable");
    }

    @Test
    @DisplayName("Should validate list with two unique elements")
    void shouldValidateListWithTwoUniqueElements() {
        List<PaymentMethod> input = Arrays.asList(PaymentMethod.PIX, PaymentMethod.CARD);
        List<PaymentMethod> result = PaymentMethod.validatedList(input);

        assertEquals(2, result.size());
        assertTrue(result.contains(PaymentMethod.PIX));
        assertTrue(result.contains(PaymentMethod.CARD));
    }

    @Test
    @DisplayName("Should reject null list")
    void shouldRejectNullList() {
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.validatedList(null));
    }

    @Test
    @DisplayName("Should reject empty list")
    void shouldRejectEmptyList() {
        List<PaymentMethod> empty = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.validatedList(empty));
    }

    @Test
    @DisplayName("Should reject list with more than two elements")
    void shouldRejectListWithMoreThanTwoElements() {
        List<PaymentMethod> input = Arrays.asList(PaymentMethod.PIX, PaymentMethod.CARD, PaymentMethod.PIX);
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.validatedList(input));
    }

    @Test
    @DisplayName("Should reject list with duplicate elements")
    void shouldRejectListWithDuplicateElements() {
        List<PaymentMethod> input = Arrays.asList(PaymentMethod.PIX, PaymentMethod.PIX);
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.validatedList(input));
    }

    @Test
    @DisplayName("Should contain exactly two enum values")
    void shouldContainExactlyTwoValues() {
        PaymentMethod[] values = PaymentMethod.values();
        assertEquals(2, values.length);
        assertNotNull(PaymentMethod.valueOf("PIX"));
        assertNotNull(PaymentMethod.valueOf("CARD"));
    }
}
