package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.MateriaPrimaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import org.springframework.stereotype.Component;

@Component
public class MateriaPrimaMapper {

    public MateriaPrima toEntity(MateriaPrimaRequestDTO dto) {
        if (dto == null) return null;

        MateriaPrima entity = new MateriaPrima();
        entity.setNome(dto.nome());
        entity.setUnidade(dto.unidade());
        entity.setStockAtual(dto.stockAtual() != null ? dto.stockAtual() : 0.0);
        entity.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0.0);
        return entity;
    }

    public MateriaPrimaResponseDTO toResponseDTO(MateriaPrima entity) {
        if (entity == null) return null;

        return new MateriaPrimaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getUnidade(),
                entity.getStockAtual(),
                entity.getStockMinimo()
        );
    }

    public void updateEntityFromDTO(MateriaPrimaRequestDTO dto, MateriaPrima entity) {
        if (dto == null) return;

        if (dto.nome() != null) {
            entity.setNome(dto.nome());
        }
        if (dto.unidade() != null) {
            entity.setUnidade(dto.unidade());
        }
        if (dto.stockAtual() != null) {
            entity.setStockAtual(dto.stockAtual());
        }
        if (dto.stockMinimo() != null) {
            entity.setStockMinimo(dto.stockMinimo());
        }
    }
}

