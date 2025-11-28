package de.hf.myfinance.valuation.persistence;

import java.util.List;

import de.hf.myfinance.restmodel.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataReader {
    Mono<Instrument> findByBusinesskey(String businesskey);
    Flux<Instrument> findAll();
    Flux<Cashflow> findAllCashflow4Instrument(String businesskey);
    Flux<Instrument> findByParentBusinesskey(String parentBusinesskey);
    Mono<ValueCurve> findValueCurve(String businesskey, ValuationType valuationType);
    Flux<Instrument> findByParentBusinesskeyAndInstrumentType(String parentBusinesskey, InstrumentType instrumentType);
    Mono<EndOfDayPrices> findPricesByInstrumentBusinesskey(String businesskey);
    Flux<ValueCurve> findMarketValueCurvesByBusinesskeyIn(Iterable<String> businesskeyIterable);
    Mono<ValueCurve> findPositonByKey(String depotBusinessKey, String securityBusinessKey);
    Flux<Trade> findTradesByKey(String depotBusinessKey, String securityBusinessKey);
    Flux<ValueCurve> findPositonValueByDepotKey(String depotBusinessKey, ValuationType valuationType);
    Flux<ValueCurve> findPositonBySecurityKey(String securityKey);
    Flux<Instrument> findByValueBudget(String valueBudget);
    Flux<ValueCurve> findAllPostions(List<String> depots);
    Flux<ValueCurve> findAllPostionValues(List<String> depots);
}
