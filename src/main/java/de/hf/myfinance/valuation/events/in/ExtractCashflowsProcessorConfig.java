package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

public class ExtractCashflowsProcessorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractCashflowsProcessorConfig.class);

    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="ExtractCashflowsProcessor_Event";

    @Autowired
    public ExtractCashflowsProcessorConfig( AuditService auditService) {

        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, Instrument>> extractCashflowsProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    auditService.saveMessage("extract cashflows of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);

                    break;

                case DELETE:
                    auditService.saveMessage("extract cashflows of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);

                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    LOG.warn(errorMessage);
            }

            LOG.info("Message processing done!");

        };
    }
}
