package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;


public class CashAccValueHandler extends AbsValueHandler {


    public CashAccValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }


    @Override
    public Mono<Void> calcValueCurve(){
        return  dataReader.findAllCashflow4Instrument(instrument.getBusinesskey())
                .switchIfEmpty(Flux.just(new Cashflow("empty Cashflow", LocalDate.now(), instrument.getBusinesskey(), 0.0)))
                .collectList().flatMap(this::calcCurveFromCashflows)
                .flatMap(this::finalizeAndSendValueCurveCalculatedEvent);
    }

    protected Mono<Void> finalizeAndSendValueCurveCalculatedEvent(TreeMap<LocalDate, Double> valueCurve) {
        if(instrument.getInstrumentType().equals(InstrumentType.BUDGET)){
            return Flux.just(ValuationType.MARKETVALUE, 
                        ValuationType.STATIC, 
                        ValuationType.PRUDENT)
                .flatMap(valueType -> 
                    addLinkedInstrumentValues(valueCurve, valueType)
                    .flatMap(finalCurve -> {
                        return sendValueCurveCalculatedEvent(finalCurve, valueType);
                    })
            ).then();
        }
        
        return Mono.just(valueCurve).flatMap(this::sendValueCurveCalculatedEvent);

    }

    Mono<TreeMap<LocalDate, Double>> addLinkedInstrumentValues(TreeMap<LocalDate, Double> valueCurve, ValuationType valuationType) {
        return dataReader.findByValueBudget(instrument.getBusinesskey())
                .flatMap(i->{
                    return getValueCurve4ValuationType(i, valuationType);
                })
                .collectList()
                .flatMap(c->{
                        var curves = extractMapsFromValueCurve(c);
                        curves.add(valueCurve);
                        return Mono.just(curves);
                })
                .flatMap(this::getCombinedValueCurve)
                .switchIfEmpty(Mono.just(valueCurve));
    }

}
