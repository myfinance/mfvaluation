package de.hf.myfinance.valuation.service;

import java.time.LocalDate;
import java.util.Map;

import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.persistence.entities.InstrumentEntity;
import de.hf.myfinance.valuation.persistence.repositories.InstrumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;


@Component
public class ValueCurveHandlerImpl {

    private InstrumentRepository instrumentRepository;

    @Autowired
    public ValueCurveHandlerImpl(InstrumentRepository instrumentRepository){
        this.instrumentRepository=instrumentRepository;
    }

    public Instrument getNewInstrument(int id) {
        var instrumentEntity = new InstrumentEntity("testkey"+id, "testvalue");
        instrumentRepository.save(instrumentEntity);
        var instrument = new Instrument();
        var newInstrumentEntity = instrumentRepository.findByBusinesskey("testkey"+id);
        /*if(newInstrumentEntity.isPresent()){
            return new Instrument(1, newInstrumentEntity.get().getBusinesskey(), newInstrumentEntity.get().getTestvalue());
        }*/
        return null;
    }

    public Map<LocalDate, Double> getValueCurve(final int instrumentId) {
        throw new MFException(MFMsgKey.WRONG_INSTRUMENTTYPE_EXCEPTION, " wrong instrumenttype to calculate positions:" );
    }
}
