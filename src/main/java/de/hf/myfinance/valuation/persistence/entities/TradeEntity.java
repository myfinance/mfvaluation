package de.hf.myfinance.valuation.persistence.entities;

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

    public TradeEntity(){}

    public TradeEntity(String tradeId) {
        this.tradeId = tradeId;
    }
    public TradeEntity(String depotBusinessKey, String securityBusinessKey, Double amount) {
        this.positionKey = new PositionKey(depotBusinessKey,securityBusinessKey);
        this.amount = amount;
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
}