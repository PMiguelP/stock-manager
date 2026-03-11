package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Cliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.repositories.ClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VendaService {
    private final ClienteRepository clienteRepo;
    private final EncomendaClienteRepository encomendaClienteRepo;
    private final ItemEncomendaClienteRepository itemEncomendaClienteRepo;
    private final StockService stockService;

    public VendaService(ClienteRepository clienteRepo, EncomendaClienteRepository encomendaClienteRepo, ItemEncomendaClienteRepository itemEncomendaClienteRepo, StockService stockService) {
        this.clienteRepo = clienteRepo;
        this.encomendaClienteRepo = encomendaClienteRepo;
        this.itemEncomendaClienteRepo = itemEncomendaClienteRepo;
        this.stockService = stockService;
    }
    @Transactional
    public Cliente registarCliente(Cliente cliente) {
        // TODO: Regista novo cliente no sistema. Retorna Cliente criado
        return new Cliente();
    }

    @Transactional
    public EncomendaCliente criarPedidoVenda(UUID clienteId, List<ItemEncomendaCliente> itens) {
        // TODO: Cria pedido de venda com itens. Retorna EncomendaCliente criada com estado PENDENTE
        return new EncomendaCliente();
    }

    @Transactional
    public EncomendaCliente expedirVenda(UUID encomendaId) {
        // TODO: Expede venda (marca como enviada):
        // 1. Verifica stock via stockService.buscarTipoPelletPorId()
        // 2. Chama stockService.atualizarStockPellet(..., false) para subtrair stock
        // 3. Atualiza estado para EXPEDIDA
        // Retorna EncomendaCliente atualizada
        return new EncomendaCliente();
    }

    public Cliente buscarClientePorId(UUID id) {
        // TODO: Busca cliente por ID. Usado para preencher detalhes de faturação na venda. Retorna Cliente ou lança exceção
        return new Cliente();
    }
}
