package de.hf.myfinance.valuation.persistence.entities;

import java.io.Serializable;
import java.util.Objects;

import de.hf.myfinance.restmodel.ValuationType;

public class ValueCurveKey implements Serializable {
    private String instrumentBusinesskey;
    private ValuationType valuationType = ValuationType.MARKETVALUE;

    public ValueCurveKey() {
    }

    public ValueCurveKey(String instrumentBusinesskey) {
        this.instrumentBusinesskey = instrumentBusinesskey;

    }

    public ValueCurveKey(String instrumentBusinesskey, ValuationType valuationType) {
        this.instrumentBusinesskey = instrumentBusinesskey;
        this.valuationType = valuationType;
    }

    public String getInstrumentBusinesskey() {
        return this.instrumentBusinesskey;
    }

    public void setInstrumentBusinesskey(String instrumentBusinesskey) {
        this.instrumentBusinesskey = instrumentBusinesskey;
    }

    public ValuationType getValuationType() {
        return this.valuationType;
    }

    public void setValuationType(ValuationType valuationType) {
        this.valuationType = valuationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ValueCurveKey that = (ValueCurveKey) o;
        return Objects.equals(instrumentBusinesskey, that.instrumentBusinesskey) &&
                valuationType == that.valuationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentBusinesskey, valuationType);
    }
}
