package de.hf.myfinance.valuation.api;

import de.hf.myfinance.restapi.ValuationApi;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Position;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.service.ValuationService;
import org.springframework.web.bind.annotation.RestController;
import de.hf.framework.utils.ServiceUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class ValuationApiImpl implements ValuationApi {
    ServiceUtil serviceUtil;
    ValuationService valuationService;

    public ValuationApiImpl(ValuationService valuationService, ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
        this.valuationService = valuationService;
    }

    @Override
    public String index() {
        return "Hello valuationservice";
    }

    @Override
    public Mono<ValueCurve> getValueCurve(String businesskey, LocalDate startDate, LocalDate endDate, ValuationType valType) {
        return valuationService.getValueCurve(businesskey, startDate, endDate, valType);
    }

    @Override
    public Mono<Double> getValue(String businesskey, LocalDate date, ValuationType valType) {
        return valuationService.getValue(businesskey, date, valType);
    }

    @Override
    public Mono<LocalDateTime> getValueTs(String businesskey) {
        return valuationService.getValueTs(businesskey);
    }

    @Override
    public Mono<Double> getAvgExpensesOfLastYear(String businesskey) {
        return valuationService.getAvgExpensesOfLastYear(businesskey);
    }

    @Override
    public Flux<Cashflow> listCashflows4Instrument(String businesskey, LocalDate startDate, LocalDate endDate) {
        return valuationService.listInstrumentCashflows(businesskey, startDate, endDate);
    }

    @Override
    public Flux<Position> getPositions(List<String> depots) {
        return valuationService.getPositions(depots);
    }

    @Override
    public Mono<ValueCurve> recalcAndGetValueCurve(String businesskey) {
        return valuationService.recalcAndGetValueCurve(businesskey);
    }

    @Override
    public Mono<Map<String, Double>> getLinkedValues(String businesskey, LocalDate valueDate, ValuationType valType) {
        return valuationService.getLinkedValues(businesskey,valueDate, valType);
    }

    @Override
    public Mono<String> recalcAllCurves() {
        return valuationService.recalcAllCurves();
    }

}