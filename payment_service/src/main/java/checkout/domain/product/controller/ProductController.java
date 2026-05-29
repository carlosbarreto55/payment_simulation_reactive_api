package checkout.domain.product.controller;

import checkout.domain.product.dto.CreateProductRequestDto;
import checkout.domain.product.dto.ProductResponseDto;
import checkout.domain.product.dto.UpdateProductRequestDto;
import checkout.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponseDto> createProduct(@Valid @RequestBody CreateProductRequestDto request) {
        return productService.createProduct(request);
    }

    @GetMapping("/{id}")
    public Mono<ProductResponseDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/sku/{sku}")
    public Mono<ProductResponseDto> getProductBySku(@PathVariable String sku) {
        return productService.getProductBySku(sku);
    }

    @GetMapping
    public Flux<ProductResponseDto> getAllActiveProducts() {
        return productService.getAllActiveProducts();
    }

    @GetMapping("/available")
    public Flux<ProductResponseDto> getAvailableProducts() {
        return productService.getAvailableProducts();
    }

    @PutMapping("/{id}")
    public Mono<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequestDto request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deactivateProduct(@PathVariable Long id) {
        return productService.deactivateProduct(id);
    }

    @PostMapping("/{id}/stock/decrease")
    public Mono<ProductResponseDto> decreaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return productService.decreaseStock(id, quantity);
    }

    @PostMapping("/{id}/stock/increase")
    public Mono<ProductResponseDto> increaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return productService.increaseStock(id, quantity);
    }
}

