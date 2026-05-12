package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.LotePelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class LotePelletMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: Ordem e TipoPellet devem ser passadas via serviço
     */
    public LotePellet toEntity(LotePelletRequestDTO dto) {
        if (dto == null) return null;

        LotePellet entity = new LotePellet();
        entity.setCodigoLote(dto.codigoLote());
        entity.setQuantidadeKg(dto.quantidadeKg());
        entity.setLocalizacaoArmazem(dto.localizacaoArmazem());
        // Ordem e TipoPellet serão setadas no serviço via seus repositórios

        return entity;
    }

    /**
     * Entidade -> DTO Response
     */
    public LotePelletResponseDTO toResponseDTO(LotePellet entity) {
        if (entity == null) return null;

        LocalDateTime dataProducao = entity.getDataProducao() != null
                ? LocalDateTime.ofInstant(entity.getDataProducao(), ZoneId.systemDefault())
                : null;

        return new LotePelletResponseDTO(
                entity.getId(),
                entity.getOrdem() != null ? entity.getOrdem().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getId() : null,
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getCodigoLote(),
                entity.getQuantidadeKg(),
                dataProducao,
                entity.getLocalizacaoArmazem()
        );
    }

    /**
     * Versão simplificada
     */
    public LotePelletSimpleDTO toSimpleDTO(LotePellet entity) {
        if (entity == null) return null;

        LocalDateTime dataProducao = entity.getDataProducao() != null
                ? LocalDateTime.ofInstant(entity.getDataProducao(), ZoneId.systemDefault())
                : null;

        return new LotePelletSimpleDTO(
                entity.getId(),
                entity.getCodigoLote(),
                entity.getTipoPellet() != null ? entity.getTipoPellet().getNome() : null,
                entity.getQuantidadeKg(),
                dataProducao,
                entity.getLocalizacaoArmazem()
        );
    }

    /**
     * Atualiza entidade existente
     */
    public void updateEntityFromDTO(LotePelletRequestDTO dto, LotePellet entity) {
        if (dto == null) return;

        if (dto.codigoLote() != null) {
            entity.setCodigoLote(dto.codigoLote());
        }
        if (dto.quantidadeKg() != null) {
            entity.setQuantidadeKg(dto.quantidadeKg());
        }
        if (dto.localizacaoArmazem() != null) {
            entity.setLocalizacaoArmazem(dto.localizacaoArmazem());
        }
    }
}

