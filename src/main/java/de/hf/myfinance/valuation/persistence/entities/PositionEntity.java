package de.hf.myfinance.valuation.persistence.entities;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "positions")
public class PositionEntity {
    @Id
    private String positionId;
    @Version
    private Integer version;

    @Indexed(unique = true)
    private PositionKey positionKey;
    private Map<LocalDate, Double> positionCurve;

    public PositionEntity(){}

    public PositionEntity(String positionId) {
        this.positionId = positionId;
    }
    public PositionEntity(String depotBusinessKey, String securityBusinessKey, Map<LocalDate, Double> positionCurve) {
        this.positionKey = new PositionKey(depotBusinessKey,securityBusinessKey);
        this.positionCurve = positionCurve;
     }
  
     public Map<LocalDate, Double> getPositionCurve() {
        return this.positionCurve;
     }
  
     public void setPositionCurve(Map<LocalDate, Double> positionCurve) {
        this.positionCurve = positionCurve;
     }

     public PositionKey getPositionKey() {
        return this.positionKey;
     }
  
     public void setPositionKey(PositionKey positionKey) {
        this.positionKey = positionKey;
     }
}
