package de.hf.myfinance.valuation.persistence.mapper;

import de.hf.myfinance.restmodel.Cashflow;
import de.hf.myfinance.valuation.persistence.entities.CashflowEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CashflowMapper {
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Cashflow entityToApi(CashflowEntity entity);

    @Mappings({
            @Mapping(target = "cashflowid", ignore = true)
    })
    CashflowEntity apiToEntity(Cashflow api);

    List<Cashflow> entityListToApiList(List<CashflowEntity> entity);

    List<CashflowEntity> apiListToEntityList(List<Cashflow> api);

    default Cashflow createCashflow() {
        return new Cashflow();
    }
}
