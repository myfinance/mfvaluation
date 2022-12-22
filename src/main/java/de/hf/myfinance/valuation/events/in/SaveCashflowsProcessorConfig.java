package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.CashflowMapper;
import de.hf.myfinance.valuation.persistence.repositories.CashflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class SaveCashflowsProcessorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SaveCashflowsProcessorConfig.class);

    private final AuditService auditService;
    private final CashflowMapper cashflowMapper;
    private final CashflowRepository cashflowRepository;
    private final ValuationEventHandler valuationEventHandler;
    protected static final String AUDIT_MSG_TYPE="SaveCashFlowProcessor_Event";

    @Autowired
    public SaveCashflowsProcessorConfig(AuditService auditService, CashflowMapper cashflowMapper, CashflowRepository cashflowRepository, ValuationEventHandler valuationEventHandler) {
        this.auditService = auditService;
        this.cashflowMapper = cashflowMapper;
        this.cashflowRepository = cashflowRepository;
        this.valuationEventHandler = valuationEventHandler;
    }

    @Bean
    public Consumer<Event<String, Cashflow>> saveCashflowsProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    auditService.saveMessage("save cashflows of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);
                    cashflowRepository.save(cashflowMapper.apiToEntity(event.getData()))
                            .flatMap(e -> {
                                valuationEventHandler.sendValuationEvent(e.getInstrumentBusinesskey());
                                return Mono.just("done");
                            }).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    LOG.warn(errorMessage);
            }

            LOG.info("Message processing done!");

        };
    }
}
