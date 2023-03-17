package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.valuation.persistence.entities.EndOfDayPricesEntity;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ValueCurveRepository  extends ReactiveCrudRepository<ValueCurveEntity, String> {
    Mono<ValueCurveEntity> findByInstrumentBusinesskey(String businesskey);
    Flux<ValueCurveEntity> findByInstrumentBusinesskeyIn(Iterable<String> instrumentBusinesskeyIterable);
}
