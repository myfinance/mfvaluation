package de.hf.myfinance.valuation.events.in;

import de.hf.framework.audit.AuditService;
import de.hf.framework.audit.Severity;
import de.hf.myfinance.event.Event;
import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.restmodel.Transaction;
import de.hf.myfinance.restmodel.TransactionType;
import de.hf.myfinance.valuation.events.out.ExtractedCashflowsEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class ExtractCashflowsProcessorConfig {

    private final AuditService auditService;
    private final ExtractedCashflowsEventHandler extractedCashflowsEventHandler;
    protected static final String AUDIT_MSG_TYPE="ExtractCashflowsProcessor_Event";

    @Autowired
    public ExtractCashflowsProcessorConfig( AuditService auditService, ExtractedCashflowsEventHandler extractedCashflowsEventHandler) {
        this.auditService = auditService;
        this.extractedCashflowsEventHandler = extractedCashflowsEventHandler;
    }

    @Bean
    public Consumer<Event<String, Transaction>> extractCashflowsProcessor() {
        return event -> {
            auditService.saveMessage("Process message in ExtractCashflowsProcessorConfig created at:" + event.getEventCreatedAt(), Severity.DEBUG, AUDIT_MSG_TYPE);
            Transaction transaction = event.getData();
            auditService.saveMessage("extract cashflows of transaction with id=" + event.getData().getTransactionId(), Severity.DEBUG, AUDIT_MSG_TYPE);
            switch (event.getEventType()) {

                case CREATE:
                    if(transaction.getTransactionType().equals(TransactionType.INCOME)
                            || transaction.getTransactionType().equals(TransactionType.EXPENSE)
                            || transaction.getTransactionType().equals(TransactionType.TRANSFER)
                            || transaction.getTransactionType().equals(TransactionType.BUDGETTRANSFER)){
                        transaction.getCashflows().entrySet().forEach(e-> {
                            extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), e.getKey(), e.getValue()));
                        });

                    }

                    break;

                case DELETE:
                    if(transaction.getTransactionType().equals(TransactionType.INCOME)
                            || transaction.getTransactionType().equals(TransactionType.EXPENSE)
                            || transaction.getTransactionType().equals(TransactionType.TRANSFER)
                            || transaction.getTransactionType().equals(TransactionType.BUDGETTRANSFER)){
                        transaction.getCashflows().entrySet().forEach(e-> {
                            extractedCashflowsEventHandler.sendExtractedCashflowsEvent(new Cashflow(transaction.getDescription(),transaction.getTransactiondate(), e.getKey(), e.getValue()*(-1)));
                        });

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
