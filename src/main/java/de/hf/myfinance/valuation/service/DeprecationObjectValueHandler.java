package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

public class DeprecationObjectValueHandler   extends AbsValueHandler{

    public DeprecationObjectValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        var valueCurve = new TreeMap<LocalDate, Double>();

        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        var acquisitionDate = LocalDate.parse(instrument.getAdditionalProperties().get(AdditionalProperties.ACQUISITIONDATE), formatter);
        var acquisitionValue = Double.parseDouble(instrument.getAdditionalProperties().get(AdditionalProperties.ACQUISITIONVALUE));
        var maturityDate = LocalDate.parse(instrument.getAdditionalProperties().get(AdditionalProperties.MATURITYDATE), formatter);

        var daysBetween = ChronoUnit.DAYS.between(acquisitionDate, maturityDate);
        var dailyValueReduction = acquisitionValue / daysBetween;

        var currentValue = acquisitionValue;
        valueCurve.put(acquisitionDate.minusDays(1), 0.0);
        for (LocalDate date = acquisitionDate; !date.isAfter(maturityDate); date = date.plusDays(1)) {
            valueCurve.put(date, currentValue);
            currentValue -= dailyValueReduction;
        }
        return sendValueCurveCalculatedEvent(valueCurve);
    }
}