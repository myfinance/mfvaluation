package de.hf.myfinance.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.valuation.persistence.DataReader;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;
import de.hf.myfinance.valuation.persistence.entities.TradeEntity;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import de.hf.testhelper.JsonHelper;

public class PostionValueProcessorTest extends EventProcessorTestBase {


    @Autowired
    DataReader dataReader;

        @Test
    void positionValuationMultiBuy() {
        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        positionCurve.put(datebetweeenTrades, 10.0);
        positionCurve.put(secTradeDate, 15.0);
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

        tradeRepository.save(new TradeEntity(depotKey, eqKey, 10.0, firstTradeDate)).block();
        tradeRepository.save(new TradeEntity(depotKey, eqKey, 5.0, secTradeDate)).block();


        var expectedMarketValueCurve = new TreeMap<LocalDate, Double>();
        expectedMarketValueCurve.put(datebeforFirstTrade, 0.0);
        expectedMarketValueCurve.put(firstTradeDate, 10.0);
        expectedMarketValueCurve.put(datebetweeenTrades, 20.0);
        expectedMarketValueCurve.put(secTradeDate, 30.0);

        var expectedStaticValueCurve = new TreeMap<LocalDate, Double>();
        expectedStaticValueCurve.put(datebeforFirstTrade, 0.0);
        expectedStaticValueCurve.put(firstTradeDate, 10.0);
        expectedStaticValueCurve.put(datebetweeenTrades, 10.0);
        expectedStaticValueCurve.put(secTradeDate, 20.0);

        var expectedPrudentValueCurve = new TreeMap<LocalDate, Double>();
        expectedPrudentValueCurve.put(datebeforFirstTrade, 0.0);
        expectedPrudentValueCurve.put(firstTradeDate, 10.0);
        expectedPrudentValueCurve.put(datebetweeenTrades, 17.0);
        expectedPrudentValueCurve.put(secTradeDate, 27.0);

        var creatEvent = new Event(Event.Type.START, eqKey, depotKey);
        positionValueProcessor.accept(creatEvent);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(3, messages.size()); // Changed from 1 to 4

        JsonHelper jsonHelper = new JsonHelper();
        for (String message : messages) {
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(message).get("data");
            var valuationType = (String) data.get("valuationType");
            var newCurve = (LinkedHashMap) data.get("valueCurve");

            switch (valuationType) {
                case "MARKETVALUE":
                    assertEquals(expectedMarketValueCurve.size(), newCurve.size());
                    assertEquals(expectedMarketValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedMarketValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedMarketValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedMarketValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                case "STATIC":
                    assertEquals(expectedStaticValueCurve.size(), newCurve.size());
                    assertEquals(expectedStaticValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedStaticValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedStaticValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedStaticValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                case "PRUDENT":
                    assertEquals(expectedPrudentValueCurve.size(), newCurve.size());
                    assertEquals(expectedPrudentValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedPrudentValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedPrudentValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedPrudentValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                default:
                    // Handle unexpected valuation types if necessary
                    break;
            }
            var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
            assertEquals(eqKey, instrumentBusinesskey);
            var parentBusinesskey = (String) data.get("parentBusinesskey");
            assertEquals(depotKey, parentBusinesskey);
        }
    }

    @Test
    void positionValuationWithSell() {
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

        tradeRepository.save(new TradeEntity(depotKey, eqKey, 10.0, firstTradeDate)).block();
        tradeRepository.save(new TradeEntity(depotKey, eqKey, -5.0, secTradeDate)).block();


        var expectedMarketValueCurve = new TreeMap<LocalDate, Double>();
        expectedMarketValueCurve.put(datebeforFirstTrade, 0.0);
        expectedMarketValueCurve.put(firstTradeDate, 10.0);
        expectedMarketValueCurve.put(datebetweeenTrades, 20.0);
        expectedMarketValueCurve.put(secTradeDate, 10.0);

        var expectedStaticValueCurve = new TreeMap<LocalDate, Double>();
        expectedStaticValueCurve.put(datebeforFirstTrade, 0.0);
        expectedStaticValueCurve.put(firstTradeDate, 10.0);
        expectedStaticValueCurve.put(datebetweeenTrades, 10.0);
        expectedStaticValueCurve.put(secTradeDate, 5.0);

        var expectedPrudentValueCurve = new TreeMap<LocalDate, Double>();
        expectedPrudentValueCurve.put(datebeforFirstTrade, 0.0);
        expectedPrudentValueCurve.put(firstTradeDate, 10.0);
        expectedPrudentValueCurve.put(datebetweeenTrades, 17.0);
        expectedPrudentValueCurve.put(secTradeDate, 8.5);

        var creatEvent = new Event(Event.Type.START, eqKey, depotKey);
        positionValueProcessor.accept(creatEvent);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(3, messages.size()); // Changed from 1 to 4

        JsonHelper jsonHelper = new JsonHelper();
        for (String message : messages) {
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(message).get("data");
            var valuationType = (String) data.get("valuationType");
            var newCurve = (LinkedHashMap) data.get("valueCurve");

            switch (valuationType) {
                case "MARKETVALUE":
                    assertEquals(expectedMarketValueCurve.size(), newCurve.size());
                    assertEquals(expectedMarketValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedMarketValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedMarketValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedMarketValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                case "STATIC":
                    assertEquals(expectedStaticValueCurve.size(), newCurve.size());
                    assertEquals(expectedStaticValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedStaticValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedStaticValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedStaticValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                case "PRUDENT":
                    assertEquals(expectedPrudentValueCurve.size(), newCurve.size());
                    assertEquals(expectedPrudentValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedPrudentValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedPrudentValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedPrudentValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                default:
                    // Handle unexpected valuation types if necessary
                    break;
            }
            var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
            assertEquals(eqKey, instrumentBusinesskey);
            var parentBusinesskey = (String) data.get("parentBusinesskey");
            assertEquals(depotKey, parentBusinesskey);
        }
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

        tradeRepository.save(new TradeEntity(depotKey, eqKey, 10.0, firstTradeDate)).block();
        tradeRepository.save(new TradeEntity(depotKey, eqKey, -5.0, secTradeDate)).block();

        var expectedMarketValueCurve = new TreeMap<LocalDate, Double>();
        expectedMarketValueCurve.put(datebeforFirstTrade, 0.0);
        expectedMarketValueCurve.put(firstTradeDate, 20.0);
        expectedMarketValueCurve.put(datebetweeenTrades, 20.0);
        expectedMarketValueCurve.put(secTradeDate, 10.0);

        var expectedStaticValueCurve = new TreeMap<LocalDate, Double>();
        expectedStaticValueCurve.put(datebeforFirstTrade, 0.0);
        expectedStaticValueCurve.put(firstTradeDate, 20.0);
        expectedStaticValueCurve.put(datebetweeenTrades, 20.0);
        expectedStaticValueCurve.put(secTradeDate, 10.0);


        var expectedPrudentValueCurve = new TreeMap<LocalDate, Double>();
        expectedPrudentValueCurve.put(datebeforFirstTrade, 0.0);
        expectedPrudentValueCurve.put(firstTradeDate, 20.0);
        expectedPrudentValueCurve.put(datebetweeenTrades, 20.0);
        expectedPrudentValueCurve.put(secTradeDate, 10.0);


        var creatEvent = new Event(Event.Type.START, eqKey, depotKey);
        positionValueProcessor.accept(creatEvent);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(3, messages.size()); // Changed from 1 to 4

        JsonHelper jsonHelper = new JsonHelper();
        for (String message : messages) {
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(message).get("data");
            var valuationType = (String) data.get("valuationType");
            var newCurve = (LinkedHashMap) data.get("valueCurve");

            switch (valuationType) {
                case "MARKETVALUE":
                    assertEquals(expectedMarketValueCurve.size(), newCurve.size());
                    assertEquals(expectedMarketValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedMarketValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedMarketValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedMarketValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                case "STATIC":
                    assertEquals(expectedStaticValueCurve.size(), newCurve.size());
                    assertEquals(expectedStaticValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedStaticValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedStaticValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedStaticValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                case "PRUDENT":
                    assertEquals(expectedPrudentValueCurve.size(), newCurve.size());
                    assertEquals(expectedPrudentValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedPrudentValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedPrudentValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedPrudentValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    break;
                default:
                    // Handle unexpected valuation types if necessary
                    break;
            }
            var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
            assertEquals(eqKey, instrumentBusinesskey);
            var parentBusinesskey = (String) data.get("parentBusinesskey");
            assertEquals(depotKey, parentBusinesskey);
        }
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

        tradeRepository.save(new TradeEntity(depotKey, eqKey, 10.0, firstTradeDate)).block();
        tradeRepository.save(new TradeEntity(depotKey, eqKey, -5.0, secTradeDate)).block();

        var instrumentCurve = new TreeMap<LocalDate, Double>();
        instrumentCurve.put(datebetweeenTrades, 1.0);
        instrumentCurve.put(secTradeDate, 2.0);
        instrumentCurve.put(dayAftersecTradeDate, 2.0);
        instrumentCurve.put(secDayAftersecTradeDate, 1.0);

        var instrumentValueCurve = new ValueCurveEntity();
        instrumentValueCurve.setInstrumentBusinesskey(eqKey);
        instrumentValueCurve.setValueCurve(instrumentCurve);
        valueCurveRepository.save(instrumentValueCurve).block();


        var expectedMarketValueCurve = new TreeMap<LocalDate, Double>();
        expectedMarketValueCurve.put(datebeforFirstTrade, 0.0);
        expectedMarketValueCurve.put(firstTradeDate, 10.0);
        expectedMarketValueCurve.put(datebetweeenTrades, 10.0);
        expectedMarketValueCurve.put(secTradeDate, 10.0);
        expectedMarketValueCurve.put(dayAftersecTradeDate, 10.0);
        expectedMarketValueCurve.put(secDayAftersecTradeDate, 5.0);

        var expectedStaticValueCurve = new TreeMap<LocalDate, Double>();
        expectedStaticValueCurve.put(datebeforFirstTrade, 0.0);
        expectedStaticValueCurve.put(firstTradeDate, 10.0);
        expectedStaticValueCurve.put(datebetweeenTrades, 10.0);
        expectedStaticValueCurve.put(secTradeDate, 5.0);
        expectedStaticValueCurve.put(dayAftersecTradeDate, 5.0);
        expectedStaticValueCurve.put(secDayAftersecTradeDate, 5.0);


        var expectedPrudentValueCurve = new TreeMap<LocalDate, Double>();
        expectedPrudentValueCurve.put(datebeforFirstTrade, 0.0);
        expectedPrudentValueCurve.put(firstTradeDate, 10.0);
        expectedPrudentValueCurve.put(datebetweeenTrades, 10.0);
        expectedPrudentValueCurve.put(secTradeDate, 8.5);
        expectedPrudentValueCurve.put(dayAftersecTradeDate, 8.5);
        expectedPrudentValueCurve.put(secDayAftersecTradeDate, 5.0);

        var creatEvent = new Event(Event.Type.START, eqKey, depotKey);
        positionValueProcessor.accept(creatEvent);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var messages = getMessages(positionValueCalculatedBindingName);
        assertEquals(3, messages.size()); // Changed from 1 to 4

        JsonHelper jsonHelper = new JsonHelper();
        for (String message : messages) {
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(message).get("data");
            var valuationType = (String) data.get("valuationType");
            var newCurve = (LinkedHashMap) data.get("valueCurve");

            switch (valuationType) {
                case "MARKETVALUE":
                    assertEquals(expectedMarketValueCurve.size(), newCurve.size());
                    assertEquals(expectedMarketValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedMarketValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedMarketValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedMarketValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    assertEquals(expectedMarketValueCurve.get(dayAftersecTradeDate), newCurve.get(dayAftersecTradeDate.toString()));
                    assertEquals(expectedMarketValueCurve.get(secDayAftersecTradeDate), newCurve.get(secDayAftersecTradeDate.toString()));
                    break;
                case "STATIC":
                    assertEquals(expectedStaticValueCurve.size(), newCurve.size());
                    assertEquals(expectedStaticValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedStaticValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedStaticValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedStaticValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    assertEquals(expectedStaticValueCurve.get(dayAftersecTradeDate), newCurve.get(dayAftersecTradeDate.toString()));
                    assertEquals(expectedStaticValueCurve.get(secDayAftersecTradeDate), newCurve.get(secDayAftersecTradeDate.toString()));
                    break;
                case "PRUDENT":
                    assertEquals(expectedPrudentValueCurve.size(), newCurve.size());
                    assertEquals(expectedPrudentValueCurve.get(datebeforFirstTrade), newCurve.get(datebeforFirstTrade.toString()));
                    assertEquals(expectedPrudentValueCurve.get(firstTradeDate), newCurve.get(firstTradeDate.toString()));
                    assertEquals(expectedPrudentValueCurve.get(datebetweeenTrades), newCurve.get(datebetweeenTrades.toString()));
                    assertEquals(expectedPrudentValueCurve.get(secTradeDate), newCurve.get(secTradeDate.toString()));
                    assertEquals(expectedPrudentValueCurve.get(dayAftersecTradeDate), newCurve.get(dayAftersecTradeDate.toString()));
                    assertEquals(expectedPrudentValueCurve.get(secDayAftersecTradeDate), newCurve.get(secDayAftersecTradeDate.toString()));
                    break;
                default:
                    // Handle unexpected valuation types if necessary
                    break;
            }
            var instrumentBusinesskey = (String) data.get("instrumentBusinesskey");
            assertEquals(eqKey, instrumentBusinesskey);
            var parentBusinesskey = (String) data.get("parentBusinesskey");
            assertEquals(depotKey, parentBusinesskey);
        }
    }

}