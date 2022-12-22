package de.hf.myfinance.valuation.persistence.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Map;

@Document(collection = "curves")
public class ValueCurveEntity {
    private static final long serialVersionUID = 1L;

    @Id
    private String curveid;
    @Version
    private Integer version;

    private Map<LocalDate, Double> valueCurve;
    private String parentBusinesskey;

    @Indexed(unique = true)
    private String instrumentBusinesskey;

    public String getCurveid() {
        return curveid;
    }
    public void setCurveid(String curveid) {
        this.curveid = curveid;
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
        return instrumentBusinesskey;
    }
    public void setInstrumentBusinesskey(String instrumentBusinesskey) {
        this.instrumentBusinesskey = instrumentBusinesskey;
    }

    public String getParentBusinesskey() {
        return parentBusinesskey;
    }
    public void setParentBusinesskey(String parentBusinesskey) {
        this.parentBusinesskey = parentBusinesskey;
    }
}
