package checkout.common.domain.event;

import reactor.core.publisher.Mono;

public interface EventBus {
    Mono<Void> publish(DomainEvent event);
}
