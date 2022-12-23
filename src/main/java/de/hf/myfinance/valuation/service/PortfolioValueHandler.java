package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public class PortfolioValueHandler extends AbsValueHandler{

    public PortfolioValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        return getChilds4Valuation()
                .flatMap(i->dataReader.findValueCurveByInstrumentBusinesskey(i.getBusinesskey()))
                .collectList()
                .flatMap(this::getCombinedValueCurve)
                .switchIfEmpty(createZeroCurve())
                .flatMap(this::sendValueCurveCalculatedEvent);
    }

    protected Flux<Instrument> getChilds4Valuation() {
        return dataReader.findByParentBusinesskey(instrument.getBusinesskey());
    }


    protected Mono<TreeMap<LocalDate, Double>> getCombinedValueCurve(List<ValueCurve> valueCurves) {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();
        var currentDate = calcCurveStartDate(valueCurves);
        while(!currentDate.isAfter(LocalDate.now())) {
            double value = 0.0;
            for (var singleValueCurve : valueCurves) {
                // use the valueCurveService to get the value instead of just map.get to handle not existing values correctly
                value+=getValue(singleValueCurve.getValueCurve(), currentDate);
            }
            valueCurve.put(currentDate, value);
            currentDate = currentDate.plusDays(1);
        }
        return Mono.just(valueCurve);
    }

    protected double getValue(TreeMap<LocalDate, Double> valueCurve, LocalDate date) {
        double value = 0.0;
        if (valueCurve.containsKey(date)) {
            value = valueCurve.get(date);
        } else if (valueCurve.firstKey().isAfter(date)) {
            value = valueCurve.get(valueCurve.firstKey());
        } else if (valueCurve.lastKey().isBefore(date)) {
            value = valueCurve.get(valueCurve.lastKey());
        }
        return value;
    }

}
