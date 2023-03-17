package de.hf.myfinance.valuation.service;

import de.hf.framework.audit.AuditService;
import de.hf.framework.exceptions.MFException;
import de.hf.myfinance.exception.MFMsgKey;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.DataReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ValuationService {
    private final DataReader dataReader;
    private final AuditService auditService;

    @Autowired
    public ValuationService(DataReader dataReader, AuditService auditService) {
        this.dataReader = dataReader;
        this.auditService = auditService;
    }

    public Mono<Double> getValue(String businesskey, LocalDate date) {
        return dataReader.findValueCurveByInstrumentBusinesskey(businesskey).flatMap(c -> extractValueFromCurve(c, date));
    }

    public Mono<ValueCurve> getValueCurve(String businesskey, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate) || startDate.getYear() < 1970) {
            throw new MFException(MFMsgKey.ILLEGAL_ARGUMENTS, "no valid dates:" + startDate +", " + endDate);
        }
        return dataReader.findValueCurveByInstrumentBusinesskey(businesskey).flatMap(c -> fillCurveGaps(c, startDate, endDate));
    }

    private Mono<Double> extractValueFromCurve(final ValueCurve valueCurve, final LocalDate date) {
        return Mono.just(AbsValueHandler.extractValueFromCurve(valueCurve.getValueCurve(), date));
    }

    private Mono<ValueCurve> fillCurveGaps(ValueCurve valueCurve, final LocalDate startDate, final LocalDate endDate) {
        TreeMap<LocalDate, Double> adjValueCurve = new TreeMap<>();

        final Map<LocalDate, Double> curve = valueCurve.getValueCurve();
        var first = LocalDate.MIN;
        var last = LocalDate.MAX;
        for (final LocalDate date : curve.keySet()) {
            if(first==LocalDate.MIN) {
                first = date;
            }
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                adjValueCurve.put(date, curve.get(date));
            }
            last = date;
        }
        if(!curve.isEmpty()) {
            if(first.isAfter(endDate)) {
                adjValueCurve = fillTheGaps(adjValueCurve, startDate, endDate, curve.get(first));
            } else if(last.isBefore(startDate)) {
                adjValueCurve = fillTheGaps(adjValueCurve, startDate, endDate, curve.get(last));
            } else {
                if (first.isAfter(startDate)) {
                    adjValueCurve = fillTheGaps(adjValueCurve, startDate, first.minusDays(1), curve.get(first));
                }
                if (last.isBefore(endDate)) {
                    adjValueCurve = fillTheGaps(adjValueCurve, last.plusDays(1), endDate, curve.get(last));
                }
            }
        }
        valueCurve.setValueCurve(adjValueCurve);
        return Mono.just(valueCurve);
    }

    private TreeMap<LocalDate, Double> fillTheGaps(TreeMap<LocalDate, Double> adjValueCurve, LocalDate start, LocalDate end, Double value) {
        for (LocalDate currentDate = start; currentDate.isBefore(end.plusDays(1)); currentDate = currentDate.plusDays(1)) {
            adjValueCurve.put(currentDate, value);
        }
        return adjValueCurve;
    }

}
