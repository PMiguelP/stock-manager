package com.pelletsfactory.stock_manager.common.mapper;

import com.pelletsfactory.stock_manager.common.dto.request.EncomendaClienteRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class EncomendaClienteMapper {

    /**
     * DTO Request -> Entidade JPA
     * Nota: clienteId e moedaId devem ser resolvidas via serviço
     */
    public EncomendaCliente toEntity(EncomendaClienteRequestDTO dto) {
        if (dto == null) return null;

        EncomendaCliente entity = new EncomendaCliente();
        entity.setData(parseLocalDate(dto.data()));
        entity.setTotalNet(dto.totalNet());
        entity.setTotalIva(dto.totalIva());
        entity.setTotalFinal(dto.totalFinal());
        entity.setCodigoTracking(dto.codigoTracking());
        // Cliente e Moeda serão setados no serviço

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

    public EncomendaClienteResponseDTO toResponseDTO(EncomendaCliente entity) {
        if (entity == null) return null;

        return new EncomendaClienteResponseDTO(
                entity.getId(),
                entity.getCliente() != null ? entity.getCliente().getId() : null,
                entity.getCliente() != null ? entity.getCliente().getNome() : null,
                entity.getData(),
                entity.getEstado(),
                entity.getTotalNet(),
                entity.getTotalIva(),
                entity.getTotalFinal(),
                entity.getMoeda() != null ? entity.getMoeda().getId() : null,
                entity.getMoeda() != null ? entity.getMoeda().getCodigo() : null,
                entity.getCodigoTracking()
        );
    }

    public EncomendaClienteSimpleDTO toSimpleDTO(EncomendaCliente entity) {
        if (entity == null) return null;

        return new EncomendaClienteSimpleDTO(
                entity.getId(),
                entity.getCliente() != null ? entity.getCliente().getNome() : null,
                entity.getData(),
                entity.getEstado(),
                entity.getTotalFinal(),
                entity.getMoeda() != null ? entity.getMoeda().getCodigo() : null,
                entity.getCodigoTracking()
        );
    }

    public void updateEntityFromDTO(EncomendaClienteRequestDTO dto, EncomendaCliente entity) {
        if (dto == null) return;

        if (dto.data() != null) {
            entity.setData(parseLocalDate(dto.data()));
        }
        if (dto.totalNet() != null) {
            entity.setTotalNet(dto.totalNet());
        }
        if (dto.totalIva() != null) {
            entity.setTotalIva(dto.totalIva());
        }
        if (dto.totalFinal() != null) {
            entity.setTotalFinal(dto.totalFinal());
        }
        if (dto.codigoTracking() != null) {
            entity.setCodigoTracking(dto.codigoTracking());
        }
    }
}
