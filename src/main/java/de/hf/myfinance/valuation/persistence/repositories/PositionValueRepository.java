package de.hf.myfinance.valuation.persistence.repositories;

import java.util.List;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.valuation.persistence.entities.PositionValueEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionValueKey;

import org.springframework.data.mongodb.repository.Query;

public interface PositionValueRepository extends ReactiveCrudRepository<PositionValueEntity, PositionValueKey>{
    @Query("{ 'positionValueKey.depotBusinessKey' : ?0, 'positionValueKey.valuationType' : ?1 }")
    Flux<PositionValueEntity> findByDepotBusinessKeyAndValuationType(String depotBusinessKey, ValuationType valuationType);
    Mono<PositionValueEntity> findByPositionValueKey(PositionValueKey positionValueKey);
    Mono<Long> deleteByPositionValueKey(PositionValueKey positionValueKey);
    @Query("{ 'positionValueKey.depotBusinessKey' : { $in: ?0 }, 'positionValueKey.valuationType' : ?1 }")
    Flux<PositionValueEntity> findByDepotBusinessKeyInAndValuationType(List<String> depotBusinessKeys, ValuationType valuationType);
    @Query("{ 'positionValueKey.valuationType' : ?0 }")
    Flux<PositionValueEntity> findByValuationType(ValuationType valuationType);
}