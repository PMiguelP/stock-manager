package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaFornecedorRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaFornecedorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CompraService {
    private final EncomendaFornecedorRepository encomendaFornecedorRepo;
    private final ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo;
    private final StockService stockService;

    public CompraService(EncomendaFornecedorRepository encomendaFornecedorRepo, ItemEncomendaFornecedorRepository itemEncomendaFornecedorRepo, StockService stockService) {
        this.encomendaFornecedorRepo = encomendaFornecedorRepo;
        this.itemEncomendaFornecedorRepo = itemEncomendaFornecedorRepo;
        this.stockService = stockService;
    }

    @Transactional
    public EncomendaFornecedor gerarEncomenda(UUID fornecedorId, List<ItemEncomendaFornecedor> itens) {
        // TODO: Cria pedido de matéria-prima ao fornecedor com lista de itens. Retorna EncomendaFornecedor criada
        return new EncomendaFornecedor();
    }

    @Transactional
    public EncomendaFornecedor confirmarRecebimento(UUID encomendaId) {
        // TODO: Marca encomenda como RECEBIDA e:
        // 1. Muda estado para RECEBIDA
        // 2. Para cada item, chama stockService.adicionarStockMateriaPrima()
        // Retorna EncomendaFornecedor atualizada
        return new EncomendaFornecedor();
    }

    public List<EncomendaFornecedor> listarEncomendasAbertasPorFornecedor(UUID fornecedorId) {
        // TODO: Lista encomendas pendentes de um fornecedor.
        // Retorna uma lista vazia temporariamente para não dar erro de compilação
        return new ArrayList<>();
    }

    public EncomendaFornecedor getEncomendaFornecedorById(UUID id) {
        return encomendaFornecedorRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrado com o ID: " + id));
    }
}
