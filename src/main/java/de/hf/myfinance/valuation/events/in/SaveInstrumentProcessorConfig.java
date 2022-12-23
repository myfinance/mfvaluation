package de.hf.myfinance.valuation.events.in;


import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.mapper.InstrumentMapper;
import de.hf.myfinance.valuation.persistence.repositories.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class SaveInstrumentProcessorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SaveInstrumentProcessorConfig.class);

    private final InstrumentMapper instrumentMapper;
    private final AuditService auditService;
    private final InstrumentRepository instrumentRepository;
    private final ValuationEventHandler valuationEventHandler;
    protected static final String AUDIT_MSG_TYPE="SaveInstrumentProcessor_Event";

    @Autowired
    public SaveInstrumentProcessorConfig(InstrumentMapper instrumentMapper, AuditService auditService, InstrumentRepository instrumentRepository, ValuationEventHandler valuationEventHandler) {
        this.instrumentMapper = instrumentMapper;
        this.auditService = auditService;
        this.instrumentRepository = instrumentRepository;
        this.valuationEventHandler = valuationEventHandler;
    }

    @Bean
    public Consumer<Event<String, Instrument>> saveInstrumentProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    Instrument instrument = event.getData();
                    LOG.info("Create instrument with ID: {}", instrument.getBusinesskey());
                    var instrumentEntity = instrumentMapper.apiToEntity(instrument);
                    instrumentRepository.findByBusinesskey(instrumentEntity.getBusinesskey())
                            .switchIfEmpty(Mono.just(instrumentEntity))
                            .map(e -> {
                                e.setAdditionalMaps(instrumentEntity.getAdditionalMaps());
                                e.setAdditionalProperties(instrumentEntity.getAdditionalProperties());
                                e.setDescription(instrumentEntity.getDescription());
                                e.setActive(instrumentEntity.isActive());
                                return e;
                            })
                            .flatMap(e -> instrumentRepository.save(e))
                            .flatMap(e -> {
                                valuationEventHandler.sendValuationEvent(e.getBusinesskey());
                                return Mono.just("done");
                            })
                            .block();
                    auditService.saveMessage("Instrument updated:businesskey=" + instrument.getBusinesskey() + " desc=" + instrument.getDescription(), Severity.INFO, AUDIT_MSG_TYPE);

                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    LOG.warn(errorMessage);
            }

            LOG.info("Message processing done!");

        };
    }
}
