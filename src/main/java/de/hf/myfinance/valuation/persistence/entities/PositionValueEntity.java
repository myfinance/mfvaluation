package de.hf.myfinance.valuation.persistence.entities;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "positionvalues")
public class PositionValueEntity {
    @Id
    private String positionValueId;
    @Version
    private Integer version;

    @Indexed(unique = true)
    private PositionKey positionKey;
    private Map<LocalDate, Double> positionValueCurve;

    public PositionValueEntity(){}

    public PositionValueEntity(String positionValueId) {
        this.positionValueId = positionValueId;
    }
    public PositionValueEntity(String depotBusinessKey, String securityBusinessKey, Map<LocalDate, Double> positionValueCurve) {
        this.positionKey = new PositionKey(depotBusinessKey,securityBusinessKey);
        this.positionValueCurve = positionValueCurve;
     }
  
     public Map<LocalDate, Double> getPositionValueCurve() {
        return this.positionValueCurve;
     }
  
     public void setPositionValueCurve(Map<LocalDate, Double> positionValueCurve) {
        this.positionValueCurve = positionValueCurve;
     }

     public PositionKey getPositionKey() {
        return this.positionKey;
     }
  
     public void setPositionKey(PositionKey positionKey) {
        this.positionKey = positionKey;
     }
}