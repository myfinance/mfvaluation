package de.hf.myfinance.valuation.persistence.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.valuation.persistence.entities.TradeEntity;

@Mapper(componentModel = "spring")
public interface TradeMapper {

    Trade entityToApi(TradeEntity entity);

    TradeEntity apiToEntity(Trade api);

    List<Trade> entityListToApiList(List<TradeEntity> entity);

    List<TradeEntity> apiListToEntityList(List<Trade> api);

    default Trade createTransaction() {
        return new Trade("","",0.0);
    }
}
