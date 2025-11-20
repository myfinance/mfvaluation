package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValuationType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SavePositionValueProcessorTest extends EventProcessorTestBase {

    @Test
    void savePositionValue() {

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



        Event creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        savePositionValueProcessor.accept(creatEvent);

        var positionValues = positionvalueRepository.findAll().collectList().block();
        assertEquals(1, positionValues.size());
        assertEquals(eqKey, positionValues.get(0).getPositionValueKey().getSecurityBusinessKey());
        assertEquals(depotKey, positionValues.get(0).getPositionValueKey().getDepotBusinessKey());
        assertEquals(ValuationType.MARKETVALUE, positionValues.get(0).getPositionValueKey().getValuationType());

        var savedPositionValueCurve = positionValues.get(0).getPositionValueCurve();
        assertEquals(4, savedPositionValueCurve.size());
        assertEquals(0.0, savedPositionValueCurve.get(datebeforFirstTrade));
        assertEquals(10.0, savedPositionValueCurve.get(firstTradeDate));
        assertEquals(10.0, savedPositionValueCurve.get(datebetweeenTrades));
        assertEquals(5.0, savedPositionValueCurve.get(secTradeDate));

        var messages = getMessages(valuationDataChangedBindingName);
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var eventtype = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("eventType");
        assertEquals("START", eventtype);
        var key = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("key");
        assertEquals(depotKey, key);
    }
}