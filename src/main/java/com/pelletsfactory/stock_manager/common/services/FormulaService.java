package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.ComposicaoPellet;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.repositories.ComposicaoPelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FormulaService {
    private final FormulaProducaoRepository formulaProducaoRepo;
    private final ComposicaoPelletRepository composicaoPelletRepo;
    private final StockService stockService;

    public FormulaService(FormulaProducaoRepository formulaProducaoRepo, ComposicaoPelletRepository composicaoPelletRepo, StockService stockService) {
        this.formulaProducaoRepo = formulaProducaoRepo;
        this.composicaoPelletRepo = composicaoPelletRepo;
        this.stockService = stockService;
    }

    @Transactional
    public FormulaProducao criarFormula(UUID tipoPelletId, String nome, List<ComposicaoPellet> composicao) {
        // TODO: Cria nova fórmula de produção com composição de matérias-primas. Retorna FormulaProducao criada
        return new FormulaProducao();
    }

    @Transactional
    public FormulaProducao alterarEstadoFormula(UUID formulaId, boolean ativa) {
        // TODO: Ativa/desativa fórmula. Apenas uma fórmula pode estar ativa por tipo de pellet. Retorna FormulaProducao atualizada
        return new FormulaProducao();
    }

    public Double calcularCustoProducao(UUID formulaId) {
        // TODO: Calcula custo teórico de produção baseado na composição e preços atuais de matérias-primas. Retorna Double com custo por kg
        return 0.1;
    }
}
