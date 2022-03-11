package de.hf.myfinance.valuation.api;

import de.hf.myfinance.restapi.ValuationApi;
import de.hf.myfinance.valuation.persistence.entities.InstrumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import de.hf.framework.exceptions.MFException;
import de.hf.framework.utils.ServiceUtil;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.mfinstrumentclient.MFInstrumentClient;
import de.hf.myfinance.valuation.service.ValueCurveHandlerImpl;
import de.hf.myfinance.restmodel.Instrument;

@RestController
public class ValuationApiImpl implements ValuationApi {
    ServiceUtil serviceUtil;
    MFInstrumentClient instrumentClient;
    ValueCurveHandlerImpl valueCurveHandler;

    @Autowired
    public ValuationApiImpl(ServiceUtil serviceUtil, MFInstrumentClient instrumentClient, ValueCurveHandlerImpl valueCurveHandler) {
        this.serviceUtil = serviceUtil;
        this.instrumentClient = instrumentClient;
        this.valueCurveHandler = valueCurveHandler;
    }

    @Override
    public String index() {
        return "Hello valuationservice";
    }


    @Override
    public Instrument helloException() {
        
        try{
            valueCurveHandler.getValueCurve(1);
            return null;
        } catch(MFException e) {
            throw e;
        }
        catch(Exception e) {
            throw new MFException(MFMsgKey.UNSPECIFIED, e.getMessage());
        }
    }

    @Override
    public Instrument helloInstrument(int instrumentId) {
        try{
            return valueCurveHandler.getNewInstrument(instrumentId);
            //return new Instrument(instrumentId, "name-" + instrumentId, serviceUtil.getServiceAddress());
        } catch(MFException e) {
            throw e;
        }
        catch(Exception e) {
            throw new MFException(MFMsgKey.UNSPECIFIED, e.getMessage());
        }
    }

    @Override
    public Instrument helloInstrumentService() {
        try{
            return instrumentClient.getInstrument(1);
        } catch(MFException e) {
            throw e;
        }
        catch(Exception e) {
            throw new MFException(MFMsgKey.UNSPECIFIED, e.getMessage());
        }
        
    }

}