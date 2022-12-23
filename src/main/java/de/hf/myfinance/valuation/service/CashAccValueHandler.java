package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;


public class CashAccValueHandler extends AbsValueHandler {


    public CashAccValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }


    @Override
    public Mono<Void> calcValueCurve(){
        return  dataReader.findAllCashflow4Instrument(instrument.getBusinesskey())
                .switchIfEmpty(Flux.just(new Cashflow("empty Cashflow", LocalDate.now(), instrument.getBusinesskey(), 0.0)))
                .collectList().flatMap(this::calcCurveFromCashflows)
                .flatMap(this::sendValueCurveCalculatedEvent);
    }

    protected Mono<TreeMap<LocalDate, Double>> calcCurveFromCashflows(List<Cashflow> cashflows) {
        TreeMap<LocalDate, Double> valueCurve = new TreeMap<>();

        double value = 0.0;

        var cashflowMap =  convert2CashflowPerDayMap(cashflows);

        SortedSet<LocalDate> sortedDates = new TreeSet<LocalDate>(cashflowMap.keySet());
        LocalDate lastDate = sortedDates.first();
        //add initial 0 value before the first cashflow
        valueCurve.put(lastDate.minusDays(1), value);
        Iterator<LocalDate> iter = sortedDates.iterator();
        while(iter.hasNext()) {

            LocalDate nextExistingDate = iter.next();
            while(lastDate.isBefore(nextExistingDate)){
                valueCurve.put(lastDate, value);
                lastDate=lastDate.plusDays(1);
            }
            lastDate=nextExistingDate.plusDays(1);
            var cashflow = cashflowMap.get(nextExistingDate);
            value += cashflow;
            valueCurve.put(nextExistingDate, value);
        }

        return Mono.just(valueCurve);
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
}
