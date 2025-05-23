package de.hf.myfinance.valuation.events.in;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.valuation.service.ValueHandlerFactory;

@Configuration
public class PositionValueProcessorConfig {

    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="PositionValueProcessor_Event";
    private final ValueHandlerFactory valueHandlerFactory;

    public PositionValueProcessorConfig( AuditService auditService, ValueHandlerFactory valueHandlerFactory) {
        this.valueHandlerFactory = valueHandlerFactory;
        this.auditService = auditService; 
    }

    @Bean
    public Consumer<Event<String, String>> positionValueProcessor() {
        return event -> {
            auditService.saveMessage("Process message in PositionValueProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            var securityId = event.getKey();
            var depotId = event.getData();
            auditService.saveMessage("valuate positions for id=" + securityId, Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case START:
                    valueHandlerFactory.getPositionHandler(depotId, securityId).calcPositionValueCurve().block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in positionProcessor done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}