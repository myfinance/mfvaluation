package de.hf.myfinance.valuation.persistence.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "cashflows")
public class CashflowEntity  implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String cashflowid;
    @Version
    private Integer version;

    private String description;
    private LocalDate transactiondate;
    private String instrumentBusinesskey;
    private double value;
    private String serviceAddress;

    public CashflowEntity() {
    }

    public CashflowEntity(String description, LocalDate transactiondate, String instrumentBusinesskey, double value) {
        this.description = description;
        this.transactiondate = transactiondate;
        this.instrumentBusinesskey = instrumentBusinesskey;
        this.value = value;
    }

    public String getCashflowid() {
        return cashflowid;
    }
    public void setCashflowid(String cashflowid) {
        this.cashflowid = cashflowid;
    }

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactiondate() {
        return transactiondate;
    }
    public void setTransactiondate(LocalDate transactiondate) {
        this.transactiondate = transactiondate;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }
    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public String getInstrumentBusinesskey() {
        return instrumentBusinesskey;
    }
    public void setInstrumentBusinesskey(String instrumentBusinesskey) {
        this.instrumentBusinesskey = instrumentBusinesskey;
    }

    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
}
