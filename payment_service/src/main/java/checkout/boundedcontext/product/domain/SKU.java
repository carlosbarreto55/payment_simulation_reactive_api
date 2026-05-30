package checkout.boundedcontext.product.domain;

public record SKU(String value) {

    public SKU {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (value.length() < 3 || value.length() > 50) {
            throw new IllegalArgumentException("SKU length must be 3-50");
        }
        if (!value.matches("^[A-Z0-9]+$")) {
            throw new IllegalArgumentException("SKU must be uppercase alphanumeric");
        }
    }

    public static SKU of(String value) {
        return new SKU(value);
    }
}
