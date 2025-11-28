package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveKey;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ValueCurveRepository  extends ReactiveCrudRepository<ValueCurveEntity, ValueCurveKey> {
    @Query("{ 'valueCurveKey.instrumentBusinesskey' : ?0}")
    Flux<ValueCurveEntity> findByInstrumentBusinesskey(String businesskey);
    @Query("{ 'valueCurveKey.instrumentBusinesskey' : { $in: ?0 }}")
    Flux<ValueCurveEntity> findByInstrumentBusinesskeyIn(Iterable<String> instrumentBusinesskeyIterable);

    Mono<ValueCurveEntity> findByValueCurveKey(ValueCurveKey valueCurveKey);
}
