package checkout.boundedcontext.payment.domain;

public record ProductItem(String externalId, String name, String description, int quantity, int priceInCents) {
    public ProductItem {
        if (externalId == null || externalId.isBlank()) throw new IllegalArgumentException("externalId is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (priceInCents < 100) throw new IllegalArgumentException("price must be >= 100 cents (R$1.00)");
    }
}
