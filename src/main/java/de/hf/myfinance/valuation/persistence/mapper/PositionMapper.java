package de.hf.myfinance.valuation.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import de.hf.myfinance.restmodel.PositionCurve;
import de.hf.myfinance.valuation.persistence.entities.PositionEntity;

import java.util.HashMap;
import java.util.List;


@Mapper(componentModel = "spring")
public interface PositionMapper {

    @Mapping(source = "positionKey.depotBusinessKey", target = "depotBusinessKey")
    @Mapping(source = "positionKey.securityBusinessKey", target = "securityBusinessKey")
    PositionCurve entityToApi(PositionEntity entity);

    @Mapping(source = "depotBusinessKey", target = "positionKey.depotBusinessKey")
    @Mapping(source = "securityBusinessKey", target = "positionKey.securityBusinessKey")
    PositionEntity apiToEntity(PositionCurve api);

    List<PositionCurve> entityListToApiList(List<PositionEntity> entity);

    List<PositionEntity> apiListToEntityList(List<PositionCurve> api);

    default PositionCurve createPosition() {
        return new PositionCurve("","",new HashMap<>());
    }
}
