package checkout.boundedcontext.customer.domain;

import checkout.common.enums.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for {@link Document} value object (refactored record).
 * These tests will fail to compile until the source class is created at the new location.
 */
class DocumentTest {

    // ========== CPF Tests ==========

    @Test
    @DisplayName("Should create valid CPF without formatting")
    void shouldCreateValidCpfWithoutFormatting() {
        Document doc = new Document(DocumentType.CPF, "52998224725");

        assertEquals(DocumentType.CPF, doc.documentType());
        assertEquals("52998224725", doc.documentNumber());
    }

    @Test
    @DisplayName("Should create valid CPF with formatting")
    void shouldCreateValidCpfWithFormatting() {
        Document doc = new Document(DocumentType.CPF, "529.982.247-25");

        assertEquals(DocumentType.CPF, doc.documentType());
        assertEquals("529.982.247-25", doc.documentNumber());
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid CPF checksums")
    @ValueSource(strings = {"11111111111", "12345678901", "52998224726", "00000000000"})
    void shouldRejectInvalidCpfChecksums(String cpf) {
        assertThrows(IllegalArgumentException.class, () -> new Document(DocumentType.CPF, cpf));
    }

    // ========== CNPJ Tests ==========

    @Test
    @DisplayName("Should create valid CNPJ without formatting")
    void shouldCreateValidCnpjWithoutFormatting() {
        Document doc = new Document(DocumentType.CNPJ, "11444777000161");

        assertEquals(DocumentType.CNPJ, doc.documentType());
        assertEquals("11444777000161", doc.documentNumber());
    }

    @Test
    @DisplayName("Should create valid CNPJ with formatting")
    void shouldCreateValidCnpjWithFormatting() {
        Document doc = new Document(DocumentType.CNPJ, "11.444.777/0001-61");

        assertEquals(DocumentType.CNPJ, doc.documentType());
        assertEquals("11.444.777/0001-61", doc.documentNumber());
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid CNPJ checksums")
    @ValueSource(strings = {"11111111111111", "12345678000199", "11444777000162"})
    void shouldRejectInvalidCnpjChecksums(String cnpj) {
        assertThrows(IllegalArgumentException.class, () -> new Document(DocumentType.CNPJ, cnpj));
    }

    // ========== CNH Tests ==========

    @Test
    @DisplayName("Should create CNH with valid length")
    void shouldCreateValidCnh() {
        Document doc = new Document(DocumentType.CNH, "12345678901");

        assertEquals(DocumentType.CNH, doc.documentType());
        assertEquals("12345678901", doc.documentNumber());
    }

    // ========== Null / Blank Rejection ==========

    @Test
    @DisplayName("Should reject null DocumentType")
    void shouldRejectNullDocumentType() {
        assertThrows(IllegalArgumentException.class, () -> new Document(null, "52998224725"));
    }

    @Test
    @DisplayName("Should reject null document number")
    void shouldRejectNullDocumentNumber() {
        assertThrows(IllegalArgumentException.class, () -> new Document(DocumentType.CPF, null));
    }

    @Test
    @DisplayName("Should reject blank document number")
    void shouldRejectBlankDocumentNumber() {
        assertThrows(IllegalArgumentException.class, () -> new Document(DocumentType.CPF, ""));
        assertThrows(IllegalArgumentException.class, () -> new Document(DocumentType.CPF, "   "));
    }

    // ========== Factory Method ==========

    @Test
    @DisplayName("Should create equal Documents via factory and constructor")
    void factoryShouldProduceEqualDocuments() {
        Document fromConstructor = new Document(DocumentType.CPF, "52998224725");
        Document fromFactory = Document.from(DocumentType.CPF, "52998224725");

        assertEquals(fromConstructor, fromFactory);
        assertEquals(fromConstructor.hashCode(), fromFactory.hashCode());
    }

    // ========== PII Masking ==========

    @Test
    @DisplayName("Should mask CPF in toString")
    void shouldMaskCpfInToString() {
        Document doc = new Document(DocumentType.CPF, "529.982.247-25");
        String str = doc.toString();

        assertTrue(str.contains("CPF"));
        assertFalse(str.contains("52998224725"), "toString should not expose raw CPF digits");
        assertFalse(str.contains("529.982.247-25"), "toString should not expose formatted CPF");
    }

    @Test
    @DisplayName("Should mask CNPJ in toString")
    void shouldMaskCnpjInToString() {
        Document doc = new Document(DocumentType.CNPJ, "11.444.777/0001-61");
        String str = doc.toString();

        assertTrue(str.contains("CNPJ"));
        assertFalse(str.contains("11444777000161"), "toString should not expose raw CNPJ digits");
        assertFalse(str.contains("11.444.777/0001-61"), "toString should not expose formatted CNPJ");
    }

    @Test
    @DisplayName("Should mask CNH in toString")
    void shouldMaskCnhInToString() {
        Document doc = new Document(DocumentType.CNH, "12345678901");
        String str = doc.toString();

        assertTrue(str.contains("CNH"));
        assertFalse(str.contains("12345678901"), "toString should not expose raw CNH digits");
    }

    // ========== Edge Cases ==========

    @ParameterizedTest
    @DisplayName("Should reject CPF with wrong length after removing non-digits")
    @CsvSource({
            "5299822472, CPF",
            "529982247255, CPF",
            "1144477700016, CNPJ",
            "114447770001611, CNPJ"
    })
    void shouldRejectWrongLength(String number, DocumentType type) {
        assertThrows(IllegalArgumentException.class, () -> new Document(type, number));
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        Document doc = new Document(DocumentType.CPF, "52998224725");

        // Records are immutable by construction; attempting to modify via reflection is not needed.
        // This test documents the expectation.
        assertEquals("52998224725", doc.documentNumber());
        assertEquals(DocumentType.CPF, doc.documentType());
    }

    @Test
    @DisplayName("Should implement value-based equality")
    void shouldHaveValueBasedEquality() {
        Document doc1 = new Document(DocumentType.CPF, "52998224725");
        Document doc2 = new Document(DocumentType.CPF, "52998224725");
        Document doc3 = new Document(DocumentType.CPF, "529.982.247-25");
        Document doc4 = new Document(DocumentType.CNPJ, "11444777000161");

        assertEquals(doc1, doc2);
        assertEquals(doc1.hashCode(), doc2.hashCode());
        assertEquals(doc1, doc3); // same digits, different formatting
        assertNotEquals(doc1, doc4);
    }
}
