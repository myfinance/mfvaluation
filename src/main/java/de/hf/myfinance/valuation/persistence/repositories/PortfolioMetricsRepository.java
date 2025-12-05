package de.hf.myfinance.valuation.persistence.repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import de.hf.myfinance.valuation.persistence.entities.PortfolioMetricsEntity;

public interface PortfolioMetricsRepository extends ReactiveCrudRepository<PortfolioMetricsEntity, String> {

}
