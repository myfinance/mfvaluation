package de.hf.myfinance.valuation.persistence.mapper;

import de.hf.myfinance.restmodel.ValueCurve;
import de.hf.myfinance.valuation.persistence.entities.ValueCurveEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ValueCurveMapper {
    @Mappings({
            @Mapping(source = "valueCurveKey.instrumentBusinesskey", target = "instrumentBusinesskey"),
            @Mapping(source = "valueCurveKey.valuationType", target = "valuationType"),
            @Mapping(target = "serviceAddress", ignore = true)
    })
    ValueCurve entityToApi(ValueCurveEntity entity);

    @Mappings({
        @Mapping(source = "instrumentBusinesskey", target = "valueCurveKey.instrumentBusinesskey"),
        @Mapping(source = "valuationType", target = "valueCurveKey.valuationType")
    })
    ValueCurveEntity apiToEntity(ValueCurve api);

    List<ValueCurve> entityListToApiList(List<ValueCurveEntity> entity);

    List<ValueCurveEntity> apiListToEntityList(List<ValueCurve> api);

    default ValueCurve createInstrument() {
        return new ValueCurve();
    }
}
