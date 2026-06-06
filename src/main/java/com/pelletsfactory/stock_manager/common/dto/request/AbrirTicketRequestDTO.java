package com.pelletsfactory.stock_manager.common.dto.request;

public record AbrirTicketRequestDTO(String assunto, String mensagem) {
    public AbrirTicketRequestDTO {
        assunto = validarTexto(assunto, "O assunto", 150);
        mensagem = validarTexto(mensagem, "A mensagem", 2000);
    }

    private static String validarTexto(String valor, String nome, int limite) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nome + " é obrigatório.");
        }
        String normalizado = valor.trim();
        if (normalizado.length() > limite) {
            throw new IllegalArgumentException(nome + " não pode exceder " + limite + " caracteres.");
        }
        return normalizado;
    }
}
