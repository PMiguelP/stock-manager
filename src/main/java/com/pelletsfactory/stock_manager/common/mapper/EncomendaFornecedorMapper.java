package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.EncomendaFornecedorRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class EncomendaFornecedorMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: fornecedorId e moedaId devem ser resolvidas via serviço
     */
    public EncomendaFornecedor toEntity(EncomendaFornecedorRequestDTO dto) {
        if (dto == null) return null;

        EncomendaFornecedor entity = new EncomendaFornecedor();
        entity.setData(parseLocalDate(dto.data()));
        entity.setTotalLiquido(dto.totalLiquido());
        entity.setTotalIva(dto.totalIva());
        entity.setTotalFinal(dto.totalFinal());
        // Fornecedor e Moeda serão setados no serviço

        return entity;
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null) return null;

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Data inválida. Use dd-MM-yyyy ou yyyy-MM-dd");
            }
        }
    }

    public EncomendaFornecedorResponseDTO toResponseDTO(EncomendaFornecedor entity) {
        if (entity == null) return null;

        return new EncomendaFornecedorResponseDTO(
                entity.getId(),
                entity.getFornecedor() != null ? entity.getFornecedor().getId() : null,
                entity.getFornecedor() != null ? entity.getFornecedor().getNome() : null,
                entity.getData(),
                entity.getEstado(),
                entity.getTotalLiquido(),
                entity.getTotalIva(),
                entity.getTotalFinal(),
                entity.getMoeda() != null ? entity.getMoeda().getId() : null,
                entity.getMoeda() != null ? entity.getMoeda().getCodigo() : null
        );
    }

    public void updateEntityFromDTO(EncomendaFornecedorRequestDTO dto, EncomendaFornecedor entity) {
        if (dto == null) return;

        if (dto.data() != null) {
            entity.setData(parseLocalDate(dto.data()));
        }
        if (dto.totalLiquido() != null) {
            entity.setTotalLiquido(dto.totalLiquido());
        }
        if (dto.totalIva() != null) {
            entity.setTotalIva(dto.totalIva());
        }
        if (dto.totalFinal() != null) {
            entity.setTotalFinal(dto.totalFinal());
        }
    }
}

