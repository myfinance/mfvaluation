package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.valuation.persistence.entities.EndOfDayPricesEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EndOfDayPricesRepository extends ReactiveCrudRepository<EndOfDayPricesEntity, String> {
    Mono<EndOfDayPricesEntity> findByInstrumentBusinesskey(String instrumentBusinesskey);
    Mono<Long> deleteByInstrumentBusinesskey(String instrumentBusinesskey);
}
