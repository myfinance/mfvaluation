package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.AdditionalMaps;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RealestateValueHandler  extends AbsValueHandler{

    public RealestateValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        var valueCurve = new TreeMap<LocalDate, Double>();

        var yieldGoals = instrument.getAdditionalMaps().get(AdditionalMaps.YIELDGOAL);
        var profits = instrument.getAdditionalMaps().get(AdditionalMaps.REALESTATEPROFITS);
        var dateStrings = yieldGoals.keySet();
        dateStrings.addAll(profits.keySet());
        var dates = new ArrayList<LocalDate>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        dateStrings.stream().distinct().forEach(date -> {
            dates.add(LocalDate.parse(date, formatter));
        });
        LocalDate startDate = Collections.min(dates);
        LocalDate endDate = Collections.max(dates);
        var currentYieldGoal = 0.0;
        var currentProfit = 0.0;
        valueCurve.put(startDate.minusDays(1), 0.0);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            var dateString = date.toString();
            if (yieldGoals.containsKey(dateString)) {
                currentYieldGoal = Double.parseDouble(yieldGoals.get(dateString));
            }
            if (profits.containsKey(dateString)) {
                currentProfit = Double.parseDouble(profits.get(dateString));
            }
            var value = currentProfit * 1200 / currentYieldGoal;
            valueCurve.put(date, value);
        }
        return sendValueCurveCalculatedEvent(valueCurve);
    }
}