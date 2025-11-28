package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.*;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValuationTest  extends EventProcessorTestBase {

    LocalDate transactionDate = LocalDate.of(2022, 1, 1);


    @Test
    void giroValuation() {

        var giroEntity = new Instrument(giroKey, giroDesc, InstrumentType.GIRO, true);
        Event creatEvent = new Event(Event.Type.CREATE, giroKey, giroEntity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        Event valuationEvent = new Event(Event.Type.START, giroKey, giroKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

        LocalDate transactionDate = LocalDate.of(2022, 1, 1);
        var desc = "testcashflow";
        var cashflow = new Cashflow(desc, transactionDate, giroKey, 100.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, giroKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        valuationEvent = new Event(Event.Type.START, giroKey, giroKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(2, newCurve.size());
        assertEquals(100.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));
    }

    @Test
    void emptyDepotValuation() {

        var depotEntity = new Instrument(depotKey, depotDesc, InstrumentType.DEPOT, true);
        var creatEvent = new Event(Event.Type.CREATE, depotKey, depotEntity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, depotKey, depotKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(3, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(1))).get("data");
        var curve2 = (LinkedHashMap) data.get("valueCurve");
        curve2.keySet().forEach(i->assertEquals(0.0, curve2.get(i)));

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(2))).get("data");
        var curve3 = (LinkedHashMap) data.get("valueCurve");
        curve3.keySet().forEach(i->assertEquals(0.0, curve3.get(i)));

    }

    @Test
    void depotValuation() {

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

        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        positionCurve.put(datebetweeenTrades, 10.0);
        positionCurve.put(secTradeDate, 5.0);
        ValueCurve position = new ValueCurve(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);
        creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        savePositionValueProcessor.accept(creatEvent);

        var valuationEvent = new Event(Event.Type.START, depotKey, depotKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(3, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        messages.forEach(m->{
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(m).get("data");
            var valType = (String) data.get("valuationType");
            if(valType.equals(ValuationType.MARKETVALUE.toString())){
                var curve = (LinkedHashMap) data.get("valueCurve");
                assertEquals(0.0, curve.get(datebeforFirstTrade.toString()));
                assertEquals(10.0, curve.get(firstTradeDate.toString()));
                assertEquals(10.0, curve.get(datebetweeenTrades.toString()));
                assertEquals(5.0, curve.get(secTradeDate.toString()));
                var linkedInstrumentKey = (String) data.get("linkedInstrumentKey");
                assertEquals("valueBudgetKey", linkedInstrumentKey);
            }

        });


    }


    @Test
    void depotValuationMultiPositions() {

        var depotEntity = new Instrument(depotKey, depotDesc, InstrumentType.DEPOT, true);
        var creatEvent = new Event(Event.Type.CREATE, depotKey, depotEntity);
        saveInstrumentProcessor.accept(creatEvent);
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var eq = new Instrument(eqKey, eqDesc, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey, eq);
        saveInstrumentProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        String eqDesc2= "anEquity2";
        String eqKey2 = eqDesc2 + "@14";
        var eq2 = new Instrument(eqKey2, eqDesc2, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey2, eq2);
        saveInstrumentProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        ValueCurve position = new ValueCurve(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);
        creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        messageDuplicationFilter.clear();
        savePositionValueProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());
        purgeMessages("valuationDataChanged-out-0");

        positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebetweeenTrades, 0.0);
        positionCurve.put(secTradeDate, 10.0);
        position = new ValueCurve(eqKey2);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);
        creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        messageDuplicationFilter.clear();
        savePositionValueProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, depotKey, depotKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(3, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        messages.forEach(m->{
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(m).get("data");
            var valType = (String) data.get("valuationType");
            if(valType.equals(ValuationType.MARKETVALUE.toString())){
                var curve = (LinkedHashMap) data.get("valueCurve");
                assertEquals(0.0, curve.get(datebeforFirstTrade.toString()));
                assertEquals(10.0, curve.get(firstTradeDate.toString()));
                assertEquals(10.0, curve.get(datebetweeenTrades.toString()));
                assertEquals(20.0, curve.get(secTradeDate.toString()));
            }

        });
    }

    @Test
    void messageDuplicationFilterTest() {

        var depotEntity = new Instrument(depotKey, depotDesc, InstrumentType.DEPOT, true);
        var creatEvent = new Event(Event.Type.CREATE, depotKey, depotEntity);
        saveInstrumentProcessor.accept(creatEvent);
        assertTrue(messageDuplicationFilter.isAlreadyQueued(depotKey));

        var eq = new Instrument(eqKey, eqDesc, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey, eq);
        saveInstrumentProcessor.accept(creatEvent);
        assertTrue(messageDuplicationFilter.isAlreadyQueued(eqKey));
        assertEquals(2, messageDuplicationFilter.getKeys().size());

        String eqDesc2= "anEquity2";
        String eqKey2 = eqDesc2 + "@14";
        var eq2 = new Instrument(eqKey2, eqDesc2, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey2, eq2);
        saveInstrumentProcessor.accept(creatEvent);
        assertTrue(messageDuplicationFilter.isAlreadyQueued(eqKey2));
        assertEquals(3, messageDuplicationFilter.getKeys().size());

        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);

        var positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebeforFirstTrade, 0.0);
        positionCurve.put(firstTradeDate, 10.0);
        ValueCurve position = new ValueCurve(eqKey);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);
        creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        savePositionValueProcessor.accept(creatEvent);
        assertEquals(3, messageDuplicationFilter.getKeys().size());

        positionCurve = new TreeMap<LocalDate, Double>();
        positionCurve.put(datebetweeenTrades, 0.0);
        positionCurve.put(secTradeDate, 10.0);
        position = new ValueCurve(eqKey2);
        position.setParentBusinesskey(depotKey);
        position.setValueCurve(positionCurve);
        creatEvent = new Event(Event.Type.CREATE, depotKey, position);
        savePositionValueProcessor.accept(creatEvent);

        var valuationEvent = new Event(Event.Type.START, depotKey, depotKey);
        valuationProcessor.accept(valuationEvent);
         
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(3, messages.size());
    }

    @Test
    void depotAndValueBudgetValuation() {

        var depotEntity = new Instrument(depotKey, depotDesc, InstrumentType.DEPOT, true);
        Map<AdditionalProperties, String> additionalProperties = new HashMap<>();
        additionalProperties.put(AdditionalProperties.VALUEBUDGETID, budgetKey);
        depotEntity.setAdditionalProperties(additionalProperties);

        var creatEvent = new Event(Event.Type.CREATE, depotKey, depotEntity);
        saveInstrumentProcessor.accept(creatEvent);
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var budgetEntity = new Instrument(budgetKey, budgetDesc, InstrumentType.BUDGET, true);
        creatEvent = new Event(Event.Type.CREATE, budgetKey, budgetEntity);
        saveInstrumentProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());


        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);
        var transactionDate = LocalDate.of(2022, 1, 4);
        var dateAftertransactionDate = LocalDate.of(2022, 1, 5);

        var valueCurve = new ValueCurve(depotKey);
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(datebeforFirstTrade, 0.0);
        valueMap.put(firstTradeDate, 10.0);
        valueMap.put(datebetweeenTrades, 10.0);
        valueMap.put(secTradeDate, 5.0);
        valueCurve.setValueCurve(valueMap);

        creatEvent = new Event(Event.Type.CREATE, depotKey, valueCurve);
        saveValueCurveProcessor.accept(creatEvent);


        var desc = "testcashflow";
        var cashflow = new Cashflow(desc, transactionDate, budgetKey, 100.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, budgetKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        var valuationEvent = new Event(Event.Type.START, budgetKey, budgetKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(3, messages.size());
        messages.forEach(m->{
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(m).get("data");
            var valType = (String) data.get("valuationType");
            if(valType.equals(ValuationType.MARKETVALUE.toString())){
                var newCurve = (LinkedHashMap) data.get("valueCurve");
                assertEquals(0.0, newCurve.get(datebeforFirstTrade.toString()));
                assertEquals(10.0, newCurve.get(firstTradeDate.toString()));
                assertEquals(10.0, newCurve.get(datebetweeenTrades.toString()));
                assertEquals(5.0, newCurve.get(secTradeDate.toString()));
                assertEquals(105.0, newCurve.get(transactionDate.toString()));
                assertEquals(105.0, newCurve.get(dateAftertransactionDate.toString()));
            }
        });
    }

    @Test
    void TenantValuation() {

        addInstrument(InstrumentType.TENANT, tenantKey, tenantDesc, null);
        addInstrument(InstrumentType.ACCOUNTPORTFOLIO, accountPfKey, accountPfDesc, tenantKey);
        addInstrument(InstrumentType.BUDGETPORTFOLIO, budgetPfKey, budgetPfDesc, tenantKey);
        addInstrument(InstrumentType.GIRO, giroKey, giroDesc, accountPfKey);
        addInstrument(InstrumentType.BUDGETGROUP, budgetGroupKey, budgetGroupDesc, budgetPfKey);
        addInstrument(InstrumentType.BUDGET, budgetKey, budgetDesc, budgetGroupKey);

        createAndSaveNewTestCurve(giroKey);
        createAndSaveNewTestCurve(budgetKey);

        startAndTestCurveCalc(accountPfKey);
        startAndTestCurveCalc(budgetGroupKey);

        startAndTestCurveCalc(budgetPfKey);

        startAndTestCurveCalc(tenantKey);
    }

    @Test
    void currencyValuation() {
        var currency = new Instrument(currencyKey, currencyDesc, InstrumentType.CURRENCY, true);
        var creatEvent = new Event(Event.Type.CREATE, currencyKey, currency);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, currencyKey, currencyKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));



        var prices = new EndOfDayPrices();
        prices.setInstrumentBusinesskey(currencyKey);
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(0.9, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var price2 = new EndOfDayPrice(0.8, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,3), price2);
        prices.setPrices(pricemap);
        creatEvent = new Event(Event.Type.CREATE, prices.getInstrumentBusinesskey(), prices);
        saveMarketDataProcessor.accept(creatEvent);

        var savedPrices = endOfDayPricesRepository.findAll().collectList().block();
        assertEquals(1, savedPrices.size());
        assertEquals(2, savedPrices.get(0).getPrices().size());

        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        valuationEvent = new Event(Event.Type.START, currencyKey, currencyKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var uscurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(0.9, uscurve.get("2022-12-01"));
        assertEquals(0.85, uscurve.get("2022-12-02"));
        assertEquals(0.8, uscurve.get("2022-12-03"));
    }

    @Test
    void equityValuationWithFx() {
        var currency = new Instrument(currencyKey, currencyDesc, InstrumentType.CURRENCY, true);
        Event creatEvent = new Event(Event.Type.CREATE, currencyKey, currency);
        saveInstrumentProcessor.accept(creatEvent);
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());
        var fxValueCurve = new ValueCurve();
        fxValueCurve.setInstrumentBusinesskey(currencyKey);
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(LocalDate.of(2022,12,1), 0.9);
        fxValueCurve.setValueCurve(valueMap);
        creatEvent = new Event(Event.Type.CREATE, currencyKey, fxValueCurve);
        saveValueCurveProcessor.accept(creatEvent);
        var fxInstruments = new HashSet<String>();
        fxInstruments.add(currencyKey);
        var fxCurves = valueCurveRepository.findByInstrumentBusinesskeyIn(fxInstruments).collectList().block();
        assertEquals(1, fxCurves.size());


        var eq = new Instrument(eqKey, eqDesc, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey, eq);
        saveInstrumentProcessor.accept(creatEvent);
        var eqPrices = new EndOfDayPrices();
        eqPrices.setInstrumentBusinesskey(eqKey);
        var eqPricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var eqPrice = new EndOfDayPrice(100, currencyKey);
        eqPricemap.put(LocalDate.of(2022,12,1), eqPrice);
        var eqPrice2 = new EndOfDayPrice(200, currencyKey);
        eqPricemap.put(LocalDate.of(2022,12,2), eqPrice2);
        eqPrices.setPrices(eqPricemap);
        creatEvent = new Event(Event.Type.CREATE, eqPrices.getInstrumentBusinesskey(), eqPrices);
        saveMarketDataProcessor.accept(creatEvent);
        var valuationEvent = new Event(Event.Type.START, eqKey, eqKey);
        valuationProcessor.accept(valuationEvent);


        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var eqcurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(90.0, eqcurve.get("2022-12-01"));
        assertEquals(180.0, eqcurve.get("2022-12-02"));
    }

    private void addInstrument(InstrumentType instrumentType, String key, String desc, String parentKey){
        var entity = new Instrument(key, desc, instrumentType, true);
        entity.setParentBusinesskey(parentKey);
        var creatEvent = new Event(Event.Type.CREATE, key, entity);
        saveInstrumentProcessor.accept(creatEvent);
        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());
        var valuationEvent = new Event(Event.Type.START, key, key);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");

        messages.forEach(m->{
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(m).get("data");
            var valType = (String) data.get("valuationType");
            if(valType.equals(ValuationType.MARKETVALUE.toString())){
                var curve = (LinkedHashMap) data.get("valueCurve");
                curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));
            }
        });
    }

    private void createAndSaveNewTestCurve(String instrumentBusinesskey) {
        var valueCurve = new ValueCurve(instrumentBusinesskey);
        var valueMap = new TreeMap<LocalDate, Double>();
        valueMap.put(transactionDate, 100.0);
        valueMap.put(transactionDate.minusDays(1), 0.0);
        valueCurve.setValueCurve(valueMap);
        Event creatEvent = new Event(Event.Type.CREATE, instrumentBusinesskey, valueCurve);
        saveValueCurveProcessor.accept(creatEvent);
    }

    private void startAndTestCurveCalc(String instrumentBusinesskey) {
        var valuationEvent = new Event(Event.Type.START, instrumentBusinesskey, instrumentBusinesskey);
        valuationProcessor.accept(valuationEvent);
        var messages = getMessages("valueCurveCalculated-out-0");
        final LinkedHashMap[] newCurve = new LinkedHashMap[1];
        messages.forEach(m->{
            var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap(m).get("data");
            var valType = (String) data.get("valuationType");
            if(valType.equals(ValuationType.MARKETVALUE.toString())){
                newCurve[0] = (LinkedHashMap) data.get("valueCurve");
                assertEquals(100.0, newCurve[0].get(transactionDate.toString()));
                assertEquals(0.0, newCurve[0].get(transactionDate.minusDays(1).toString()));
            }
        });

        //save calculated curve for portfoliotest
        var valueCurve = new ValueCurve(instrumentBusinesskey);
        valueCurve.setInstrumentBusinesskey(instrumentBusinesskey);
        TreeMap<LocalDate, Double> calculatedCurve = new TreeMap<>();
        calculatedCurve.putAll(newCurve[0]);
        valueCurve.setValueCurve(calculatedCurve);
        var createEvent = new Event(Event.Type.CREATE, instrumentBusinesskey, valueCurve);
        saveValueCurveProcessor.accept(createEvent);
    }

    @Test
    void realestateValuation() {

        var entity = new Instrument(realestateKey, realestateDesc, InstrumentType.REALESTATE, true);
        var yieldgoaldate = "2025-01-01";
        var yieldgoalvalue = "5";
        var yieldgoals = new HashMap<String, String>();
        yieldgoals.put(yieldgoaldate, yieldgoalvalue);
        Map<AdditionalMaps, Map<String, String>> additionalMaps = new HashMap<>();
        additionalMaps.put(AdditionalMaps.YIELDGOAL, yieldgoals);
        var profitdate = "2025-01-01";
        var profitvalue = "1000";
        var profits = new HashMap<String, String>();
        profits.put(profitdate, profitvalue);
        additionalMaps.put(AdditionalMaps.REALESTATEPROFITS, profits);
        entity.setAdditionalMaps(additionalMaps);

        var properties = new HashMap<AdditionalProperties, String>();
        properties.put(AdditionalProperties.VALUEBUDGETID, bgtKey);
        entity.setAdditionalProperties(properties);

        var creatEvent = new Event(Event.Type.CREATE, realestateKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, realestateKey, realestateKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");

        assertEquals(2, curve.size());
        var dateOfFirstValueChange = LocalDate.of(2025,1,1);
        assertEquals(240000.0, curve.get(dateOfFirstValueChange.toString()));
        assertEquals(0.0, curve.get(dateOfFirstValueChange.minusDays(1).toString()));

        var linkedInstrumentKey = (String) data.get("linkedInstrumentKey");
        assertEquals(bgtKey, linkedInstrumentKey);

    }

    @Test
    void deprecationObjectValuation() {

        var acquisitionDate = LocalDate.of(2025,1,1);
        Double acquisitionValue = 364.0;
        var maturityDate = LocalDate.of(2025,12,31);

        var entity = new Instrument(deprecationObjectKey, deprecationObjectDesc, InstrumentType.DEPRECATIONOBJECT, true);
        var properties = new HashMap<AdditionalProperties, String>();
        properties.put(AdditionalProperties.VALUEBUDGETID, bgtKey);
        properties.put(AdditionalProperties.ACQUISITIONDATE, acquisitionDate.toString());
        properties.put(AdditionalProperties.ACQUISITIONVALUE, acquisitionValue.toString());
        properties.put(AdditionalProperties.MATURITYDATE, maturityDate.toString());
        entity.setAdditionalProperties(properties);

        var creatEvent = new Event(Event.Type.CREATE, deprecationObjectKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, deprecationObjectKey, deprecationObjectKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(366, curve.size());
        assertEquals(0.0, curve.get(acquisitionDate.minusDays(1).toString()));
        assertEquals(acquisitionValue, curve.get(acquisitionDate.toString()));
        assertEquals(acquisitionValue-1, curve.get(acquisitionDate.plusDays(1).toString()));
        assertEquals(acquisitionValue-2, curve.get(acquisitionDate.plusDays(2).toString()));
        assertEquals(0.0, curve.get(maturityDate.toString()));

        var linkedInstrumentKey = (String) data.get("linkedInstrumentKey");
        assertEquals(bgtKey, linkedInstrumentKey);

    }

    @Test
    void lifeinsuranceValuation() {

        var entity = new Instrument(lifeInsurenceKey, lifeInsurenceDesc, InstrumentType.LIFEINSURANCE, true);
        var surrenderValueDate = "2025-01-01";
        var surrenderValue = "1000.0";
        var surrenderValues = new HashMap<String, String>();
        surrenderValues.put(surrenderValueDate, surrenderValue);
        Map<AdditionalMaps, Map<String, String>> additionalMaps = new HashMap<>();
        additionalMaps.put(AdditionalMaps.SURRENDERVALUES, surrenderValues);
        entity.setAdditionalMaps(additionalMaps);

        var properties = new HashMap<AdditionalProperties, String>();
        properties.put(AdditionalProperties.VALUEBUDGETID, bgtKey);
        entity.setAdditionalProperties(properties);

        var creatEvent = new Event(Event.Type.CREATE, lifeInsurenceKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, lifeInsurenceKey, lifeInsurenceKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");

        assertEquals(2, curve.size());
        var dateOfFirstValueChange = LocalDate.of(2025,1,1);
        assertEquals(1000.0, curve.get(dateOfFirstValueChange.toString()));
        assertEquals(0.0, curve.get(dateOfFirstValueChange.minusDays(1).toString()));

        var linkedInstrumentKey = (String) data.get("linkedInstrumentKey");
        assertEquals(bgtKey, linkedInstrumentKey);

    }

    @Test
    void loanValuation() {

        var entity = new Instrument(loanKey, loanDesc, InstrumentType.LOAN, true);
        var creatEvent = new Event(Event.Type.CREATE, loanKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, loanKey, loanKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));


        LocalDate transactionDate = LocalDate.of(2025, 1, 1);
        var desc = "kreditrauszahlung";
        var cashflow = new Cashflow(desc, transactionDate, loanKey, -1000.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, loanKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        LocalDate redemptionDate = LocalDate.of(2025, 1, 3);
        desc = "tilgung";
        cashflow = new Cashflow(desc, redemptionDate, loanKey, 100.0);
        cashflowEEvent = new Event(Event.Type.CREATE, loanKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        valuationEvent = new Event(Event.Type.START, loanKey, loanKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(4, newCurve.size());
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));
        assertEquals(-1000.0, newCurve.get(transactionDate.toString()));
        assertEquals(-1000.0, newCurve.get(transactionDate.plusDays(1).toString()));
        assertEquals(-900.0, newCurve.get(transactionDate.plusDays(2).toString()));

    }

    @Test
    void moneyAtCallValuation() {

        var entity = new Instrument(moneyAtCallKey, moneyAtCallDesc, InstrumentType.MONEYATCALL, true);
        var creatEvent = new Event(Event.Type.CREATE, moneyAtCallKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, moneyAtCallKey, moneyAtCallKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

        LocalDate transactionDate = LocalDate.of(2025, 1, 1);
        var desc = "transaction";
        var cashflow = new Cashflow(desc, transactionDate, moneyAtCallKey, 1000.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, moneyAtCallKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);


        valuationEvent = new Event(Event.Type.START, moneyAtCallKey, moneyAtCallKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(2, newCurve.size());
        assertEquals(1000.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));


    }

    @Test
    void timeDepositValuation() {

        var entity = new Instrument(timeDepositKey, timeDepositDesc, InstrumentType.TIMEDEPOSIT, true);
        var creatEvent = new Event(Event.Type.CREATE, timeDepositKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, timeDepositKey, timeDepositKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

        LocalDate transactionDate = LocalDate.of(2025, 1, 1);
        var desc = "transaction";
        var cashflow = new Cashflow(desc, transactionDate, timeDepositKey, 1000.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, timeDepositKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);


        valuationEvent = new Event(Event.Type.START, timeDepositKey, timeDepositKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(2, newCurve.size());
        assertEquals(1000.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));

    }

    @Test
    void buildingSavingAccValuation() {

        var entity = new Instrument(buildingsavingAccountKey, buildingsavingAccountDesc, InstrumentType.BUILDINGSAVINGACCOUNT, true);
        var creatEvent = new Event(Event.Type.CREATE, buildingsavingAccountKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, buildingsavingAccountKey, buildingsavingAccountKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

        LocalDate transactionDate = LocalDate.of(2025, 1, 1);
        var desc = "transaction";
        var cashflow = new Cashflow(desc, transactionDate, buildingsavingAccountKey, 1000.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, buildingsavingAccountKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);


        valuationEvent = new Event(Event.Type.START, buildingsavingAccountKey, buildingsavingAccountKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(2, newCurve.size());
        assertEquals(1000.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));

    }

    @Test
    void fondValuation() {

        var entity = new Instrument(fondKey, fondDesc, InstrumentType.FONDS, true);
        var creatEvent = new Event(Event.Type.CREATE, fondKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());


        var prices = new EndOfDayPrices();
        prices.setInstrumentBusinesskey(fondKey);
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var pice2 = new EndOfDayPrice(200, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,2), pice2);
        prices.setPrices(pricemap);
        creatEvent = new Event(Event.Type.CREATE, prices.getInstrumentBusinesskey(), prices);
        saveMarketDataProcessor.accept(creatEvent);
        var valuationEvent = new Event(Event.Type.START, fondKey, fondKey);
        valuationProcessor.accept(valuationEvent);


        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var eqcurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(100.0, eqcurve.get("2022-12-01"));
        assertEquals(200.0, eqcurve.get("2022-12-02"));

    }

    @Test
    void etfValuation() {

        var entity = new Instrument(etfKey, etfDesc, InstrumentType.ETF, true);
        var creatEvent = new Event(Event.Type.CREATE, etfKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var prices = new EndOfDayPrices();
        prices.setInstrumentBusinesskey(etfKey);
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var pice2 = new EndOfDayPrice(200, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,2), pice2);
        prices.setPrices(pricemap);
        creatEvent = new Event(Event.Type.CREATE, prices.getInstrumentBusinesskey(), prices);
        saveMarketDataProcessor.accept(creatEvent);
        var valuationEvent = new Event(Event.Type.START, etfKey, etfKey);
        valuationProcessor.accept(valuationEvent);


        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var eqcurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(100.0, eqcurve.get("2022-12-01"));
        assertEquals(200.0, eqcurve.get("2022-12-02"));

    }

    @Test
    void bondValuation() {

        var entity = new Instrument(bondKey, bondDesc, InstrumentType.BOND, true);
        var creatEvent = new Event(Event.Type.CREATE, bondKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var prices = new EndOfDayPrices();
        prices.setInstrumentBusinesskey(bondKey);
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var pice2 = new EndOfDayPrice(200, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,2), pice2);
        prices.setPrices(pricemap);
        creatEvent = new Event(Event.Type.CREATE, prices.getInstrumentBusinesskey(), prices);
        saveMarketDataProcessor.accept(creatEvent);
        var valuationEvent = new Event(Event.Type.START, bondKey, bondKey);
        valuationProcessor.accept(valuationEvent);


        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var eqcurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(100.0, eqcurve.get("2022-12-01"));
        assertEquals(200.0, eqcurve.get("2022-12-02"));
    }

    @Test
    void kryptoValuation() {

        var entity = new Instrument(kryptoKey, kryptoDesc, InstrumentType.KRYPTO, true);
        var creatEvent = new Event(Event.Type.CREATE, kryptoKey, entity);
        saveInstrumentProcessor.accept(creatEvent);


        var messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var prices = new EndOfDayPrices();
        prices.setInstrumentBusinesskey(kryptoKey);
        var pricemap = new HashMap<LocalDate, EndOfDayPrice>();
        var price = new EndOfDayPrice(100, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,1), price);
        var pice2 = new EndOfDayPrice(200, "EUR@13");
        pricemap.put(LocalDate.of(2022,12,2), pice2);
        prices.setPrices(pricemap);
        creatEvent = new Event(Event.Type.CREATE, prices.getInstrumentBusinesskey(), prices);
        saveMarketDataProcessor.accept(creatEvent);
        var valuationEvent = new Event(Event.Type.START, kryptoKey, kryptoKey);
        valuationProcessor.accept(valuationEvent);


        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var eqcurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(100.0, eqcurve.get("2022-12-01"));
        assertEquals(200.0, eqcurve.get("2022-12-02"));
    }

}
