package de.hf.myfinance.valuation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

public class AbsCurveHandler {
    protected final DataReader dataReader;
    protected final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="ValueHandler_User_Event";

    protected AbsCurveHandler(DataReader dataReader, AuditService auditService){

        this.dataReader = dataReader;
        this.auditService = auditService;
    }

    protected Mono<TreeMap<LocalDate, Double>> createZeroCurve() {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();
        valueCurve.put(LocalDate.now(), 0.0);
        return Mono.just(valueCurve);
    }

    protected List<TreeMap<LocalDate, Double>> extractMapsFromValueCurve(List<ValueCurve> valueCurves){
        return valueCurves.stream().map(vc -> vc.getValueCurve()).collect(Collectors.toList());
    }

    protected LocalDate calcCurveStartDate(List<TreeMap<LocalDate, Double>> valueCurves) {
        LocalDate startDate = LocalDate.now();
        for (var childValueCurve : valueCurves) {
            LocalDate minDate = childValueCurve.firstKey();
            if(minDate.isBefore(startDate)) {
                startDate = minDate;
            }
        }
        return startDate;
    }

    public static Double extractValueFromCurve(final TreeMap<LocalDate, Double> valueCurve, final LocalDate date) {
        if(valueCurve.containsKey(date)) {
            return  valueCurve.get(date);
        }
        var firstEntry = valueCurve.firstEntry();
        if(firstEntry.getKey().isAfter(date)) {
            return firstEntry.getValue();
        }
        var lastEntry = valueCurve.lastEntry();
        return lastEntry.getValue();
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    /**
     * builds a Curve from the given ValuePerDateMap. The Curve starts with Value 0 before the first Date with a Value and sets a Value for every Day till the Date of the last change of the Value
     * @param valuePerDateMap a map of Values per Date. not every Date has a value
     * @return
     */
    protected Mono<TreeMap<LocalDate, Double>> buildCurveFromValueMap(TreeMap<LocalDate, Double> valuePerDateMap) {
        TreeMap<LocalDate, Double> curve = new TreeMap<>();
        double value = 0.0;
        SortedSet<LocalDate> sortedDates = new TreeSet<LocalDate>(valuePerDateMap.keySet());
        LocalDate lastDate = sortedDates.first();
        //add initial 0 value before the first cashflow
        curve.put(lastDate.minusDays(1), value);
        Iterator<LocalDate> iter = sortedDates.iterator();
        while(iter.hasNext()) {

            LocalDate nextExistingDate = iter.next();
            while(lastDate.isBefore(nextExistingDate)){
                curve.put(lastDate, round(value, 6));
                lastDate=lastDate.plusDays(1);
            }
            lastDate=nextExistingDate.plusDays(1);
            var currentValue = valuePerDateMap.get(nextExistingDate);
            value += currentValue;
            curve.put(nextExistingDate, round(value, 6));
        }

        return Mono.just(curve);
    }
}
