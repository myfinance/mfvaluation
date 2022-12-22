package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.*;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValuationTest  extends EventProcessorTestBase {
    @Autowired
    @Qualifier("valuationProcessor")
    protected Consumer<Event<String, Instrument>> valuationProcessor;

    @Test
    void giroValuation() {

        var giroDesc = "testGiro";
        var giroKey = giroDesc + "@1";
        var giroEntity = new Instrument(giroKey, giroDesc, InstrumentType.GIRO, true);
        Event creatEvent = new Event(Event.Type.CREATE, giroKey, giroEntity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        Event valuationEvent = new Event(Event.Type.START, giroKey, giroKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var desc = "testcashflow";
        var cashflow = new Cashflow(desc, transactionDate, giroKey, 100.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, giroKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        valuationEvent = new Event(Event.Type.START, giroKey, giroKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(2, newCurve.size());
        assertEquals(100.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));
    }
}
