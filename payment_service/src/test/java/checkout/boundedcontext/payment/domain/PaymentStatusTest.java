package checkout.boundedcontext.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link PaymentStatus} enum.
 * These tests will fail to compile until the source enum is created.
 */
class PaymentStatusTest {

    @ParameterizedTest(name = "[{0}] canTransitionTo [{1}] => {2}")
    @DisplayName("Should validate all state transitions")
    @CsvSource({
            // PENDING transitions
            "PENDING, PENDING, false",
            "PENDING, PROCESSING, true",
            "PENDING, APPROVED, false",
            "PENDING, DENIED, false",
            "PENDING, REFUNDED, false",
            // PROCESSING transitions
            "PROCESSING, PENDING, false",
            "PROCESSING, PROCESSING, false",
            "PROCESSING, APPROVED, true",
            "PROCESSING, DENIED, true",
            "PROCESSING, REFUNDED, false",
            // APPROVED transitions
            "APPROVED, PENDING, false",
            "APPROVED, PROCESSING, false",
            "APPROVED, APPROVED, false",
            "APPROVED, DENIED, false",
            "APPROVED, REFUNDED, true",
            // DENIED transitions
            "DENIED, PENDING, false",
            "DENIED, PROCESSING, false",
            "DENIED, APPROVED, false",
            "DENIED, DENIED, false",
            "DENIED, REFUNDED, false",
            // REFUNDED transitions
            "REFUNDED, PENDING, false",
            "REFUNDED, PROCESSING, false",
            "REFUNDED, APPROVED, false",
            "REFUNDED, DENIED, false",
            "REFUNDED, REFUNDED, false"
    })
    void shouldValidateAllTransitions(String from, String to, boolean expected) {
        PaymentStatus source = PaymentStatus.valueOf(from);
        PaymentStatus target = PaymentStatus.valueOf(to);

        assertEquals(expected, source.canTransitionTo(target));
    }

    @ParameterizedTest(name = "[{0}] isTerminal => {1}")
    @DisplayName("Should correctly identify terminal states")
    @CsvSource({
            "PENDING, false",
            "PROCESSING, false",
            "APPROVED, true",
            "DENIED, true",
            "REFUNDED, true"
    })
    void shouldIdentifyTerminalStates(String statusName, boolean expectedTerminal) {
        PaymentStatus status = PaymentStatus.valueOf(statusName);

        assertEquals(expectedTerminal, status.isTerminal());
    }

    @Test
    @DisplayName("Should contain all expected enum values")
    void shouldContainAllExpectedValues() {
        PaymentStatus[] values = PaymentStatus.values();

        assertEquals(5, values.length);
        assertNotNull(PaymentStatus.valueOf("PENDING"));
        assertNotNull(PaymentStatus.valueOf("PROCESSING"));
        assertNotNull(PaymentStatus.valueOf("APPROVED"));
        assertNotNull(PaymentStatus.valueOf("DENIED"));
        assertNotNull(PaymentStatus.valueOf("REFUNDED"));
    }

    @Test
    @DisplayName("Terminal states should not transition to any other state")
    void terminalStatesShouldNotTransition() {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.isTerminal()) {
                for (PaymentStatus target : PaymentStatus.values()) {
                    if (status == PaymentStatus.APPROVED && target == PaymentStatus.REFUNDED) {
                        continue; // APPROVED is terminal but may transition to REFUNDED
                    }
                    assertFalse(status.canTransitionTo(target),
                            status + " should not transition to " + target);
                }
            }
        }
    }

    @Test
    @DisplayName("Non-terminal states should have at least one valid transition")
    void nonTerminalStatesShouldHaveAtLeastOneTransition() {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (!status.isTerminal()) {
                boolean hasValidTransition = false;
                for (PaymentStatus target : PaymentStatus.values()) {
                    if (status.canTransitionTo(target)) {
                        hasValidTransition = true;
                        break;
                    }
                }
                assertTrue(hasValidTransition,
                        status + " should have at least one valid transition");
            }
        }
    }
}
