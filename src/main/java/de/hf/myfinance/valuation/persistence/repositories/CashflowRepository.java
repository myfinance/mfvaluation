package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.valuation.persistence.entities.CashflowEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CashflowRepository extends ReactiveCrudRepository<CashflowEntity, String> {
    Flux<CashflowEntity> findByInstrumentBusinesskey(String businesskey);
}

