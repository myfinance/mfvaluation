package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionKey;
import de.hf.myfinance.valuation.persistence.entities.PositionValueEntity;
import de.hf.myfinance.valuation.persistence.entities.PositionValueKey;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;
import de.hf.myfinance.valuation.service.ValuationService;
import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
public class ValuationServiceTest extends EventProcessorTestBase {

    @Autowired
    ValueCurveRepository valueCurveRepository;

    @Autowired
    ValuationService valuationService;


    @Test
    void getValueCurveAllDatesAvailable() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueMap.put(LocalDate.of(2022,1,3), 120.0);
        valueMap.put(LocalDate.of(2022,1,4), 130.0);
        valueMap.put(LocalDate.of(2022,1,5), 140.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var result = valuationService.getValueCurve("testKey", LocalDate.of(2022,1,2), LocalDate.of(2022,1,4)).block();

        assertEquals("testKey", result.getInstrumentBusinesskey());
        assertEquals(3, result.getValueCurve().keySet().size());
        assertEquals(110.0, result.getValueCurve().get(LocalDate.of(2022,1,2)));
        assertEquals(120.0, result.getValueCurve().get(LocalDate.of(2022,1,3)));
        assertEquals(130.0, result.getValueCurve().get(LocalDate.of(2022,1,4)));
    }

    @Test
    void getValueCurveSomeDatesBefore() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueMap.put(LocalDate.of(2022,1,3), 120.0);
        valueMap.put(LocalDate.of(2022,1,4), 130.0);
        valueMap.put(LocalDate.of(2022,1,5), 140.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var result = valuationService.getValueCurve("testKey", LocalDate.of(2021,12,30), LocalDate.of(2022,1,4)).block();

        assertEquals("testKey", result.getInstrumentBusinesskey());
        assertEquals(6, result.getValueCurve().keySet().size());
        assertEquals(100.0, result.getValueCurve().get(LocalDate.of(2021,12,30)));
        assertEquals(100.0, result.getValueCurve().get(LocalDate.of(2021,12,31)));
        assertEquals(100.0, result.getValueCurve().get(LocalDate.of(2022,1,1)));
        assertEquals(110.0, result.getValueCurve().get(LocalDate.of(2022,1,2)));
        assertEquals(120.0, result.getValueCurve().get(LocalDate.of(2022,1,3)));
        assertEquals(130.0, result.getValueCurve().get(LocalDate.of(2022,1,4)));
    }

    @Test
    void getValueCurveSomeDatesAfter() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueMap.put(LocalDate.of(2022,1,3), 120.0);
        valueMap.put(LocalDate.of(2022,1,4), 130.0);
        valueMap.put(LocalDate.of(2022,1,5), 140.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var result = valuationService.getValueCurve("testKey", LocalDate.of(2022,1,3), LocalDate.of(2022,1,7)).block();

        assertEquals("testKey", result.getInstrumentBusinesskey());
        assertEquals(5, result.getValueCurve().keySet().size());

        assertEquals(120.0, result.getValueCurve().get(LocalDate.of(2022,1,3)));
        assertEquals(130.0, result.getValueCurve().get(LocalDate.of(2022,1,4)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,5)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,6)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,7)));
    }

    @Test
    void getValueCurveSomeDatesBeforeAndSomeDatesAfter() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueMap.put(LocalDate.of(2022,1,3), 120.0);
        valueMap.put(LocalDate.of(2022,1,4), 130.0);
        valueMap.put(LocalDate.of(2022,1,5), 140.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var result = valuationService.getValueCurve("testKey", LocalDate.of(2021,12,30), LocalDate.of(2022,1,7)).block();

        assertEquals("testKey", result.getInstrumentBusinesskey());
        assertEquals(9, result.getValueCurve().keySet().size());
        assertEquals(100.0, result.getValueCurve().get(LocalDate.of(2021,12,30)));
        assertEquals(100.0, result.getValueCurve().get(LocalDate.of(2021,12,31)));
        assertEquals(100.0, result.getValueCurve().get(LocalDate.of(2022,1,1)));
        assertEquals(110.0, result.getValueCurve().get(LocalDate.of(2022,1,2)));
        assertEquals(120.0, result.getValueCurve().get(LocalDate.of(2022,1,3)));
        assertEquals(130.0, result.getValueCurve().get(LocalDate.of(2022,1,4)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,5)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,6)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,7)));
    }

    @Test
    void getValueCurveOnlyDatesAfter() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueMap.put(LocalDate.of(2022,1,3), 120.0);
        valueMap.put(LocalDate.of(2022,1,4), 130.0);
        valueMap.put(LocalDate.of(2022,1,5), 140.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var result = valuationService.getValueCurve("testKey", LocalDate.of(2022,1,7), LocalDate.of(2022,1,9)).block();

        assertEquals("testKey", result.getInstrumentBusinesskey());
        assertEquals(3, result.getValueCurve().keySet().size());

        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,7)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,8)));
        assertEquals(140.0, result.getValueCurve().get(LocalDate.of(2022,1,9)));
    }


    @Test
    void getValue() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueMap.put(LocalDate.of(2022,1,3), 120.0);
        valueMap.put(LocalDate.of(2022,1,4), 130.0);
        valueMap.put(LocalDate.of(2022,1,5), 140.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var result = valuationService.getValue("testKey", LocalDate.of(2022,1,2)).block();
        assertEquals(110, result);

        result = valuationService.getValue("testKey", LocalDate.of(2021,1,2)).block();
        assertEquals(100, result);

        result = valuationService.getValue("testKey", LocalDate.of(2022,2,2)).block();
        assertEquals(140, result);
    }

    @Test
    void getValuesForListOfBusinessKeys() {
        var valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey");
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 110.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        valueCurve = new ValueCurveEntity();
        valueCurve.setInstrumentBusinesskey("testKey2");
        valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 200.0);
        valueMap.put(LocalDate.of(2022,1,2), 210.0);
        valueCurve.setValueCurve(valueMap);
        valueCurveRepository.save(valueCurve).block();

        var listOfBusinessKeys = new ArrayList<String>();
        listOfBusinessKeys.add("testKey");
        listOfBusinessKeys.add("testKey2");

        Map<String,Double> resultMap = new TreeMap<String, Double>();
        Flux<Map<String,Double>> result = valuationService.getValues(listOfBusinessKeys, LocalDate.of(2022,1,2));
        result.collectList().block().forEach(r->resultMap.putAll(r));
        assertEquals(110, resultMap.get("testKey"));
        assertEquals(210, resultMap.get("testKey2"));


    }


    @Test
    void getAvgExpensesOfLastYear_noExpenses() {

        var desc = "testeinkommen";
        LocalDate transactionDate = LocalDate.of(LocalDate.now().getYear(), 1, 2);
        var cashflow = new Cashflow(desc, transactionDate, giroKey, 100.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, giroKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);


        var avgExpenses = valuationService.getAvgExpensesOfLastYear(giroKey).block();
        assertEquals(0, avgExpenses);
    }

    @Test
    void getAvgExpensesOfLastYear_singleExpenses() {
        var desc = "testeinkommen";
        LocalDate transactionDate = LocalDate.now().minusMonths(2);
        var cashflow = new Cashflow(desc, transactionDate, giroKey, -120.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, giroKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        var avgExpenses = valuationService.getAvgExpensesOfLastYear(giroKey).block();
        assertEquals(-10, avgExpenses);
    }

    @Test
    void listInstrumentCashflows() {
        var desc = "testeinkommen";
        LocalDate transactionDate = LocalDate.of(2024, 1, 2);
        var cashflow = new Cashflow(desc, transactionDate, giroKey, 100.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, giroKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        desc = "testausgabe";
        var cashflow2 = new Cashflow(desc, transactionDate, giroKey, -10.0);
        var cashflowEEvent2 = new Event(Event.Type.CREATE, giroKey, cashflow2);
        saveCashflowProcessor.accept(cashflowEEvent);

        var result = valuationService.listInstrumentCashflows(giroKey, transactionDate.minusDays(1), transactionDate.plusDays(1)).collectList().block();
        assertEquals(2, result.size());
    }

    @Test
    void getPositionsTest() {
        var positionCurve = new PositionEntity();
        positionCurve.setPositionKey(new PositionKey(depotKey, eqKey));
        var positionMap = new TreeMap<LocalDate, Double>();
        positionMap.put(LocalDate.of(2022,1,1), 10.0);
        positionMap.put(LocalDate.of(2022,1,2), 20.0);
        positionCurve.setPositionCurve(positionMap);
        positionRepository.save(positionCurve).block();

        var positionValueCurve = new PositionValueEntity();
        positionValueCurve.setPositionValueKey(new PositionValueKey(depotKey, eqKey));
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,1,1), 100.0);
        valueMap.put(LocalDate.of(2022,1,2), 220.0);
        positionValueCurve.setPositionValueCurve(valueMap);
        positionvalueRepository.save(positionValueCurve).block();

        var depotkey2 = "depotkey2";

        var positionCurve2 = new PositionEntity();
        positionCurve2.setPositionKey(new PositionKey(depotkey2, eqKey));
        var positionMap2 = new TreeMap<LocalDate, Double>();
        positionMap2.put(LocalDate.of(2022,1,1), 5.0);
        positionCurve2.setPositionCurve(positionMap2);
        positionRepository.save(positionCurve2).block();

        var positionValueCurve2 = new PositionValueEntity();
        positionValueCurve2.setPositionValueKey(new PositionValueKey(depotkey2, eqKey));
        var valueMap2 = new TreeMap<LocalDate, Double>();
        valueMap2.put(LocalDate.of(2022,1,1), 50.0);
        positionValueCurve2.setPositionValueCurve(valueMap2);
        positionvalueRepository.save(positionValueCurve2).block();

        var securitykey2 = "securitykey2";

        var positionCurve3 = new PositionEntity();
        positionCurve3.setPositionKey(new PositionKey(depotkey2, securitykey2));
        var positionMap3 = new TreeMap<LocalDate, Double>();
        positionMap3.put(LocalDate.of(2022,1,1), 20.0);
        positionCurve3.setPositionCurve(positionMap3);
        positionRepository.save(positionCurve3).block();

        var positionValueCurve3 = new PositionValueEntity();
        positionValueCurve3.setPositionValueKey(new PositionValueKey(depotkey2, securitykey2));
        var valueMap3 = new TreeMap<LocalDate, Double>();
        valueMap3.put(LocalDate.of(2022,1,1), 50.0);
        positionValueCurve3.setPositionValueCurve(valueMap3);
        positionvalueRepository.save(positionValueCurve3).block();

        var depotlist = new ArrayList<String>();
        depotlist.add(depotKey);
        depotlist.add(depotkey2);
        var result = valuationService.getPositions(depotlist).collectList().block();

        assertEquals(3, result.size());
        var positionDepot1Sec1 = result.stream().filter(p->p.getDepotId().equals(depotKey) && p.getSecurityId().equals(eqKey)).findFirst();
        assertTrue(positionDepot1Sec1.isPresent());
        assertEquals(20.0, positionDepot1Sec1.get().getAmount());
        assertEquals(220.0, positionDepot1Sec1.get().getValue());

        var positionDepot1Sec2 = result.stream().filter(p->p.getDepotId().equals(depotkey2) && p.getSecurityId().equals(eqKey)).findFirst();
        assertTrue(positionDepot1Sec2.isPresent());
        assertEquals(5.0, positionDepot1Sec2.get().getAmount());
        assertEquals(50.0, positionDepot1Sec2.get().getValue());

        var positionDepot1Sec3 = result.stream().filter(p->p.getDepotId().equals(depotkey2) && p.getSecurityId().equals(securitykey2)).findFirst();
        assertTrue(positionDepot1Sec3.isPresent());
        assertEquals(20.0, positionDepot1Sec3.get().getAmount());
        assertEquals(50.0, positionDepot1Sec3.get().getValue());
    }
}
