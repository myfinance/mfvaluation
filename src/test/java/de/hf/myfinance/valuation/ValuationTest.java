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

    LocalDate transactionDate = LocalDate.of(2022, 1, 1);


    @Test
    void giroValuation() {

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

    @Test
    void TenantValuation() {

        addInstrument(InstrumentType.TENANT, tenantKey, tenantDesc, null);
        addInstrument(InstrumentType.ACCOUNTPORTFOLIO, accountPfKey, accountPfDesc, tenantKey);
        addInstrument(InstrumentType.BUDGETPORTFOLIO, budgetPfKey, budgetPfDesc, tenantKey);
        addInstrument(InstrumentType.GIRO, giroKey, giroDesc, accountPfKey);
        addInstrument(InstrumentType.BUDGETGROUP, budgetGroupKey, budgetGroupDesc, budgetPfKey);
        addInstrument(InstrumentType.BUDGET, budgetKey, budgetDesc, budgetGroupKey);

        createAndSaveNewTestCurve(giroKey);
        createAndSaveNewTestCurve(budgetKey);

        startAndTestCurveCalc(accountPfKey);
        startAndTestCurveCalc(budgetGroupKey);

        startAndTestCurveCalc(budgetPfKey);

        startAndTestCurveCalc(tenantKey);
    }

    private void addInstrument(InstrumentType instrumentType, String key, String desc, String parentKey){
        var entity = new Instrument(key, desc, instrumentType, true);
        entity.setParentBusinesskey(parentKey);
        var creatEvent = new Event(Event.Type.CREATE, key, entity);
        saveInstrumentProcessor.accept(creatEvent);
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());
        var valuationEvent = new Event(Event.Type.START, key, key);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));
    }

    private void createAndSaveNewTestCurve(String instrumentBusinesskey) {
        var valueCurve = new ValueCurve(instrumentBusinesskey);
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(transactionDate, 100.0);
        valueMap.put(transactionDate.minusDays(1), 0.0);
        valueCurve.setValueCurve(valueMap);
        Event creatEvent = new Event(Event.Type.CREATE, instrumentBusinesskey, valueCurve);
        saveValueCurveProcessor.accept(creatEvent);
    }

    private void startAndTestCurveCalc(String instrumentBusinesskey) {
        var valuationEvent = new Event(Event.Type.START, instrumentBusinesskey, instrumentBusinesskey);
        valuationProcessor.accept(valuationEvent);
        var messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(100.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));

        //save calculated curve for portfoliotest
        var valueCurve = new ValueCurve(instrumentBusinesskey);
        valueCurve.setInstrumentBusinesskey(instrumentBusinesskey);
        TreeMap<LocalDate, Double> calculatedCurve = new TreeMap<>();
        calculatedCurve.putAll(newCurve);
        valueCurve.setValueCurve(calculatedCurve);
        var createEvent = new Event(Event.Type.CREATE, instrumentBusinesskey, valueCurve);
        saveValueCurveProcessor.accept(createEvent);
    }
}
