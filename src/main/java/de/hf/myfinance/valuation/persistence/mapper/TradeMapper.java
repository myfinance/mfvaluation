package de.hf.myfinance.valuation.persistence.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import de.hf.myfinance.restmodel.Trade;
import de.hf.myfinance.valuation.persistence.entities.TradeEntity;

@Mapper(componentModel = "spring")
public interface TradeMapper {

    @Mapping(source = "positionKey.depotBusinessKey", target = "depotBusinessKey")
    @Mapping(source = "positionKey.securityBusinessKey", target = "securityBusinessKey")
    Trade entityToApi(TradeEntity entity);

    @Mapping(source = "depotBusinessKey", target = "positionKey.depotBusinessKey")
    @Mapping(source = "securityBusinessKey", target = "positionKey.securityBusinessKey")
    TradeEntity apiToEntity(Trade api);

    List<Trade> entityListToApiList(List<TradeEntity> entity);

    List<TradeEntity> apiListToEntityList(List<Trade> api);

    default Trade createTransaction() {
        return new Trade("","",0.0);
    }
}
