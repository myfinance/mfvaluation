package de.hf.myfinance.valuation.api;

import de.hf.myfinance.restapi.ValuationApi;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.service.ValuationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import de.hf.framework.utils.ServiceUtil;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
public class ValuationApiImpl implements ValuationApi {
    ServiceUtil serviceUtil;
    ValuationService valuationService;

    @Autowired
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
        return null;
    }


}