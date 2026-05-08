package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.OrdemProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class OrdemProducaoMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: tipoPelletId, funcionarioId e formulaId devem ser resolvidas via serviço
     * O serviço é responsável por buscar estas entidades nos repositórios
     */
    public OrdemProducao toEntity(OrdemProducaoRequestDTO dto) {
        if (dto == null) return null;

        OrdemProducao entity = new OrdemProducao();
        entity.setQuantidadePlaneada(dto.quantidadePlaneada());
        entity.setDataInicio(parseInstant(dto.dataInicio()));
        entity.setEstado(EstadoOrdemProducao.valueOf(dto.estado()));
        // TipoPellet, Funcionario e FormulaProducao serão setados no serviço

        return entity;
    }

    /**
     * Entidade -> DTO Response
     */
    public OrdemProducaoResponseDTO toResponseDTO(OrdemProducao entity) {
        if (entity == null) return null;

        return new OrdemProducaoResponseDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getNome() : null,
                entity.getFormula() != null ? entity.getFormula().getId() : null,
                entity.getFormula() != null ? entity.getFormula().getNome() : null,
                entity.getQuantidadePlaneada(),
                entity.getQuantidadeProduzidaReal(),
                entity.getDataInicio(),
                entity.getDataFim(),
                entity.getEstado()
        );
    }

    /**
     * Versão simplificada para listagens
     */
    public OrdemProducaoSimpleDTO toSimpleDTO(OrdemProducao entity) {
        if (entity == null) return null;

        return new OrdemProducaoSimpleDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getNome() : null,
                entity.getQuantidadePlaneada(),
                entity.getQuantidadeProduzidaReal(),
                entity.getDataInicio(),
                entity.getEstado()
        );
    }

    /**
     * Detalhes completos (com relacionamentos)
     */
    public OrdemProducaoDetailsDTO toDetailsDTO(OrdemProducao entity) {
        if (entity == null) return null;

        return new OrdemProducaoDetailsDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getId() : null,
                entity.getFuncionario() != null ? entity.getFuncionario().getNome() : null,
                entity.getFormula() != null ? entity.getFormula().getId() : null,
                entity.getFormula() != null ? entity.getFormula().getNome() : null,
                entity.getQuantidadePlaneada(),
                entity.getQuantidadeProduzidaReal(),
                entity.getDataInicio(),
                entity.getDataFim(),
                entity.getEstado(),
                entity.getConsumos() != null ? 
                    entity.getConsumos().stream()
                        .map(c -> new com.pelletsfactory.stock_manager.common.dto.response.ConsumoResponseDTO(
                            c.getId(),
                            c.getOrdem().getId(),
                            c.getMateriaPrima().getId(),
                            c.getMateriaPrima().getNome(),
                            c.getMateriaPrima().getUnidade(),
                            c.getQuantidadeConsumidaReal()
                        ))
                        .toList() 
                    : null,
                entity.getLotes() != null ?
                    entity.getLotes().stream()
                        .map(l -> new com.pelletsfactory.stock_manager.common.dto.response.LotePelletSimpleDTO(
                            l.getId(),
                            l.getCodigoLote(),
                            l.getQuantidadeKg(),
                            l.getDataProducao() != null ? l.getDataProducao().atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
                            l.getLocalizacaoArmazem()
                        ))
                        .toList()
                    : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Atualiza entidade existente (para PUT/PATCH)
     */
    public void updateEntityFromDTO(OrdemProducaoRequestDTO dto, OrdemProducao entity) {
        if (dto == null) return;

        if (dto.quantidadePlaneada() != null) {
            entity.setQuantidadePlaneada(dto.quantidadePlaneada());
        }
        if (dto.dataInicio() != null) {
            entity.setDataInicio(parseInstant(dto.dataInicio()));
        }
        if (dto.estado() != null) {
            entity.setEstado(EstadoOrdemProducao.valueOf(dto.estado()));
        }
    }

    private Instant parseInstant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
            LocalDate localDate = LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
            return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
    }
}
