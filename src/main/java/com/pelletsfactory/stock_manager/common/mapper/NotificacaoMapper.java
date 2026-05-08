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
        entity.setLinkReferencia(dto.linkReferencia());

        return entity;
    }

    public NotificacaoResponseDTO toResponseDTO(Notificacao entity) {
        if (entity == null) return null;

        return new NotificacaoResponseDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getMensagem(),
                entity.getTipoEvento(),
                entity.getCargoAlvo(),
                entity.getLida(),
                entity.getLinkReferencia(),
                entity.getCreatedAt()
        );
    }

    public NotificacaoSimpleDTO toSimpleDTO(Notificacao entity) {
        if (entity == null) return null;

        return new NotificacaoSimpleDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getTipoEvento(),
                entity.getCargoAlvo(),
                entity.getLida(),
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
        if (dto.linkReferencia() != null) {
            entity.setLinkReferencia(dto.linkReferencia());
        }
    }
}
