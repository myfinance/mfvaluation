package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.AdditionalMaps;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SaveInstrumentProcessorTest extends EventProcessorTestBase {

    @Autowired
    @Qualifier("saveInstrumentProcessor")
    protected Consumer<Event<String, Instrument>> saveInstrumentProcessor;

    @Test
    void createEquity() {

        var desc = "testEquity";
        var equity = new Instrument(desc, desc, InstrumentType.EQUITY, true);

        var symbols = new HashMap<AdditionalMaps, Map<String, String>>();
        var symbol = new HashMap<String, String>();
        symbol.put("aSymbol", "USD");
        symbols.put(AdditionalMaps.EQUITYSYMBOLS, symbol);
        equity.setAdditionalMaps(symbols);


        Event creatEvent = new Event(Event.Type.CREATE, equity.getBusinesskey(), equity);
        saveInstrumentProcessor.accept(creatEvent);

        var instruments = instrumentRepository.findAll().collectList().block();
        assertEquals(1, instruments.size());

        var savedinstrument = instruments.get(0);
        assertEquals(desc, savedinstrument.getBusinesskey());
        assertEquals(InstrumentType.EQUITY, savedinstrument.getInstrumentType());
        assertTrue(savedinstrument.isActive());
        assertEquals(1, savedinstrument.getAdditionalMaps().size());
        assertEquals(1, savedinstrument.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS).size());
        assertEquals("USD", savedinstrument.getAdditionalMaps().get(AdditionalMaps.EQUITYSYMBOLS).get("aSymbol"));

        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var eventtype = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("eventType");
        assertEquals("START", eventtype);
        var key = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("key");
        assertEquals(equity.getBusinesskey(), key);
    }
}
