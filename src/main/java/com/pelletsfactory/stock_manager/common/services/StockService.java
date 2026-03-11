package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Fornecedor;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.repositories.FornecedorRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StockService {
    private final MateriaPrimaRepository materiaPrimaRepo;
    private final TipoPelletRepository tipoPelletRepo;
    private final FornecedorRepository forncedorRepo;

    public StockService(MateriaPrimaRepository materiaPrimaRepo, TipoPelletRepository tipoPelletRepo, FornecedorRepository forncedorRepo) {
        this.materiaPrimaRepo = materiaPrimaRepo;
        this.tipoPelletRepo = tipoPelletRepo;
        this.forncedorRepo = forncedorRepo;
    }

    @Transactional
    public MateriaPrima registarMateriaPrima(MateriaPrima materiaPrima) {
        // TODO: Regista nova matéria-prima no sistema e retorna a MateriaPrima criada
        return new MateriaPrima();
    }

    @Transactional
    public Fornecedor registarFornecedor(Fornecedor fornecedor) {
        // TODO: Regista novo fornecedor e retorna o Fornecedor criado
        return new Fornecedor();
    }


    @Transactional
    public TipoPellet configurarTipoPellet(TipoPellet tipoPellet) {
        // TODO: Define novo tipo de pellet (produto final) e retorna o TipoPellet criado
        return new TipoPellet();
    }

    @Transactional
    public int ajustarStock() {
        return 0;
    }

    public int verificarAlertasStock() {
        return 0; //TODO vai retornar uma lista com o stock abaixo do minimo
    }


    public MateriaPrima buscarMateriaPrimaPorId(UUID id) {
        // TODO: Busca matéria-prima por ID. Usado pelo ProducaoService e CompraService. Retorna MateriaPrima ou lança exceção
        return new MateriaPrima();
    }

    public TipoPellet buscarTipoPelletPorId(UUID id) {
        // TODO: Busca tipo de pellet por ID. Usado pelo VendaService e ProducaoService. Retorna TipoPellet ou lança exceção
        return new TipoPellet();
    }

    @Transactional
    public MateriaPrima subtrairStockMateriaPrima(UUID materiaPrimaId, Double quantidade) {
        // TODO: @Transactional - Reduz stock de matéria-prima e verifica se ficou abaixo do mínimo. Retorna MateriaPrima atualizada
        return new MateriaPrima();
    }

    @Transactional
    public MateriaPrima adicionarStockMateriaPrima(UUID materiaPrimaId, Double quantidade) {
        // TODO: Adiciona stock quando encomenda de fornecedor é recebida. Retorna MateriaPrima atualizada
        return new MateriaPrima();
    }

    @Transactional
    public TipoPellet atualizarStockPellet(UUID tipoPelletId, Double quantidade, boolean isAdicao) {
        // TODO: @Transactional - Gere entrada (produção) ou saída (venda) de pellets. Se isAdicao=true adiciona, senão subtrai. Retorna TipoPellet atualizado
        return new TipoPellet();
    }
}
