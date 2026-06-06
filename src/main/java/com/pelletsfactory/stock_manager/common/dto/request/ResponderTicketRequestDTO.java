package com.pelletsfactory.stock_manager.common.dto.request;

public record ResponderTicketRequestDTO(String mensagem) {
    public ResponderTicketRequestDTO {
        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("A mensagem é obrigatória.");
        }
        mensagem = mensagem.trim();
        if (mensagem.length() > 2000) {
            throw new IllegalArgumentException("A mensagem não pode exceder 2000 caracteres.");
        }
    }
}
