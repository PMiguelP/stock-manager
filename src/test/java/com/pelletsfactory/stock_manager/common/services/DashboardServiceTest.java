package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.Cliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.Moeda;
import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.repositories.ClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.FormulaProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.LotePelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.MoedaRepository;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EncomendaClienteRepository encomendaClienteRepository;
    @Autowired private FormulaProducaoRepository formulaRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private LotePelletRepository lotePelletRepository;
    @Autowired private MateriaPrimaRepository materiaPrimaRepository;
    @Autowired private MoedaRepository moedaRepository;
    @Autowired private MovimentoFinanceiroRepository movimentoRepository;
    @Autowired private OrdemProducaoRepository ordemRepository;
    @Autowired private TipoPelletRepository tipoPelletRepository;

    @Test
    void constroiGraficosComDadosPersistidosSemMisturarUnidades() {
        LocalDate hoje = LocalDate.now();
        Instant agora = Instant.now();
        String mesAtual = YearMonth.from(hoje).toString();

        Moeda moeda = moedaRepository.save(new Moeda("EUR", "€"));
        TipoPellet pellet = tipoPelletRepository.save(
                new TipoPellet("A1", 6.0, 4.8, 75.0, 10.0, BigDecimal.ONE, moeda));
        Funcionario funcionario = funcionarioRepository.save(
                new Funcionario("Operador", "123456789", "910000000", Cargo.OPERADOR_PRODUCAO, 1001, hoje, "hash"));
        FormulaProducao formula = formulaRepository.save(new FormulaProducao(pellet, "A1 v1", true, List.of()));

        OrdemProducao ordem = new OrdemProducao(pellet, funcionario, formula, 100.0, agora, EstadoOrdemProducao.CONCLUIDA);
        ordem.setQuantidadeProduzidaReal(80.0);
        ordemRepository.save(ordem);
        lotePelletRepository.save(new LotePellet(pellet, ordem, "LOT-DASHBOARD", 75.0, agora, "A-01"));

        Cliente cliente = clienteRepository.save(new Cliente("Cliente", "987654321", "910000001", "client@example.com"));
        EncomendaCliente encomenda = encomendaClienteRepository.save(
                new EncomendaCliente(cliente, hoje, EstadoEncomendaCliente.PENDENTE, 10.0, 2.3, 12.3, moeda, List.of()));
        movimentoRepository.save(new MovimentoFinanceiro(null, TipoMovimento.ENTRADA, 12.3, moeda, encomenda, null));

        materiaPrimaRepository.save(new MateriaPrima("Serradura kg", "kg", 300.0, 20.0));
        materiaPrimaRepository.save(new MateriaPrima("Serradura ton", "ton", 5.0, 1.0));

        var charts = dashboardService.obterGraficos();

        assertThat(charts.dailyProduction()).hasSize(7);
        assertThat(charts.dailyProduction().getLast().producedKg()).isEqualTo(80.0);
        assertThat(charts.monthlyProduction()).filteredOn(point -> point.month().equals(mesAtual))
                .singleElement()
                .satisfies(point -> {
                    assertThat(point.plannedKg()).isEqualTo(100.0);
                    assertThat(point.producedKg()).isEqualTo(80.0);
                });
        assertThat(charts.ordersPerMonth()).filteredOn(point -> point.month().equals(mesAtual))
                .singleElement()
                .satisfies(point -> assertThat(point.orders()).isEqualTo(1));
        assertThat(charts.rawMaterialsStockKg()).extracting(point -> point.material())
                .contains("Serradura kg")
                .doesNotContain("Serradura ton");
        assertThat(charts.pelletProductionByMonth()).filteredOn(series -> series.pelletType().equals("A1"))
                .singleElement()
                .satisfies(series -> assertThat(series.points()).filteredOn(point -> point.month().equals(mesAtual))
                        .singleElement()
                        .satisfies(point -> assertThat(point.producedKg()).isEqualTo(75.0)));
        assertThat(charts.weeklyFinancialMovements()).extracting(point -> point.entries())
                .contains(1L);
    }
}
