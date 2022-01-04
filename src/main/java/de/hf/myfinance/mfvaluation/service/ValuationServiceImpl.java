package de.hf.myfinance.mfvaluation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import de.hf.myfinance.restapi.ValuationService;
import de.hf.myfinance.utils.ServiceUtil;

@RestController
public class ValuationServiceImpl implements ValuationService {
    ServiceUtil serviceUtil;

    @Autowired
    public ValuationServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public String index() {
        return "Hello valuationservice";
    }

}