package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ValueHandlerFactory {

    private final DataReader dataReader;
    private final AuditService auditService;
    protected final ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler;

    public ValueHandlerFactory(DataReader dataReader, AuditService auditService, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler) {
        this.dataReader = dataReader;
        this.auditService = auditService;
        this.valueCurveCalculatedEventHandler = valueCurveCalculatedEventHandler;
    }

    public Mono<ValueHandler> getValueHandler(String businesskey){
        return dataReader.findByBusinesskey(businesskey)
                .switchIfEmpty(Mono.error(new MFException(MFMsgKey.UNKNOWN_INSTRUMENT_EXCEPTION, " Instrument for id:" + businesskey + " not found")))
                .flatMap(this::createValueHandler);
    }

    private Mono<ValueHandler> createValueHandler(final Instrument instrument) {
        ValueHandler valueHandler;
        switch (instrument.getInstrumentType().getTypeGroup()) {
            case SECURITY:
                valueHandler = new SecurityValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case CASHACCOUNT:
                valueHandler = new CashAccValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case TENANT:
                valueHandler = new TenantValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case PORTFOLIO:
                valueHandler = new PortfolioValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case REALESTATE:
            case DEPOT:
            case DEPRECATIONOBJECT:
            case LIVEINSURANCE:
            case LOAN:
            case UNKNOWN:
            default:
                throw new MFException(MFMsgKey.UNKNOWN_INSTRUMENTTYPE_EXCEPTION, "Type:" + instrument.getInstrumentType());
        }
        return Mono.just(valueHandler);
    }
}
