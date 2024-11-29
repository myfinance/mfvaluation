package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Trade;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SaveTradeProcessorTest extends EventProcessorTestBase {

    @Test
    void createTrade() {

        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var trade = new Trade(depotKey, eqKey, 10.0);
        trade.setTradeDate(transactionDate);


        Event creatEvent = new Event(Event.Type.CREATE, eqKey, trade);
        saveTradeProcessor.accept(creatEvent);

        var trades = tradeRepository.findAll().collectList().block();
        assertEquals(1, trades.size());

        var savedTrades = trades.get(0);
        assertEquals(10.0, savedTrades.getAmount());
        assertEquals(depotKey, savedTrades.getPositionKey().getDepotBusinessKey());
        assertEquals(eqKey, savedTrades.getPositionKey().getSecurityBusinessKey());
        assertEquals(transactionDate, savedTrades.getTradeDate());

        var messages = getMessages("tradeSaved-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var eventtype = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("eventType");
        assertEquals("CREATE", eventtype);
        var key = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("key");
        assertEquals(eqKey, key);
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        assertEquals(depotKey, data.get("depotBusinessKey"));
        assertEquals(eqKey, data.get("securityBusinessKey"));
    }
}