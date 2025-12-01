package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TenantValueHandler  extends PortfolioValueHandler{

    public TenantValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    protected Flux<Instrument> getChilds4Valuation() {
        return dataReader.findInstrumentByParentBusinesskeyAndInstrumentType(instrument.getBusinesskey(), InstrumentType.ACCOUNTPORTFOLIO);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        return super.calcValueCurve();
    }
}
