package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.EndOfDayPrices;
import de.hf.myfinance.valuation.events.out.ValuationEventHandler;
import de.hf.myfinance.valuation.persistence.mapper.EndOfDayPricesMapper;
import de.hf.myfinance.valuation.persistence.repositories.EndOfDayPricesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Configuration
public class SaveMarketDataProcessorConfig {

    private final AuditService auditService;
    private final EndOfDayPricesMapper endOfDayPricesMapper;
    private final EndOfDayPricesRepository endOfDayPricesRepository;
    private final ValuationEventHandler valuationEventHandler;
    protected static final String AUDIT_MSG_TYPE="SaveMarketDataProcessor_Event";

    @Autowired
    public SaveMarketDataProcessorConfig(EndOfDayPricesMapper endOfDayPricesMapper, EndOfDayPricesRepository endOfDayPricesRepository, AuditService auditService, ValuationEventHandler valuationEventHandler) {
        this.endOfDayPricesMapper = endOfDayPricesMapper;
        this.endOfDayPricesRepository = endOfDayPricesRepository;
        this.valuationEventHandler = valuationEventHandler;
        this.auditService = auditService;
    }

    @Bean
    public Consumer<Event<String, EndOfDayPrices>> saveMarketDataProcessor() {
        return event -> {
            auditService.saveMessage("Process message in SaveMarketDataProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    EndOfDayPrices endOfDayPrices = event.getData();
                    auditService.saveMessage("Create EndOfDayPrices for Instrument: "+ endOfDayPrices.getInstrumentBusinesskey(), Severity.INFO, AUDIT_MSG_TYPE);
                    var endOfDayPricesEntity = endOfDayPricesMapper.apiToEntity(endOfDayPrices);
                    endOfDayPricesRepository.deleteByInstrumentBusinesskey(endOfDayPricesEntity.getInstrumentBusinesskey())
                            .then(endOfDayPricesRepository
                                    .save(endOfDayPricesEntity)
                                    .flatMap(e -> {
                                        valuationEventHandler.sendValuationEvent(e.getInstrumentBusinesskey());
                                        return Mono.just("done");
                                    })
                            )
                            .block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in SaveMarketDataProcessorConfig done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}
