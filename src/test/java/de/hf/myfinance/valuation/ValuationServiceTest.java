package de.hf.myfinance.valuation;

import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;
import de.hf.myfinance.valuation.service.ValuationService;
import de.hf.testhelper.MongoDbTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
public class ValuationServiceTest extends MongoDbTestBase {

    @Autowired
    ValueCurveRepository valueCurveRepository;

    @Autowired
    ValuationService valuationService;

    @BeforeEach
    void setupDb() {
        valueCurveRepository.deleteAll().block();
    }

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
}
