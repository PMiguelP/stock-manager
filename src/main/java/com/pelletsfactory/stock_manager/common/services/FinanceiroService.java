package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FinanceiroService {
    private final MovimentoFinanceiroRepository movimentoFinanceiroRepo;
    private final CompraService compraService;
    private final VendaService vendaService;

    public FinanceiroService(MovimentoFinanceiroRepository movimentoFinanceiroRepo, CompraService compraService, VendaService vendaService) {
        this.movimentoFinanceiroRepo = movimentoFinanceiroRepo;
        this.compraService = compraService;
        this.vendaService = vendaService;
    }

    @Transactional
    public MovimentoFinanceiro registarEntrada(UUID encomendaClienteId, Double valor) {
        // TODO: Regista movimento financeiro de entrada (venda) vinculado a EncomendaCliente. Retorna MovimentoFinanceiro criado
        return new MovimentoFinanceiro();
    }

    @Transactional
    public MovimentoFinanceiro registarSaida(UUID encomendaFornecedorId, Double valor) {
        // TODO: Regista movimento financeiro de saída (compra) vinculado a EncomendaFornecedor. Retorna MovimentoFinanceiro criado
        return new MovimentoFinanceiro();
    }

    public Double calcularSaldoAtual() {
        // TODO: Calcula saldo atual (soma de entradas - saídas). Retorna Double com o saldo
        return 0.1;
    }
}

