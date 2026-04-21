package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.FuncionarioRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import org.springframework.stereotype.Component;

@Component
public class FuncionarioMapper {

    /**
     * DTO Request -> Entidade JPA
     */
    public Funcionario toEntity(FuncionarioRequestDTO dto) {
        if (dto == null) return null;

        Funcionario entity = new Funcionario();
        entity.setCargo(Cargo.valueOf(dto.cargo())); // String -> Enum
        entity.setNome(dto.nome());
        entity.setNif(dto.nif());
        entity.setContacto(dto.contacto());

        return entity;
    }

    /**
     * Entidade -> DTO Response
     */
    public FuncionarioResponseDTO toResponseDTO(Funcionario entity) {
        if (entity == null) return null;

        return new FuncionarioResponseDTO(
                entity.getId(),
                entity.getCargo().name(), // Enum -> String
                entity.getNome(),
                entity.getNif(),
                entity.getNumeroFuncionario(),
                entity.getDataAdmissao()
        );
    }

    /**
     * Versão simplificada para tabelas e comboboxes
     */
    public FuncionarioSimpleDTO toSimpleDTO(Funcionario entity) {
        if (entity == null) return null;

        return new FuncionarioSimpleDTO(
                entity.getId(),
                entity.getNome(),
                entity.getCargo().name(), // Enum -> String
                entity.getNumeroFuncionario(),
                entity.getDataAdmissao()
        );
    }

    /**
     * Detalhes completos (base - estatísticas preenchidas no Service)
     */
    public FuncionarioDetailsDTO toDetailsDTO(Funcionario entity) {
        if (entity == null) return null;

        return new FuncionarioDetailsDTO(
                entity.getId(),
                entity.getNome(),
                entity.getNif(),
                entity.getContacto(),
                entity.getCargo(), // Enum direto
                entity.getNumeroFuncionario(),
                entity.getDataAdmissao(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Atualiza uma entidade existente (para PUT/Edit)
     */
    public void updateEntityFromDTO(FuncionarioRequestDTO dto, Funcionario entity) {
        if (dto == null || entity == null) return;

        entity.setCargo(Cargo.valueOf(dto.cargo())); // String -> Enum
        entity.setNome(dto.nome());
        entity.setNif(dto.nif());
        entity.setContacto(dto.contacto());
    }
}