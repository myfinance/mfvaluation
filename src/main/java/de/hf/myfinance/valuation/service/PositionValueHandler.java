package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;

import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.PositionBuildedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

public class PositionValueHandler extends AbsCurveHandler{

    private String depotId;
    private String securityId;
    private PositionBuildedEventHandler positionBuildedEventHandler;

    public PositionValueHandler(String depotId, String securityId, DataReader dataReader, AuditService auditService, PositionBuildedEventHandler positionBuildedEventHandler){
        super(dataReader, auditService);
        this.depotId = depotId;
        this.securityId = securityId;
        this.positionBuildedEventHandler = positionBuildedEventHandler;
    }


    public Mono<Void> calcPositionCurve() {
        return dataReader.findTradesByKey(this.depotId, this.securityId).collectList().flatMap(this::calcPositionCurve).flatMap(this::sendPositionBuildedEvent);
    }

    public Mono<Void> calcPositionValueCurve() {
        return dataReader.findPositonByKey(depotId, securityId).flatMap(this::calcPositionValueCurve).flatMap(this::sendPositionValueCalculatedEvent);
    }

    protected Mono<TreeMap<LocalDate, Double>> calcPositionCurve(List<Trade> trades) {
        return buildCurveFromValueMap(convert2TradeAmountPerDayMap(trades));
    }

    protected Mono<TreeMap<LocalDate, Double>> calcPositionValueCurve(ValueCurve positionCurve) {
        return dataReader.findValueCurveByInstrumentBusinesskey(positionCurve.getInstrumentBusinesskey()).flatMap(v->{
            var result = new ValueCurve();
            result.setInstrumentBusinesskey(positionCurve.getInstrumentBusinesskey());
            result.setParentBusinesskey(positionCurve.getParentBusinesskey());
            TreeMap<LocalDate, Double> positionValueCurve = new TreeMap<>();
            var priceCurve = v.getValueCurve();
            
            positionCurve.getValueCurve().entrySet().forEach(entry -> {
                positionValueCurve.put(entry.getKey(), entry.getValue()*priceCurve.get(entry.getKey()));
            });
            var currentDate = positionCurve.getValueCurve().lastKey();
            var currentPosition = positionCurve.getValueCurve().get(currentDate);
            if(currentPosition>0){
                currentDate=currentDate.plusDays(1);
                var lastPriceDay = priceCurve.lastKey();
                while(!currentDate.isAfter(lastPriceDay)){
                    positionValueCurve.put(currentDate, currentPosition+priceCurve.get(currentDate));
                    currentDate = currentDate.plusDays(1);
                }
            }

            return Mono.just(positionValueCurve);
        });
    }

    private TreeMap<LocalDate, Double> convert2TradeAmountPerDayMap(List<Trade> trades) {
        TreeMap<LocalDate, Double> returnValue = new TreeMap<>();
        trades.forEach(t -> {
            if(returnValue.containsKey(t.getTradeDate())) {
                returnValue.put(t.getTradeDate(), t.getAmount()+returnValue.get(t.getTradeDate()));
            } else {
                returnValue.put(t.getTradeDate(), t.getAmount());
            }
        });
        return returnValue;
    }

    protected Mono<Void> sendPositionBuildedEvent(TreeMap<LocalDate, Double> curve) {
        auditService.saveMessage(" new positioncurve calculated for instrument: " + securityId + " and depot:"+depotId, Severity.INFO, AUDIT_MSG_TYPE);
        var valueCurveObject = new ValueCurve(securityId);
        valueCurveObject.setValueCurve(curve);
        valueCurveObject.setParentBusinesskey(depotId);
        positionBuildedEventHandler.sendPositionBuildedEvent(valueCurveObject);
        return Mono.just("").then();
    }

    protected Mono<Void> sendPositionValueCalculatedEvent(TreeMap<LocalDate, Double> curve) {
        auditService.saveMessage(" new positionValuecurve calculated for instrument: " + securityId + " and depot:"+depotId, Severity.INFO, AUDIT_MSG_TYPE);
        var valueCurveObject = new ValueCurve(securityId);
        valueCurveObject.setValueCurve(curve);
        valueCurveObject.setParentBusinesskey(depotId);
        positionBuildedEventHandler.sendPositionBuildedEvent(valueCurveObject);
        return Mono.just("").then();
    }
    
}
