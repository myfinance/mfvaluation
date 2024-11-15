package de.hf.myfinance.valuation.events.in;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.valuation.events.out.TradeSavedEventHandler;
import de.hf.myfinance.valuation.persistence.mapper.TradeMapper;
import de.hf.myfinance.valuation.persistence.repositories.TradeRepository;
import reactor.core.publisher.Mono;

@Configuration
public class SaveTradeProcessorConfig {

    private final AuditService auditService;
    private final TradeMapper tradeMapper;
    private final TradeRepository tradeRepository;
    private final TradeSavedEventHandler tradeSavedEventHandler;
    protected static final String AUDIT_MSG_TYPE="SaveTradeProcessor_Event";

    public SaveTradeProcessorConfig(AuditService auditService, TradeMapper tradeMapper, TradeRepository tradeRepository, 
    TradeSavedEventHandler tradeSavedEventHandler) {
        this.auditService = auditService;
        this.tradeMapper = tradeMapper;
        this.tradeRepository = tradeRepository;
        this.tradeSavedEventHandler = tradeSavedEventHandler;
    }

    @Bean
    public Consumer<Event<String, Trade>> saveTradeProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    auditService.saveMessage("save trade of instrument with businesskey=" + event.getKey(), Severity.INFO, AUDIT_MSG_TYPE);
                    var trade = tradeMapper.apiToEntity(event.getData());
                    tradeRepository.save(trade)
                            .flatMap(e -> {
                                tradeSavedEventHandler.sendExtractedTradeEvent(event.getData());
                                return Mono.just("done");
                            }).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}