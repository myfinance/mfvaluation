package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PortfolioValueHandler extends AbsValueHandler{

    public PortfolioValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        return getChilds4Valuation()
                .flatMap(i->dataReader.findValueCurve(i.getBusinesskey(), ValuationType.MARKETVALUE))
                .collectList()
                .flatMap(this::extractAndGetCombinedValueCurve)
                .switchIfEmpty(createZeroCurve())
                .flatMap(this::sendValueCurveCalculatedEvent);


    }


}
