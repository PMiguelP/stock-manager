package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.pelletsfactory.stock_manager.common.utils.FuncoesAuxiliares.validarPermissaoAdmin;

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
        //TODO: vai ser chamado na funcao marcarEncomendaClienteComoConcluida do VendaService
        EncomendaCliente encomendaCliente = vendaService.getEncomendaClienteById(encomendaClienteId);
        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipoMovimento(TipoMovimento.E);
        movimento.setValor(valor);
        movimento.setEncomendaCliente(encomendaCliente);
        return movimentoFinanceiroRepo.save(movimento);
    }

    @Transactional
    public MovimentoFinanceiro registarSaida(UUID encomendaFornecedorId, Double valor) {
        // TODO: Regista movimento financeiro de saída (compra) vinculado a EncomendaFornecedor. Retorna MovimentoFinanceiro criado
        //TODO: vai ser chamado na funcao marcarComoRecebida do CompraService
        EncomendaFornecedor encomendaFornecedor = compraService.getEncomendaFornecedorById(encomendaFornecedorId);
        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipoMovimento(TipoMovimento.S);
        movimento.setValor(valor);
        movimento.setEncomendaFornecedor(encomendaFornecedor);
        return movimentoFinanceiroRepo.save(movimento);
    }

    @Transactional
    public MovimentoFinanceiro atualizarMovimentoFinanceiro(UUID movimentoId, Double novoValor) {
        //TODO: recebe um movimento financeiro e permite a um administrador e apenas ele atualizar algo como valor/data
        validarPermissaoAdmin();
        return new MovimentoFinanceiro();
    }

    public List<MovimentoFinanceiro> listarMovimentosFinanceiros() {
        //TODO: lista todos os movimentos financeiros como temos a retornar todos os funcionarios no funcService!
        return new ArrayList<>();
    }


    public Double calcularSaldoAtual() {
        // TODO: Calcula saldo atual (soma de entradas - saídas). Retorna Double com o saldo
        List<MovimentoFinanceiro> movimentos = movimentoFinanceiroRepo.findAll();
        double entradas = movimentos.stream()
                .filter(m -> m.getTipoMovimento() == TipoMovimento.E)
                .mapToDouble(MovimentoFinanceiro::getValor)
                .sum();
        double saidas = movimentos.stream()
                .filter(m -> m.getTipoMovimento() == TipoMovimento.S)
                .mapToDouble(MovimentoFinanceiro::getValor)
                .sum();
        return entradas - saidas;
    }
}

