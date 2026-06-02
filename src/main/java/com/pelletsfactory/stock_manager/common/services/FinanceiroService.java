package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.mapper.MovimentoFinanceiroMapper;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import com.pelletsfactory.stock_manager.common.utils.CalculationUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;
import jakarta.persistence.EntityNotFoundException;

@Service
public class FinanceiroService {
    private final MovimentoFinanceiroRepository movimentoFinanceiroRepo;
    private final MovimentoFinanceiroMapper movimentoMapper;

    public FinanceiroService(MovimentoFinanceiroRepository movimentoFinanceiroRepo,
                             MovimentoFinanceiroMapper movimentoMapper) {
        this.movimentoFinanceiroRepo = movimentoFinanceiroRepo;
        this.movimentoMapper = movimentoMapper;
    }

    @Transactional
    public MovimentoFinanceiroResponseDTO registarEntrada(
            EncomendaCliente encomendaCliente,
            Double valor) {
        if (encomendaCliente == null || encomendaCliente.getId() == null) {
            throw new IllegalArgumentException("Encomenda de cliente é obrigatória");
        }
        CalculationUtils.requirePositive(valor, "Valor do movimento");
        if (movimentoFinanceiroRepo.existsByEncomendaClienteId(encomendaCliente.getId())) {
            throw new IllegalStateException("A receita desta encomenda já foi registada");
        }

        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipoMovimento(TipoMovimento.ENTRADA);
        movimento.setValorTotal(CalculationUtils.money(valor));
        movimento.setMoeda(encomendaCliente.getMoeda());
        movimento.setEncomendaCliente(encomendaCliente);
        return movimentoMapper.toResponseDTO(movimentoFinanceiroRepo.save(movimento));
    }

    @Transactional
    public MovimentoFinanceiroResponseDTO registarSaida(
            EncomendaFornecedor encomendaFornecedor,
            Double valor) {
        if (encomendaFornecedor == null || encomendaFornecedor.getId() == null) {
            throw new IllegalArgumentException("Encomenda de fornecedor é obrigatória");
        }
        CalculationUtils.requirePositive(valor, "Valor do movimento");
        if (movimentoFinanceiroRepo.existsByEncomendaFornecedorId(encomendaFornecedor.getId())) {
            throw new IllegalStateException("A despesa desta encomenda já foi registada");
        }

        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipoMovimento(TipoMovimento.SAIDA);
        movimento.setValorTotal(CalculationUtils.money(valor));
        movimento.setMoeda(encomendaFornecedor.getMoeda());
        movimento.setEncomendaFornecedor(encomendaFornecedor);
        return movimentoMapper.toResponseDTO(movimentoFinanceiroRepo.save(movimento));
    }

    public MovimentoFinanceiroResponseDTO obterMovimentoFinanceiro(UUID movimentoId) {
        MovimentoFinanceiro movimento = movimentoFinanceiroRepo.findById(movimentoId)
                .orElseThrow(() -> new EntityNotFoundException("Movimento financeiro não encontrado"));
        return movimentoMapper.toResponseDTO(movimento);
    }

    public org.springframework.data.domain.Page<MovimentoFinanceiroResponseDTO> listarMovimentosFinanceiros(
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        org.springframework.data.domain.Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "createdAt");
        return movimentoFinanceiroRepo.findAll(pageable).map(movimentoMapper::toResponseDTO);
    }

    public org.springframework.data.domain.Page<MovimentoFinanceiroSimpleDTO> listarMovimentosFinanceirosSimples(
            int page,
            int pageSize,
            TipoMovimento tipoMovimento,
            UUID moedaId,
            String sortBy,
            String direction) {

        org.springframework.data.domain.Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "createdAt");

        return movimentoFinanceiroRepo.findByFiltros(tipoMovimento, moedaId, pageable)
                .map(movimentoMapper::toSimpleDTO);
    }

    /**
     * Calcula saldo apenas quando todos os movimentos usam a mesma moeda.
     * Misturar moedas produziria um valor contabilístico incorreto.
     */
    public Double calcularSaldoAtual() {
        if (movimentoFinanceiroRepo.countDistinctCurrencies() > 1) {
            throw new IllegalStateException("Não é possível calcular um saldo único com moedas diferentes");
        }
        double entradas = movimentoFinanceiroRepo.sumByTipoMovimento(TipoMovimento.ENTRADA);
        double saidas = movimentoFinanceiroRepo.sumByTipoMovimento(TipoMovimento.SAIDA);
        return CalculationUtils.money(entradas - saidas);
    }
}
