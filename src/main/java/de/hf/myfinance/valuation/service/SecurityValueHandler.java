package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.EndOfDayPrice;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class SecurityValueHandler extends AbsValueHandler{

    class ValueCalculationInfo{
        public HashMap<String, ValueCurve> fxprices;
        public EndOfDayPrices instrumentPrices;
    }

    public SecurityValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){

        return dataReader.findPricesByInstrumentBusinesskey(instrument.getBusinesskey())
                .switchIfEmpty(Mono.just(new EndOfDayPrices(instrument.getBusinesskey())))
                .flatMap(this::loadFxCurves)
                .flatMap(this::calcCurveFromPrices)
                .flatMap(this::sendValueCurveCalculatedEvent);
    }

    private Mono<ValueCalculationInfo> loadFxCurves(EndOfDayPrices endOfDayPrices) {
        var curves2Load = new HashSet<String>();
        if(endOfDayPrices.getPrices()!=null && !endOfDayPrices.getPrices().isEmpty()){
            endOfDayPrices.getPrices().values().forEach(i->{
                if(!i.getCurrencyKey().equals(Instrument.DEFAULTCURRENCY)) {
                    curves2Load.add(i.getCurrencyKey());
                }
            });
        }
        return dataReader.findValueCurvesByBusinesskeyIn(curves2Load).collectList()
                .flatMap(i->createPriceMap(i, endOfDayPrices));
    }

    private Mono<ValueCalculationInfo> createPriceMap(List<ValueCurve> fxprices, EndOfDayPrices instrumentPrices){
        var curvemap = new HashMap<String, ValueCurve>();
        fxprices.forEach(i->curvemap.put(i.getInstrumentBusinesskey(),i));
        var valueCalculationInfo = new ValueCalculationInfo();
        valueCalculationInfo.fxprices=curvemap;
        valueCalculationInfo.instrumentPrices=instrumentPrices;
        return Mono.just(valueCalculationInfo);
    }

    protected Mono<TreeMap<LocalDate, Double>> calcCurveFromPrices(ValueCalculationInfo valueCalculationInfo) {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();
        var endOfDayPrices = valueCalculationInfo.instrumentPrices;
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
            var value = getValueInEuro(endOfDayPrices.getPrices().get(nextExistingDate), nextExistingDate, valueCalculationInfo.fxprices);
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

    protected double getValueInEuro(EndOfDayPrice endOfDayPrice, LocalDate date, HashMap<String, ValueCurve> fxPriceCurves) {
        var value = endOfDayPrice.getValue();
        if(endOfDayPrice.getCurrencyKey().equals(Instrument.DEFAULTCURRENCY)){
            return value;
        }
        var fxCurve = fxPriceCurves.get(endOfDayPrice.getCurrencyKey());
        if(fxCurve==null) return 0.0;
        var fxValue = AbsValueHandler.extractValueFromCurve(fxCurve.getValueCurve(), date);
        return value*fxValue;
    }

}
