package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.ItemEncomendaFornecedorRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaFornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaFornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaFornecedor;
import org.springframework.stereotype.Component;

@Component
public class ItemEncomendaFornecedorMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: encomendaId e materiaPrimaId devem ser resolvidas via serviço
     */
    public ItemEncomendaFornecedor toEntity(ItemEncomendaFornecedorRequestDTO dto) {
        if (dto == null) return null;

        ItemEncomendaFornecedor entity = new ItemEncomendaFornecedor();
        entity.setQuantidade(dto.quantidade());
        entity.setPrecoUnitarioNet(dto.precoUnitarioNet());
        entity.setTaxaIva(dto.taxaIva());
        // Calcular IVA
        Double valorIva = dto.precoUnitarioNet() * dto.quantidade() * (dto.taxaIva() / 100.0);
        entity.setValorIvaCalculado(valorIva);
        // Encomenda e MateriaPrima serão setadas no serviço

        return entity;
    }

    public ItemEncomendaFornecedorResponseDTO toResponseDTO(ItemEncomendaFornecedor entity) {
        if (entity == null) return null;

        return new ItemEncomendaFornecedorResponseDTO(
                entity.getId(),
                entity.getEncomenda() != null ? entity.getEncomenda().getId() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getId() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getNome() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getUnidade() : null,
                entity.getQuantidade(),
                entity.getPrecoUnitarioNet(),
                entity.getTaxaIva(),
                entity.getValorIvaCalculado()
        );
    }

    public ItemEncomendaFornecedorSimpleDTO toSimpleDTO(ItemEncomendaFornecedor entity) {
        if (entity == null) return null;

        return new ItemEncomendaFornecedorSimpleDTO(
                entity.getId(),
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getNome() : null,
                entity.getMateriaPrima() != null ? entity.getMateriaPrima().getUnidade() : null,
                entity.getQuantidade(),
                entity.getPrecoUnitarioNet(),
                entity.getTaxaIva()
        );
    }

    public void updateEntityFromDTO(ItemEncomendaFornecedorRequestDTO dto, ItemEncomendaFornecedor entity) {
        if (dto == null) return;

        if (dto.quantidade() != null) {
            entity.setQuantidade(dto.quantidade());
        }
        if (dto.precoUnitarioNet() != null) {
            entity.setPrecoUnitarioNet(dto.precoUnitarioNet());
        }
        if (dto.taxaIva() != null) {
            entity.setTaxaIva(dto.taxaIva());
            // Recalcular IVA
            Double valorIva = entity.getPrecoUnitarioNet() * entity.getQuantidade() * (dto.taxaIva() / 100.0);
            entity.setValorIvaCalculado(valorIva);
        }
    }
}
