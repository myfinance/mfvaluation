package de.hf.myfinance.valuation.persistence.entities;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "portfoliometrics")
public class PortfolioMetricsEntity {
    // TOTAL - Complete Portfolio with all securities and Cash Positions
    // SECURITIES - ALL Security 
    // EQUITIES, BONDS, FUNDS, ETF - Asset Classes
    // INDUSTRY - like Technology, Health Care
    // Region like Europe
    // Single Security Businesskey
    @Id
    private String portfolio;
    @Version
    private Integer version;

    private Map<Integer, Double> cagrPerYear;
    private Double totalCagr;

    private Boolean isSingleSecurity;

    public PortfolioMetricsEntity(){}

    public PortfolioMetricsEntity(String portfolio) {
        this.portfolio = portfolio;
    }
    public PortfolioMetricsEntity(String portfolio, String securityBusinessKey, Double totalCagr, Map<Integer, Double> cagrPerYear) {
        this.portfolio = portfolio;
        this.cagrPerYear = cagrPerYear;
        this.totalCagr = totalCagr;
     }
  

    public String getPortfolio() {
        return this.portfolio;
    }

    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<Integer,Double> getCagrPerYear() {
        return this.cagrPerYear;
    }

    public void setCagrPerYear(Map<Integer,Double> cagrPerYear) {
        this.cagrPerYear = cagrPerYear;
    }

    public Double getTotalCagr() {
        return this.totalCagr;
    }

    public void setTotalCagr(Double totalCagr) {
        this.totalCagr = totalCagr;
    }

    public Boolean getIsSingleSecurity() {
        return this.isSingleSecurity;
    }

    public void setIsSingleSecurity(Boolean isSingleSecurity) {
        this.isSingleSecurity = isSingleSecurity;
    }
     
}
