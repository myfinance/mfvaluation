package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;

import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SaveCashflowsProcessorTest extends EventProcessorTestBase {

    @Test
    void createCashflow() {

        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var instrumentBusinesKey = "instrumentBusinesskey";
        var desc = "testcashflow";
        var cashflow = new Cashflow(desc, transactionDate, instrumentBusinesKey, 100.0);


        Event creatEvent = new Event(Event.Type.CREATE, instrumentBusinesKey, cashflow);
        saveCashflowProcessor.accept(creatEvent);

        var cashflows = cashflowRepository.findAll().collectList().block();
        assertEquals(1, cashflows.size());

        var savedcashflow = cashflows.get(0);
        assertEquals(desc, savedcashflow.getDescription());
        assertEquals(instrumentBusinesKey, savedcashflow.getInstrumentBusinesskey());
        assertEquals(100.0, savedcashflow.getValue());
        assertEquals(transactionDate, savedcashflow.getTransactiondate());

        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());
        JsonHelper jsonHelper = new JsonHelper();
        var eventtype = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("eventType");
        assertEquals("START", eventtype);
        var key = (String)jsonHelper.convertJsonStringToMap((messages.get(0))).get("key");
        assertEquals(instrumentBusinesKey, key);

    }
}
