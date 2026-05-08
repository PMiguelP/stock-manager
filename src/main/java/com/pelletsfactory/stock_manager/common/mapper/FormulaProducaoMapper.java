package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.FormulaProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ComposicaoPelletResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FormulaSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import org.springframework.stereotype.Component;

@Component
public class FormulaProducaoMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: tipoPelletId deve ser resolvida via serviço
     */
    public FormulaProducao toEntity(FormulaProducaoRequestDTO dto) {
        if (dto == null) return null;

        FormulaProducao entity = new FormulaProducao();
        entity.setNome(dto.nome());
        entity.setAtiva(dto.ativa());
        // TipoPellet será setado no serviço via seu repositório

        return entity;
    }

    /**
     * Entidade -> DTO Response (completo com composições)
     */
    public FormulaProducaoResponseDTO toResponseDTO(FormulaProducao entity) {
        if (entity == null) return null;

        var composicoes = entity.getComposicao() != null ?
                entity.getComposicao().stream()
                        .map(c -> new ComposicaoPelletResponseDTO(
                                c.getId(),
                                c.getFormulaProducao().getId(),
                                c.getMateriaPrima().getId(),
                                c.getMateriaPrima().getNome(),
                                c.getMateriaPrima().getUnidade(),
                                c.getQuantidadePorKg()
                        ))
                        .toList()
                : null;

        return new FormulaProducaoResponseDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getNome(),
                entity.getAtiva(),
                composicoes
        );
    }

    /**
     * Versão simplificada
     */
    public FormulaSimpleDTO toSimpleDTO(FormulaProducao entity) {
        if (entity == null) return null;

        return new FormulaSimpleDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getNome(),
                entity.getAtiva()
        );
    }

    /**
     * Atualiza entidade existente
     */
    public void updateEntityFromDTO(FormulaProducaoRequestDTO dto, FormulaProducao entity) {
        if (dto == null) return;

        if (dto.nome() != null) {
            entity.setNome(dto.nome());
        }
        if (dto.ativa() != null) {
            entity.setAtiva(dto.ativa());
        }
    }
}
