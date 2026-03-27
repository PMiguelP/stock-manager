package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaFornecedorRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaFornecedorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.pelletsfactory.stock_manager.common.utils.FuncoesAuxiliares.validarPermissaoTratarFornecedores;

@Service
public class CompraService {
    private final EncomendaFornecedorRepository encomendaFornecedorRepo;
    private final ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo;
    private final StockService stockService;
    private final EncomendaClienteRepository encomendaClienteRepository;

    public CompraService(EncomendaFornecedorRepository encomendaFornecedorRepo, ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo, StockService stockService, EncomendaClienteRepository encomendaClienteRepository) {
        this.encomendaFornecedorRepo = encomendaFornecedorRepo;
        this.itemEncomendaFornecedorRepo = itemEncomendaFornecedorRepo;
        this.stockService = stockService;
        this.encomendaClienteRepository = encomendaClienteRepository;
    }

    @Transactional
    public EncomendaFornecedor gerarEncomenda(UUID fornecedorId, List<ItemEncomendaFornecedor> itens) {
        // TODO: Cria pedido de matéria-prima ao fornecedor com lista de itens. Retorna EncomendaFornecedor criada
        //estado pendente
        validarPermissaoTratarFornecedores(); //verifica se o user logado tem permissoes para tratar fornecedores
        //aqui depois vai gerar um pdf tambem
        return new EncomendaFornecedor();
    }

    @Transactional
    public EncomendaFornecedor atualizarEncomenda(EncomendaFornecedor encomendaFornecedor) {
        EncomendaFornecedor encomendaAtual = encomendaFornecedorRepo.findById(encomendaFornecedor.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Encomenda não encontrada com ID: " + encomendaFornecedor.getId()));

        if (!EstadoEncomendaFornecedor.RASCUNHO.equals(encomendaAtual.getEstado())) {
            throw new IllegalStateException(
                    "Apenas encomendas em estado de rascunho podem ser atualizadas");
        }

        encomendaAtual.setFornecedor(encomendaFornecedor.getFornecedor());

        return encomendaFornecedorRepo.save(encomendaAtual);
        //VERIFICAR se isto esta bem acho quenao mas nao tenho a certeza
    }

    @Transactional
    public EncomendaFornecedor mudarEstadoParaEfetiva(EncomendaFornecedor encomendaFornecedor) {
        //TODO: recebe uma encomenda fornecedor verificaa se o estado nao e nulo se nao for nulo marca como efetiva
        EncomendaFornecedor encomendaAtual = encomendaFornecedorRepo.findById(encomendaFornecedor.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Encomenda não encontrada com ID: " + encomendaFornecedor.getId()));
        if (encomendaAtual.getEstado() == EstadoEncomendaFornecedor.ANULADA) {
            throw new IllegalStateException(
                    "Nao pode mudar para efetiva uma encomenda que esta anulada");
        }

        encomendaAtual.setEstado(EstadoEncomendaFornecedor.EFETIVA);
        return encomendaFornecedorRepo.save(encomendaAtual);
    }

    @Transactional
    public EncomendaFornecedor confirmarRecebimento(UUID encomendaId) {
        // TODO: Marca encomenda como RECEBIDA e
        // 1. Muda estado para RECEBIDA
        // 2. Para cada item, chama stockService.adicionarStockMateriaPrima()
        // 3. e aqui que a funcao do financeiroService registarSaida vai ser chamada
        // Retorna EncomendaFornecedor atualizada
        return new EncomendaFornecedor();
    }

    public List<EncomendaFornecedor> listarEncomendasFornecedores(UUID fornecedorId) {
        // TODO: Lista encomendas de um forneedor com paginacao e tudo mais
        return new ArrayList<>();
    }

    public EncomendaFornecedor getEncomendaFornecedorById(UUID id) {
        return encomendaFornecedorRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrado com o ID: " + id));
    }
}
