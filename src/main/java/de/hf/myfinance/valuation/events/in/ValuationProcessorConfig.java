package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.service.ValueHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ValuationProcessorConfig  {
    private static final Logger LOG = LoggerFactory.getLogger(ValuationProcessorConfig.class);

    private final AuditService auditService;
    private final ValueHandlerFactory valueHandlerFactory;
    protected static final String AUDIT_MSG_TYPE="ValuationProcessor_Event";

    @Autowired
    public ValuationProcessorConfig( AuditService auditService, ValueHandlerFactory valueHandlerFactory) {
        this.valueHandlerFactory = valueHandlerFactory;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, Instrument>> valuationProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case START:
                    auditService.saveMessage("valuation of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);
                    valueHandlerFactory.getValueHandler(event.getKey()).flatMap(i->i.calcValueCurve()).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Start event";
                    LOG.warn(errorMessage);
            }

            LOG.info("Message processing done!");

        };
    }
}
