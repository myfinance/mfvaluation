package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.ValuationType;
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
        Mono<ValueCurve> positionCurveMono = dataReader.findPositonByKey(depotId, securityId);
        Mono<ValueCurve> priceCurveMono = dataReader.findValueCurve(securityId, ValuationType.MARKETVALUE);
        Mono<List<Trade>> tradesMono = dataReader.findTradesByKey(depotId, securityId).collectList();
        Mono<List<Cashflow>> cashflowsMono = dataReader.findAllCashflow4Instrument(securityId).collectList();

        return Mono.zip(positionCurveMono, priceCurveMono, tradesMono, cashflowsMono)
                .flatMapMany(tuple -> { // Changed to flatMapMany
                    ValueCurve positionCurve = tuple.getT1();
                    ValueCurve priceCurve = tuple.getT2();
                    List<Trade> trades = tuple.getT3();
                    List<Cashflow> cashflows = tuple.getT4();
                    return positionValueCurveCalculation(positionCurve, priceCurve, trades, cashflows);
                })
                .flatMap(this::sendPositionValueCalculatedEvent)
                .then(); // Add .then() to convert Flux<Void> to Mono<Void>
    }

    protected Mono<TreeMap<LocalDate, Double>> positionCurveCalculation(List<Trade> trades) {
        return buildCurveFromValueMap(convert2TradeAmountPerDayMap(trades));
    }

    protected Flux<ValueCurve> positionValueCurveCalculation(ValueCurve positionCurve, ValueCurve priceCurve, List<Trade> trades, List<Cashflow> cashflows) {
            TreeMap<LocalDate, Double> staticValueCurve = new TreeMap<>();
            TreeMap<LocalDate, Double> prudentValueCurve = new TreeMap<>();
            TreeMap<LocalDate, Double> marketValueCurve = new TreeMap<>();
            
            LocalDate startDate = positionCurve.getValueCurve().firstKey();
            LocalDate endDate = positionCurve.getValueCurve().lastKey(); 

            LocalDate currentDate = startDate;
            double staticValuePreviousDay = 0.0;
            var lastPriceDay = priceCurve.getValueCurve().lastKey();
            double positionAmountToday = 0;
            double avgPrice =0;
            while (!currentDate.isAfter(endDate) || (positionAmountToday>0 && !currentDate.isAfter(lastPriceDay))) {
                final LocalDate loopDate = currentDate; // Make a final copy for use in lambdas

                positionAmountToday = AbsValueHandler.extractValueFromCurve(positionCurve.getValueCurve(), loopDate);
                
                // MARKETVALUE calculation
                double currentPrice = AbsValueHandler.extractValueFromCurve(priceCurve.getValueCurve(), loopDate);
                double marketValue = round(currentPrice * positionAmountToday, 2);
                marketValueCurve.put(loopDate, marketValue);

                // STATIC calculation
                List<Trade> tradesOnDay = trades.stream().filter(t -> t.getTradeDate().equals(loopDate)).collect(Collectors.toList());
                double totalTradeAmountOnDay = tradesOnDay.stream().mapToDouble(Trade::getAmount).sum();
                double totalCostToday = currentPrice * totalTradeAmountOnDay;
                if(totalTradeAmountOnDay<0){
                    totalCostToday = avgPrice * totalTradeAmountOnDay;
                }
                staticValuePreviousDay = staticValuePreviousDay + totalCostToday;
                if (positionAmountToday == 0) {
                    staticValueCurve.put(loopDate, 0.0);
                } else {
                    staticValueCurve.put(loopDate, round(staticValuePreviousDay,2));
                }
                if(positionAmountToday>0){
                    avgPrice = round(staticValuePreviousDay/positionAmountToday, 2);
                }
                currentDate = currentDate.plusDays(1);

                // PRUDENT calculation
                double prudentValue = currentPrice * positionAmountToday;
                if(prudentValue>staticValuePreviousDay){
                    prudentValue = staticValuePreviousDay+(prudentValue-staticValuePreviousDay)*0.7;
                }
                prudentValueCurve.put(loopDate, round(prudentValue,2));
            }
            
            ValueCurve marketValueCurveObject = new ValueCurve(securityId);
            marketValueCurveObject.setValueCurve(marketValueCurve);
            marketValueCurveObject.setParentBusinesskey(depotId);
            marketValueCurveObject.setValuationType(ValuationType.MARKETVALUE);

            ValueCurve staticValueCurveObject = new ValueCurve(securityId);
            staticValueCurveObject.setValueCurve(staticValueCurve);
            staticValueCurveObject.setParentBusinesskey(depotId);
            staticValueCurveObject.setValuationType(ValuationType.STATIC);

            ValueCurve prudentValueCurveObject = new ValueCurve(securityId);
            prudentValueCurveObject.setValueCurve(prudentValueCurve);
            prudentValueCurveObject.setParentBusinesskey(depotId);
            prudentValueCurveObject.setValuationType(ValuationType.PRUDENT);


            return Flux.just(marketValueCurveObject, staticValueCurveObject, prudentValueCurveObject);
    }

        protected Mono<ValueCurve> oldpositionValueCurveCalculation(ValueCurve positionCurve, ValueCurve priceCurve, List<Trade> trades, List<Cashflow> cashflows) {
            var result = new ValueCurve();
            result.setInstrumentBusinesskey(positionCurve.getInstrumentBusinesskey());
            result.setParentBusinesskey(positionCurve.getParentBusinesskey());
            TreeMap<LocalDate, Double> positionValueCurve = new TreeMap<>();
            
            positionCurve.getValueCurve().entrySet().forEach(entry -> {
                var instrumentValue = AbsValueHandler.extractValueFromCurve(priceCurve.getValueCurve(), entry.getKey());
                var positionValue  = round(entry.getValue()*instrumentValue, 2);
                positionValueCurve.put(entry.getKey(), positionValue);
            });
            var currentDate = positionCurve.getValueCurve().lastKey();
            var currentPosition = positionCurve.getValueCurve().get(currentDate);
            if(currentPosition>0){
                currentDate=currentDate.plusDays(1);
                var lastPriceDay = priceCurve.getValueCurve().lastKey();
                while(!currentDate.isAfter(lastPriceDay)){
                    var price = AbsValueHandler.extractValueFromCurve(priceCurve.getValueCurve(), currentDate);
                    positionValueCurve.put(currentDate, currentPosition*price);
                    currentDate = currentDate.plusDays(1);
                }
            }
            var valueCurveObject = new ValueCurve(securityId);
            valueCurveObject.setValueCurve(positionValueCurve);
            valueCurveObject.setParentBusinesskey(depotId);
            valueCurveObject.setValuationType(ValuationType.MARKETVALUE);

            return Mono.just(valueCurveObject);
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

    protected Mono<Void> sendPositionValueCalculatedEvent(ValueCurve curve) {
        auditService.saveMessage(" new positionValuecurve calculated for instrument: " + securityId + " and depot:"+depotId, Severity.INFO, AUDIT_MSG_TYPE);
        positionValueCalculatedEventHandler.sendPositionValueCalculatedEvent(curve);
        return Mono.just("").then();
    }
    
}
