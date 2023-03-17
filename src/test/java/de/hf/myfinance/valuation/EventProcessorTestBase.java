package de.hf.myfinance.valuation;

import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.repositories.CashflowRepository;
import de.hf.myfinance.valuation.persistence.repositories.EndOfDayPricesRepository;
import de.hf.myfinance.valuation.persistence.repositories.InstrumentRepository;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;
import de.hf.testhelper.JsonHelper;
import de.hf.testhelper.MongoDbTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@Import({TestChannelBinderConfiguration.class})
public class EventProcessorTestBase extends MongoDbTestBase {
    @Autowired
    InstrumentRepository instrumentRepository;

    @Autowired
    CashflowRepository cashflowRepository;

    @Autowired
    ValueCurveRepository valueCurveRepository;

    @Autowired
    EndOfDayPricesRepository endOfDayPricesRepository;

    @Autowired
    private OutputDestination target;

    @Autowired
    @Qualifier("saveInstrumentProcessor")
    protected Consumer<Event<String, Instrument>> saveInstrumentProcessor;

    @Autowired
    @Qualifier("saveCashflowsProcessor")
    protected Consumer<Event<String, Cashflow>> saveCashflowProcessor;

    @Autowired
    @Qualifier("saveValueCurveProcessor")
    protected Consumer<Event<String, ValueCurve>> saveValueCurveProcessor;

    @Autowired
    @Qualifier("valuationProcessor")
    protected Consumer<Event<String, Instrument>> valuationProcessor;

    @Autowired
    @Qualifier("saveMarketDataProcessor")
    protected Consumer<Event<String, EndOfDayPrices>> saveMarketDataProcessor;

    String instrumentProcessorBindingName = "saveInstrumentProcessor-in-0";
    String cashflowProcessorBindingName = "saveCashflowsProcessor-in-0";
    String valuationDataChangedBindingName = "valuationDataChanged-out-0";

    JsonHelper jsonHelper = new JsonHelper();

    String giroDesc = "testGiro";
    String giroKey = giroDesc + "@1";
    String tenantDesc = "tenant";
    String tenantKey = tenantDesc + "@6";
    String accountPfDesc = "acountPf";
    String accountPfKey = accountPfDesc + "@8";
    String budgetPfDesc = "budgetPf";
    String budgetPfKey = budgetPfDesc + "@23";
    String budgetGroupDesc = "budgetgroup";
    String budgetGroupKey = budgetGroupDesc + "@10";
    String budgetDesc = "budget";
    String budgetKey = budgetDesc + "@5";
    String currencyDesc = "USD";
    String currencyKey = currencyDesc + "@13";


    @BeforeEach
    void setupDb() {
        instrumentRepository.deleteAll().block();
        cashflowRepository.deleteAll().block();
        valueCurveRepository.deleteAll().block();
        purgeMessages(instrumentProcessorBindingName);
        purgeMessages(cashflowProcessorBindingName);
        purgeMessages(valuationDataChangedBindingName);
    }

    protected void purgeMessages(String bindingName) {
        getMessages(bindingName);
    }

    protected List<String> getMessages(String bindingName){
        List<String> messages = new ArrayList<>();
        boolean anyMoreMessages = true;

        while (anyMoreMessages) {
            Message<byte[]> message =
                    getMessage(bindingName);

            if (message == null) {
                anyMoreMessages = false;

            } else {
                messages.add(new String(message.getPayload()));
            }
        }
        return messages;
    }

    protected Message<byte[]> getMessage(String bindingName){
        try {
            return target.receive(0, bindingName);
        } catch (NullPointerException npe) {
            LOG.error("getMessage() received a NPE with binding = {}", bindingName);
            return null;
        }
    }

}
