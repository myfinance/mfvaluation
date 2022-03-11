package de.hf.myfinance.valuation.persistence.repositories;

import de.hf.myfinance.valuation.persistence.entities.InstrumentEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InstrumentRepository extends CrudRepository<InstrumentEntity, Integer> {
    Optional<InstrumentEntity> findByBusinesskey(String businesskey);
}
