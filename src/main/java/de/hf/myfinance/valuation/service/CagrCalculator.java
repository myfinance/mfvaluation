package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.InstrumentTypeGroup;
import de.hf.myfinance.restmodel.PortfolioMetrics;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.PortfolioMetricsCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Mono;

@Component
public class CagrCalculator extends AbsCurveHandler {

    private final DataReader dataReader;
    private final PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler;

    public CagrCalculator(DataReader dataReader, PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler, AuditService auditService){
        super(dataReader, auditService);
        this.portfolioMetricsCalculatedEventHandler = portfolioMetricsCalculatedEventHandler;
        this.dataReader = dataReader;
    }
    public Mono<Void> calcAllCagrValues(){
        Mono<List<Cashflow>> cashflows = dataReader.findAllInstruments().filter(i->i.getInstrumentType().getTypeGroup().equals(InstrumentTypeGroup.SECURITY)).map(i->i.getBusinesskey()).collectList().flatMap(instrumentList->dataReader.findAllCashflows4InstrumentKeyList(instrumentList).collectList());
        Mono<List<ValueCurve>> positions = dataReader.findAllPostions().collectList();
        Mono<List<ValueCurve>> positionValues = dataReader.findAllPostionValues().collectList();

        return Mono.zip(cashflows, positions, positionValues)
                .flatMap(tuple -> Mono.fromRunnable(() -> {
                    List<Cashflow> cashflowList = tuple.getT1();
                    List<ValueCurve> positionList = tuple.getT2();
                    List<ValueCurve> positionValueList = tuple.getT3();

                    LocalDate beginDate = cashflowList.stream()
                        .map(Cashflow::getTransactiondate)
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.now());

                    calculatePositionMetrics(positionList, positionValueList, cashflowList, beginDate, LocalDate.now());
                }).then());
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

    public void calculatePositionMetrics(List<ValueCurve> positions, List<ValueCurve> positionValues, List<Cashflow> cashflows, 
                                LocalDate beginDate, LocalDate endDate) {
        var totalPortfolio = new PortfolioMetrics("TOTAL");
        totalPortfolio.setIsSingleSecurity(false);
        var EquityPortfolio = new PortfolioMetrics(InstrumentType.EQUITY.name());
        EquityPortfolio.setIsSingleSecurity(false);
        var etfportfolio = new PortfolioMetrics(InstrumentType.ETF.name());
        etfportfolio.setIsSingleSecurity(false);
        var bondportfolio = new PortfolioMetrics(InstrumentType.BOND.name());
        bondportfolio.setIsSingleSecurity(false);
        var fondportfolio = new PortfolioMetrics(InstrumentType.FONDS.name());
        fondportfolio.setIsSingleSecurity(false);
        var kryptoportfolio = new PortfolioMetrics(InstrumentType.KRYPTO.name());
        kryptoportfolio.setIsSingleSecurity(false);

        positionValues.forEach(pv -> {
            var cf = new Cashflow();
            cf.setInstrumentBusinesskey(pv.getInstrumentBusinesskey());
            cf.setTransactiondate(endDate);
            cf.setValue(extractValueFromCurve(pv.getValueCurve(), endDate));
            cashflows.add(cf);
        });                            

        var value = calcCagr(cashflows, endDate);

        totalPortfolio.setTotalCagr(value);
        EquityPortfolio.setTotalCagr(0.0);
        etfportfolio.setTotalCagr(0.0);     
        bondportfolio.setTotalCagr(0.0);
        fondportfolio.setTotalCagr(0.0);
        kryptoportfolio.setTotalCagr(0.0);


        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(totalPortfolio);
        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(EquityPortfolio);
        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(etfportfolio);
        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(bondportfolio);
        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(fondportfolio);
        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(kryptoportfolio);
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

    
}
