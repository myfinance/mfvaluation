package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

@Component
public class CagrCalculator {

    private final DataReader dataReader;

    public CagrCalculator(DataReader dataReader) {
        this.dataReader = dataReader;
    }
    public Mono<Double> calcAllCagrValues(){
        Mono<List<Cashflow>> cashflows = dataReader.findAllCashflows().collectList();
        Mono<List<Trade>> trades = dataReader.findAllTrades().collectList();
        Mono<List<ValueCurve>> positions = dataReader.findAllPostions().collectList();
        Mono<List<ValueCurve>> positionValues = dataReader.findAllPostionValues().collectList();

        return Mono.zip(cashflows, trades, positions, positionValues)
                .map(tuple -> {
                    List<Cashflow> cashflowList = tuple.getT1();
                    List<Trade> tradeList = tuple.getT2();
                    List<ValueCurve> positionList = tuple.getT3();
                    List<ValueCurve> positionValueList = tuple.getT4();

                    double cagr = calculateCagr(positionList, positionValueList, tradeList, cashflowList, LocalDate.now().minusYears(5), LocalDate.now());
                    System.out.println("Calculated CAGR: " + cagr);
                    return cagr;
                });
    }

    public double calculateCagr(List<ValueCurve> positions, List<ValueCurve> positionValues,List<Trade> trades, List<Cashflow> cashflows, 
                                LocalDate beginDate, LocalDate endDate) {
        return 0.0;
    }
    
}
