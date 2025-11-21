package de.hf.myfinance.valuation.persistence.entities;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "positionvalues")
public class PositionValueEntity {
    @Id
    private PositionValueKey positionValueKey;
    @Version
    private Integer version;

    private Map<LocalDate, Double> positionValueCurve;

    public PositionValueEntity(){}

    public PositionValueEntity(String depotBusinessKey, String securityBusinessKey, Map<LocalDate, Double> positionValueCurve) {
        this.positionValueKey = new PositionValueKey(depotBusinessKey,securityBusinessKey);
        this.positionValueCurve = positionValueCurve;
     }

   public PositionValueEntity(PositionValueKey positionValueKey, Map<LocalDate, Double> positionValueCurve) {
        this.positionValueKey = positionValueKey;
        this.positionValueCurve = positionValueCurve;
     }
  
     public Map<LocalDate, Double> getPositionValueCurve() {
        return this.positionValueCurve;
     }
  
     public void setPositionValueCurve(Map<LocalDate, Double> positionValueCurve) {
        this.positionValueCurve = positionValueCurve;
     }

     public PositionValueKey getPositionValueKey() {
        return this.positionValueKey;
     }
  
     public void setPositionValueKey(PositionValueKey positionValueKey) {
        this.positionValueKey = positionValueKey;
     }
}