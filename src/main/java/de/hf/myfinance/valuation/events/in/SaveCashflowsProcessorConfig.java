package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.mapper.CashflowMapper;
import de.hf.myfinance.valuation.persistence.repositories.CashflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class SaveCashflowsProcessorConfig {

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
            auditService.saveMessage("Process message created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);

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
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}
