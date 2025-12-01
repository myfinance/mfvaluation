package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.InstrumentTypeGroup;
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
        Mono<List<Cashflow>> cashflows = dataReader.findAllInstruments().filter(i->i.getInstrumentType().getTypeGroup().equals(InstrumentTypeGroup.SECURITY)).map(i->i.getBusinesskey()).collectList().flatMap(instrumentList->dataReader.findAllCashflows4InstrumentKeyList(instrumentList).collectList());
        Mono<List<ValueCurve>> positions = dataReader.findAllPostions().collectList();
        Mono<List<ValueCurve>> positionValues = dataReader.findAllPostionValues().collectList();

        return Mono.zip(cashflows, positions, positionValues)
                .map(tuple -> {
                    List<Cashflow> cashflowList = tuple.getT1();
                    List<ValueCurve> positionList = tuple.getT2();
                    List<ValueCurve> positionValueList = tuple.getT3();

                    LocalDate beginDate = cashflowList.stream()
                        .map(Cashflow::getTransactiondate)
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.now());

                    double cagr = calculateCagr(positionList, positionValueList, cashflowList, beginDate, LocalDate.now());
                    System.out.println("Calculated CAGR: " + cagr);
                    return cagr;
                });
    }

    public double calculateCagr(List<ValueCurve> positions, List<ValueCurve> positionValues, List<Cashflow> cashflows, 
                                LocalDate beginDate, LocalDate endDate) {
        return 0.0;
    }
    
}
