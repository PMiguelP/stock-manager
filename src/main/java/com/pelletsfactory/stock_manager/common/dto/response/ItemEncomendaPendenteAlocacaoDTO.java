package com.pelletsfactory.stock_manager.common.dto.response;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;

import java.util.List;
import java.util.UUID;

public record ItemEncomendaPendenteAlocacaoDTO(
        UUID itemEncomendaId,
        UUID encomendaId,
        String clienteNome,
        UUID tipoPelletId,
        String tipoPelletNome,
        EstadoEncomendaCliente estado,
        Double quantidadePedida,
        Double quantidadeAlocada,
        Double quantidadeEmFalta,
        List<AlocacaoLoteEncomendaResponseDTO> alocacoes
) {
}
