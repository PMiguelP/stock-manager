package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.HistoricoCustoProducao;
import com.pelletsfactory.stock_manager.common.repositories.HistoricoCustoProducaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HistoricoCustoService {
    private final HistoricoCustoProducaoRepository historicoCustoProducaoRepo;
    private final StockService stockService;

    public HistoricoCustoService(HistoricoCustoProducaoRepository historicoCustoProducaoRepo, StockService stockService) {
        this.historicoCustoProducaoRepo = historicoCustoProducaoRepo;
        this.stockService = stockService;
    }

    @Transactional
    public HistoricoCustoProducao registarNovoCusto(UUID tipoPelletId, Double novoCusto) {
        // TODO: Regista novo custo de produção para um tipo de pellet:
        // 1. Fecha registo anterior (define data_fim)
        // 2. Cria novo registo com data_inicio = now()
        // 3. Atualiza TipoPellet.custoAtualPorKg
        // Retorna HistoricoCustoProducao criado
        return new HistoricoCustoProducao();
    }

    public List<HistoricoCustoProducao> obterHistorico(UUID tipoPelletId) {
        // TODO: Retorna histórico de custos de um tipo de pellet ordenado por data. Útil para gráficos de evolução de custos
        return new ArrayList<>();
    }
}
