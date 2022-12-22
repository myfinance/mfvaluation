package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

public class SecurityValueHandler extends AbsValueHandler{



    public SecurityValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }


    public Mono<Void> calcValueCurve(){
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();

        return Mono.just("").then();
    }

}
