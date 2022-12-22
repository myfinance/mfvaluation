package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValueCurveCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public abstract class AbsValueHandler implements ValueHandler {

    protected Instrument instrument;
    protected final DataReader dataReader;
    protected final ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler;
    protected final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="ValueHandler_User_Event";

    protected AbsValueHandler(Instrument instrument, DataReader dataReader, ValueCurveCalculatedEventHandler valueCurveCalculatedEventHandler, AuditService auditService){
        this.instrument = instrument;
        this.dataReader = dataReader;
        this.auditService = auditService;
        this.valueCurveCalculatedEventHandler = valueCurveCalculatedEventHandler;
    }

    protected TreeMap<LocalDate, Double> createZeroCurve(TreeMap<LocalDate, Double> valueCurve) {
        valueCurve.put(LocalDate.now(), 0.0);
        return valueCurve;
    }



    protected LocalDate calcCurveStartDate(List<TreeMap<LocalDate, Double>> valueCurves) {
        LocalDate startDate = LocalDate.now();
        for (TreeMap<LocalDate, Double> childValueCurve : valueCurves) {
            LocalDate minDate = childValueCurve.firstKey();
            if(minDate.isBefore(startDate)) {
                startDate = minDate;
            }
        }
        return startDate;
    }
}