package checkout.boundedcontext.customer.domain;

import checkout.common.enums.DocumentType;

import java.util.Objects;

public record Document(DocumentType documentType, String documentNumber) {

    public Document {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentType is required");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("Document number is required");
        }
        String digits = documentNumber.replaceAll("\\D", "");
        switch (documentType) {
            case CPF -> validateCpf(digits);
            case CNPJ -> validateCnpj(digits);
            case CNH -> validateCnh(digits);
        }
    }

    public static Document from(DocumentType type, String number) {
        return new Document(type, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return documentType == document.documentType &&
                Objects.equals(documentNumber.replaceAll("\\D", ""), document.documentNumber.replaceAll("\\D", ""));
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentType, documentNumber.replaceAll("\\D", ""));
    }

    @Override
    public String toString() {
        return "Document{" +
                "type=" + documentType +
                ", number=" + mask(documentNumber) +
                '}';
    }

    private static String mask(String number) {
        StringBuilder sb = new StringBuilder();
        int digitCount = 0;
        for (char c : number.toCharArray()) {
            if (Character.isDigit(c)) {
                digitCount++;
            }
        }
        int visibleDigits = 2;
        int maskedSoFar = 0;
        for (char c : number.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digitCount - maskedSoFar > visibleDigits) {
                    sb.append('*');
                    maskedSoFar++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void validateCpf(String digits) {
        if (digits.length() != 11) {
            throw new IllegalArgumentException("Invalid CPF length");
        }
        if (isAllSameDigit(digits)) {
            throw new IllegalArgumentException("Invalid CPF");
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * (10 - i);
        }
        int remainder = sum % 11;
        int firstCheckDigit = (remainder < 2) ? 0 : 11 - remainder;
        if (firstCheckDigit != digits.charAt(9) - '0') {
            throw new IllegalArgumentException("Invalid CPF");
        }
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (digits.charAt(i) - '0') * (11 - i);
        }
        remainder = sum % 11;
        int secondCheckDigit = (remainder < 2) ? 0 : 11 - remainder;
        if (secondCheckDigit != digits.charAt(10) - '0') {
            throw new IllegalArgumentException("Invalid CPF");
        }
    }

    private static void validateCnpj(String digits) {
        if (digits.length() != 14) {
            throw new IllegalArgumentException("Invalid CNPJ length");
        }
        if (isAllSameDigit(digits)) {
            throw new IllegalArgumentException("Invalid CNPJ");
        }
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (digits.charAt(i) - '0') * weights1[i];
        }
        int remainder = sum % 11;
        int firstCheckDigit = (remainder < 2) ? 0 : 11 - remainder;
        if (firstCheckDigit != digits.charAt(12) - '0') {
            throw new IllegalArgumentException("Invalid CNPJ");
        }
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += (digits.charAt(i) - '0') * weights2[i];
        }
        remainder = sum % 11;
        int secondCheckDigit = (remainder < 2) ? 0 : 11 - remainder;
        if (secondCheckDigit != digits.charAt(13) - '0') {
            throw new IllegalArgumentException("Invalid CNPJ");
        }
    }

    private static void validateCnh(String digits) {
        if (digits.length() != 11) {
            throw new IllegalArgumentException("Invalid CNH length");
        }
    }

    private static boolean isAllSameDigit(String digits) {
        char first = digits.charAt(0);
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }
}
