package checkout.boundedcontext.payment.domain;

import checkout.boundedcontext.payment.domain.event.PaymentDenied;
import checkout.boundedcontext.payment.domain.event.PaymentInitiated;
import checkout.boundedcontext.payment.domain.event.PaymentProcessed;
import checkout.boundedcontext.payment.domain.port.CreateBillingDomainRequest;
import checkout.boundedcontext.payment.domain.port.ExternalBillingResponse;
import checkout.boundedcontext.payment.domain.port.PaymentProviderPort;
import checkout.boundedcontext.payment.domain.repository.PaymentIntentRepository;
import checkout.common.domain.event.DomainEvent;
import checkout.common.domain.event.EventBus;
import checkout.common.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentDomainServiceTest {

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private PaymentProviderPort paymentProviderPort;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private PaymentDomainService paymentDomainService;

    private IdempotencyKey idempotencyKey;
    private CreateBillingDomainRequest request;
    private List<ProductItem> products;

    @BeforeEach
    void setUp() {
        idempotencyKey = IdempotencyKey.of("test-key");
        products = List.of(
                new ProductItem("prod-1", "Product 1", "Description 1", 2, 1000),
                new ProductItem("prod-2", "Product 2", "Description 2", 3, 500)
        );
        request = new CreateBillingDomainRequest(
                List.of(PaymentMethod.PIX),
                null,
                null,
                products,
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("initiatePayment - new idempotency key: saves new PaymentIntent, publishes events, calls PSP, processes response, saves processed intent, publishes processed events")
    void initiatePaymentNewIdempotencyKey() {
        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.assignId(PaymentIntentId.of(1L));
            }
            return Mono.just(intent);
        });

        ExternalBillingResponse pspResponse = new ExternalBillingResponse(
                "billing-123", "PROCESSING", null, null, null, 3500
        );
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.just(pspResponse));
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .assertNext(intent -> {
                    assertEquals(PaymentStatus.PROCESSING, intent.getStatus());
                    assertEquals(PaymentIntentId.of(1L), intent.getId());
                    assertNotNull(intent.getTransactions());
                    assertEquals(1, intent.getTransactions().size());
                    assertEquals(ExternalBillingId.of("billing-123"), intent.getTransactions().get(0).getExternalId());
                    assertEquals("PROCESSING", intent.getTransactions().get(0).getProviderStatus());
                })
                .verifyComplete();

        verify(paymentIntentRepository, times(2)).save(any(PaymentIntent.class));
        verify(paymentProviderPort).createBilling(request);
        verify(eventBus, times(2)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("initiatePayment - duplicate idempotency key: returns existing intent without creating new one, still calls PSP")
    void initiatePaymentDuplicateIdempotencyKey() {
        PaymentIntent existingIntent = PaymentIntent.initiate(
                idempotencyKey, Money.ofCents(3500), List.of(PaymentMethod.PIX), null, products
        );
        existingIntent.assignId(PaymentIntentId.of(1L));
        existingIntent.clearDomainEvents();

        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(true));
        when(paymentIntentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(existingIntent));

        ExternalBillingResponse pspResponse = new ExternalBillingResponse(
                "billing-456", "PROCESSING", null, null, null, 3500
        );
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.just(pspResponse));
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        );
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .assertNext(intent -> {
                    assertEquals(PaymentStatus.PROCESSING, intent.getStatus());
                    assertEquals(PaymentIntentId.of(1L), intent.getId());
                })
                .verifyComplete();

        verify(paymentIntentRepository, never()).save(argThat(intent -> intent.getId() == null));
        verify(paymentIntentRepository).findByIdempotencyKey(idempotencyKey);
        verify(paymentProviderPort).createBilling(request);
    }

    @Test
    @DisplayName("initiatePayment - PSP success: verify PaymentIntent transitions to PROCESSING and gets an ExternalBillingId")
    void initiatePaymentPspSuccess() {
        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.assignId(PaymentIntentId.of(1L));
            }
            return Mono.just(intent);
        });

        ExternalBillingResponse pspResponse = new ExternalBillingResponse(
                "ext-billing-789", "PROCESSING", null, null, null, 3500
        );
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.just(pspResponse));
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .assertNext(intent -> {
                    assertEquals(PaymentStatus.PROCESSING, intent.getStatus());
                    assertEquals(1, intent.getTransactions().size());
                    assertEquals(ExternalBillingId.of("ext-billing-789"), intent.getTransactions().get(0).getExternalId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("initiatePayment - PSP failure: verify PaymentIntent is denied with 'PSP processing error' reason")
    void initiatePaymentPspFailure() {
        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.assignId(PaymentIntentId.of(1L));
            }
            return Mono.just(intent);
        });
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.error(new RuntimeException("PSP timeout")));
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .assertNext(intent -> {
                    assertEquals(PaymentStatus.DENIED, intent.getStatus());
                    assertEquals("PSP processing error", intent.getFailureReason());
                })
                .verifyComplete();

        verify(paymentIntentRepository, times(2)).save(any(PaymentIntent.class));
    }

    @Test
    @DisplayName("initiatePayment - event publishing: verify EventBus.publish is called for each domain event")
    void initiatePaymentEventPublishing() {
        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.assignId(PaymentIntentId.of(1L));
            }
            return Mono.just(intent);
        });

        ExternalBillingResponse pspResponse = new ExternalBillingResponse(
                "billing-abc", "PROCESSING", null, null, null, 3500
        );
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.just(pspResponse));
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventBus, times(2)).publish(eventCaptor.capture());

        List<DomainEvent> capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());
        assertInstanceOf(PaymentInitiated.class, capturedEvents.get(0));
        assertInstanceOf(PaymentProcessed.class, capturedEvents.get(1));
    }

    @Test
    @DisplayName("initiatePayment - no events after publish: verify getDomainEvents() is empty after publishEventsAndClear")
    void initiatePaymentNoEventsAfterPublish() {
        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));
        when(paymentIntentRepository.save(any(PaymentIntent.class))).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.assignId(PaymentIntentId.of(1L));
            }
            return Mono.just(intent);
        });

        ExternalBillingResponse pspResponse = new ExternalBillingResponse(
                "billing-def", "PROCESSING", null, null, null, 3500
        );
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.just(pspResponse));
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .assertNext(intent -> {
                    assertTrue(intent.getDomainEvents().isEmpty(),
                            "Domain events should be empty after publishEventsAndClear");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getPaymentIntent - delegates to repository: verify findById is called")
    void getPaymentIntentDelegatesToRepository() {
        PaymentIntentId id = PaymentIntentId.of(1L);
        PaymentIntent intent = PaymentIntent.initiate(
                idempotencyKey, Money.ofCents(1000), List.of(PaymentMethod.PIX), null, products
        );
        intent.assignId(id);

        when(paymentIntentRepository.findById(id)).thenReturn(Mono.just(intent));

        StepVerifier.create(paymentDomainService.getPaymentIntent(id))
                .expectNext(intent)
                .verifyComplete();

        verify(paymentIntentRepository).findById(id);
    }

    @Test
    @DisplayName("getPaymentIntentWithTransactions - delegates to repository: verify findByIdWithTransactions is called")
    void getPaymentIntentWithTransactionsDelegatesToRepository() {
        PaymentIntentId id = PaymentIntentId.of(1L);
        PaymentIntent intent = PaymentIntent.initiate(
                idempotencyKey, Money.ofCents(1000), List.of(PaymentMethod.PIX), null, products
        );
        intent.assignId(id);

        when(paymentIntentRepository.findByIdWithTransactions(id)).thenReturn(Mono.just(intent));

        StepVerifier.create(paymentDomainService.getPaymentIntentWithTransactions(id))
                .expectNext(intent)
                .verifyComplete();

        verify(paymentIntentRepository).findByIdWithTransactions(id);
    }

    @Test
    @DisplayName("calculateTotalFromProducts - multiple products: verify total amount calculation is correct")
    void calculateTotalFromProductsMultipleProducts() {
        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));

        ArgumentCaptor<PaymentIntent> intentCaptor = ArgumentCaptor.forClass(PaymentIntent.class);
        when(paymentIntentRepository.save(intentCaptor.capture())).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.assignId(PaymentIntentId.of(1L));
            }
            return Mono.just(intent);
        });

        ExternalBillingResponse pspResponse = new ExternalBillingResponse(
                "billing-total", "PROCESSING", null, null, null, 3500
        );
        when(paymentProviderPort.createBilling(request)).thenReturn(Mono.just(pspResponse));
        when(eventBus.publish(any(DomainEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(paymentDomainService.initiatePayment(request, idempotencyKey))
                .expectNextCount(1)
                .verifyComplete();

        PaymentIntent capturedIntent = intentCaptor.getAllValues().get(0);
        assertEquals(3500, capturedIntent.getAmount().toCents());
    }

    @Test
    @DisplayName("calculateTotalFromProducts - overflow: verify ArithmeticException is thrown when total exceeds Integer.MAX_VALUE")
    void calculateTotalFromProductsOverflow() {
        List<ProductItem> overflowProducts = List.of(
                new ProductItem("prod-overflow", "Overflow Product", "Description", 2, Integer.MAX_VALUE)
        );
        CreateBillingDomainRequest overflowRequest = new CreateBillingDomainRequest(
                List.of(PaymentMethod.PIX),
                null,
                null,
                overflowProducts,
                null,
                null,
                null
        );

        when(paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(Mono.just(false));

        StepVerifier.create(paymentDomainService.initiatePayment(overflowRequest, idempotencyKey))
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(ArithmeticException.class, error);
                    assertEquals("Total amount exceeds maximum allowed value", error.getMessage());
                })
                .verify();
    }
}
