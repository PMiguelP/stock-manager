package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.mapper.MovimentoFinanceiroMapper;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
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

        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipoMovimento(TipoMovimento.ENTRADA);
        movimento.setValorTotal(valor);
        movimento.setMoeda(encomendaCliente.getMoeda());
        movimento.setEncomendaCliente(encomendaCliente);
        return movimentoMapper.toResponseDTO(movimentoFinanceiroRepo.save(movimento));
    }

    @Transactional
    public MovimentoFinanceiroResponseDTO registarSaida(
            EncomendaFornecedor encomendaFornecedor,
            Double valor) {

        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setTipoMovimento(TipoMovimento.SAIDA);
        movimento.setValorTotal(valor);
        movimento.setMoeda(encomendaFornecedor.getMoeda());
        movimento.setEncomendaFornecedor(encomendaFornecedor);
        return movimentoMapper.toResponseDTO(movimentoFinanceiroRepo.save(movimento));
    }

    @Transactional
    public MovimentoFinanceiroResponseDTO atualizarMovimentoFinanceiro(UUID movimentoId, Double novoValor) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);
        MovimentoFinanceiro movimento = movimentoFinanceiroRepo.findById(movimentoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Movimento financeiro não encontrado"));
        movimento.setValorTotal(novoValor);
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

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
        return movimentoFinanceiroRepo.findAll(pageable).map(movimentoMapper::toResponseDTO);
    }

    public org.springframework.data.domain.Page<MovimentoFinanceiroSimpleDTO> listarMovimentosFinanceirosSimples(
            int page,
            int pageSize,
            TipoMovimento tipoMovimento,
            UUID moedaId,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));

        return movimentoFinanceiroRepo.findByFiltros(tipoMovimento, moedaId, pageable)
                .map(movimentoMapper::toSimpleDTO);
    }

    public java.util.List<MovimentoFinanceiroResponseDTO> listarMovimentosFinanceirosSimples() {
        return movimentoFinanceiroRepo.findAll().stream()
                .map(movimentoMapper::toResponseDTO)
                .toList();
    }


    public Double calcularSaldoAtual() {
        // TODO: Calcula saldo atual (soma de entradas - saídas). Retorna Double com o saldo
        List<MovimentoFinanceiro> movimentos = movimentoFinanceiroRepo.findAll();
        double entradas = movimentos.stream()
                .filter(m -> m.getTipoMovimento() == TipoMovimento.ENTRADA)
                .mapToDouble(MovimentoFinanceiro::getValorTotal)
                .sum();
        double saidas = movimentos.stream()
                .filter(m -> m.getTipoMovimento() == TipoMovimento.SAIDA)
                .mapToDouble(MovimentoFinanceiro::getValorTotal)
                .sum();
        return entradas - saidas;
    }
}
