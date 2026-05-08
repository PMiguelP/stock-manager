package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.MovimentoFinanceiroRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import org.springframework.stereotype.Component;

@Component
public class MovimentoFinanceiroMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: moedaId, idEncomendaCliente e idEncomendaFornecedor devem ser resolvidas via serviço
     */
    public MovimentoFinanceiro toEntity(MovimentoFinanceiroRequestDTO dto) {
        if (dto == null) return null;

        MovimentoFinanceiro entity = new MovimentoFinanceiro();
        entity.setTipoMovimento(dto.tipoMovimento());
        entity.setValorTotal(dto.valorTotal());
        // Moeda, EncomendaCliente e EncomendaFornecedor serão setados no serviço

        return entity;
    }

    public MovimentoFinanceiroResponseDTO toResponseDTO(MovimentoFinanceiro entity) {
        if (entity == null) return null;

        return new MovimentoFinanceiroResponseDTO(
                entity.getId(),
                entity.getTipoMovimento(),
                entity.getValorTotal(),
                entity.getMoeda() != null ? entity.getMoeda().getId() : null,
                entity.getMoeda() != null ? entity.getMoeda().getCodigo() : null,
                entity.getEncomendaCliente() != null ? entity.getEncomendaCliente().getId() : null,
                entity.getEncomendaFornecedor() != null ? entity.getEncomendaFornecedor().getId() : null,
                entity.getCreatedAt()
        );
    }

    public MovimentoFinanceiroSimpleDTO toSimpleDTO(MovimentoFinanceiro entity) {
        if (entity == null) return null;

        return new MovimentoFinanceiroSimpleDTO(
                entity.getId(),
                entity.getTipoMovimento(),
                entity.getValorTotal(),
                entity.getMoeda() != null ? entity.getMoeda().getCodigo() : null,
                entity.getCreatedAt()
        );
    }

    public void updateEntityFromDTO(MovimentoFinanceiroRequestDTO dto, MovimentoFinanceiro entity) {
        if (dto == null) return;

        if (dto.tipoMovimento() != null) {
            entity.setTipoMovimento(dto.tipoMovimento());
        }
        if (dto.valorTotal() != null) {
            entity.setValorTotal(dto.valorTotal());
        }
    }
}
