package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProducaoService {
    private final OrdemProducaoRepository ordemProducaoRepo;
    private final ConsumoProducaoRepository consumoProducaoRepo;
    private final LotePelletRepository lotePelletRepo;
    private final FormulaProducaoRepository formulaProducaoRepo;
    private final FuncionarioService funcService;
    private final StockService stockService;

    public ProducaoService(OrdemProducaoRepository ordemProducaoRepo, ConsumoProducaoRepository consumoProducaoRepo, LotePelletRepository lotePelletRepo, FormulaProducaoRepository formulaProducaoRepo, FuncionarioService funcService, StockService stockService) {
        this.ordemProducaoRepo = ordemProducaoRepo;
        this.consumoProducaoRepo = consumoProducaoRepo;
        this.lotePelletRepo = lotePelletRepo;
        this.formulaProducaoRepo = formulaProducaoRepo;
        this.funcService = funcService;
        this.stockService = stockService;
    }

    @Transactional
    public OrdemProducao abrirNovaOrdem(UUID funcionarioId, UUID formulaId, UUID tipoPelletId,
                                        Double quantidadeMaxima) {
        // TODO: Cria nova ordem de produção após validar:
        // 1. Funcionário existe (via funcionarioService.buscarPorId())
        // 2. Tipo de pellet existe (via stockService.buscarTipoPelletPorId())
        // Retorna OrdemProducao criada
        return new OrdemProducao();
    }

    @Transactional
    public void registarConsumo(UUID ordemId, UUID materiaPrimaId, Double quantidade) {
            // TODO: Regista consumo de matérias-primas para uma ordem e:
            // 1. Grava na tabela Consumo_Producao
            // 2. Chama stockService.subtrairStockMateriaPrima() para cada item
            // Retorna void
    }

    @Transactional
    public OrdemProducao finalizarOrdem(UUID ordemId, String localizacao) {
        // TODO: Finaliza ordem de produção:
        // 1. Calcula produção
        // 2. Cria LotePellet
        // 3. Chama stockService.atualizarStockPellet(..., true) para adicionar ao stock
        // Retorna OrdemProducao finalizada
        return new OrdemProducao();
    }


    public FormulaProducao buscarFormulaPorId(UUID id) {
        // TODO: Valida se fórmula existe antes de iniciar produção. Retorna FormulaProducao ou lança exceção
        return new FormulaProducao();
    }


}