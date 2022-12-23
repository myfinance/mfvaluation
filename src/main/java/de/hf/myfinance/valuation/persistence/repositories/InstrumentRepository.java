package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.valuation.persistence.entities.InstrumentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface InstrumentRepository extends ReactiveCrudRepository<InstrumentEntity, String> {
    Mono<InstrumentEntity> findByBusinesskey(String businesskey);
    Flux<InstrumentEntity> findByParentBusinesskey(String parentBusinesskey);
    Flux<InstrumentEntity> findByParentBusinesskeyAndInstrumentType(String parentBusinesskey, InstrumentType instrumentType);
}
