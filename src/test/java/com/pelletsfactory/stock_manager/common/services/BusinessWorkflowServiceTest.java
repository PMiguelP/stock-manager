package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.AlocacaoLoteEncomendaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.LotePelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.ComposicaoPelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.FormulaProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.NotificacaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.OrdemProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.entities.Cliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.Fornecedor;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.Moeda;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.ComposicaoPellet;
import com.pelletsfactory.stock_manager.common.entities.ConsumoProducao;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.mapper.NotificacaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.ClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.ComposicaoPelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.ConsumoProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.FornecedorRepository;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.LotePelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.MoedaRepository;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusinessWorkflowServiceTest {

    @Autowired private CompraService compraService;
    @Autowired private VendaService vendaService;
    @Autowired private LotePelletService lotePelletService;
    @Autowired private OrdemProducaoService ordemProducaoService;
    @Autowired private FormulaProducaoService formulaService;
    @Autowired private AlocacaoLoteEncomendaService alocacaoService;
    @Autowired private ComposicaoPelletService composicaoService;
    @Autowired private MoedaRepository moedaRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private MateriaPrimaRepository materiaPrimaRepository;
    @Autowired private TipoPelletRepository tipoPelletRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EncomendaClienteRepository encomendaClienteRepository;
    @Autowired private ItemEncomendaClienteRepository itemEncomendaClienteRepository;
    @Autowired private FormulaProducaoRepository formulaRepository;
    @Autowired private OrdemProducaoRepository ordemRepository;
    @Autowired private ComposicaoPelletRepository composicaoRepository;
    @Autowired private ConsumoProducaoRepository consumoRepository;
    @Autowired private LotePelletRepository lotePelletRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private MovimentoFinanceiroRepository movimentoFinanceiroRepository;
    @Autowired private NotificacaoMapper notificacaoMapper;

    @AfterEach
    void clearSession() {
        SessaoFuncionario.logout();
    }

    @Test
    void purchaseOrderRoundsVatAndTotalsToCents() {
        login(Cargo.ASSISTENTE_COMERCIAL);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        Fornecedor fornecedor = fornecedorRepository.save(new Fornecedor("supplier@example.com", "910000000", "123456789", "Fornecedor"));
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 0.0, 0.0));

        var draft = compraService.gerarEncomendaRascunho(fornecedor.getId(), moeda.getId());
        compraService.adicionarItemEncomenda(draft.id(), materiaPrima.getId(), 3.0, 1.005, 23.0);

        var order = compraService.getEncomendaFornecedorById(draft.id());
        assertThat(order.getTotalLiquido()).isEqualTo(3.02);
        assertThat(order.getTotalIva()).isEqualTo(0.69);
        assertThat(order.getTotalFinal()).isEqualTo(3.71);
    }

    @Test
    void editingBatchKeepsFinishedPelletStockInSync() {
        login(Cargo.OPERADOR_PRODUCAO);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        OrdemProducao ordem = ordemRepository.save(order(pellet, 100.0, EstadoOrdemProducao.EM_PRODUCAO));

        var created = lotePelletService.criarLote(new LotePelletRequestDTO(ordem.getId(), pellet.getId(), "LOT-001", 60.0, null));
        lotePelletService.atualizarLote(created.id(), new LotePelletRequestDTO(ordem.getId(), pellet.getId(), "LOT-001", 80.0, null));

        assertThat(tipoPelletRepository.findById(pellet.getId()).orElseThrow().getStockAtual()).isEqualTo(80.0);
    }

    @Test
    void allocationCannotExceedOrderAndShipmentConsumesStockOnce() {
        Funcionario operator = login(Cargo.RESPONSAVEL_LOGISTICA);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        OrdemProducao ordem = ordemRepository.save(order(pellet, 100.0, EstadoOrdemProducao.CONCLUIDA));
        Cliente cliente = clienteRepository.save(new Cliente("Cliente", "987654321", "910000001", "client@example.com"));
        EncomendaCliente encomenda = encomendaClienteRepository.save(customerOrder(cliente, moeda));
        itemEncomendaClienteRepository.save(customerItem(encomenda, pellet, 80.0));

        SessaoFuncionario.login(login(Cargo.OPERADOR_PRODUCAO));
        var lote = lotePelletService.criarLote(new LotePelletRequestDTO(ordem.getId(), pellet.getId(), "LOT-SHIP", 100.0, null));

        SessaoFuncionario.login(operator);
        var allocation = alocacaoService.criarAlocacao(new AlocacaoLoteEncomendaRequestDTO(lote.id(), itemEncomendaClienteRepository.findByEncomendaId(encomenda.getId()).getFirst().getId(), 80.0));
        assertThat(alocacaoService.listarItensPendentes(null)).isEmpty();
        assertThat(alocacaoService.listarLotesDisponiveis(null).getFirst().quantidadeDisponivel()).isEqualTo(20.0);
        assertThatThrownBy(() -> alocacaoService.atualizarAlocacao(
                allocation.id(), 101.0))
                .isInstanceOf(IllegalArgumentException.class);
        SessaoFuncionario.login(login(Cargo.OPERADOR_PRODUCAO));
        assertThatThrownBy(() -> lotePelletService.apagarLote(lote.id()))
                .isInstanceOf(IllegalStateException.class);

        SessaoFuncionario.login(operator);
        vendaService.expedir(encomenda.getId(), "TRK-001");

        assertThat(tipoPelletRepository.findById(pellet.getId()).orElseThrow().getStockAtual()).isEqualTo(20.0);
        assertThat(movimentoFinanceiroRepository.existsByEncomendaClienteId(encomenda.getId())).isTrue();
        assertThatThrownBy(() -> vendaService.expedir(encomenda.getId(), "TRK-002"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> alocacaoService.apagarAlocacao(allocation.id()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void formulaUsedByProductionOrderBecomesImmutable() {
        Funcionario manager = login(Cargo.RESPONSAVEL_PRODUCAO);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 100.0, 0.0));
        OrdemProducao ordem = ordemRepository.save(order(pellet, 100.0, EstadoOrdemProducao.PENDENTE));

        SessaoFuncionario.login(manager);
        assertThatThrownBy(() -> composicaoService.adicionarMateriaPrimaAFormula(
                new ComposicaoPelletRequestDTO(ordem.getFormula().getId(), materiaPrima.getId(), 1.0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já usada");
    }

    @Test
    void productionFormulaRejectsRawMaterialOutsideKilograms() {
        login(Cargo.RESPONSAVEL_PRODUCAO);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        FormulaProducao formula = formulaRepository.save(new FormulaProducao(pellet, "Formula m3", true, null));
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Madeira", "m3", 10.0, 0.0));

        assertThatThrownBy(() -> composicaoService.adicionarMateriaPrimaAFormula(
                new ComposicaoPelletRequestDTO(formula.getId(), materiaPrima.getId(), 1.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kg");
    }

    @Test
    void formulaServiceRejectsCompositionThatDoesNotCloseOneKilogram() {
        login(Cargo.RESPONSAVEL_PRODUCAO);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 10.0, 0.0));

        assertThatThrownBy(() -> formulaService.criarFormula(
                new FormulaProducaoRequestDTO(pellet.getId(), "Fórmula incompleta", true),
                Map.of(materiaPrima.getId(), 0.9)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente 1 kg");
    }

    @Test
    void productionOrderWithBatchCannotBeCancelled() {
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        OrdemProducao ordem = ordemRepository.save(order(pellet, 100.0, EstadoOrdemProducao.EM_PRODUCAO));

        SessaoFuncionario.login(login(Cargo.OPERADOR_PRODUCAO));
        lotePelletService.criarLote(new LotePelletRequestDTO(ordem.getId(), pellet.getId(), "LOT-CANCEL", 100.0, null));

        SessaoFuncionario.login(login(Cargo.RESPONSAVEL_PRODUCAO));
        assertThatThrownBy(() -> ordemProducaoService.mudarEstado(ordem.getId(), "ANULADA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lotes");
    }

    @Test
    void productionOrderCannotReserveRawMaterialAlreadyCommittedToAnotherOrder() {
        Funcionario manager = login(Cargo.RESPONSAVEL_PRODUCAO);
        Funcionario worker = login(Cargo.OPERADOR_PRODUCAO);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 10.0, 0.0));
        FormulaProducao formula = formulaRepository.save(new FormulaProducao(pellet, "Fórmula A1", true, null));
        composicaoRepository.save(new ComposicaoPellet(formula, materiaPrima, 1.0));
        SessaoFuncionario.login(manager);

        ordemProducaoService.criarOrdem(productionOrderRequest(pellet, worker, formula, 7.0));

        assertThatThrownBy(() -> ordemProducaoService.criarOrdem(productionOrderRequest(pellet, worker, formula, 4.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void productionCannotFinishWithoutConsumptionForEveryFormulaIngredient() {
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(new TipoPellet("A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, moeda));
        MateriaPrima serradura = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 10.0, 0.0));
        MateriaPrima amido = materiaPrimaRepository.save(new MateriaPrima("Amido", "kg", 10.0, 0.0));
        OrdemProducao ordem = order(pellet, 10.0, EstadoOrdemProducao.EM_PRODUCAO);
        FormulaProducao formula = ordem.getFormula();
        composicaoRepository.save(new ComposicaoPellet(formula, serradura, 0.5));
        composicaoRepository.save(new ComposicaoPellet(formula, amido, 0.5));
        ordemRepository.save(ordem);
        consumoRepository.save(new ConsumoProducao(ordem, serradura, 5.0));
        lotePelletRepository.save(new LotePellet(pellet, ordem, "LOT-INCOMPLETE", 10.0, Instant.now(), null));

        SessaoFuncionario.login(login(Cargo.RESPONSAVEL_PRODUCAO));
        assertThatThrownBy(() -> ordemProducaoService.mudarEstado(ordem.getId(), "CONCLUIDA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("todas as matérias-primas");
    }

    @Test
    void purchaseOrderRejectsFreeItemsBeforeTheyReachFinance() {
        login(Cargo.ASSISTENTE_COMERCIAL);
        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        Fornecedor fornecedor = fornecedorRepository.save(new Fornecedor("supplier@example.com", "910000000", "123456789", "Fornecedor"));
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 0.0, 0.0));
        var draft = compraService.gerarEncomendaRascunho(fornecedor.getId(), moeda.getId());

        assertThatThrownBy(() -> compraService.adicionarItemEncomenda(draft.id(), materiaPrima.getId(), 1.0, 0.0, 23.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preço unitário");
    }

    @Test
    void actionableNotificationUsesEventDefaultWhenNotExplicitlyConfigured() {
        var notificacao = notificacaoMapper.toEntity(new NotificacaoRequestDTO(
                "Stock crítico",
                "Stock abaixo do mínimo",
                TipoEventoNotificacao.STOCK_BAIXO,
                null,
                null,
                null
        ));

        assertThat(notificacao.getRequerAcao()).isTrue();
    }

    private Funcionario login(Cargo cargo) {
        int number = 1000 + (int) funcionarioRepository.count();
        Funcionario funcionario = funcionarioRepository.save(new Funcionario(
                "User " + number, String.valueOf(100000000 + number), "910000000",
                cargo, number, LocalDate.now(), "hash"
        ));
        SessaoFuncionario.login(funcionario);
        return funcionario;
    }

    private OrdemProducao order(TipoPellet pellet, double produced, EstadoOrdemProducao state) {
        FormulaProducao formula = formulaRepository.save(new FormulaProducao(pellet, "Formula " + produced + state, true, null));
        Funcionario worker = login(Cargo.OPERADOR_PRODUCAO);
        OrdemProducao ordem = new OrdemProducao(pellet, worker, formula, produced, null, state);
        ordem.setQuantidadeProduzidaReal(produced);
        return ordem;
    }

    private EncomendaCliente customerOrder(Cliente cliente, Moeda moeda) {
        EncomendaCliente encomenda = new EncomendaCliente();
        encomenda.setCliente(cliente);
        encomenda.setMoeda(moeda);
        encomenda.setData(LocalDate.now());
        encomenda.setEstado(EstadoEncomendaCliente.PRONTA);
        encomenda.setTotalNet(80.0);
        encomenda.setTotalIva(18.4);
        encomenda.setTotalFinal(98.4);
        return encomenda;
    }

    private OrdemProducaoRequestDTO productionOrderRequest(
            TipoPellet pellet,
            Funcionario worker,
            FormulaProducao formula,
            double quantity) {
        return new OrdemProducaoRequestDTO(
                pellet.getId(),
                worker.getId(),
                formula.getId(),
                quantity,
                0.0,
                LocalDate.now().toString(),
                EstadoOrdemProducao.PENDENTE.name()
        );
    }

    private ItemEncomendaCliente customerItem(EncomendaCliente order, TipoPellet pellet, double quantity) {
        return new ItemEncomendaCliente(order, pellet, quantity, 1.0, 23.0, quantity * 0.23);
    }
}
