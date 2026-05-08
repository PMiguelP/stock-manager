package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.FuncionarioRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FuncionarioSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
        entity.setNumeroFuncionario(dto.numeroFuncionario());
        entity.setDataAdmissao(parseLocalDate(dto.dataAdmissao()));

        return entity;
    }

    /**
     * Parse String para LocalDate (dd-MM-yyyy ou yyyy-MM-dd)
     */
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null) return null;
        
        try {
            // Tenta formato dd-MM-yyyy
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (DateTimeParseException e1) {
            try {
                // Tenta formato yyyy-MM-dd
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Data inválida. Use dd-MM-yyyy ou yyyy-MM-dd");
            }
        }
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
        if (dto.numeroFuncionario() != null) {
            entity.setNumeroFuncionario(dto.numeroFuncionario());
        }
        if (dto.dataAdmissao() != null) {
            entity.setDataAdmissao(parseLocalDate(dto.dataAdmissao()));
        }
    }
}