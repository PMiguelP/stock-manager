package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.AlocacaoOrdemEncomendaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoOrdemEncomendaResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoOrdemEncomendaSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.AlocacaoOrdemEncomenda;
import org.springframework.stereotype.Component;

@Component
public class AlocacaoOrdemEncomendaMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: ordemId e encomendaClienteId devem ser resolvidas via serviço
     */
    public AlocacaoOrdemEncomenda toEntity(AlocacaoOrdemEncomendaRequestDTO dto) {
        if (dto == null) return null;

        AlocacaoOrdemEncomenda entity = new AlocacaoOrdemEncomenda();
        entity.setQuantidadeReservada(dto.quantidadeReservada());
        // Ordem e EncomendaCliente serão setadas no serviço via seus repositórios

        return entity;
    }

    public AlocacaoOrdemEncomendaResponseDTO toResponseDTO(AlocacaoOrdemEncomenda entity) {
        if (entity == null) return null;

        return new AlocacaoOrdemEncomendaResponseDTO(
                entity.getId(),
                entity.getOrdem() != null ? entity.getOrdem().getId() : null,
                entity.getOrdem() != null ? "Ordem-" + entity.getOrdem().getId().toString().substring(0, 8) : null,
                entity.getEncomendaCliente() != null ? entity.getEncomendaCliente().getId() : null,
                entity.getEncomendaCliente() != null ? entity.getEncomendaCliente().getCliente().getNome() : null,
                entity.getQuantidadeReservada()
        );
    }

    public AlocacaoOrdemEncomendaSimpleDTO toSimpleDTO(AlocacaoOrdemEncomenda entity) {
        if (entity == null) return null;

        return new AlocacaoOrdemEncomendaSimpleDTO(
                entity.getId(),
                entity.getOrdem() != null ? "Ordem-" + entity.getOrdem().getId().toString().substring(0, 8) : null,
                entity.getEncomendaCliente() != null ? entity.getEncomendaCliente().getCliente().getNome() : null,
                entity.getQuantidadeReservada()
        );
    }

    public void updateEntityFromDTO(AlocacaoOrdemEncomendaRequestDTO dto, AlocacaoOrdemEncomenda entity) {
        if (dto == null) return;

        if (dto.quantidadeReservada() != null) {
            entity.setQuantidadeReservada(dto.quantidadeReservada());
        }
    }
}