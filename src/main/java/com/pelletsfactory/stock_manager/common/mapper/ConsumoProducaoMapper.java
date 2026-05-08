package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.ConsumoProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ConsumoResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.ConsumoProducao;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import org.springframework.stereotype.Component;

@Component
public class ConsumoProducaoMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: Ordem e MateriaPrima devem ser passadas via serviço
     */
    public ConsumoProducao toEntity(ConsumoProducaoRequestDTO dto) {
        if (dto == null) return null;

        ConsumoProducao entity = new ConsumoProducao();
        entity.setQuantidadeConsumidaReal(dto.quantidadeConsumidaReal());
        // Ordem e MateriaPrima serão setadas no serviço via seus repositórios

        return entity;
    }

    /**
     * Entidade -> DTO Response
     */
    public ConsumoResponseDTO toResponseDTO(ConsumoProducao entity) {
        if (entity == null) return null;

        return new ConsumoResponseDTO(
                entity.getId(),
                entity.getOrdem() != null ? entity.getOrdem().getId() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getId() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getNome() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getUnidade() : null,
                entity.getQuantidadeConsumidaReal()
        );
    }

    /**
     * Atualiza entidade existente
     */
    public void updateEntityFromDTO(ConsumoProducaoRequestDTO dto, ConsumoProducao entity) {
        if (dto == null) return;

        if (dto.quantidadeConsumidaReal() != null) {
            entity.setQuantidadeConsumidaReal(dto.quantidadeConsumidaReal());
        }
    }
}

