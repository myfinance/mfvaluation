package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeMap;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.AdditionalMaps;
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

        var surrenderValues = instrument.getAdditionalMaps().get(AdditionalMaps.SURRENDERVALUES);
        var dateStrings = new HashSet<String>();
        dateStrings.addAll(surrenderValues.keySet());

        var dates = new ArrayList<LocalDate>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        dateStrings.stream().distinct().forEach(date -> {
            dates.add(LocalDate.parse(date, formatter));
        });

        LocalDate startDate = Collections.min(dates);
        LocalDate endDate = Collections.max(dates);
        var currentSurrenderValues = 0.0;
        valueCurve.put(startDate.minusDays(1), 0.0);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            var dateString = date.toString();
            if (surrenderValues.containsKey(dateString)) {
                currentSurrenderValues = Double.parseDouble(surrenderValues.get(dateString));
            }
            valueCurve.put(date, currentSurrenderValues);
        }
        return sendValueCurveCalculatedEvent(valueCurve);
    }
}
