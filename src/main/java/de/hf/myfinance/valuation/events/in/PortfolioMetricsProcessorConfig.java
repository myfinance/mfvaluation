package de.hf.myfinance.valuation.events.in;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.valuation.service.MessageDuplicationFilter;
import de.hf.myfinance.valuation.service.ValuationService;

@Configuration
public class PortfolioMetricsProcessorConfig {

    private final AuditService auditService;
    private final MessageDuplicationFilter messageDuplicationFilter;
    private final ValuationService valuationService;
    protected static final String AUDIT_MSG_TYPE="SavePortfolioMetricsProcessor_Event";

    public PortfolioMetricsProcessorConfig(ValuationService valuationService, AuditService auditService, MessageDuplicationFilter messageDuplicationFilter) {
        this.auditService = auditService;
        this.messageDuplicationFilter = messageDuplicationFilter;
        this.valuationService = valuationService;
    }

    @Bean
    public Consumer<Event<String, String>> portfolioMetricsProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            messageDuplicationFilter.processKey(event.getKey());
            switch (event.getEventType()) {

                case START:
                    auditService.saveMessage("calculate PortfolioMetrics", Severity.DEBUG, AUDIT_MSG_TYPE);
                    valuationService.recalcAPortfolioMetrics().block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Start event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in portfolioMetricsProcessor done!", Severity.DEBUG, AUDIT_MSG_TYPE);
        };
    }
}
