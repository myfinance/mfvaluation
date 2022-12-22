package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.util.TreeMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class saveValueCurveTest  extends EventProcessorTestBase {

    @Autowired
    @Qualifier("saveValueCurveProcessor")
    protected Consumer<Event<String, ValueCurve>> saveValueCurveProcessor;

    @Test
    void saveValueCurve() {

        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var instrumentBusinesKey = "instrumentBusinesskey";

        var valueCurve = new ValueCurve(instrumentBusinesKey);
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(transactionDate, 100.0);
        valueCurve.setValueCurve(valueMap);

        Event creatEvent = new Event(Event.Type.CREATE, instrumentBusinesKey, valueCurve);
        saveValueCurveProcessor.accept(creatEvent);

        var curves = valueCurveRepository.findAll().collectList().block();
        assertEquals(1, curves.size());

        var curve = curves.get(0);
        assertEquals(instrumentBusinesKey, curve.getInstrumentBusinesskey());
        assertEquals(1, curve.getValueCurve().size());
        assertEquals(100.0, curve.getValueCurve().get(transactionDate));

        var messages = getMessages(valuationDataChangedBindingName);
        assertEquals(0, messages.size());
    }

    @Test
    void saveValueCurveTriggerParent() {

        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var instrumentBusinesKey = "instrumentBusinesskey";

        var valueCurve = new ValueCurve(instrumentBusinesKey);
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(transactionDate, 100.0);
        valueCurve.setValueCurve(valueMap);
        valueCurve.setParentBusinesskey("parentKey");

        Event creatEvent = new Event(Event.Type.CREATE, instrumentBusinesKey, valueCurve);
        saveValueCurveProcessor.accept(creatEvent);

        var curves = valueCurveRepository.findAll().collectList().block();
        assertEquals(1, curves.size());

        var curve = curves.get(0);
        assertEquals(instrumentBusinesKey, curve.getInstrumentBusinesskey());
        assertEquals(1, curve.getValueCurve().size());
        assertEquals(100.0, curve.getValueCurve().get(transactionDate));

        var messages = getMessages(valuationDataChangedBindingName);
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var eventtype = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("eventType");
        assertEquals("START", eventtype);
        var key = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("key");
        assertEquals("parentKey", key);
    }
}
