package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.TreeMap;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

public class LifeInsuranceValueHandler   extends AbsValueHandler{

    public LifeInsuranceValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){

        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        var valueCurve = new TreeMap<LocalDate, Double>();


        return sendValueCurveCalculatedEvent(valueCurve);
    }
}
