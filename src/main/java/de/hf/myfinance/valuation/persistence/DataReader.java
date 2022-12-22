package de.hf.myfinance.valuation.persistence;

import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataReader {
    Mono<Instrument> findByBusinesskey(String businesskey);
    Flux<Instrument> findAll();
    Flux<Cashflow> findAllCashflow4Instrument(String businesskey);
}
