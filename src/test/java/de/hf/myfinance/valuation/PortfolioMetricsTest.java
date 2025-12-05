package de.hf.myfinance.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.testhelper.JsonHelper;

public class PortfolioMetricsTest extends EventProcessorTestBase {
    @Test
    void simpleCagrTest() {

        var depotEntity = new Instrument(depotKey, depotDesc, InstrumentType.DEPOT, true);
        Map<AdditionalProperties, String> additionalProperties = new HashMap<>();
        additionalProperties.put(AdditionalProperties.VALUEBUDGETID, "valueBudgetKey");
        depotEntity.setAdditionalProperties(additionalProperties);

        var creatEvent = new Event(Event.Type.CREATE, depotKey, depotEntity);
        saveInstrumentProcessor.accept(creatEvent);
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var eq = new Instrument(eqKey, eqDesc, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey, eq);
        saveInstrumentProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var firstTradeDate = LocalDate.of(2020, 1, 1);
        var secTradeDate = LocalDate.of(2021, 3, 1);
        var firstDividendDate = LocalDate.of(2022, 2, 15);
        var thirdTradeDate = LocalDate.of(2023, 1, 20);
        var lastDate = LocalDate.of(2025, 1, 1);

        // for the testcase i only need the value at the end
        var positionValueCurve = new TreeMap<LocalDate, Double>();
        positionValueCurve.put(lastDate, 20000.0);
        ValueCurve position = new ValueCurve(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionValueCurve);
        creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        savePositionValueProcessor.accept(creatEvent);

        var desc = "buy";
        var cashflow = new Cashflow(desc, firstTradeDate, eqKey, -10000.0);
        var cashflowEvent = new Event(Event.Type.CREATE, eqKey, cashflow);
        saveCashflowProcessor.accept(cashflowEvent);

         desc = "buy2";
         cashflow = new Cashflow(desc, secTradeDate, eqKey, -5000.0);
         cashflowEvent = new Event(Event.Type.CREATE, eqKey, cashflow);
        saveCashflowProcessor.accept(cashflowEvent);

        desc = "dividend";
         cashflow = new Cashflow(desc, firstDividendDate, eqKey, 300.0);
         cashflowEvent = new Event(Event.Type.CREATE, eqKey, cashflow);
        saveCashflowProcessor.accept(cashflowEvent);

        desc = "sell";
         cashflow = new Cashflow(desc, thirdTradeDate, eqKey, 2000.0);
         cashflowEvent = new Event(Event.Type.CREATE, eqKey, cashflow);
        saveCashflowProcessor.accept(cashflowEvent);

        var event = new Event(Event.Type.START, "start", "start");
        portfolioMetricsProcessor.accept(event);
        messages = getMessages("portfolioMetricsCalculated-out-0");
        assertEquals(6, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        messages.forEach(m->{
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(m).get("data");
            var portfolio = (String) data.get("portfolio");
            var totalCagr = (Double) data.get("totalCagr");
            if(portfolio.equals("TOTAL")) {
                 assertEquals(0.0, totalCagr);
            }

        });
    }
}
