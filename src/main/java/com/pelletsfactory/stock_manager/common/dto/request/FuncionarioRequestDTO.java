package com.pelletsfactory.stock_manager.common.dto.request;

public record FuncionarioRequestDTO(
        String cargo,
        String nome,
        String nif,
        String contacto,
        Integer numeroFuncionario,
        String dataAdmissao
) {
    public FuncionarioRequestDTO {
        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("Cargo é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (nif == null || !nif.matches("\\d{9}")) {
            throw new IllegalArgumentException("NIF deve ter exatamente 9 dígitos numéricos");
        }
        if (contacto == null || contacto.isBlank()) {
            throw new IllegalArgumentException("Contacto é obrigatório");
        }
        if (numeroFuncionario != null && numeroFuncionario <= 0) {
            throw new IllegalArgumentException("Número de funcionário deve ser positivo");
        }
        if (dataAdmissao != null && dataAdmissao.isBlank()) {
            throw new IllegalArgumentException("Data de admissão é obrigatória");
        }
    }
}