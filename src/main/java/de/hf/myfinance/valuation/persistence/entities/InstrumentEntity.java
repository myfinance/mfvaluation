package de.hf.myfinance.valuation.persistence.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "instruments")
public class InstrumentEntity {
    @Id
    private String id;

    @Indexed(unique = true)
    private String businesskey;;

    private String testvalue;

    public InstrumentEntity() {}

    public InstrumentEntity(String businesskey, String testvalue) {
        this.businesskey = businesskey;
        this.testvalue = testvalue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusinesskey() {
        return businesskey;
    }

    public void setBusinesskey(String businesskey) {
        this.businesskey = businesskey;
    }

    public String getTestvalue() {
        return testvalue;
    }

    public void setTestvalue(String testvalue) {
        this.testvalue = testvalue;
    }

}
