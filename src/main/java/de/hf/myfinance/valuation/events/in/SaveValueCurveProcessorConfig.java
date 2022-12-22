package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.ValueCurveMapper;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class SaveValueCurveProcessorConfig  {
    private static final Logger LOG = LoggerFactory.getLogger(SaveValueCurveProcessorConfig.class);

    private final AuditService auditService;
    private final ValueCurveRepository valueCurveRepository;
    private final ValueCurveMapper valueCurveMapper;
    private final ValuationEventHandler valuationEventHandler;
    protected static final String AUDIT_MSG_TYPE="SaveCashFlowProcessor_Event";

    @Autowired
    public SaveValueCurveProcessorConfig( AuditService auditService, ValueCurveRepository valueCurveRepository, ValueCurveMapper valueCurveMapper, ValuationEventHandler valuationEventHandler) {

        this.auditService = auditService;
        this.valueCurveRepository = valueCurveRepository;
        this.valueCurveMapper = valueCurveMapper;
        this.valuationEventHandler = valuationEventHandler;
    }

    @Bean
    public Consumer<Event<String, ValueCurve>> saveValueCurveProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    auditService.saveMessage("save valueCurve of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);
                    valueCurveRepository.save(valueCurveMapper.apiToEntity(event.getData()))
                            .flatMap(e -> {
                                if(e.getParentBusinesskey()!=null && !e.getParentBusinesskey().isEmpty()){
                                    valuationEventHandler.sendValuationEvent(e.getParentBusinesskey());
                                }
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