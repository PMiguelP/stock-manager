package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.*;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        Funcionario func = funcService.buscarPorIdOuFalhar(funcionarioId);
        FormulaProducao formula = buscarFormulaPorId(formulaId);
        TipoPellet tipo = stockService.buscarTipoPelletPorId(tipoPelletId);

        OrdemProducao ordem = new OrdemProducao();
        ordem.setFuncionario(func);
        ordem.setFormula(formula);
        ordem.setTipoPellet(tipo);
        ordem.setQuantidadeMaxima(quantidadeMaxima);
        ordem.setEstado(EstadoOrdemProducao.EM_PRODUCAO);
        ordem.setDataInicio(LocalDate.now());

        return ordemProducaoRepo.save(ordem);
    }

    @Transactional
    public void registarConsumo(UUID ordemId, UUID materiaPrimaId, Double quantidade) {
        OrdemProducao ordem = ordemProducaoRepo.findById(ordemId)
                .orElseThrow(() -> new EntityNotFoundException("Ordem de produção não encontrada."));

        ConsumoProducao consumo = new ConsumoProducao();
        consumo.setOrdem(ordem);
        consumo.setMateriaPrima(stockService.buscarMateriaPrimaPorId(materiaPrimaId));
        consumo.setQuantidadeConsumida(quantidade);
        consumoProducaoRepo.save(consumo);

        stockService.subtrairStockMateriaPrima(materiaPrimaId, quantidade);
    }

    @Transactional
    public OrdemProducao finalizarOrdem(UUID ordemId, Double qtdRealProduzida, String localizacao) {
        OrdemProducao ordem = ordemProducaoRepo.findById(ordemId)
                .orElseThrow(() -> new EntityNotFoundException("Ordem não encontrada."));

        ordem.setQuantidadeProduzidaKg(qtdRealProduzida);
        ordem.setDataFim(LocalDate.now());
        ordem.setEstado(EstadoOrdemProducao.CONCLUIDA);

        LotePellet lote = new LotePellet();
        lote.setOrdem(ordem);
        lote.setTipoPellet(ordem.getTipoPellet());
        lote.setQuantidadeKg(qtdRealProduzida);
        lote.setLocalizacao(localizacao);
        lote.setDataProducao(LocalDate.now());
        lotePelletRepo.save(lote);

        stockService.atualizarStockPellet(ordem.getTipoPellet().getId(), qtdRealProduzida, true);
        return ordemProducaoRepo.save(ordem);
    }

    public FormulaProducao buscarFormulaPorId(UUID id) {
        return formulaProducaoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fórmula de produção " + id + " não encontrada."));
    }
}