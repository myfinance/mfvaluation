package de.hf.myfinance.valuation.persistence;

import java.util.List;

import de.hf.myfinance.restmodel.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataReader {
    Mono<Instrument> findInstrumentByBusinesskey(String businesskey);
    Flux<Instrument> findAllInstruments();
    Flux<Instrument> findInstrumentByParentBusinesskeyAndInstrumentType(String parentBusinesskey, InstrumentType instrumentType);
    Flux<Instrument> findInstrumentByValueBudget(String valueBudget);
    Flux<Instrument> findInstrumentByParentBusinesskey(String parentBusinesskey);

    Flux<Cashflow> findAllCashflow4Instrument(String businesskey);
    Flux<Cashflow> findAllCashflows();

    Flux<Trade> findTradesByKey(String depotBusinessKey, String securityBusinessKey);
    Flux<Trade> findAllTrades();

    Mono<ValueCurve> findValueCurve(String businesskey, ValuationType valuationType);
    Flux<ValueCurve> findMarketValueCurvesByBusinesskeyIn(Iterable<String> businesskeyIterable);

    Mono<ValueCurve> findPositonByKey(String depotBusinessKey, String securityBusinessKey);
    Flux<ValueCurve> findPositonValueByDepotKey(String depotBusinessKey, ValuationType valuationType);
    Flux<ValueCurve> findPositonBySecurityKey(String securityKey);
    Flux<ValueCurve> findAllPostions4Depots(List<String> depots);
    Flux<ValueCurve> findAllPostionValues4Depots(List<String> depots);
    Flux<ValueCurve> findAllPostionValues();
    Flux<ValueCurve> findAllPostions();

    Mono<EndOfDayPrices> findPricesByInstrumentBusinesskey(String businesskey);
}
