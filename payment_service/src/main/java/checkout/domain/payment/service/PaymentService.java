package checkout.domain.payment.service;

import checkout.domain.client.PaymentServiceClient;
import checkout.domain.payment.dto.CreateBillingRequestDto;
import checkout.domain.payment.mapper.PaymentMapper;
import checkout.domain.payment.repository.PaymentIntentRepository;
import checkout.domain.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentServiceClient paymentServiceClient;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMapper paymentMapper;


    public Mono<CreateBillingRequestDto> createBilling (CreateBillingRequestDto request){




    }




}
