package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

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

    private static final Boolean SAVECASHFLOWS=true;
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

                    var instrumentBusinessKeysWithPositions = positionList.stream()
                        .filter(p -> !p.getValueCurve().isEmpty() && p.getValueCurve().lastEntry().getValue() != 0)
                        .map(ValueCurve::getInstrumentBusinesskey)
                        .distinct()
                        .toList();

                    instrumentBusinessKeysWithPositions.forEach(instrumentBusinessKey -> {
                        var cashflowsForInstrument = cashflowList.stream()
                                .filter(cf -> cf.getInstrumentBusinesskey().equals(instrumentBusinessKey))
                                .toList();
                        var positionValuesForInstrument = positionValueList.stream()
                                .filter(pv -> pv.getInstrumentBusinesskey().equals(instrumentBusinessKey))
                                .toList();
                        calculateMetricsForPortfolio(positionValuesForInstrument, cashflowsForInstrument, instrumentBusinessKey, true);
                    });

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
        calculateMetricsForPortfolio(positionValues, cashflows, portfolioName, false);
    }

    public void calculateMetricsForPortfolio(List<ValueCurve> positionValues, List<Cashflow> cashflows, String portfolioName, boolean isSingleSecurity) {
        var portfolio = new PortfolioMetrics(portfolioName);
        portfolio.setIsSingleSecurity(isSingleSecurity);

        LocalDate startDate = cashflows.stream()
                        .map(Cashflow::getTransactiondate)
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.now());
        
        var value =  calcCagr(getCfWithStartAndFinalValues(positionValues, cashflows, startDate, LocalDate.now()), LocalDate.now());
        portfolio.setTotalCagr(value);
        if(SAVECASHFLOWS) {
            portfolio.setCashflows(cashflows.stream().map(cf->cf.getValue()).toList());
        }

        var years = new ArrayList<Integer>();
        cashflows.forEach(cf -> {
            years.add(cf.getTransactiondate().getYear());
        });
        years.stream().distinct().forEach(year -> {
           
            var endDate = LocalDate.of(year, 12, 31);
            if (endDate.isAfter(LocalDate.now())) {
                endDate = LocalDate.now();
            }
            var firstDateOfTheYear = LocalDate.of(year, 1, 1);
            List<Cashflow> cfThisYear = cashflows.stream().filter(cf -> cf.getTransactiondate().getYear() == year).toList();
            var cfWithFinalValues = getCfWithStartAndFinalValues(positionValues, cfThisYear, firstDateOfTheYear, endDate);   
            Double cagrPerYear = calcCagr(cfWithFinalValues, endDate);
            if (portfolio.getCagrPerYear() == null) {
                portfolio.setCagrPerYear(new HashMap<>());
            }
            portfolio.getCagrPerYear().put(year, cagrPerYear);

            Double yieldPerYear = calcYield(cfWithFinalValues, firstDateOfTheYear, endDate);
            if (portfolio.getYieldPerYear() == null) {
                portfolio.setYieldPerYear(new HashMap<>());
            }
            portfolio.getYieldPerYear().put(year, yieldPerYear);
            
            if(SAVECASHFLOWS) {
                if(portfolio.getCashflowsWithStartAndEndValues()==null) {
                    portfolio.setCashflowsWithStartAndEndValues(new HashMap<>());
                }
                var cfValuesList = new ArrayList<Double>();   
                cfWithFinalValues.forEach(cf->cfValuesList.add(cf.getValue()));
                portfolio.getCashflowsWithStartAndEndValues().put(year, cfValuesList);
            }
        });


        portfolioMetricsCalculatedEventHandler.sendPortfolioMetricsCalculatedEvent(portfolio);
    }

    private ArrayList<Cashflow> getCfWithStartAndFinalValues(List<ValueCurve> positionValues, List<Cashflow> cashflows,
            LocalDate startDate, LocalDate endDate) {
        var cfWithFinalValues = new ArrayList<Cashflow>(cashflows);
        positionValues.forEach(pv -> {
            //-1 to get the value before start date, as on start date there could be a cashflow
            var startValue = extractValueFromCurve(pv.getValueCurve(), startDate.minusDays(1)) *(-1);
            var endValue = extractValueFromCurve(pv.getValueCurve(), endDate);
            if(startValue!=0) {
                var cf = new Cashflow();
                cf.setInstrumentBusinesskey(pv.getInstrumentBusinesskey());
                cf.setTransactiondate(startDate);
                cf.setValue(startValue);
                cfWithFinalValues.add(cf);
            }
            if(endValue!=0) {
                var cf = new Cashflow();
                cf.setInstrumentBusinesskey(pv.getInstrumentBusinesskey());
                cf.setTransactiondate(endDate);
                cf.setValue(endValue);
                cfWithFinalValues.add(cf);
            }

        });
        return cfWithFinalValues;
    }

    public Double calcYield( List<Cashflow> cashflows, LocalDate startDate, LocalDate endDate){
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return calcYield(cashflows, endDate, (double) days);
    }

    public Double calcCagr( List<Cashflow> cashflows, LocalDate endDate){
        return calcYield(cashflows, endDate, 365.0) ;
    }

    private Double calcYield( List<Cashflow> cashflows, LocalDate endDate, Double daysInPeriod) { 

        final int maxIterations = 1000;
        final double tolerance = 1.0e-9;

        java.util.function.Function<Double, Double> npvFunc = rate -> {
            double npv = 0.0;
            for (Cashflow cf : cashflows) {
                long days = ChronoUnit.DAYS.between(endDate, cf.getTransactiondate());
                double t = (double) days / daysInPeriod;
                double base = 1.0 + rate;
                if (base <= 0) return Double.NaN;
                npv += cf.getValue() / Math.pow(base, t);
            }
            return npv;
        };

        double low = -0.99;
        double high = 1.0;
        
        double npvLow = npvFunc.apply(low);
        if(Double.isNaN(npvLow)) npvLow = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 30; i++) {
            double npvHigh = npvFunc.apply(high);
            if(Double.isNaN(npvHigh)) {
                high /= 10;
                break;
            }
            if (npvLow * npvHigh < 0) break;
            low = high;
            npvLow = npvHigh;
            high *= 10;
        }

        if (npvFunc.apply(low) * npvFunc.apply(high) > 0) {
            return 0.0; // no bracket
        }


        // Bisection method
        for (int i = 0; i < maxIterations; i++) {
            double mid = low + (high - low) / 2; 
            if (mid == low || mid == high) return mid;
            
            double npvMid = npvFunc.apply(mid);

            if (Double.isNaN(npvMid)) {
                high = mid;
                continue;
            }

            if (Math.abs(npvMid) < tolerance) {
                return mid;
            }

            if (npvLow * npvMid < 0) {
                high = mid;
            } else {
                low = mid;
                npvLow = npvMid;
            }

            if ((high - low) / Math.abs(mid) < tolerance) {
                return mid;
            }
        }
        
        return (low+high)/2.0;
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
