package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.ItemEncomendaClienteRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import org.springframework.stereotype.Component;

@Component
public class ItemEncomendaClienteMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: encomendaId e tipoPelletId devem ser resolvidas via serviço
     */
    public ItemEncomendaCliente toEntity(ItemEncomendaClienteRequestDTO dto) {
        if (dto == null) return null;

        ItemEncomendaCliente entity = new ItemEncomendaCliente();
        entity.setQuantidadeKg(dto.quantidadeKg());
        entity.setPrecoUnitarioNet(dto.precoUnitarioNet());
        entity.setTaxaIva(dto.taxaIva());
        // Calcular IVA
        Double valorIva = dto.precoUnitarioNet() * dto.quantidadeKg() * (dto.taxaIva() / 100.0);
        entity.setValorIvaCalculado(valorIva);
        // Encomenda e TipoPellet serão setados no serviço

        return entity;
    }

    public ItemEncomendaClienteResponseDTO toResponseDTO(ItemEncomendaCliente entity) {
        if (entity == null) return null;

        return new ItemEncomendaClienteResponseDTO(
                entity.getId(),
                entity.getEncomenda() != null ? entity.getEncomenda().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getQuantidadeKg(),
                entity.getPrecoUnitarioNet(),
                entity.getTaxaIva(),
                entity.getValorIvaCalculado()
        );
    }

    public ItemEncomendaClienteSimpleDTO toSimpleDTO(ItemEncomendaCliente entity) {
        if (entity == null) return null;

        return new ItemEncomendaClienteSimpleDTO(
                entity.getId(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getQuantidadeKg(),
                entity.getPrecoUnitarioNet(),
                entity.getTaxaIva()
        );
    }

    public void updateEntityFromDTO(ItemEncomendaClienteRequestDTO dto, ItemEncomendaCliente entity) {
        if (dto == null) return;

        if (dto.quantidadeKg() != null) {
            entity.setQuantidadeKg(dto.quantidadeKg());
        }
        if (dto.precoUnitarioNet() != null) {
            entity.setPrecoUnitarioNet(dto.precoUnitarioNet());
        }
        if (dto.taxaIva() != null) {
            entity.setTaxaIva(dto.taxaIva());
            // Recalcular IVA
            Double valorIva = entity.getPrecoUnitarioNet() * entity.getQuantidadeKg() * (dto.taxaIva() / 100.0);
            entity.setValorIvaCalculado(valorIva);
        }
    }
}
