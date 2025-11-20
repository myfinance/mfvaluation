package de.hf.myfinance.valuation.events.in;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.entities.PositionValueKey;
import de.hf.myfinance.valuation.persistence.entities.PositionValueEntity;
import de.hf.myfinance.valuation.persistence.repositories.PositionValueRepository;
import reactor.core.publisher.Mono;

@Configuration
public class SavePositionValueProcessorConfig {
    
    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="SavePositionProcessor_Event";
    private final PositionValueRepository positionValueRepository;
    private final ValuationEventHandler valuationEventHandler;

    public SavePositionValueProcessorConfig( AuditService auditService, PositionValueRepository positionValueRepository, ValuationEventHandler valuationEventHandler) {
        this.auditService = auditService;
        this.positionValueRepository = positionValueRepository;
        this.valuationEventHandler = valuationEventHandler;
    }

    @Bean
    public Consumer<Event<String, ValueCurve>> savePositionValueProcessor() {
        return event -> {
            auditService.saveMessage("Process message in SavePositionValueProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            var positionValueCurve = event.getData();
            auditService.saveMessage("save positions for id=" + positionValueCurve.getInstrumentBusinesskey(), Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case CREATE:
                    positionValueRepository.findByPositionValueKey(new PositionValueKey(positionValueCurve.getParentBusinesskey(), positionValueCurve.getInstrumentBusinesskey(), positionValueCurve.getValuationType()))
                        .switchIfEmpty(Mono.just(new PositionValueEntity(positionValueCurve.getParentBusinesskey(), positionValueCurve.getInstrumentBusinesskey(), positionValueCurve.getValueCurve())))
                        .flatMap(p->{
                            p.setPositionValueCurve(positionValueCurve.getValueCurve());
                            return positionValueRepository.save(p);
                        }).block();
                        auditService.saveMessage("positions saved", Severity.INFO, AUDIT_MSG_TYPE);
                        valuationEventHandler.sendValuationEvent(positionValueCurve.getParentBusinesskey());

                        break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message saved in positionProcessor!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}