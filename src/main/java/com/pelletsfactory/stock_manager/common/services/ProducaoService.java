package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ProducaoService {

    private final FuncionarioRepository funcRepo;
    private final OrdemProducaoRepository ordemProdRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final FormulaProducaoRepository formulaProdRepo;

    public ProducaoService(FuncionarioRepository funcRepo, OrdemProducaoRepository ordemProdRepo, TipoPelletRepository tipoPelletRepo, FormulaProducaoRepository formulaProdRepo) {
        this.funcRepo = funcRepo;
        this.ordemProdRepo = ordemProdRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.formulaProdRepo = formulaProdRepo;
    }

    @Transactional
    public UUID abrirNovaOrdem(UUID funcionarioId, UUID formulaId, UUID tipoPelletId, Double quantidade) {
        Funcionario funcionario = funcRepo.findById(funcionarioId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado!"));

        FormulaProducao formula = formulaProdRepo.findById(formulaId)
                .orElseThrow(() -> new RuntimeException("Fórmula de produção não encontrada!"));

        TipoPellet tipo = tipoPelletRepo.findById(tipoPelletId)
                .orElseThrow(() -> new RuntimeException("Tipo de Pellet não encontrado!"));

        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade a produzir deve ser maior que zero.");
        }

        // TODO: implementar quando o construtor de OrdemProducao estiver pronto
        throw new UnsupportedOperationException("Ainda não implementado");
    }
}