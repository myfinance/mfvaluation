package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import reactor.core.publisher.Flux;
import java.util.List;

import reactor.core.publisher.Mono;

public class PortfolioValueHandler extends AbsValueHandler{

    public PortfolioValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        super(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
    }

    @Override
    public Mono<Void> calcValueCurve(){
        return getChilds4Valuation()
                .collectList()
                .flatMap(this::calculateCurves);
    }


    protected Mono<Void> calculateCurves(List<Instrument> childs4Valuation){
        return Flux.just(ValuationType.MARKETVALUE, 
                        ValuationType.STATIC, 
                        ValuationType.PRUDENT)
            .flatMap(valueType -> 
                Flux.fromIterable(childs4Valuation)
                    .flatMap(i->getValueCurve4ValuationType(i, valueType))
                    .collectList()
                    .flatMap(this::extractAndGetCombinedValueCurve)
                    .switchIfEmpty(createZeroCurve())
                    .flatMap(valueCurve -> {
                        return sendValueCurveCalculatedEvent(valueCurve, valueType);
                    })
            ).then();
    }
}
