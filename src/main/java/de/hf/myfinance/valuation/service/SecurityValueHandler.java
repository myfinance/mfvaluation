package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class SecurityValueHandler extends AbsValueHandler{



    public SecurityValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){

        return dataReader.findPricesByInstrumentBusinesskey(instrument.getBusinesskey())
                .switchIfEmpty(Mono.just(new EndOfDayPrices(instrument.getBusinesskey())))
                .flatMap(this::calcCurveFromPrices)
                .flatMap(this::sendValueCurveCalculatedEvent);
    }

    protected Mono<TreeMap<LocalDate, Double>> calcCurveFromPrices(EndOfDayPrices endOfDayPrices) {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();

        if(endOfDayPrices.getPrices() == null || endOfDayPrices.getPrices().isEmpty() ) {
            valueCurve.put(LocalDate.now(), 0.0);
            return Mono.just(valueCurve);
        }

        SortedSet<LocalDate> sortedDates = new TreeSet<LocalDate>(endOfDayPrices.getPrices().keySet());

        LocalDate lastDate = sortedDates.first().minusDays(1);
        double lastValue = 0.0;
        Iterator<LocalDate> iter = sortedDates.iterator();
        while(iter.hasNext()) {

            LocalDate nextExistingDate = iter.next();
            var value = endOfDayPrices.getPrices().get(nextExistingDate).getValue();
            var daysbetween = DAYS.between(lastDate, nextExistingDate);
            if(daysbetween == 1) {
                valueCurve.put(nextExistingDate, value);
                lastValue = value;
                lastDate = nextExistingDate;
            } else {
                var valueDiffPerDay = (value - lastValue) / daysbetween;
                while(!lastDate.equals(nextExistingDate)){
                    lastDate=lastDate.plusDays(1);
                    lastValue = lastValue + valueDiffPerDay;
                    lastValue = Math.round(lastValue * 10000.0) / 10000.0;
                    valueCurve.put(lastDate, lastValue);
                }
            }
        }

        return Mono.just(valueCurve);
    }

}
