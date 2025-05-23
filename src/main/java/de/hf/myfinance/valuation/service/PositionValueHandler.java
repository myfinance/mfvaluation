package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.PositionBuildedEventHandler;
import de.hf.myfinance.valuation.events.out.PositionValueCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PositionValueHandler extends AbsCurveHandler{

    private String depotId;
    private String securityId;
    private PositionBuildedEventHandler positionBuildedEventHandler;
    private PositionValueCalculatedEventHandler positionValueCalculatedEventHandler;

    public PositionValueHandler(String depotId, String securityId, DataReader dataReader, AuditService auditService, PositionBuildedEventHandler positionBuildedEventHandler, PositionValueCalculatedEventHandler positionValueCalculatedEventHandler){
        super(dataReader, auditService);
        this.depotId = depotId;
        this.securityId = securityId;
        this.positionBuildedEventHandler = positionBuildedEventHandler;
        this.positionValueCalculatedEventHandler = positionValueCalculatedEventHandler;
    }


    public Mono<Void> calcPositionCurve() {
        System.out.println("mytest: ");
        return dataReader.findTradesByKey(this.depotId, this.securityId)
                    .doOnNext(trade -> System.out.println("Trade found: " + trade))
                    .switchIfEmpty(handleNotExisting())
                    .collectList().flatMap(this::positionCurveCalculation).flatMap(this::sendPositionBuildedEvent);
    }

    private Flux<Trade> handleNotExisting(){
        return Flux.error(new MFException(MFMsgKey.UNKNOWN_INSTRUMENT_EXCEPTION, "No Trades for this Id available."));
    }

    public Mono<Void> calcPositionValueCurve() {
        return dataReader.findPositonByKey(depotId, securityId).flatMap(this::positionValueCurveCalculation).flatMap(this::sendPositionValueCalculatedEvent);
    }

    protected Mono<TreeMap<LocalDate, Double>> positionCurveCalculation(List<Trade> trades) {
        return buildCurveFromValueMap(convert2TradeAmountPerDayMap(trades));
    }

    protected Mono<TreeMap<LocalDate, Double>> positionValueCurveCalculation(ValueCurve positionCurve) {
        return dataReader.findValueCurveByInstrumentBusinesskey(positionCurve.getInstrumentBusinesskey()).flatMap(v->{
            var result = new ValueCurve();
            result.setInstrumentBusinesskey(positionCurve.getInstrumentBusinesskey());
            result.setParentBusinesskey(positionCurve.getParentBusinesskey());
            TreeMap<LocalDate, Double> positionValueCurve = new TreeMap<>();
            var priceCurve = v.getValueCurve();
            
            positionCurve.getValueCurve().entrySet().forEach(entry -> {
                var instrumentValue = AbsValueHandler.extractValueFromCurve(priceCurve, entry.getKey());
                var positionValue  = round(entry.getValue()*instrumentValue, 2);
                positionValueCurve.put(entry.getKey(), positionValue);
            });
            var currentDate = positionCurve.getValueCurve().lastKey();
            var currentPosition = positionCurve.getValueCurve().get(currentDate);
            if(currentPosition>0){
                currentDate=currentDate.plusDays(1);
                var lastPriceDay = priceCurve.lastKey();
                while(!currentDate.isAfter(lastPriceDay)){
                    var price = AbsValueHandler.extractValueFromCurve(priceCurve, currentDate);
                    positionValueCurve.put(currentDate, currentPosition*price);
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
        positionValueCalculatedEventHandler.sendPositionValueCalculatedEvent(valueCurveObject);
        return Mono.just("").then();
    }
    
}
