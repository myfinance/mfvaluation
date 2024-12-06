package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SavePositionProcessorTest extends EventProcessorTestBase {

    @Test
    void savePosition() {

        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        positionCurve.put(datebetweeenTrades, 10.0);
        positionCurve.put(secTradeDate, 5.0);

        ValueCurve position = new ValueCurve(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);



        Event creatEvent = new Event(Event.Type.CREATE, eqKey, position);
        savePositionProcessor.accept(creatEvent);

        var positions = positionRepository.findAll().collectList().block();
        assertEquals(1, positions.size());
        assertEquals(eqKey, positions.get(0).getPositionKey().getSecurityBusinessKey());
        assertEquals(depotKey, positions.get(0).getPositionKey().getDepotBusinessKey());

        var savedPositionCurve = positions.get(0).getPositionCurve();
        assertEquals(4, savedPositionCurve.size());
        assertEquals(0.0, savedPositionCurve.get(datebeforFirstTrade));
        assertEquals(10.0, savedPositionCurve.get(firstTradeDate));
        assertEquals(10.0, savedPositionCurve.get(datebetweeenTrades));
        assertEquals(5.0, savedPositionCurve.get(secTradeDate));

        var messages = getMessages(positionSavedBindingName);
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var eventtype = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("eventType");
        assertEquals("START", eventtype);
        var key = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("key");
        assertEquals(eqKey, key);
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals(depotKey, data.get("parentBusinesskey"));
        assertEquals(eqKey, data.get("instrumentBusinesskey"));
    }
}