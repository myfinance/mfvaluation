package de.hf.myfinance.valuation.persistence.repositories;

import java.util.List;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import de.hf.myfinance.valuation.persistence.entities.PositionValueEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionValueKey;
import de.hf.myfinance.restmodel.ValuationType;

public interface PositionValueRepository extends ReactiveCrudRepository<PositionValueEntity, String>{
    Flux<PositionValueEntity> findByPositionValueKeyDepotBusinessKey(String depotBusinessKey, ValuationType valuationType);
    Mono<PositionValueEntity> findByPositionValueKey(PositionValueKey positionValueKey);
    Mono<Long> deleteByPositionValueKey(PositionValueKey positionValueKey);
    Flux<PositionValueEntity> findByPositionValueKey_DepotBusinessKeyIn(List<String> depotBusinessKeys, ValuationType valuationType);
}