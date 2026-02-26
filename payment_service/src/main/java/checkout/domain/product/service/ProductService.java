package checkout.domain.product.service;

import checkout.common.exception.ResourceNotFoundException;
import checkout.domain.product.dto.CreateProductRequestDto;
import checkout.domain.product.dto.ProductResponseDto;
import checkout.domain.product.dto.UpdateProductRequestDto;
import checkout.domain.product.entity.Product;
import checkout.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Mono<ProductResponseDto> createProduct(CreateProductRequestDto request) {
        return productRepository.existsBySku(request.getSku())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.<Product>error(new IllegalArgumentException(
                                "Product with SKU " + request.getSku() + " already exists"));
                    }

                    Product product = Product.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .price(request.getPrice())
                            .currency("BRL")
                            .sku(request.getSku())
                            .stockQuantity(request.getStockQuantity())
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return productRepository.save(product);
                })
                .map(ProductResponseDto::fromEntity)
                .doOnSuccess(dto -> log.info("Product created successfully. id={}, sku={}", dto.getId(), dto.getSku()));
    }

    public Mono<ProductResponseDto> getProductById(Long id) {
        return findProductOrError(id)
                .map(ProductResponseDto::fromEntity);
    }

    public Mono<ProductResponseDto> getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product with sku=" + sku)))
                .map(ProductResponseDto::fromEntity);
    }

    public Flux<ProductResponseDto> getAllActiveProducts() {
        return productRepository.findByActiveTrue()
                .map(ProductResponseDto::fromEntity);
    }

    public Flux<ProductResponseDto> getAvailableProducts() {
        return productRepository.findAvailableProducts()
                .map(ProductResponseDto::fromEntity);
    }

    @Transactional
    public Mono<ProductResponseDto> updateProduct(Long id, UpdateProductRequestDto request) {
        return findProductOrError(id)
                .flatMap(product -> {
                    if (request.getName() != null) product.setName(request.getName());
                    if (request.getDescription() != null) product.setDescription(request.getDescription());
                    if (request.getPrice() != null) product.setPrice(request.getPrice());
                    if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
                    if (request.getActive() != null) product.setActive(request.getActive());
                    product.setUpdatedAt(LocalDateTime.now());

                    return productRepository.save(product);
                })
                .map(ProductResponseDto::fromEntity)
                .doOnSuccess(dto -> log.info("Product updated successfully. id={}", dto.getId()));
    }

    @Transactional
    public Mono<Void> deactivateProduct(Long id) {
        return findProductOrError(id)
                .flatMap(product -> {
                    product.setActive(false);
                    product.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(product);
                })
                .doOnSuccess(p -> log.info("Product deactivated. id={}", id))
                .then();
    }

    @Transactional
    public Mono<ProductResponseDto> decreaseStock(Long productId, int quantity) {
        return findProductOrError(productId)
                .flatMap(product -> {
                    product.decreaseStock(quantity);
                    product.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(product);
                })
                .map(ProductResponseDto::fromEntity)
                .doOnSuccess(dto -> log.info("Stock decreased for product. id={}, quantity={}", productId, quantity));
    }

    @Transactional
    public Mono<ProductResponseDto> increaseStock(Long productId, int quantity) {
        return findProductOrError(productId)
                .flatMap(product -> {
                    product.increaseStock(quantity);
                    product.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(product);
                })
                .map(ProductResponseDto::fromEntity)
                .doOnSuccess(dto -> log.info("Stock increased for product. id={}, quantity={}", productId, quantity));
    }

    private Mono<Product> findProductOrError(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product with id=" + id)));
    }
}

