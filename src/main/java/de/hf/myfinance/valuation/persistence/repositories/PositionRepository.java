package de.hf.myfinance.valuation.persistence.repositories;

import java.util.List;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionKey;

public interface PositionRepository  extends ReactiveCrudRepository<PositionEntity, String> {
    Mono<PositionEntity> findByPositionKey(PositionKey positionKey);
    Mono<Long> deleteByPositionKey(PositionKey positionKey);

    Flux<PositionEntity> findByPositionKey_DepotBusinessKeyIn(List<String> depotBusinessKeys);
}