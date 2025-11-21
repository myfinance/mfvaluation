package de.hf.myfinance.valuation.persistence.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "curves")
public class ValueCurveEntity {

    @Id
    private ValueCurveKey valueCurveKey;
    @Version
    private Integer version;

    private Map<LocalDate, Double> valueCurve;
    private String parentBusinesskey;
    private String linkedInstrumentKey;
    private LocalDateTime lastUpdateTs;

    public ValueCurveEntity(){} 

    public ValueCurveEntity(String businessKey, Map<LocalDate, Double> valueCurve) {
        this.valueCurveKey = new ValueCurveKey(businessKey);
        this.valueCurve = valueCurve;
     }

   public ValueCurveEntity(ValueCurveKey valueCurveKey, Map<LocalDate, Double> valueCurve) {
        this.valueCurveKey = valueCurveKey;
        this.valueCurve = valueCurve;
     }

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<LocalDate, Double> getValueCurve() {
        return valueCurve;
    }
    public void setValueCurve(Map<LocalDate, Double> valueCurve) {
        this.valueCurve = valueCurve;
    }

    public String getInstrumentBusinesskey() {
        return this.valueCurveKey.getInstrumentBusinesskey();
    }

    public String getParentBusinesskey() {
        return parentBusinesskey;
    }
    public void setParentBusinesskey(String parentBusinesskey) {
        this.parentBusinesskey = parentBusinesskey;
    }
    public String getLinkedInstrumentKey() {
        return this.linkedInstrumentKey;
    }

    public void setLinkedInstrumentKey(String linkedInstrumentKey) {
        this.linkedInstrumentKey = linkedInstrumentKey;
    }

    public LocalDateTime getLastUpdateTs() {
        return lastUpdateTs;
    }
    public void setLastUpdateTs(LocalDateTime lastUpdateTs) {
        this.lastUpdateTs = lastUpdateTs;
    }

    public ValueCurveKey getValueCurveKey() {
        return valueCurveKey;
    }
    public void setValueCurveKey(ValueCurveKey valueCurveKey) {
        this.valueCurveKey = valueCurveKey;
    }
}
