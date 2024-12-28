package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Transaction;
import de.hf.myfinance.restmodel.TransactionType;
import de.hf.myfinance.valuation.events.out.ExtractedCashflowsEventHandler;
import de.hf.myfinance.valuation.events.out.ExtractedTradeEventHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ExtractCashflowsAndTradesProcessorConfig {

    private final AuditService auditService;
    private final ExtractedCashflowsEventHandler extractedCashflowsEventHandler;
    private final ExtractedTradeEventHandler extractedTradeEventHandler;
    protected static final String AUDIT_MSG_TYPE="ExtractCashflowsProcessor_Event";

    public ExtractCashflowsAndTradesProcessorConfig( AuditService auditService, ExtractedCashflowsEventHandler extractedCashflowsEventHandler, ExtractedTradeEventHandler extractedTradeEventHandler) {
        this.auditService = auditService;
        this.extractedCashflowsEventHandler = extractedCashflowsEventHandler;
        this.extractedTradeEventHandler = extractedTradeEventHandler;    }

    @Bean
    public Consumer<Event<String, Transaction>> extractCashflowsAndTradesProcessor() {
        return event -> {
            auditService.saveMessage("Process message in ExtractCashflowsProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            Transaction transaction = event.getData();
            auditService.saveMessage("extract cashflows of transaction with id=" + event.getData().getTransactionId(), Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case CREATE:
                    transaction.getCashflows().entrySet().forEach(e-> {
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), e.getKey(), e.getValue()));
                    });
                    if(transaction.getTransactionType() == TransactionType.BUY || transaction.getTransactionType() == TransactionType.SELL){
                        var trade = transaction.getTradeInfo();
                        if (transaction.getTransactionType() == TransactionType.SELL){
                            trade.setAmount(trade.getAmount() * (-1));
                        }
                        trade.setTradeDate(transaction.getTransactiondate());
                        extractedTradeEventHandler.sendExtractedTradeEvent(trade);
                        var value = transaction.getCashflows().values().iterator().next();
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), trade.getDepotBusinessKey(), value));
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), trade.getSecurityBusinessKey(), value));
                    }
                    break;

                case DELETE:
                    transaction.getCashflows().entrySet().forEach(e-> {
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), e.getKey(), e.getValue()*(-1)));
                    });
                    if(transaction.getTransactionType() == TransactionType.BUY || transaction.getTransactionType() == TransactionType.SELL){
                        var trade = transaction.getTradeInfo();
                        if (transaction.getTransactionType() == TransactionType.BUY){
                            trade.setAmount(trade.getAmount() * (-1));
                        }
                        trade.setTradeDate(transaction.getTransactiondate());
                        extractedTradeEventHandler.sendExtractedTradeEvent(trade);
                        var value = transaction.getCashflows().values().iterator().next();
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), trade.getDepotBusinessKey(), value));
                        extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), trade.getSecurityBusinessKey(), value));
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
