package de.hf.myfinance.valuation.events.in;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.PortfolioMetrics;
import de.hf.myfinance.valuation.persistence.mapper.PortfolioMetricsMapper;
import de.hf.myfinance.valuation.persistence.repositories.PortfolioMetricsRepository;
import reactor.core.publisher.Mono;

@Configuration
public class SavePortfolioMetricsProcessorConfig {

    private final PortfolioMetricsMapper portfolioMetricsMapper;
    private final AuditService auditService;
    private final PortfolioMetricsRepository portfolioMetricsRepository;
    protected static final String AUDIT_MSG_TYPE="SavePortfolioMetricsProcessor_Event";

    public SavePortfolioMetricsProcessorConfig(PortfolioMetricsMapper portfolioMetricsMapper, AuditService auditService, PortfolioMetricsRepository portfolioMetricsRepository) {
        this.portfolioMetricsMapper = portfolioMetricsMapper;
        this.auditService = auditService;
        this.portfolioMetricsRepository = portfolioMetricsRepository;
    }

    @Bean
    public Consumer<Event<String, PortfolioMetrics>> savePortfolioMetricsProcessor() {
        return event -> {
            auditService.saveMessage("Process message created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);

            switch (event.getEventType()) {

                case CREATE:
                    PortfolioMetrics portfolioMetrics = event.getData();
                    auditService.saveMessage("Create PortfolioMetrics with ID: " + portfolioMetrics.getPortfolio(), Severity.DEBUG, AUDIT_MSG_TYPE);
                    var portfolioMetricsEntity = portfolioMetricsMapper.apiToEntity(portfolioMetrics);
                    portfolioMetricsRepository.findById(portfolioMetricsEntity.getPortfolio())
                            .switchIfEmpty(Mono.just(portfolioMetricsEntity))
                            .map(e -> {
                                e.setPortfolio(portfolioMetricsEntity.getPortfolio());
                                e.setCagrPerYear(portfolioMetricsEntity.getCagrPerYear());
                                e.setTotalCagr(portfolioMetricsEntity.getTotalCagr());
                                e.setCashflows(portfolioMetricsEntity.getCashflows());
                                e.setCashflowsWithStartAndEndValues(portfolioMetricsEntity.getCashflowsWithStartAndEndValues());
                                e.setIsSingleSecurity(portfolioMetricsEntity.getIsSingleSecurity());
                                return e;
                            })
                            .flatMap(e -> portfolioMetricsRepository.save(e))
                            .flatMap(e -> {
                                return Mono.just("done");
                            })
                            .block();
                    auditService.saveMessage("portfolioMetrics updated for " + portfolioMetrics.getPortfolio(), Severity.INFO, AUDIT_MSG_TYPE);

                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}
