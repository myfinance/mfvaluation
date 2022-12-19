package de.hf.myfinance.valuation.persistence;

import de.hf.myfinance.restmodel.Instrument;
import de.hf.myfinance.valuation.persistence.entities.InstrumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InstrumentMapper {
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Instrument entityToApi(InstrumentEntity entity);

    @Mappings({
            @Mapping(target = "instrumentid", ignore = true)
    })
    InstrumentEntity apiToEntity(Instrument api);

    List<Instrument> entityListToApiList(List<InstrumentEntity> entity);

    List<InstrumentEntity> apiListToEntityList(List<Instrument> api);

    default Instrument createInstrument() {
        return new Instrument();
    }
}
