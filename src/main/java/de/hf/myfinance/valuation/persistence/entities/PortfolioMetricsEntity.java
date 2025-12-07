package de.hf.myfinance.valuation.persistence.entities;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import de.hf.myfinance.restmodel.Cashflow;

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

    //just to understand from which cashflows the metrics were calculated
    private List<Double> cashflows;
    // with start and final values added
    private Map<Integer, List<Double>> cashflowsWithStartAndEndValues;

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
     
    public List<Double> getCashflows() {
        return this.cashflows;
    }

    public void setCashflows(List<Double> cashflows) {
        this.cashflows = cashflows;
    }
    public Map<Integer, List<Double>> getCashflowsWithStartAndEndValues() {
        return this.cashflowsWithStartAndEndValues;
    }
    public void setCashflowsWithStartAndEndValues(Map<Integer, List<Double>> cashflowsWithStartAndEndValues) {
        this.cashflowsWithStartAndEndValues = cashflowsWithStartAndEndValues;
    }
}
