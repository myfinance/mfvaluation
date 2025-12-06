package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JacksonInject.Value;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.InstrumentTypeGroup;
import de.hf.myfinance.restmodel.PortfolioMetrics;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.PortfolioMetricsCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

@Component
public class PortfolioMetricsCalculator extends AbsCurveHandler {

    private final DataReader dataReader;
    private final PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler;

    public PortfolioMetricsCalculator(DataReader dataReader, PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler, AuditService auditService){
        super(dataReader, auditService);
        this.portfolioMetricsCalculatedEventHandler = portfolioMetricsCalculatedEventHandler;
        this.dataReader = dataReader;
    }
    public Mono<Void> calcAllPortfolioMetrics(){
        Mono<List<Cashflow>> cashflows = dataReader.findAllInstruments().filter(i->i.getInstrumentType().getTypeGroup().equals(InstrumentTypeGroup.SECURITY)).map(i->i.getBusinesskey()).collectList().flatMap(instrumentList->dataReader.findAllCashflows4InstrumentKeyList(instrumentList).collectList());
        Mono<List<ValueCurve>> positions = dataReader.findAllPostions().collectList();
        Mono<List<ValueCurve>> positionValues = dataReader.findAllPostionValues().collectList();
        Mono<List<Instrument>> instruments = dataReader.findAllInstruments().collectList();


        return Mono.zip(cashflows, positions, positionValues, instruments)
                .flatMap(tuple -> Mono.fromRunnable(() -> {
                    List<Cashflow> cashflowList = tuple.getT1();
                    List<ValueCurve> positionList = tuple.getT2();
                    List<ValueCurve> positionValueList = tuple.getT3();
                    List<Instrument> instrumentList = tuple.getT4();


                    calculateMetricsForPortfolio(positionValueList, cashflowList, "TOTAL");

                    var equityPositionValues = filterPositionValuesForInstruments(positionValueList, instrumentList, InstrumentType.EQUITY);
                    var cashflowListEquity = filterCashflowsForInstruments(cashflowList, instrumentList, InstrumentType.EQUITY);
                    calculateMetricsForPortfolio(equityPositionValues, cashflowListEquity, InstrumentType.EQUITY.name());

                    var bondPositionValues = filterPositionValuesForInstruments(positionValueList, instrumentList, InstrumentType.BOND);
                    var cashflowListBond = filterCashflowsForInstruments(cashflowList, instrumentList, InstrumentType.BOND);
                    calculateMetricsForPortfolio(bondPositionValues, cashflowListBond, InstrumentType.BOND.name());

                    var etfPositionValues = filterPositionValuesForInstruments(positionValueList, instrumentList, InstrumentType.ETF);
                    var cashflowListEtf = filterCashflowsForInstruments(cashflowList, instrumentList, InstrumentType.ETF);
                    calculateMetricsForPortfolio(etfPositionValues, cashflowListEtf, InstrumentType.ETF.name());

                    var fondsPositionValues = filterPositionValuesForInstruments(positionValueList, instrumentList, InstrumentType.FONDS);
                    var cashflowListFonds = filterCashflowsForInstruments(cashflowList, instrumentList, InstrumentType.FONDS);
                    calculateMetricsForPortfolio(fondsPositionValues, cashflowListFonds, InstrumentType.FONDS.name());

                    var kryptoPositionValues = filterPositionValuesForInstruments(positionValueList, instrumentList, InstrumentType.KRYPTO);
                    var cashflowListKrypto = filterCashflowsForInstruments(cashflowList, instrumentList, InstrumentType.KRYPTO);
                    calculateMetricsForPortfolio(kryptoPositionValues, cashflowListKrypto, InstrumentType.KRYPTO.name());

                }).then());
    }

    private List<Cashflow> filterCashflowsForInstruments(List<Cashflow> cashflows, List<Instrument> instruments, InstrumentType instrumentType) {
        var filteredInstruments = instruments.stream()
                .filter(i -> i.getInstrumentType() == instrumentType)
                .map(Instrument::getBusinesskey)
                .toList();

        var filteredCashflows = cashflows.stream()
                .filter(cf -> filteredInstruments.contains(cf.getInstrumentBusinesskey()))
                .toList();

        return filteredCashflows;
    }

    private List<ValueCurve> filterPositionValuesForInstruments(List<ValueCurve> positionValues, List<Instrument> instruments, InstrumentType instrumentType) {
        var filteredInstruments = instruments.stream()
                .filter(i -> i.getInstrumentType() == instrumentType)
                .map(Instrument::getBusinesskey)
                .toList();

        var filteredPositionValues = positionValues.stream()
                .filter(pv -> filteredInstruments.contains(pv.getInstrumentBusinesskey()))
                .toList();

        return filteredPositionValues;
    }

    public void calculateMetricsForPortfolio(List<ValueCurve> positionValues, List<Cashflow> cashflows, String portfolioName) {
        var portfolio = new PortfolioMetrics(portfolioName);
        portfolio.setIsSingleSecurity(false);     

        
        var value = addFinalValuesAndCalcCagr(positionValues, cashflows, LocalDate.now());
        portfolio.setTotalCagr(value);

        var years = new ArrayList<Integer>();
        cashflows.forEach(cf -> {
            years.add(cf.getTransactiondate().getYear());
        });
        years.stream().forEach(year -> {
            List<Cashflow> cfThisYear = cashflows.stream().filter(cf -> cf.getTransactiondate().getYear() == year).toList();
            Double cagrPerYear = addFinalValuesAndCalcCagr(positionValues, cfThisYear, LocalDate.of(year, 12, 31));
            if (portfolio.getCagrPerYear() == null) {
                portfolio.setCagrPerYear(new HashMap<>());
            }
            portfolio.getCagrPerYear().put(year, cagrPerYear);
        });


        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(portfolio);
    }

    private Double addFinalValuesAndCalcCagr( List<ValueCurve> positionValues, List<Cashflow> cashflows, LocalDate endDate) {
        LocalDate startDate = cashflows.stream()
                        .map(Cashflow::getTransactiondate)
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.now());
        var cfWithFinalValues = new ArrayList<Cashflow>(cashflows);
        positionValues.forEach(pv -> {
            var startValue = extractValueFromCurve(pv.getValueCurve(), startDate.minusDays(1));
            var endValue = extractValueFromCurve(pv.getValueCurve(), endDate);
            var value = endValue - startValue;
            if(value==0) return;
            var cf = new Cashflow();
            cf.setInstrumentBusinesskey(pv.getInstrumentBusinesskey());
            cf.setTransactiondate(endDate);
            cf.setValue(value);
            cfWithFinalValues.add(cf);
        });   
        return calcCagr(cfWithFinalValues, endDate);
    }

    public Double calcCagr( List<Cashflow> cashflows, LocalDate endDate) { 

        final int maxIterations = 100;
        final double tolerance = 1.0e-6;
        double guess = 0.1;

        for (int i = 0; i < maxIterations; i++) {
            double npv = 0.0;
            double npvDerivative = 0.0;
            for (Cashflow cf : cashflows) {
                long days = ChronoUnit.DAYS.between(endDate, cf.getTransactiondate());
                double t = (double) days / 365.0;
                npv += cf.getValue() / Math.pow(1.0 + guess, t);
                npvDerivative -= cf.getValue() * t / Math.pow(1.0 + guess, t + 1);
            }

            double newGuess = guess - npv / npvDerivative;
            if (Math.abs(newGuess - guess) < tolerance) {
                return newGuess;
            }
            guess = newGuess;
        }
        return 0.0; // Failed to converge
    }

    private Map<String, Double> getPositionValuesPerSecurity(String businesskey, List<ValueCurve> positions, List<ValueCurve> positionValues) {
        Map<String, Double> valueMap = positions.stream()
                .filter(p -> p.getInstrumentBusinesskey().equals(businesskey))
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getInstrumentBusinesskey(),
                        p -> {
                            ValueCurve pv = positionValues.stream()
                                    .filter(v -> v.getInstrumentBusinesskey().equals(p.getInstrumentBusinesskey()))
                                    .findFirst()
                                    .orElse(null);
                            if (pv != null && !pv.getValueCurve().isEmpty()) {
                                return pv.getValueCurve().lastEntry().getValue();
                            } else {
                                return 0.0;
                            }
                        }
                ));
        return valueMap;
    }
    
}
