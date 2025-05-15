package de.hf.myfinance.valuation.persistence.repositories;

import java.util.List;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import de.hf.myfinance.valuation.persistence.entities.PositionValueEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionKey;

public interface PositionValueRepository extends ReactiveCrudRepository<PositionValueEntity, String>{
    Flux<PositionValueEntity> findByPositionKeyDepotBusinessKey(String depotBusinessKey);
    Mono<PositionValueEntity> findByPositionKey(PositionKey positionKey);
    Mono<Long> deleteByPositionKey(PositionKey positionKey);
    Flux<PositionValueEntity> findByPositionKey_DepotBusinessKeyIn(List<String> depotBusinessKeys);
}