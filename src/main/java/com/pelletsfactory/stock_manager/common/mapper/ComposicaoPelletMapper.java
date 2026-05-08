package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.ComposicaoPelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ComposicaoPelletResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ComposicaoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.ComposicaoPellet;
import org.springframework.stereotype.Component;

@Component
public class ComposicaoPelletMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: formulaId e materiaPrimaId devem ser resolvidas via serviço
     */
    public ComposicaoPellet toEntity(ComposicaoPelletRequestDTO dto) {
        if (dto == null) return null;

        ComposicaoPellet entity = new ComposicaoPellet();
        entity.setQuantidadePorKg(dto.quantidadePorKg());
        // Formula e MateriaPrima serão setadas no serviço via seus repositórios

        return entity;
    }

    /**
     * Entidade -> DTO Response
     */
    public ComposicaoPelletResponseDTO toResponseDTO(ComposicaoPellet entity) {
        if (entity == null) return null;

        return new ComposicaoPelletResponseDTO(
                entity.getId(),
                entity.getFormulaProducao() != null ? entity.getFormulaProducao().getId() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getId() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getNome() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getUnidade() : null,
                entity.getQuantidadePorKg()
        );
    }

    public ComposicaoPelletSimpleDTO toSimpleDTO(ComposicaoPellet entity) {
        if (entity == null) return null;

        return new ComposicaoPelletSimpleDTO(
                entity.getId(),
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getNome() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getUnidade() : null,
                entity.getQuantidadePorKg()
        );
    }

    /**
     * Atualiza entidade existente
     */
    public void updateEntityFromDTO(ComposicaoPelletRequestDTO dto, ComposicaoPellet entity) {
        if (dto == null) return;

        if (dto.quantidadePorKg() != null) {
            entity.setQuantidadePorKg(dto.quantidadePorKg());
        }
    }
}
