package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.DashboardChartsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.DashboardStatsDTO;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.LotePelletRepository;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.MovimentoFinanceiroRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.utils.ProductionUnitUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final FuncionarioRepository funcionarioRepository;
    private final OrdemProducaoRepository ordemProducaoRepository;
    private final EncomendaClienteRepository encomendaClienteRepository;
    private final LotePelletRepository lotePelletRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final MovimentoFinanceiroRepository movimentoFinanceiroRepository;
    private final StockService stockService;

    public DashboardService(FuncionarioRepository funcionarioRepository,
                            OrdemProducaoRepository ordemProducaoRepository,
                            EncomendaClienteRepository encomendaClienteRepository,
                            LotePelletRepository lotePelletRepository,
                            MateriaPrimaRepository materiaPrimaRepository,
                            MovimentoFinanceiroRepository movimentoFinanceiroRepository,
                            StockService stockService) {
        this.funcionarioRepository = funcionarioRepository;
        this.ordemProducaoRepository = ordemProducaoRepository;
        this.encomendaClienteRepository = encomendaClienteRepository;
        this.lotePelletRepository = lotePelletRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.movimentoFinanceiroRepository = movimentoFinanceiroRepository;
        this.stockService = stockService;
    }

    public DashboardStatsDTO obterEstatisticas() {
        long totalFuncionarios = funcionarioRepository.count();
        long encomendasPendentes = encomendaClienteRepository.countByEstado(EstadoEncomendaCliente.PENDENTE);
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        Instant inicio = inicioMes.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant fim = inicioMes.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        double producaoMesKg = ordemProducaoRepository.sumQuantidadeProduzidaBetween(inicio, fim);

        double stockPelletsKg = stockService.calcularStockPelletsAtual();
        return new DashboardStatsDTO(
                totalFuncionarios,
                encomendasPendentes,
                producaoMesKg,
                stockPelletsKg,
                stockService.existemPelletsAbaixoMinimo()
        );
    }

    /**
     * Constrói os gráficos apenas com registos persistidos. Quando não existe
     * histórico suficiente, a série fica vazia ou com zero no período real.
     */
    @Transactional
    public DashboardChartsDTO obterGraficos() {
        LocalDate hoje = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        LocalDate inicioDias = hoje.minusDays(6);
        Instant inicioDiasInstant = inicioDias.atStartOfDay(zone).toInstant();
        Instant fimHojeInstant = hoje.plusDays(1).atStartOfDay(zone).toInstant();

        YearMonth primeiroMes = YearMonth.from(hoje).minusMonths(5);
        LocalDate inicioMeses = primeiroMes.atDay(1);
        LocalDate fimMeses = YearMonth.from(hoje).plusMonths(1).atDay(1);
        Instant inicioMesesInstant = inicioMeses.atStartOfDay(zone).toInstant();
        Instant fimMesesInstant = fimMeses.atStartOfDay(zone).toInstant();

        LocalDate inicioSemanas = hoje.minusWeeks(11).with(java.time.DayOfWeek.MONDAY);
        Instant inicioSemanasInstant = inicioSemanas.atStartOfDay(zone).toInstant();

        List<OrdemProducao> ordensUltimosMeses =
                ordemProducaoRepository.findByDataInicioGreaterThanEqualAndDataInicioLessThan(inicioMesesInstant, fimMesesInstant);

        return new DashboardChartsDTO(
                producaoDiaria(inicioDias, hoje, inicioDiasInstant, fimHojeInstant, zone),
                producaoMensal(primeiroMes, ordensUltimosMeses),
                encomendasMensais(primeiroMes, inicioMeses, fimMeses),
                stockMateriasPrimasKg(),
                producaoPelletsMensal(primeiroMes, inicioMesesInstant, fimMesesInstant),
                movimentosFinanceirosSemanais(inicioSemanas, hoje, inicioSemanasInstant, fimHojeInstant, zone)
        );
    }

    private List<DashboardChartsDTO.DailyProductionPoint> producaoDiaria(
            LocalDate inicio, LocalDate fim, Instant inicioInstant, Instant fimInstant, ZoneId zone) {
        Map<LocalDate, Double> totais = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            totais.put(dia, 0.0);
        }
        ordemProducaoRepository.findByDataInicioGreaterThanEqualAndDataInicioLessThan(inicioInstant, fimInstant).stream()
                .filter(this::ordemValidaParaDashboard)
                .forEach(ordem -> totais.computeIfPresent(
                        ordem.getDataInicio().atZone(zone).toLocalDate(),
                        (dia, total) -> total + valor(ordem.getQuantidadeProduzidaReal())));
        return totais.entrySet().stream()
                .map(entry -> new DashboardChartsDTO.DailyProductionPoint(entry.getKey().toString(), entry.getValue()))
                .toList();
    }

    private List<DashboardChartsDTO.MonthlyProductionPoint> producaoMensal(
            YearMonth primeiroMes, List<OrdemProducao> ordens) {
        Map<YearMonth, double[]> totais = mesesVazios(primeiroMes);
        ordens.stream()
                .filter(this::ordemValidaParaDashboard)
                .forEach(ordem -> {
                    YearMonth mes = YearMonth.from(ordem.getDataInicio().atZone(ZoneId.systemDefault()));
                    double[] total = totais.get(mes);
                    if (total != null) {
                        total[0] += valor(ordem.getQuantidadePlaneada());
                        total[1] += valor(ordem.getQuantidadeProduzidaReal());
                    }
                });
        return totais.entrySet().stream()
                .map(entry -> new DashboardChartsDTO.MonthlyProductionPoint(
                        entry.getKey().toString(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private List<DashboardChartsDTO.MonthlyOrdersPoint> encomendasMensais(
            YearMonth primeiroMes, LocalDate inicio, LocalDate fim) {
        Map<YearMonth, Long> totais = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            totais.put(primeiroMes.plusMonths(i), 0L);
        }
        for (EncomendaCliente encomenda : encomendaClienteRepository.findByDataGreaterThanEqualAndDataLessThan(inicio, fim)) {
            totais.computeIfPresent(YearMonth.from(encomenda.getData()), (mes, total) -> total + 1);
        }
        return totais.entrySet().stream()
                .map(entry -> new DashboardChartsDTO.MonthlyOrdersPoint(entry.getKey().toString(), entry.getValue()))
                .toList();
    }

    private List<DashboardChartsDTO.RawMaterialStockPoint> stockMateriasPrimasKg() {
        return materiaPrimaRepository.findByUnidadeIgnoreCaseOrderByStockAtualDesc(ProductionUnitUtils.KILOGRAM).stream()
                .filter(materia -> valor(materia.getStockAtual()) > 0)
                .map(materia -> new DashboardChartsDTO.RawMaterialStockPoint(materia.getNome(), valor(materia.getStockAtual())))
                .toList();
    }

    private List<DashboardChartsDTO.PelletProductionSeries> producaoPelletsMensal(
            YearMonth primeiroMes, Instant inicio, Instant fim) {
        Map<String, Map<YearMonth, Double>> totaisPorTipo = new LinkedHashMap<>();
        for (LotePellet lote : lotePelletRepository.findByDataProducaoGreaterThanEqualAndDataProducaoLessThan(inicio, fim)) {
            if (!ordemValidaParaDashboard(lote.getOrdem())) {
                continue;
            }
            String tipo = lote.getTipoPellet().getNome();
            Map<YearMonth, Double> totais = totaisPorTipo.computeIfAbsent(tipo, ignored -> mesesComZero(primeiroMes));
            YearMonth mes = YearMonth.from(lote.getDataProducao().atZone(ZoneId.systemDefault()));
            totais.computeIfPresent(mes, (key, total) -> total + valor(lote.getQuantidadeKg()));
        }
        return totaisPorTipo.entrySet().stream()
                .map(entry -> new DashboardChartsDTO.PelletProductionSeries(
                        entry.getKey(),
                        entry.getValue().entrySet().stream()
                                .map(point -> new DashboardChartsDTO.MonthlyPelletProductionPoint(
                                        point.getKey().toString(), point.getValue()))
                                .toList()))
                .toList();
    }

    private List<DashboardChartsDTO.WeeklyFinancialPoint> movimentosFinanceirosSemanais(
            LocalDate inicio, LocalDate hoje, Instant inicioInstant, Instant fimInstant, ZoneId zone) {
        Map<LocalDate, long[]> totais = new LinkedHashMap<>();
        for (LocalDate semana = inicio; !semana.isAfter(hoje); semana = semana.plusWeeks(1)) {
            totais.put(semana, new long[2]);
        }
        for (MovimentoFinanceiro movimento :
                movimentoFinanceiroRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(inicioInstant, fimInstant)) {
            LocalDate semana = movimento.getCreatedAt().atZone(zone).toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            long[] total = totais.get(semana);
            if (total == null) {
                continue;
            }
            if (movimento.getTipoMovimento() == TipoMovimento.ENTRADA) {
                total[0]++;
            } else {
                total[1]++;
            }
        }
        List<DashboardChartsDTO.WeeklyFinancialPoint> points = new ArrayList<>();
        int week = 1;
        for (long[] total : totais.values()) {
            points.add(new DashboardChartsDTO.WeeklyFinancialPoint(week++, total[0], total[1]));
        }
        return points;
    }

    private Map<YearMonth, double[]> mesesVazios(YearMonth primeiroMes) {
        Map<YearMonth, double[]> meses = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            meses.put(primeiroMes.plusMonths(i), new double[2]);
        }
        return meses;
    }

    private Map<YearMonth, Double> mesesComZero(YearMonth primeiroMes) {
        Map<YearMonth, Double> meses = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            meses.put(primeiroMes.plusMonths(i), 0.0);
        }
        return meses;
    }

    private boolean ordemValidaParaDashboard(OrdemProducao ordem) {
        return ordem != null && ordem.getDataInicio() != null && ordem.getEstado() != EstadoOrdemProducao.ANULADA;
    }

    private double valor(Double valor) {
        return valor != null && Double.isFinite(valor) ? valor : 0.0;
    }
}
