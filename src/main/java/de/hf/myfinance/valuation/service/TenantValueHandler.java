package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.valuation.events.out.PortfolioMetricsEventHandler;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TenantValueHandler  extends PortfolioValueHandler{
    protected final PortfolioMetricsEventHandler portfolioMetricsEventHandler;

    public TenantValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService, PortfolioMetricsEventHandler portfolioMetricsEventHandler){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
        this.portfolioMetricsEventHandler = portfolioMetricsEventHandler;
    }

    @Override
    protected Flux<Instrument> getChilds4Valuation() {
        return dataReader.findInstrumentByParentBusinesskeyAndInstrumentType(instrument.getBusinesskey(), InstrumentType.ACCOUNTPORTFOLIO);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        return super.calcValueCurve().then(portfolioMetricsEvent());
    }

    protected Mono<Void> portfolioMetricsEvent() {
        portfolioMetricsEventHandler.sendPortfolioMetricsEvent();
        return Mono.just("").then();
    }
}
