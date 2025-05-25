package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Position;
import de.hf.myfinance.restmodel.Transaction;
import de.hf.myfinance.restmodel.TransactionType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.DataReader;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ValuationService {
    private final DataReader dataReader;
    private final AuditService auditService;

    public ValuationService(DataReader dataReader, AuditService auditService) {
        this.dataReader = dataReader;
        this.auditService = auditService;
    }

    public Mono<Double> getValue(String businesskey, LocalDate date) {
        return dataReader.findValueCurveByInstrumentBusinesskey(businesskey).flatMap(c -> extractValueFromCurve(c, date));
    }

    public Flux<Map<String,Double>> getValues(List<String> businesskeys, LocalDate date) {
        return Flux.fromIterable(businesskeys).flatMap(b->{
            return getValue(b, date).flatMap(v->{
                var returnValue = new HashMap<String,Double>();
                returnValue.put(b,v);
                return Mono.just(returnValue);
            });
        });
        
    }

    public Mono<ValueCurve> getValueCurve(String businesskey, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate) || startDate.getYear() < 1970) {
            throw new MFException(MFMsgKey.ILLEGAL_ARGUMENTS, "no valid dates:" + startDate +", " + endDate);
        }
        return dataReader.findValueCurveByInstrumentBusinesskey(businesskey).flatMap(c -> fillCurveGaps(c, startDate, endDate));
    }

    private Mono<Double> extractValueFromCurve(final ValueCurve valueCurve, final LocalDate date) {
        return Mono.just(AbsValueHandler.extractValueFromCurve(valueCurve.getValueCurve(), date));
    }

    private Mono<ValueCurve> fillCurveGaps(ValueCurve valueCurve, final LocalDate startDate, final LocalDate endDate) {
        TreeMap<LocalDate, Double> adjValueCurve = new TreeMap<>();

        final Map<LocalDate, Double> curve = valueCurve.getValueCurve();
        var first = LocalDate.MIN;
        var last = LocalDate.MAX;
        for (final LocalDate date : curve.keySet()) {
            if(first==LocalDate.MIN) {
                first = date;
            }
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                adjValueCurve.put(date, curve.get(date));
            }
            last = date;
        }
        if(!curve.isEmpty()) {
            if(first.isAfter(endDate)) {
                adjValueCurve = fillTheGaps(adjValueCurve, startDate, endDate, curve.get(first));
            } else if(last.isBefore(startDate)) {
                adjValueCurve = fillTheGaps(adjValueCurve, startDate, endDate, curve.get(last));
            } else {
                if (first.isAfter(startDate)) {
                    adjValueCurve = fillTheGaps(adjValueCurve, startDate, first.minusDays(1), curve.get(first));
                }
                if (last.isBefore(endDate)) {
                    adjValueCurve = fillTheGaps(adjValueCurve, last.plusDays(1), endDate, curve.get(last));
                }
            }
        }
        valueCurve.setValueCurve(adjValueCurve);
        return Mono.just(valueCurve);
    }

    private TreeMap<LocalDate, Double> fillTheGaps(TreeMap<LocalDate, Double> adjValueCurve, LocalDate start, LocalDate end, Double value) {
        for (LocalDate currentDate = start; currentDate.isBefore(end.plusDays(1)); currentDate = currentDate.plusDays(1)) {
            adjValueCurve.put(currentDate, value);
        }
        return adjValueCurve;
    }

    public List<Cashflow> generateCashflows(Transaction transaction, boolean isDelete){
        var cashflows = new ArrayList<Cashflow>();
        var value = transaction.getValue();
        if(isDelete) {
            value = value * (-1);
        }
        if(transaction.getTransactionType().equals(TransactionType.INCOME)){
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value));
        }
        if(transaction.getTransactionType().equals(TransactionType.EXPENSE)){
            value = value * (-1);
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value));
        }
        if(transaction.getTransactionType().equals(TransactionType.LIFEINSURANCEEXPENSE)){
            value = value * (-1);
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getInsuranceKey(), value));
        }
        if(transaction.getTransactionType().equals(TransactionType.BUDGETTRANSFER)){
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getTrgBudgetKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value * (-1)));
        }
        if(transaction.getTransactionType().equals(TransactionType.TRANSFER)){
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getTrgAccKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value * (-1)));
        }
        if(transaction.getTransactionType().equals(TransactionType.BUY)){
            value = value * (-1);
            getTradeCashflows(transaction, cashflows, value);
        }
        if(transaction.getTransactionType().equals(TransactionType.SELL)){
            getTradeCashflows(transaction, cashflows, value);
        }
        if(transaction.getTransactionType().equals(TransactionType.DEPOTCASHFLOW)){
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value));
            cashflows.add(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getSecurityBusinessKey(), value));
        }
        if(transaction.getTransactionType().equals(TransactionType.INTERESTS)){
            var accCashflow = new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value);
            accCashflow.setIsInterest(true);
            cashflows.add(accCashflow);
            var bgtCashflow = new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value);
            bgtCashflow.setIsInterest(true);
            cashflows.add(bgtCashflow);
        }
        return cashflows;
    }

    private void getTradeCashflows(Transaction transaction, ArrayList<Cashflow> cashflows, double value) {
        var accCashflow = new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getAccKey(), value);
        accCashflow.setIsTrade(true);
        cashflows.add(accCashflow);
        var bgtCashflow = new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getBudgetKey(), value);
        bgtCashflow.setIsTrade(true);
        cashflows.add(bgtCashflow);
        var depotCashflow = new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getDepotBusinessKey(), value);
        depotCashflow.setIsTrade(true);
        cashflows.add(depotCashflow);
        var securityCashflow = new Cashflow(transaction.getDescription(),transaction.getTransactiondate(),transaction.getSecurityBusinessKey(), value);
        securityCashflow.setIsTrade(true);
        cashflows.add(securityCashflow);
    }

    public Flux<Cashflow> listInstrumentCashflows(String businesskey, LocalDate startDate, LocalDate endDate) {
        return dataReader.findAllCashflow4Instrument(businesskey)    
            .filter(cashflow -> 
                !cashflow.getTransactiondate().isBefore(startDate) &&
                !cashflow.getTransactiondate().isAfter(endDate)
            );
    }

    public Mono<Double> getAvgExpensesOfLastYear(String businesskey){
        LocalDate today = LocalDate.now();
        // Get the previous month
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        // Get the last day of the last month
        LocalDate lastDayOfLastMonth = lastMonth.atEndOfMonth();
        return listInstrumentCashflows(businesskey, lastDayOfLastMonth.minusYears(1), lastDayOfLastMonth)
            .filter(c->c.getValue()<0).map(Cashflow::getValue).reduce(0.0,Double::sum).map(s->s/12);
    }

    public Flux<Position> getPositions(List<String> depots) {
        var postions = dataReader.findAllPostions(depots).map(this::mapPositionValueCurveToPosition);
        var postionValues = dataReader.findAllPostionValues(depots).map(this::mapPositionValueValueCurveToPosition);

        var result = Mono.zip(
            postions.collectList(),
            postionValues.collectList()
        ).map(tuple -> {
            List<Position> pos = tuple.getT1();
            List<Position> values = tuple.getT2();
        
            // Create map from flux2 for fast lookup
            Map<String, Position> map = values.stream()
                .collect(Collectors.toMap(
                    p -> p.getDepotId() + ":" + p.getSecurityId(),
                    Function.identity()
                ));
        
            List<Position> mergedList = new ArrayList<>();
        
            for (Position p1 : pos) {
                String key = p1.getDepotId() + ":" + p1.getSecurityId();
                Position positionValue = map.remove(key); // remove to avoid double-use
        
                if (positionValue != null) {
                    p1.setValue(positionValue.getValue());
                    mergedList.add(p1);
                } else {
                    mergedList.add(p1);
                }
            }
        
            // Add any remaining positions from flux2 (non-matching)
            mergedList.addAll(map.values());
        
            return Flux.fromIterable(mergedList);
        });
        return result.flatMapMany(Function.identity());
    }

    private Position mapPositionValueCurveToPosition(ValueCurve valueCurve) {
        // Construct Position from valueCurve data
        Position position = new Position(valueCurve.getParentBusinesskey(), null, valueCurve.getInstrumentBusinesskey(), null, null);
        var values = valueCurve.getValueCurve();
        Double latestValue = values.isEmpty() ? null : values.lastEntry().getValue();
        position.setAmount(latestValue);
        return position;
    }

    private Position mapPositionValueValueCurveToPosition(ValueCurve valueCurve) {
        // Construct Position from valueCurve data
        Position position = new Position(valueCurve.getParentBusinesskey(), null, valueCurve.getInstrumentBusinesskey(), null, null);
        var values = valueCurve.getValueCurve();
        Double latestValue = values.isEmpty() ? null : values.lastEntry().getValue();
        position.setValue(latestValue);
        return position;
    }

}
