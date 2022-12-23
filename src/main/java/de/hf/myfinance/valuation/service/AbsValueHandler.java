package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public abstract class AbsValueHandler implements ValueHandler {

    protected Instrument instrument;
    protected final DataReader dataReader;
    protected final ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler;
    protected final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="ValueHandler_User_Event";

    protected AbsValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        this.instrument = instrument;
        this.dataReader = dataReader;
        this.auditService = auditService;
        this.valueCurveCalculatedEventHandler = valueCurveCalculatedEventHandler;
    }

    protected Mono<TreeMap<LocalDate, Double>> createZeroCurve() {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();
        valueCurve.put(LocalDate.now(), 0.0);
        return Mono.just(valueCurve);
    }

    protected Mono<Void> sendValueCurveCalculatedEvent(TreeMap<LocalDate, Double> valueCurve) {
        auditService.saveMessage(" new valuecurve calculated for instrument: " + instrument.getBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
        var valueCurveObject = new ValueCurve(instrument.getBusinesskey());
        valueCurveObject.setValueCurve(valueCurve);
        valueCurveObject.setParentBusinesskey(instrument.getParentBusinesskey());
        valueCurveCalculatedEventHandler.sendValueCurveCalculatedEvent(instrument.getBusinesskey(), valueCurveObject);
        return Mono.just("").then();
    }

    protected LocalDate calcCurveStartDate(List<ValueCurve> valueCurves) {
        LocalDate startDate = LocalDate.now();
        for (var childValueCurve : valueCurves) {
            LocalDate minDate = childValueCurve.getValueCurve().firstKey();
            if(minDate.isBefore(startDate)) {
                startDate = minDate;
            }
        }
        return startDate;
    }
}