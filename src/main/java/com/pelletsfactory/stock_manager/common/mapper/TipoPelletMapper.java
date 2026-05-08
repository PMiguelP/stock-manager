package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.TipoPelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import org.springframework.stereotype.Component;

@Component
public class TipoPelletMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: moedaId deve ser resolvida via serviço
     */
    public TipoPellet toEntity(TipoPelletRequestDTO dto) {
        if (dto == null) return null;

        TipoPellet entity = new TipoPellet();
        entity.setNome(dto.nome());
        entity.setDiametroMm(dto.diametroMm());
        entity.setPoderCalorifico(dto.poderCalorifico());
        entity.setStockAtual(dto.stockAtual() != null ? dto.stockAtual() : 0.0);
        entity.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0.0);
        entity.setCustoAtualPorKg(dto.custoAtualPorKg());
        // Moeda será setada no serviço

        return entity;
    }

    public TipoPelletResponseDTO toResponseDTO(TipoPellet entity) {
        if (entity == null) return null;

        return new TipoPelletResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDiametroMm(),
                entity.getPoderCalorifico(),
                entity.getStockAtual(),
                entity.getStockMinimo(),
                entity.getCustoAtualPorKg(),
                entity.getMoeda() != null ? entity.getMoeda().getId() : null,
                entity.getMoeda() != null ? entity.getMoeda().getCodigo() : null
        );
    }

    public void updateEntityFromDTO(TipoPelletRequestDTO dto, TipoPellet entity) {
        if (dto == null) return;

        if (dto.nome() != null) {
            entity.setNome(dto.nome());
        }
        if (dto.diametroMm() != null) {
            entity.setDiametroMm(dto.diametroMm());
        }
        if (dto.poderCalorifico() != null) {
            entity.setPoderCalorifico(dto.poderCalorifico());
        }
        if (dto.stockAtual() != null) {
            entity.setStockAtual(dto.stockAtual());
        }
        if (dto.stockMinimo() != null) {
            entity.setStockMinimo(dto.stockMinimo());
        }
        if (dto.custoAtualPorKg() != null) {
            entity.setCustoAtualPorKg(dto.custoAtualPorKg());
        }
    }
}

