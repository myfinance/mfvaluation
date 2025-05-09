package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public abstract class AbsValueHandler extends AbsCurveHandler implements ValueHandler {

    protected Instrument instrument;
    protected final ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler;
    protected static final String AUDIT_MSG_TYPE="ValueHandler_User_Event";

    protected AbsValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(dataReader, auditService);
        this.instrument = instrument;
        this.valueCurveCalculatedEventHandler = valueCurveCalculatedEventHandler;
    }

    protected Mono<Void> sendValueCurveCalculatedEvent(TreeMap<LocalDate, Double> valueCurve) {
        auditService.saveMessage(" new valuecurve calculated for instrument: " + instrument.getBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
        var valueCurveObject = new ValueCurve(instrument.getBusinesskey());
        valueCurveObject.setValueCurve(valueCurve);
        valueCurveObject.setParentBusinesskey(instrument.getParentBusinesskey());
        if(instrument.getAdditionalProperties()!= null && instrument.getAdditionalProperties().containsKey(AdditionalProperties.VALUEBUDGETID)) {
            valueCurveObject.setLinkedInstrumentKey(instrument.getAdditionalProperties().get(AdditionalProperties.VALUEBUDGETID));
        }
        valueCurveCalculatedEventHandler.sendValueCurveCalculatedEvent(instrument.getBusinesskey(), valueCurveObject);
        return Mono.just("").then();
    }

    protected Mono<TreeMap<LocalDate, Double>> calcCurveFromCashflows(List<Cashflow> cashflows) {
        return buildCurveFromValueMap(convert2CashflowPerDayMap(cashflows));
    }

    private TreeMap<LocalDate, Double> convert2CashflowPerDayMap(List<Cashflow> cashflows) {
        TreeMap<LocalDate, Double> returnValue = new TreeMap<>();
        cashflows.forEach(c -> {
            if(returnValue.containsKey(c.getTransactiondate())) {
                returnValue.put(c.getTransactiondate(), c.getValue()+returnValue.get(c.getTransactiondate()));
            } else {
                returnValue.put(c.getTransactiondate(), c.getValue());
            }
        });
        return returnValue;
    }

    protected Flux<Instrument> getChilds4Valuation() {
        return dataReader.findByParentBusinesskey(instrument.getBusinesskey());
    }

    protected Mono<TreeMap<LocalDate, Double>> extractAndGetCombinedValueCurve(List<ValueCurve>  valueCurves) {
        return getCombinedValueCurve(extractMapsFromValueCurve(valueCurves));
    }

    protected Mono<TreeMap<LocalDate, Double>> getCombinedValueCurve(List<TreeMap<LocalDate, Double>> valueCurves) {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();
        var currentDate = calcCurveStartDate(valueCurves);
        while(!currentDate.isAfter(LocalDate.now())) {
            double value = 0.0;
            for (var singleValueCurve : valueCurves) {
                // use the valueCurveService to get the value instead of just map.get to handle not existing values correctly
                value+=extractValueFromCurve(singleValueCurve, currentDate);
            }
            valueCurve.put(currentDate, value);
            currentDate = currentDate.plusDays(1);
        }
        return Mono.just(valueCurve);
    }
}