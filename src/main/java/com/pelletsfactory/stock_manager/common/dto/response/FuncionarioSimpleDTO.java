package com.pelletsfactory.stock_manager.common.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Objects;

public record FuncionarioSimpleDTO(
        UUID id,
        String nome,
        String cargo,
        Integer numeroFuncionario,
        LocalDate dataAdmissao
) {
    public FuncionarioSimpleDTO {
        Objects.requireNonNull(id, "O ID é obrigatório");
        Objects.requireNonNull(nome, "O nome é obrigatório");

        if (numeroFuncionario != null && numeroFuncionario <= 0) {
            throw new IllegalArgumentException("O número de funcionário deve ser positivo");
        }
    }
}