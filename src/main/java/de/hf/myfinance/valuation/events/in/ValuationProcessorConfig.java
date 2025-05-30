package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.service.MessageDuplicationFilter;
import de.hf.myfinance.valuation.service.ValueHandlerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ValuationProcessorConfig  {

    private final AuditService auditService;
    private final ValueHandlerFactory valueHandlerFactory;
    protected static final String AUDIT_MSG_TYPE="ValuationProcessor_Event";
    private final MessageDuplicationFilter messageDuplicationFilter;

    public ValuationProcessorConfig( AuditService auditService, ValueHandlerFactory valueHandlerFactory, MessageDuplicationFilter messageDuplicationFilter) {
        this.valueHandlerFactory = valueHandlerFactory;
        this.auditService = auditService;
        this.messageDuplicationFilter = messageDuplicationFilter;
    }

    @Bean
    public Consumer<Event<String, String>> valuationProcessor() {
        return event -> {
            auditService.saveMessage("Process message in ValuationProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            messageDuplicationFilter.processKey(event.getKey());
            switch (event.getEventType()) {

                case START:
                    auditService.saveMessage("valuation of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);
                    valueHandlerFactory.getValueHandler(event.getKey()).flatMap(i->i.calcValueCurve()).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Start event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in ValuationProcessorConfig done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}
