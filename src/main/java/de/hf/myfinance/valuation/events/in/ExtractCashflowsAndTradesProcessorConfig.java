package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.restmodel.Transaction;
import de.hf.myfinance.restmodel.TransactionType;
import de.hf.myfinance.valuation.events.out.ExtractedCashflowsEventHandler;
import de.hf.myfinance.valuation.events.out.ExtractedTradeEventHandler;
import de.hf.myfinance.valuation.service.ValuationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ExtractCashflowsAndTradesProcessorConfig {

    private final AuditService auditService;
    private final ExtractedCashflowsEventHandler extractedCashflowsEventHandler;
    private final ExtractedTradeEventHandler extractedTradeEventHandler;
    private final ValuationService valuationService;
    protected static final String AUDIT_MSG_TYPE="ExtractCashflowsProcessor_Event";

    public ExtractCashflowsAndTradesProcessorConfig( AuditService auditService, ExtractedCashflowsEventHandler extractedCashflowsEventHandler, 
        ExtractedTradeEventHandler extractedTradeEventHandler, ValuationService valuationService) {
        this.auditService = auditService;
        this.extractedCashflowsEventHandler = extractedCashflowsEventHandler;
        this.extractedTradeEventHandler = extractedTradeEventHandler;    
        this.valuationService =valuationService;
    }

    @Bean
    public Consumer<Event<String, Transaction>> extractCashflowsAndTradesProcessor() {
        return event -> {
            auditService.saveMessage("Process message in ExtractCashflowsProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            Transaction transaction = event.getData();
            auditService.saveMessage("extract cashflows of transaction with id=" + event.getData().getTransactionId(), Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case CREATE:
                    var cashflows = valuationService.generateCashflows(transaction, false);
                    cashflows.forEach(e-> {
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(e);
                    });
                    if(transaction.getTransactionType() == TransactionType.BUY || transaction.getTransactionType() == TransactionType.SELL){
                        var amount = transaction.getAmount();
                        if (transaction.getTransactionType() == TransactionType.SELL){
                            amount = amount * (-1);
                        }
                        var trade = new Trade(transaction.getDepotBusinessKey(), transaction.getSecurityBusinessKey(), amount);
                        trade.setTradeDate(transaction.getTransactiondate());
                        extractedTradeEventHandler.sendExtractedTradeEvent(trade);
                    }
                    break;

                case DELETE:
                    var cashflows2delete = valuationService.generateCashflows(transaction, true);
                    cashflows2delete.forEach(e-> {
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(e);
                    });
                    if(transaction.getTransactionType() == TransactionType.BUY || transaction.getTransactionType() == TransactionType.SELL){
                        var amount = transaction.getAmount();
                        if (transaction.getTransactionType() == TransactionType.BUY){
                            amount = amount * (-1);
                        }
                        var trade = new Trade(transaction.getDepotBusinessKey(), transaction.getSecurityBusinessKey(), amount);
                        trade.setTradeDate(transaction.getTransactiondate());
                        extractedTradeEventHandler.sendExtractedTradeEvent(trade);
                    }
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a Create event";
                    auditService.saveMessage(errorMessage, Severity.FATAL, AUDIT_MSG_TYPE);
            }

            auditService.saveMessage("Message processing in ExtractCashflowsProcessorConfig done!", Severity.DEBUG, AUDIT_MSG_TYPE);

        };
    }
}
