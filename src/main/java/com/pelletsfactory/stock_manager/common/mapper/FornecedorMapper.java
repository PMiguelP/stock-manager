package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.FornecedorRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Fornecedor;
import org.springframework.stereotype.Component;

@Component
public class FornecedorMapper {

    public Fornecedor toEntity(FornecedorRequestDTO dto) {
        if (dto == null) return null;

        Fornecedor entity = new Fornecedor();
        entity.setNome(dto.nome());
        entity.setNif(dto.nif());
        entity.setContacto(dto.contacto());
        entity.setEmail(dto.email());
        return entity;
    }

    public FornecedorResponseDTO toResponseDTO(Fornecedor entity) {
        if (entity == null) return null;

        return new FornecedorResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getNif(),
                entity.getContacto(),
                entity.getEmail()
        );
    }

    public FornecedorSimpleDTO toSimpleDTO(Fornecedor entity) {
        if (entity == null) return null;

        return new FornecedorSimpleDTO(
                entity.getId(),
                entity.getNome(),
                entity.getNif(),
                entity.getContacto()
        );
    }

    public void updateEntityFromDTO(FornecedorRequestDTO dto, Fornecedor entity) {
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
