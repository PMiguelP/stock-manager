package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.ClienteRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteRequestDTO dto) {
        if (dto == null) return null;

        return new Cliente(
                dto.nome(),
                dto.nif(),
                dto.contacto(),
                dto.email()
        );
    }

    public ClienteResponseDTO toResponseDTO(Cliente entity) {
        if (entity == null) return null;

        return new ClienteResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getNif(),
                entity.getContacto(),
                entity.getEmail()
        );
    }

    public ClienteSimpleDTO toSimpleDTO(Cliente entity) {
        if (entity == null) return null;

        return new ClienteSimpleDTO(
                entity.getId(),
                entity.getNome(),
                entity.getNif(),
                entity.getContacto()
        );
    }

    public void updateEntityFromDTO(ClienteRequestDTO dto, Cliente entity) {
        if (dto == null) return;

        if (dto.nome() != null) {
            entity.setNome(dto.nome());
        }
        if (dto.nif() != null) {
            entity.setNif(dto.nif());
        }
        if (dto.contacto() != null) {
            entity.setContacto(dto.contacto());
        }
        if (dto.email() != null) {
            entity.setEmail(dto.email());
        }
    }
}
