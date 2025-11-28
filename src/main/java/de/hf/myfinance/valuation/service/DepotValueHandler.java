package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DepotValueHandler  extends PortfolioValueHandler{

    public DepotValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve() {
        return Flux.just(ValuationType.MARKETVALUE, 
                        ValuationType.STATIC, 
                        ValuationType.PRUDENT)
            .flatMap(valuationType -> 
                getAllPositionValuesForDepotId(valuationType)
                    .collectList()
                    .flatMap(this::extractAndGetCombinedValueCurve)
                    .switchIfEmpty(createZeroCurve())
                    .flatMap(valueCurve -> {
                        return sendValueCurveCalculatedEvent(valueCurve, valuationType);
                    })
            ).then();
    }

    protected Flux<ValueCurve> getAllPositionValuesForDepotId(ValuationType valuationType) {
        return dataReader.findPositonValueByDepotKey(instrument.getBusinesskey(), valuationType);
    }
}
