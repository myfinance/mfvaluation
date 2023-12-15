package de.hf.myfinance.valuation.persistence.entities;

import de.hf.myfinance.restmodel.AdditionalLists;
import de.hf.myfinance.restmodel.AdditionalMaps;
import de.hf.myfinance.restmodel.AdditionalProperties;
import de.hf.myfinance.restmodel.InstrumentType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "instruments")
public class InstrumentEntity implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String instrumentid;
    @Version
    private Integer version;
    private Integer instrumentTypeId;
    private InstrumentType instrumentType;
    private boolean active;
    @Indexed(unique = true)
    private String businesskey;
    private String parentBusinesskey;

    private Map<AdditionalMaps, Map<String, String>> additionalMaps = new HashMap<>();
    private Map<AdditionalProperties, String> additionalProperties = new HashMap<>();
    private Map<AdditionalLists, List<String>> additionalLists = new HashMap<>();

    public InstrumentEntity() {
    }

    public InstrumentEntity(InstrumentType instrumentType, boolean active) {
        setInstrumentTypeId(instrumentType.getValue());
        this.active = active;
    }

    public String getInstrumentid() {
        return this.instrumentid;
    }
    public void setInstrumentid(String instrumentid) {
        this.instrumentid = instrumentid;
    }

    protected Integer getInstrumentTypeId() {
        return this.instrumentTypeId;
    }
    protected void setInstrumentTypeId(Integer instrumentTypeId) {
        this.instrumentTypeId = instrumentTypeId;
        instrumentType = InstrumentType.getInstrumentTypeById(instrumentTypeId);
    }

    public InstrumentType getInstrumentType(){
        return instrumentType;
    }
    public void setInstrumentType(InstrumentType instrumentType) {
        this.instrumentType = instrumentType;
        this.instrumentTypeId = instrumentType.getValue();
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBusinesskey() {
        return this.businesskey;
    }
    public void setBusinesskey(String businesskey) {
        this.businesskey = businesskey;
    }

    public Map<AdditionalProperties, String> getAdditionalProperties() {
        return additionalProperties;
    }
    public void setAdditionalProperties(Map<AdditionalProperties, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Map<AdditionalMaps, Map<String, String>> getAdditionalMaps() {
        return additionalMaps;
    }
    public void setAdditionalMaps(Map<AdditionalMaps, Map<String, String>> additionalMaps) {
        this.additionalMaps = additionalMaps;
    }

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getParentBusinesskey() {
        return parentBusinesskey;
    }
    public void setParentBusinesskey(String parentBusinesskey) {
        this.parentBusinesskey = parentBusinesskey;
    }

    public Map<AdditionalLists, List<String>> getAdditionalLists() {
        return additionalLists;
    }
    public void setAdditionalLists(Map<AdditionalLists, List<String>> additionalLists) {
        this.additionalLists = additionalLists;
    }

}
