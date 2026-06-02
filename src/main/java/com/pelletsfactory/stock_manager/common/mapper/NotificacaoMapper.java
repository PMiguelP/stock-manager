package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.NotificacaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Notificacao;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoMapper {

    public Notificacao toEntity(NotificacaoRequestDTO dto) {
        if (dto == null) return null;

        Notificacao entity = new Notificacao(dto.titulo(), dto.mensagem(), dto.tipoEvento());
        entity.setCargoAlvo(dto.cargoAlvo());
        entity.setRequerAcao(dto.requerAcao());
        entity.setLinkReferencia(dto.linkReferencia());

        return entity;
    }

    public NotificacaoResponseDTO toResponseDTO(Notificacao entity) {
        return toResponseDTO(entity, entity != null ? entity.getLida() : null);
    }

    public NotificacaoResponseDTO toResponseDTO(Notificacao entity, Boolean lida) {
        if (entity == null) return null;

        return new NotificacaoResponseDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getMensagem(),
                entity.getTipoEvento(),
                entity.getCargoAlvo(),
                Boolean.TRUE.equals(lida),
                entity.getRequerAcao(),
                entity.getConcluida(),
                entity.getConcluidaPor() != null ? entity.getConcluidaPor().getNome() : null,
                entity.getConcluidaEm(),
                entity.getLinkReferencia(),
                entity.getCreatedAt()
        );
    }

    public NotificacaoSimpleDTO toSimpleDTO(Notificacao entity) {
        return toSimpleDTO(entity, entity != null ? entity.getLida() : null);
    }

    public NotificacaoSimpleDTO toSimpleDTO(Notificacao entity, Boolean lida) {
        if (entity == null) return null;

        return new NotificacaoSimpleDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getTipoEvento(),
                entity.getCargoAlvo(),
                Boolean.TRUE.equals(lida),
                entity.getRequerAcao(),
                entity.getConcluida(),
                entity.getConcluidaPor() != null ? entity.getConcluidaPor().getNome() : null,
                entity.getConcluidaEm(),
                entity.getCreatedAt()
        );
    }

    public void updateEntityFromDTO(NotificacaoRequestDTO dto, Notificacao entity) {
        if (dto == null) return;

        if (dto.titulo() != null) {
            entity.setTitulo(dto.titulo());
        }
        if (dto.mensagem() != null) {
            entity.setMensagem(dto.mensagem());
        }
        if (dto.tipoEvento() != null) {
            entity.setTipoEvento(dto.tipoEvento());
        }
        if (dto.cargoAlvo() != null) {
            entity.setCargoAlvo(dto.cargoAlvo());
        }
        if (dto.requerAcao() != null) {
            entity.setRequerAcao(dto.requerAcao());
        }
        if (dto.linkReferencia() != null) {
            entity.setLinkReferencia(dto.linkReferencia());
        }
    }
}
