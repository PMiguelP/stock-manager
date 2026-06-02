package com.pelletsfactory.stock_manager.common.dto.request;

public record ClienteRequestDTO(
        String nome,
        String nif,
        String contacto,
        String email
) {
    public ClienteRequestDTO {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório");
        }
        if (nome.length() > 200) {
            throw new IllegalArgumentException("Nome não pode exceder 200 caracteres");
        }

        if (nif == null || !nif.matches("\\d{9}")) {
            throw new IllegalArgumentException("NIF deve ter exatamente 9 dígitos");
        }

        if (contacto != null && contacto.length() > 20) {
            throw new IllegalArgumentException("Contacto não pode exceder 20 caracteres");
        }

        if (email != null && email.length() > 100) {
            throw new IllegalArgumentException("Email não pode exceder 100 caracteres");
        }
        if (email != null && !email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email inválido");
        }
    }
}
