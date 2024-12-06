package de.hf.myfinance.valuation;



import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.valuation.persistence.DataReader;
import de.hf.myfinance.valuation.persistence.entities.TradeEntity;
import de.hf.testhelper.JsonHelper;

public class PositionProcessorTest  extends EventProcessorTestBase {


    @Autowired
    DataReader dataReader;

    @Test
    void positionValuation() {
        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);
        tradeRepository.save(new TradeEntity(depotKey, eqKey, 10.0, firstTradeDate)).block();
        tradeRepository.save(new TradeEntity(depotKey, eqKey, -5.0, secTradeDate)).block();


        var trade = new Trade(depotKey, eqKey, 10.0);
        trade.setTradeDate(firstTradeDate);

        var expectedCurve = new TreeMap<LocalDate, Double>();
        expectedCurve.put(datebeforFirstTrade, 0.0);
        expectedCurve.put(firstTradeDate, 10.0);
        expectedCurve.put(datebetweeenTrades, 10.0);
        expectedCurve.put(secTradeDate, 5.0);


        var creatEvent = new Event(Event.Type.CREATE, eqKey, trade);
        positionProcessor.accept(creatEvent);
        var messages = getMessages("positionBuilded-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(4, newCurve.size());
        assertEquals(0.0, newCurve.get(datebeforFirstTrade.toString()));
        assertEquals(10.0, newCurve.get(firstTradeDate.toString()));
        assertEquals(10.0, newCurve.get(datebetweeenTrades.toString()));
        assertEquals(5.0, newCurve.get(secTradeDate.toString()));
        var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
        assertEquals(eqKey, instrumentBusinesskey);
        var parentBusinesskey = (String) data.get("parentBusinesskey");
        assertEquals(depotKey, parentBusinesskey);

    }

}
