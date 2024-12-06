package de.hf.myfinance.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.DataReader;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import de.hf.testhelper.JsonHelper;

public class PostionValueProcessorTest extends EventProcessorTestBase {


    @Autowired
    DataReader dataReader;

    @Test
    void positionValuation() {
        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        positionCurve.put(datebetweeenTrades, 10.0);
        positionCurve.put(secTradeDate, 5.0);
        positionRepository.save(new PositionEntity(depotKey, eqKey, positionCurve)).block();

        var instrumentCurve = new TreeMap<LocalDate, Double>();
        instrumentCurve.put(datebeforFirstTrade, 1.0);
        instrumentCurve.put(firstTradeDate, 1.0);
        instrumentCurve.put(datebetweeenTrades, 2.0);
        instrumentCurve.put(secTradeDate, 2.0);

        var instrumentValueCurve = new ValueCurveEntity();
        instrumentValueCurve.setInstrumentBusinesskey(eqKey);
        instrumentValueCurve.setValueCurve(instrumentCurve);
        valueCurveRepository.save(instrumentValueCurve).block();


        var expectedCurve = new TreeMap<LocalDate, Double>();
        expectedCurve.put(datebeforFirstTrade, 0.0);
        expectedCurve.put(firstTradeDate, 10.0);
        expectedCurve.put(datebetweeenTrades, 20.0);
        expectedCurve.put(secTradeDate, 10.0);

        var position = new ValueCurve();
        position.setInstrumentBusinesskey(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);

        var creatEvent = new Event(Event.Type.START, eqKey, position);
        positionValueProcessor.accept(creatEvent);

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(4, newCurve.size());
        assertEquals(expectedCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
        assertEquals(expectedCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
        assertEquals(expectedCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
        assertEquals(expectedCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
        var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
        assertEquals(eqKey, instrumentBusinesskey);
        var parentBusinesskey = (String) data.get("parentBusinesskey");
        assertEquals(depotKey, parentBusinesskey);

    }

    @Test
    void positionValuationOldInstrumentCurve() {
        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        positionCurve.put(datebetweeenTrades, 10.0);
        positionCurve.put(secTradeDate, 5.0);
        positionRepository.save(new PositionEntity(depotKey, eqKey, positionCurve)).block();

        var instrumentCurve = new TreeMap<LocalDate, Double>();
        instrumentCurve.put(LocalDate.of(2021, 12, 30), 2.0);

        var instrumentValueCurve = new ValueCurveEntity();
        instrumentValueCurve.setInstrumentBusinesskey(eqKey);
        instrumentValueCurve.setValueCurve(instrumentCurve);
        valueCurveRepository.save(instrumentValueCurve).block();


        var expectedCurve = new TreeMap<LocalDate, Double>();
        expectedCurve.put(datebeforFirstTrade, 0.0);
        expectedCurve.put(firstTradeDate, 20.0);
        expectedCurve.put(datebetweeenTrades, 20.0);
        expectedCurve.put(secTradeDate, 10.0);

        var position = new ValueCurve();
        position.setInstrumentBusinesskey(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);

        var creatEvent = new Event(Event.Type.START, eqKey, position);
        positionValueProcessor.accept(creatEvent);

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(4, newCurve.size());
        assertEquals(expectedCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
        assertEquals(expectedCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
        assertEquals(expectedCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
        assertEquals(expectedCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
        var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
        assertEquals(eqKey, instrumentBusinesskey);
        var parentBusinesskey = (String) data.get("parentBusinesskey");
        assertEquals(depotKey, parentBusinesskey);
    }

    @Test
    void positionValuationFutureInstrumentCurveChanges() {
        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);
        var dayAftersecTradeDate = LocalDate.of(2022, 1, 4);
        var secDayAftersecTradeDate = LocalDate.of(2022, 1, 5);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        positionCurve.put(datebetweeenTrades, 10.0);
        positionCurve.put(secTradeDate, 5.0);
        positionRepository.save(new PositionEntity(depotKey, eqKey, positionCurve)).block();

        var instrumentCurve = new TreeMap<LocalDate, Double>();
        instrumentCurve.put(datebetweeenTrades, 1.0);
        instrumentCurve.put(secTradeDate, 2.0);
        instrumentCurve.put(dayAftersecTradeDate, 2.0);
        instrumentCurve.put(secDayAftersecTradeDate, 1.0);

        var instrumentValueCurve = new ValueCurveEntity();
        instrumentValueCurve.setInstrumentBusinesskey(eqKey);
        instrumentValueCurve.setValueCurve(instrumentCurve);
        valueCurveRepository.save(instrumentValueCurve).block();


        var expectedCurve = new TreeMap<LocalDate, Double>();
        expectedCurve.put(datebeforFirstTrade, 0.0);
        expectedCurve.put(firstTradeDate, 10.0);
        expectedCurve.put(datebetweeenTrades, 10.0);
        expectedCurve.put(secTradeDate, 10.0);
        expectedCurve.put(dayAftersecTradeDate, 10.0);
        expectedCurve.put(secDayAftersecTradeDate, 5.0);

        var position = new ValueCurve();
        position.setInstrumentBusinesskey(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);

        var creatEvent = new Event(Event.Type.START, eqKey, position);
        positionValueProcessor.accept(creatEvent);

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(6, newCurve.size());
        assertEquals(expectedCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
        assertEquals(expectedCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
        assertEquals(expectedCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
        assertEquals(expectedCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
        assertEquals(expectedCurve.get(dayAftersecTradeDate), newCurve.get(dayAftersecTradeDate.toString()));
        assertEquals(expectedCurve.get(secDayAftersecTradeDate), newCurve.get(secDayAftersecTradeDate.toString()));
        var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
        assertEquals(eqKey, instrumentBusinesskey);
        var parentBusinesskey = (String) data.get("parentBusinesskey");
        assertEquals(depotKey, parentBusinesskey);
    }

}