package checkout.domain.payment.controller;

import checkout.domain.payment.dto.CreateBillingRequestDto;
import checkout.domain.payment.dto.CreateBillingResponseDto;
import checkout.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/billing")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateBillingResponseDto> createBilling(
            @Valid @RequestBody CreateBillingRequestDto request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        return paymentService.createBilling(request, idempotencyKey);
    }
}

