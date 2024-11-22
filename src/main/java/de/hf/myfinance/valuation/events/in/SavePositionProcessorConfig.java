package de.hf.myfinance.valuation.events.in;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.PositionSavedEventHandler;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;
import de.hf.myfinance.valuation.persistence.repositories.PositionRepository;
import reactor.core.publisher.Mono;

@Configuration
public class SavePositionProcessorConfig {
    


    private final AuditService auditService;
    protected static final String AUDIT_MSG_TYPE="SavePositionProcessor_Event";
    private final PositionRepository positionRepository;
    private final PositionSavedEventHandler positionSavedEventHandler;

    public SavePositionProcessorConfig( AuditService auditService, PositionRepository positionRepository, PositionSavedEventHandler positionSavedEventHandler) {
        this.auditService = auditService;
        this.positionRepository = positionRepository;
        this.positionSavedEventHandler = positionSavedEventHandler;
    }

    @Bean
    public Consumer<Event<String, ValueCurve>> savePositionProcessor() {
        return event -> {
            auditService.saveMessage("Process message in SavePositionProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            var positionCurve = event.getData();
            auditService.saveMessage("save positions for id=" + positionCurve.getInstrumentBusinesskey(), Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case CREATE:
                    var positionEntity = new PositionEntity(positionCurve.getParentBusinesskey(), positionCurve.getInstrumentBusinesskey(), positionCurve.getValueCurve());
                    positionRepository.save(positionEntity).flatMap(e -> {
                            positionSavedEventHandler.sendPositionSavedEvent(positionCurve);
                            return Mono.just("done");
                        }).block();

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message saved in positionProcessor!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}