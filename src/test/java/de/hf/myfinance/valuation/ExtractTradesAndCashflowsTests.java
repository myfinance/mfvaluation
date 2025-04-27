package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.Transaction;
import de.hf.myfinance.restmodel.TransactionType;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtractTradesAndCashflowsTests extends EventProcessorTestBase {
    @Autowired
    @Qualifier("extractCashflowsAndTradesProcessor")
    protected Consumer<Event<String, Transaction>> extractCashflowsProcessor;

    String bgtKey = "incomeBgt_@10";
    String giroKey = "newGiro@1";
    String depotKey = "depot@11";
    String securityKey = "equity@14";

    @Test
    void createTransaction() {


        var desc = "testeinkommen";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.INCOME);
        transaction.setAccKey(giroKey);
        transaction.setBudgetKey(bgtKey);
        transaction.setValue(100.0);

        Event creatEvent = new Event(Event.Type.CREATE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        final List<String> messages = getMessages("extractedCashflows-out-0");


        var keyList = new ArrayList<String>();
        keyList.add(bgtKey);
        keyList.add(giroKey);
        assertEquals(2, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(keyList, desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
    }

    private void compareCashflowEvent(ArrayList<String> keyList, String desc, LocalDate transactionDate, double value, LinkedHashMap data) {
        assertEquals(transactionDate.toString(), data.get("transactiondate"));
        assertEquals(value, data.get("value"));
        assertEquals(desc, data.get("description"));

        var key = data.get("instrumentBusinesskey");
        assertTrue(keyList.contains(key));
        keyList.remove(key);
    }

    @Test
    void deleteTransaction() {


        var desc = "testeinkommen";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.INCOME);
        transaction.setAccKey(giroKey);
        transaction.setBudgetKey(bgtKey);
        transaction.setValue(100.0);

        Event creatEvent = new Event(Event.Type.DELETE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        final List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(2, messages.size());


        var keyList = new ArrayList<String>();
        keyList.add(bgtKey);
        keyList.add(giroKey);

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(keyList, desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
    }

    @Test
    void extractTradeFromBuy() {


        var desc = "testTrade";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.BUY);
        transaction.setAccKey(giroKey);
        transaction.setBudgetKey(bgtKey);
        transaction.setValue(100.0);
        transaction.setAmount(10.0);
        transaction.setDepotBusinessKey(depotKey);
        transaction.setSecurityBusinessKey(securityKey);

        var creatEvent = new Event(Event.Type.CREATE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(4, messages.size());
        var keyList = new ArrayList<String>();
        keyList.add(bgtKey);
        keyList.add(giroKey);
        keyList.add(depotKey);
        keyList.add(securityKey);
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(keyList, desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(2))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(3))).get("data"));

        messages = getMessages("extractedTrade-out-0");
        assertEquals(1, messages.size());
        jsonHelper = new JsonHelper();
        data = (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareTradeEvent(depotKey, securityKey, 10.0, transactionDate,(LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
    }

    @Test
    void extractTradeFromSell() {


        var desc = "testTrade";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.SELL);
        transaction.setAccKey(giroKey);
        transaction.setBudgetKey(bgtKey);
        transaction.setValue(100.0);
        transaction.setAmount(10.0);
        transaction.setDepotBusinessKey(depotKey);
        transaction.setSecurityBusinessKey(securityKey);

        var creatEvent = new Event(Event.Type.CREATE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(4, messages.size());
        var keyList = new ArrayList<String>();
        keyList.add(bgtKey);
        keyList.add(giroKey);
        keyList.add(depotKey);
        keyList.add(securityKey);
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(keyList, desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(2))).get("data"));
        compareCashflowEvent(keyList, desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(3))).get("data"));
    
        messages = getMessages("extractedTrade-out-0");
        assertEquals(1, messages.size());
        jsonHelper = new JsonHelper();
        data = (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareTradeEvent(depotKey, securityKey, -10.0, transactionDate, (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
    }

    private void compareTradeEvent(String depotKey, String securityKey, double amount,LocalDate tradeDate, LinkedHashMap data) {
        assertEquals(depotKey, data.get("depotBusinessKey"));
        assertEquals(amount, data.get("amount"));
        assertEquals(securityKey, data.get("securityBusinessKey"));
        assertEquals(tradeDate.toString(), data.get("tradeDate"));
    }
}
