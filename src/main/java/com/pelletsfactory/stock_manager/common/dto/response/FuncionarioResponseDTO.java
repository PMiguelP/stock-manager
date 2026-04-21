package com.pelletsfactory.stock_manager.common.dto.response;


import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record FuncionarioResponseDTO(
        UUID id,
        String cargo,
        String nome,
        String nif,
        Integer numeroFuncionario,
        LocalDate dataAdmissao
) {
    public FuncionarioResponseDTO {
        Objects.requireNonNull(id, "O ID do funcionário não pode ser nulo");
        Objects.requireNonNull(nome, "O nome do funcionário não pode ser nulo");
    }
}