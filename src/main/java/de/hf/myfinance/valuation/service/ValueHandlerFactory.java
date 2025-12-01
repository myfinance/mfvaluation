package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.PositionBuildedEventHandler;
import de.hf.myfinance.valuation.events.out.PositionValueCalculatedEventHandler;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ValueHandlerFactory {

    private final DataReader dataReader;
    private final AuditService auditService;
    protected final ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler;
    protected final PositionBuildedEventHandler positionBuildedEventHandler;
    protected final PositionValueCalculatedEventHandler positionValueCalculatedEventHandler;

    public ValueHandlerFactory(DataReader dataReader, AuditService auditService, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, 
                            PositionBuildedEventHandler positionBuildedEventHandler, PositionValueCalculatedEventHandler positionValueCalculatedEventHandler) {
        this.dataReader = dataReader;
        this.auditService = auditService;
        this.valueCurveCalculatedEventHandler = valueCurveCalculatedEventHandler;
        this.positionBuildedEventHandler = positionBuildedEventHandler;
        this.positionValueCalculatedEventHandler = positionValueCalculatedEventHandler;
    }

    public Mono<ValueHandler> getValueHandler(String businesskey){
        return dataReader.findInstrumentByBusinesskey(businesskey)
                .switchIfEmpty(Mono.error(new MFException(MFMsgKey.UNKNOWN_INSTRUMENT_EXCEPTION, " Instrument for id:" + businesskey + " not found")))
                .flatMap(this::createValueHandler);
    }

    private Mono<ValueHandler> createValueHandler(final Instrument instrument) {
        ValueHandler valueHandler;
        switch (instrument.getInstrumentType().getTypeGroup()) {
            case SECURITY:
                valueHandler = new SecurityValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case LOAN:
            case CASHACCOUNT:
                valueHandler = new CashAccValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case TENANT:
                valueHandler = new TenantValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case PORTFOLIO:
                valueHandler = new PortfolioValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case DEPOT:
                valueHandler = new DepotValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case REALESTATE:
                valueHandler = new RealestateValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case DEPRECATIONOBJECT:
                valueHandler = new DeprecationObjectValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case LIVEINSURANCE:
                valueHandler = new LifeInsuranceValueHandler(instrument, dataReader, valueCurveCalculatedEventHandler, auditService);
                break;
            case UNKNOWN:
            default:
                throw new MFException(MFMsgKey.UNKNOWN_INSTRUMENTTYPE_EXCEPTION, "Type:" + instrument.getInstrumentType());
        }
        return Mono.just(valueHandler);
    }

    public PositionValueHandler getPositionHandler(String depotId, String securityId){
        return new PositionValueHandler(depotId, securityId, dataReader, auditService,positionBuildedEventHandler, positionValueCalculatedEventHandler);
    }
}