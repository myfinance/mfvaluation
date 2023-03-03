package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
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

public class ExtractCashflowsTests extends EventProcessorTestBase {
    @Autowired
    @Qualifier("extractCashflowsProcessor")
    protected Consumer<Event<String, Transaction>> extractCashflowsProcessor;

    String bgtKey = "incomeBgt_@10";
    String giroKey = "newGiro@1";

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
}
