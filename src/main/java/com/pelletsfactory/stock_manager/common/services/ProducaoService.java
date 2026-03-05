package com.pelletsfactory.stock_manager.common.services;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProducaoService {

    @Autowired
    private FuncionarioRepository funcRepo;
    @Autowired
    private OrdemProducaoRepository ordemProdRepo;
    @Autowired
    private TipoPelletRepository tipoPelletRepo;
    @Autowired
    private FormulaProducaoRepository formulaProdRepo;

    //TODO make the logic responsible for the producao service
}
