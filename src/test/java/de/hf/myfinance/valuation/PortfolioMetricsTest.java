package de.hf.myfinance.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import de.hf.framework.audit.AuditService;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.InstrumentType;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.PortfolioMetricsCalculatedEventHandler;
import de.hf.myfinance.valuation.persistence.DataReader;
import de.hf.myfinance.valuation.service.PortfolioMetricsCalculator;
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

    }

    @Test
    void testCalcCagr() {
        DataReader dataReader = mock(DataReader.class);
        PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler = mock(PortfolioMetricsCalculatedEventHandler.class);
        AuditService auditService = mock(AuditService.class);

        PortfolioMetricsCalculator cagrCalculator = new PortfolioMetricsCalculator(dataReader, portfolioMetricsCalculatedEventHandler, auditService);

        List<Cashflow> cashflows = new ArrayList<>();
        cashflows.add(new Cashflow("buy", LocalDate.of(2020, 1, 1), "sec1", -10000.0));
        cashflows.add(new Cashflow("buy2", LocalDate.of(2021, 3, 1), "sec1", -5000.0));
        cashflows.add(new Cashflow("dividend", LocalDate.of(2022, 2, 15), "sec1", 300.0));
        cashflows.add(new Cashflow("sell", LocalDate.of(2023, 1, 20), "sec1", 2000.0));
        cashflows.add(new Cashflow("final", LocalDate.of(2025, 1, 1), "sec1", 20000.0));
        


        var result = cagrCalculator.calcCagr(cashflows, LocalDate.of(2025, 1, 1));

        assertEquals(0.094, result, 0.001);
    }

    @Test
    void testCalcNegativCagr() {
        DataReader dataReader = mock(DataReader.class);
        PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler = mock(PortfolioMetricsCalculatedEventHandler.class);
        AuditService auditService = mock(AuditService.class);

        PortfolioMetricsCalculator cagrCalculator = new PortfolioMetricsCalculator(dataReader, portfolioMetricsCalculatedEventHandler, auditService);

        List<Cashflow> cashflows = new ArrayList<>();
        cashflows.add(new Cashflow("buy", LocalDate.of(2006, 5, 28), "sec1", -4166.64));
        cashflows.add(new Cashflow("buy2", LocalDate.of(2006, 6, 18), "sec1", -312.47));
        cashflows.add(new Cashflow("buy3", LocalDate.of(2006, 7, 17), "sec1", -312.52));
        cashflows.add(new Cashflow("final", LocalDate.of(2006, 12, 31), "sec1", 2014.57));
        
        var result = cagrCalculator.calcCagr(cashflows, LocalDate.of(2006, 12, 31));

        assertEquals(-0.775, result, 0.001);
    }

    @Test
    void testCalcStrongPositivCagr() {
        DataReader dataReader = mock(DataReader.class);
        PortfolioMetricsCalculatedEventHandler portfolioMetricsCalculatedEventHandler = mock(PortfolioMetricsCalculatedEventHandler.class);
        AuditService auditService = mock(AuditService.class);

        PortfolioMetricsCalculator cagrCalculator = new PortfolioMetricsCalculator(dataReader, portfolioMetricsCalculatedEventHandler, auditService);

        List<Cashflow> cashflows = new ArrayList<>();
        cashflows.add(new Cashflow("start", LocalDate.of(2006, 1, 1), "sec1", -2014.57));
        cashflows.add(new Cashflow("buy2", LocalDate.of(2006, 1, 2), "sec1", -180.09));
        cashflows.add(new Cashflow("dividende", LocalDate.of(2006, 1, 2), "sec1", 180.09));
        cashflows.add(new Cashflow("sell", LocalDate.of(2006, 1, 14), "sec1", 4622.98));
        
        
        var result = cagrCalculator.calcCagr(cashflows, LocalDate.of(2006, 12, 31));
        var result2 = cagrCalculator.calcCagr(cashflows, LocalDate.of(2006, 1, 14));
        var result3 = cagrCalculator.calcYield(cashflows, LocalDate.of(2006, 1, 1), LocalDate.of(2006, 1, 14));
//warum ist result1 = result2 und warum result 3 negativ?
        assertEquals(13441863054.0, result, 1000.0);
        assertEquals(13441863054.0, result2, 1000.0);
        assertEquals(1.294773, result3, 0.0001);
    }
}

