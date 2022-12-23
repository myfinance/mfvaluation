package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;

public class TenantValueHandler  extends PortfolioValueHandler{

    public TenantValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    protected Flux<Instrument> getChilds4Valuation() {
        return dataReader.findByParentBusinesskeyAndInstrumentType(instrument.getBusinesskey(), InstrumentType.ACCOUNTPORTFOLIO);
    }
}
