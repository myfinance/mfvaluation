package de.hf.myfinance.valuation.persistence.entities;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trades")
public class TradeEntity {
    @Id
    private String tradeId;
    @Version
    private Integer version;

    @Indexed(unique = true)
    private PositionKey positionKey;

    private Double amount;
    private LocalDate tradeDate;

    public TradeEntity(){}

    public TradeEntity(String tradeId) {
        this.tradeId = tradeId;
    }
    public TradeEntity(String depotBusinessKey, String securityBusinessKey, Double amount, LocalDate tradeDate) {
        this.positionKey = new PositionKey(depotBusinessKey,securityBusinessKey);
        this.amount = amount;
        this.tradeDate = tradeDate;
     }

     public PositionKey getPositionKey() {
        return this.positionKey;
     }
  
     public void setPositionKey(PositionKey positionKey) {
        this.positionKey = positionKey;
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
}