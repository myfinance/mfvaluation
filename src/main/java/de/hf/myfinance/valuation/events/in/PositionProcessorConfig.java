package de.hf.myfinance.valuation.events.in;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.Transaction;
import de.hf.myfinance.restmodel.TransactionType;
import de.hf.myfinance.valuation.events.out.ExtractedCashflowsEventHandler;
import de.hf.myfinance.valuation.events.out.ExtractedTradeEventHandler;
import de.hf.myfinance.valuation.events.out.PositionBuildedEventHandler;

@Configuration
public class PositionProcessorConfig {

    private final AuditService auditService;
    private final PositionBuildedEventHandler positionBuildedEventHandler;
    protected static final String AUDIT_MSG_TYPE="ExtractCashflowsProcessor_Event";

    public PositionProcessorConfig( AuditService auditService, PositionBuildedEventHandler positionBuildedEventHandler) {
        this.auditService = auditService;
        this.positionBuildedEventHandler = positionBuildedEventHandler;    
    }

    @Bean
    public Consumer<Event<String, Trade>> positionProcessor() {
        return event -> {
            auditService.saveMessage("Process message in ExtractCashflowsProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            var trade = event.getData();
            auditService.saveMessage("build positions for id=" + event.getData().getSecurityBusinessKey(), Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case CREATE:
                    
                    positionBuildedEventHandler.sendPositionBuildedEvent(null);
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in positionProcessor done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}