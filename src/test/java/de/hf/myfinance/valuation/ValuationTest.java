package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.*;
import de.hf.testhelper.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

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
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));

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
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(0.0, curve.get(datebeforFirstTrade.toString()));
        assertEquals(10.0, curve.get(firstTradeDate.toString()));
        assertEquals(10.0, curve.get(datebetweeenTrades.toString()));
        assertEquals(5.0, curve.get(secTradeDate.toString()));


        var linkedInstrumentKey = (String) data.get("linkedInstrumentKey");
        assertEquals("valueBudgetKey", linkedInstrumentKey);

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
        savePositionValueProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var valuationEvent = new Event(Event.Type.START, depotKey, depotKey);
        valuationProcessor.accept(valuationEvent);
         messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(0.0, curve.get(datebeforFirstTrade.toString()));
        assertEquals(10.0, curve.get(firstTradeDate.toString()));
        assertEquals(10.0, curve.get(datebetweeenTrades.toString()));
        assertEquals(20.0, curve.get(secTradeDate.toString()));

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

        var eq = new Instrument(eqKey, eqDesc, InstrumentType.EQUITY, true);
        creatEvent = new Event(Event.Type.CREATE, eqKey, eq);
        saveInstrumentProcessor.accept(creatEvent);
        messages = getMessages("valuationDataChanged-out-0");
        assertEquals(1, messages.size());

        var datebeforFirstTrade = LocalDate.of(2021, 12, 31);
        var firstTradeDate = LocalDate.of(2022, 1, 1);
        var datebetweeenTrades = LocalDate.of(2022, 1, 2);
        var secTradeDate = LocalDate.of(2022, 1, 3);
        var transactionDate = LocalDate.of(2022, 1, 4);
        var dateAftertransactionDate = LocalDate.of(2022, 1, 5);

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
        assertEquals(1, messages.size());

        JsonHelper jsonHelper = new JsonHelper();
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var curve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(0.0, curve.get(datebeforFirstTrade.toString()));
        assertEquals(10.0, curve.get(firstTradeDate.toString()));
        assertEquals(10.0, curve.get(datebetweeenTrades.toString()));
        assertEquals(5.0, curve.get(secTradeDate.toString()));


        var linkedInstrumentKey = (String) data.get("linkedInstrumentKey");
        assertEquals(budgetKey, linkedInstrumentKey);



        
        var desc = "testcashflow";
        var cashflow = new Cashflow(desc, transactionDate, budgetKey, 100.0);
        var cashflowEEvent = new Event(Event.Type.CREATE, budgetKey, cashflow);
        saveCashflowProcessor.accept(cashflowEEvent);

        valuationEvent = new Event(Event.Type.START, budgetKey, budgetKey);
        valuationProcessor.accept(valuationEvent);
        messages = getMessages("valueCurveCalculated-out-0");
        assertEquals(1, messages.size());

        data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");

        var newCurve = (LinkedHashMap) data.get("valueCurve");
        /*assertEquals(0.0, newCurve.get(datebeforFirstTrade.toString()));
        assertEquals(10.0, newCurve.get(firstTradeDate.toString()));
        assertEquals(10.0, newCurve.get(datebetweeenTrades.toString()));
        assertEquals(5.0, newCurve.get(secTradeDate.toString()));
        assertEquals(105.0, newCurve.get(transactionDate.toString()));
        assertEquals(105.0, newCurve.get(dateAftertransactionDate.toString()));*/

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
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var curve = (LinkedHashMap) data.get("valueCurve");
        curve.keySet().forEach(i->assertEquals(0.0, curve.get(i)));
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
        assertEquals(1, messages.size());
        var data = (LinkedHashMap)jsonHelper.convertJsonStringToMap((messages.get(0))).get("data");
        var newCurve = (LinkedHashMap) data.get("valueCurve");
        assertEquals(100.0, newCurve.get(transactionDate.toString()));
        assertEquals(0.0, newCurve.get(transactionDate.minusDays(1).toString()));

        //save calculated curve for portfoliotest
        var valueCurve = new ValueCurve(instrumentBusinesskey);
        valueCurve.setInstrumentBusinesskey(instrumentBusinesskey);
        TreeMap<LocalDate, Double> calculatedCurve = new TreeMap<>();
        calculatedCurve.putAll(newCurve);
        valueCurve.setValueCurve(calculatedCurve);
        var createEvent = new Event(Event.Type.CREATE, instrumentBusinesskey, valueCurve);
        saveValueCurveProcessor.accept(createEvent);
    }

}
