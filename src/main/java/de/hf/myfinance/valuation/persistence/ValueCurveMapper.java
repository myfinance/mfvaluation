package de.hf.myfinance.valuation.persistence;

import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ValueCurveMapper {
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    ValueCurve entityToApi(ValueCurveEntity entity);

    @Mappings({
            @Mapping(target = "curveid", ignore = true)
    })
    ValueCurveEntity apiToEntity(ValueCurve api);

    List<ValueCurve> entityListToApiList(List<ValueCurveEntity> entity);

    List<ValueCurveEntity> apiListToEntityList(List<ValueCurve> api);

    default ValueCurve createInstrument() {
        return new ValueCurve();
    }
}
