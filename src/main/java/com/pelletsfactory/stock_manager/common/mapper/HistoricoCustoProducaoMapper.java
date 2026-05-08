package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.HistoricoCustoProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.HistoricoCustoProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.HistoricoCustoProducao;
import org.springframework.stereotype.Component;

@Component
public class HistoricoCustoProducaoMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: tipoPelletId deve ser resolvida via serviço
     */
    public HistoricoCustoProducao toEntity(HistoricoCustoProducaoRequestDTO dto) {
        if (dto == null) return null;

        HistoricoCustoProducao entity = new HistoricoCustoProducao();
        entity.setCustoBasePorKg(dto.custoBasePorKg());
        // TipoPellet será setado no serviço

        return entity;
    }

    public HistoricoCustoProducaoResponseDTO toResponseDTO(HistoricoCustoProducao entity) {
        if (entity == null) return null;

        return new HistoricoCustoProducaoResponseDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getCustoBasePorKg(),
                entity.getDataInicio(),
                entity.getDataFim()
        );
    }

    public void updateEntityFromDTO(HistoricoCustoProducaoRequestDTO dto, HistoricoCustoProducao entity) {
        if (dto == null) return;

        if (dto.custoBasePorKg() != null) {
            entity.setCustoBasePorKg(dto.custoBasePorKg());
        }
    }
}

