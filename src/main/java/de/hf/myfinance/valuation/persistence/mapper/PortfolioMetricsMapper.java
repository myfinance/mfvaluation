package de.hf.myfinance.valuation.persistence.mapper;

import de.hf.myfinance.restmodel.PortfolioMetrics;
import de.hf.myfinance.valuation.persistence.entities.PortfolioMetricsEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PortfolioMetricsMapper {
    @Mappings({
            
    })
    PortfolioMetrics entityToApi(PortfolioMetricsEntity entity);

    @Mappings({
            @Mapping(target = "version", ignore = true)
    })
    PortfolioMetricsEntity apiToEntity(PortfolioMetrics api);

    List<PortfolioMetrics> entityListToApiList(List<PortfolioMetricsEntity> entity);

    List<PortfolioMetricsEntity> apiListToEntityList(List<PortfolioMetrics> api);

    default PortfolioMetrics createPortfolioMetrics() {
        return new PortfolioMetrics();
    }
}
