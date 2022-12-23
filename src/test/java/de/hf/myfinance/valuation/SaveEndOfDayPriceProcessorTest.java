package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.EndOfDayPrice;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SaveEndOfDayPriceProcessorTest extends EventProcessorTestBase{


    @Test
    void saveEquityPrices() {
        var prices = new EndOfDayPrices();
        prices.setInstrumentBusinesskey("testkey");
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100.0, "EUR");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var price2 = new EndOfDayPrice(150.0, "USD");
        pricemap.put(LocalDate.of(2022,12,3), price2);
        prices.setPrices(pricemap);
        Event creatEvent = new Event(Event.Type.CREATE, prices.getInstrumentBusinesskey(), prices);
        saveMarketDataProcessor.accept(creatEvent);

        var savedPrices = endOfDayPricesRepository.findAll().collectList().block();
        assertEquals(1, savedPrices.size());

        assertEquals(2, savedPrices.get(0).getPrices().size());

        assertEquals("testkey", savedPrices.get(0).getInstrumentBusinesskey());

        var savePrice = savedPrices.get(0).getPrices().get(LocalDate.of(2022,12,1));
        assertEquals(100.0, savePrice.getValue());
        assertEquals("EUR", savePrice.getCurrencyKey());

        savePrice = savedPrices.get(0).getPrices().get(LocalDate.of(2022,12,3));
        assertEquals(150.0, savePrice.getValue());
        assertEquals("USD", savePrice.getCurrencyKey());

    }
}
