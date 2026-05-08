package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.MoedaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.Moeda;
import org.springframework.stereotype.Component;

@Component
public class MoedaMapper {

    public Moeda toEntity(MoedaRequestDTO dto) {
        if (dto == null) return null;

        return new Moeda(dto.codigo(), dto.simbolo());
    }

    public MoedaResponseDTO toResponseDTO(Moeda entity) {
        if (entity == null) return null;

        return new MoedaResponseDTO(
                entity.getId(),
                entity.getCodigo(),
                entity.getSimbolo()
        );
    }

    public void updateEntityFromDTO(MoedaRequestDTO dto, Moeda entity) {
        if (dto == null) return;

        if (dto.codigo() != null) {
            entity.setCodigo(dto.codigo());
        }
        if (dto.simbolo() != null) {
            entity.setSimbolo(dto.simbolo());
        }
    }
}

