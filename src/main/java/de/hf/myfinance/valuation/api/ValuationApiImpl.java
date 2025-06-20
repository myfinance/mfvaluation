package de.hf.myfinance.valuation.api;

import de.hf.myfinance.restapi.ValuationApi;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Position;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.service.ValuationService;
import org.springframework.web.bind.annotation.RestController;
import de.hf.framework.utils.ServiceUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

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
    public Mono<ValueCurve> getValueCurve(String businesskey, LocalDate startDate, LocalDate endDate) {
        return valuationService.getValueCurve(businesskey, startDate, endDate);
    }

    @Override
    public Mono<Double> getValue(String businesskey, LocalDate date) {
        return valuationService.getValue(businesskey, date);
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

}