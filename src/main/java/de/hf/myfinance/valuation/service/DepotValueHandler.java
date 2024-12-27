package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
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
        return getAllPositionValuesForDepotId()
        .collectList()
        .flatMap(this::extractAndGetCombinedValueCurve)
        .switchIfEmpty(createZeroCurve())
        .flatMap(this::sendValueCurveCalculatedEvent);
    }

    protected Flux<ValueCurve> getAllPositionValuesForDepotId() {
        return dataReader.findPositonValueByDepotKey(instrument.getBusinesskey());
    }

}
