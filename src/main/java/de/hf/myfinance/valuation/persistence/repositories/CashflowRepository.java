package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.valuation.persistence.entities.CashflowEntity;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CashflowRepository extends ReactiveCrudRepository<CashflowEntity, String> {
    Flux<CashflowEntity> findByInstrumentBusinesskey(String businesskey);
    @Query("{ 'instrumentBusinesskey' : { $in: ?0 } }")
    Flux<CashflowEntity> findByinstrumentBusinesskeyIn(List<String> instrumentBusinessKeys);
}

