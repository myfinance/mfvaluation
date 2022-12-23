package de.hf.myfinance.valuation.persistence;

import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.entities.InstrumentEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DataReader {
    Mono<Instrument> findByBusinesskey(String businesskey);
    Flux<Instrument> findAll();
    Flux<Cashflow> findAllCashflow4Instrument(String businesskey);
    Flux<Instrument> findByParentBusinesskey(String parentBusinesskey);
    Mono<ValueCurve> findValueCurveByInstrumentBusinesskey(String businesskey);
    Flux<Instrument> findByParentBusinesskeyAndInstrumentType(String parentBusinesskey, InstrumentType instrumentType);
}
