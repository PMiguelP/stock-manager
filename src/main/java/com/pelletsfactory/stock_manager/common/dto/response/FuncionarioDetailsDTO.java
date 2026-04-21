package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.Cargo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Objects;

public record FuncionarioDetailsDTO(
        UUID id,
        String nome,
        String nif,
        String contacto,
        Cargo cargo,
        Integer numeroFuncionario,
        LocalDate dataAdmissao,
        Instant createdAt,
        Instant updatedAt
) {
    public FuncionarioDetailsDTO {
        Objects.requireNonNull(id, "ID é obrigatório");
        Objects.requireNonNull(nome, "Nome é obrigatório");
    }
}