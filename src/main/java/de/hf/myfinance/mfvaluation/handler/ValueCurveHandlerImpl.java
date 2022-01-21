package de.hf.myfinance.mfvaluation.handler;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;


@Component
public class ValueCurveHandlerImpl {

    public Map<LocalDate, Double> getValueCurve(final int instrumentId) {
        throw new MFException(MFMsgKey.WRONG_INSTRUMENTTYPE_EXCEPTION, " wrong instrumenttype to calculate positions:" );
    }
}
