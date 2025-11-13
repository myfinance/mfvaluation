package de.hf.myfinance.valuation.persistence.entities;

import java.util.Objects;

public class PositionKey {
    
    private String depotBusinessKey;
    private String securityBusinessKey;
    private ValuationType valuationType = ValuationType.MARKETVALUE;

    public PositionKey() {
   }

    public PositionKey(String depotBusinessKey, String securityBusinessKey) {
        this.depotBusinessKey = depotBusinessKey;
        this.securityBusinessKey = securityBusinessKey;
 
    }

    public PositionKey(String depotBusinessKey, String securityBusinessKey, ValuationType valuationType) {
        this.depotBusinessKey = depotBusinessKey;
        this.securityBusinessKey = securityBusinessKey;
        this.valuationType = valuationType;
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

     public ValuationType getValuationType() {
        return valuationType;
    }

    public void setValuationType(ValuationType valuationType) {
        this.valuationType = valuationType;
    }

     @Override
     public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionKey that = (PositionKey) o;
        return Objects.equals(securityBusinessKey, that.securityBusinessKey) &&
               Objects.equals(depotBusinessKey, that.depotBusinessKey) &&
               valuationType == that.valuationType;
     }
 
     @Override
     public int hashCode() {
        return Objects.hash(securityBusinessKey, depotBusinessKey, valuationType);
     }
}