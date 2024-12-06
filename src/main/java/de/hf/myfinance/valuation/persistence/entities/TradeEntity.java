package de.hf.myfinance.valuation.persistence.entities;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trades")
public class TradeEntity {
    @Id
    private String tradeId;
    @Version
    private Integer version;

    private Double amount;
    private LocalDate tradeDate;
    private String depotBusinessKey;
    private String securityBusinessKey;

    public TradeEntity(){}

    public TradeEntity(String tradeId) {
        this.tradeId = tradeId;
    }
    public TradeEntity(String depotBusinessKey, String securityBusinessKey, Double amount, LocalDate tradeDate) {
        this.securityBusinessKey = securityBusinessKey;
        this.depotBusinessKey = depotBusinessKey;
        this.amount = amount;
        this.tradeDate = tradeDate;
     }

     public Double getAmount() {
        return this.amount;
     }
  
     public void setAmount(Double amount) {
        this.amount = amount;
     }

     public LocalDate getTradeDate() {
      return this.tradeDate;
   }

   public void setTradeDate(LocalDate tradeDate) {
      this.tradeDate = tradeDate;
   }

   public String getDepotBusinessKey() {
      return this.depotBusinessKey;
   }

   public void setDepotBusinessKey(String depotBusinessKey) {
      this.depotBusinessKey = depotBusinessKey;
   }

   public String getSecurityBusinessKey() {
      return this.securityBusinessKey;
   }

   public void setSecurityBusinessKey(String securityBusinessKey) {
      this.securityBusinessKey = securityBusinessKey;
   }
}