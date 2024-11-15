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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

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
        var cashflows = new HashMap<String, Double>();
        cashflows.put(bgtKey, 100.0);
        cashflows.put(giroKey, 100.0);
        transaction.setCashflows(cashflows);

        Event creatEvent = new Event(Event.Type.CREATE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        final List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(2, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
    }

    private void compareCashflowEvent(String desc, LocalDate transactionDate, double value, LinkedHashMap data) {
        assertEquals(transactionDate.toString(), data.get("transactiondate"));
        assertEquals(value, data.get("value"));
        assertEquals(desc, data.get("description"));
        if(!data.get("instrumentBusinesskey").equals(bgtKey)) {
            assertEquals(giroKey, data.get("instrumentBusinesskey"));
        }
    }

    @Test
    void deleteTransaction() {


        var desc = "testeinkommen";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.INCOME);
        var cashflows = new HashMap<String, Double>();
        cashflows.put(bgtKey, 100.0);
        cashflows.put(giroKey, 100.0);
        transaction.setCashflows(cashflows);

        Event creatEvent = new Event(Event.Type.DELETE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        final List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(2, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
    }

    @Test
    void extractTradeFromBuy() {


        var desc = "testTrade";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.BUY);
        var cashflows = new HashMap<String, Double>();
        cashflows.put(bgtKey, -100.0);
        cashflows.put(giroKey, -100.0);
        transaction.setCashflows(cashflows);
        transaction.setTradeInfo(new Trade(depotKey, securityKey, 10.0));

        var creatEvent = new Event(Event.Type.CREATE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(2, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(desc, transactionDate, -100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
    
        messages = getMessages("extractedTrade-out-0");
        assertEquals(1, messages.size());
        jsonHelper = new JsonHelper();
        data = (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareTradeEvent(depotKey, securityKey, 10.0, (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
    }

    @Test
    void extractTradeFromSell() {


        var desc = "testTrade";
        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var transaction = new Transaction(desc, transactionDate, TransactionType.SELL);
        var cashflows = new HashMap<String, Double>();
        cashflows.put(bgtKey, 100.0);
        cashflows.put(giroKey, 100.0);
        transaction.setCashflows(cashflows);
        transaction.setTradeInfo(new Trade(depotKey, securityKey, 10.0));

        var creatEvent = new Event(Event.Type.CREATE, transaction.hashCode(), transaction);
        extractCashflowsProcessor.accept(creatEvent);

        List<String> messages = getMessages("extractedCashflows-out-0");
        assertEquals(2, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareCashflowEvent(desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
        compareCashflowEvent(desc, transactionDate, 100.0, (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data"));
    
        messages = getMessages("extractedTrade-out-0");
        assertEquals(1, messages.size());
        jsonHelper = new JsonHelper();
        data = (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        compareTradeEvent(depotKey, securityKey, -10.0, (LinkedHashMap<String, Object>)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data"));
    }

    private void compareTradeEvent(String depotKey, String securityKey, double amount, LinkedHashMap data) {
        assertEquals(depotKey, data.get("depotBusinessKey"));
        assertEquals(amount, data.get("amount"));
        assertEquals(securityKey, data.get("securityBusinessKey"));
    }
}
