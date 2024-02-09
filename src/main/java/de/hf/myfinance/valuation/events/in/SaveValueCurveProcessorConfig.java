package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.mapper.ValueCurveMapper;
import de.hf.myfinance.valuation.persistence.repositories.ValueCurveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class SaveValueCurveProcessorConfig  {

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
            auditService.saveMessage("Process message in SaveValueCurveProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    auditService.saveMessage("save valueCurve of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);
                    var valueCurve = valueCurveMapper.apiToEntity(event.getData());
                    valueCurveRepository.findByInstrumentBusinesskey(valueCurve.getInstrumentBusinesskey())
                            .switchIfEmpty(Mono.just(valueCurve))
                            .map(e -> {
                                e.setValueCurve(valueCurve.getValueCurve());
                                e.setParentBusinesskey(valueCurve.getParentBusinesskey());
                                e.setInstrumentBusinesskey(valueCurve.getInstrumentBusinesskey());
                                return e;
                            })
                            .flatMap(e -> valueCurveRepository.save(e))
                            .flatMap(e -> {
                                if(e.getParentBusinesskey()!=null && !e.getParentBusinesskey().isEmpty()){
                                    valuationEventHandler.sendValuationEvent(e.getParentBusinesskey());
                                }
                                return Mono.just("done");
                            }).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in SaveValueCurveProcessorConfig done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}