package de.hf.myfinance.valuation.persistence.repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import de.hf.myfinance.valuation.persistence.entities.PositionKey;
import de.hf.myfinance.valuation.persistence.entities.TradeEntity;
import reactor.core.publisher.Flux;

public interface TradeRepository  extends ReactiveCrudRepository<TradeEntity, String> {
    Flux<TradeEntity> findByPositionKey(PositionKey positionKey);
}