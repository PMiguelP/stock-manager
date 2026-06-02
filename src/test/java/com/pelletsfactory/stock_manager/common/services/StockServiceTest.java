package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.dto.request.MateriaPrimaRequestDTO;
import com.pelletsfactory.stock_manager.common.repositories.MateriaPrimaRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import com.pelletsfactory.stock_manager.common.utils.DecimalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private MateriaPrimaRepository materiaPrimaRepository;

    @Autowired
    private TipoPelletRepository tipoPelletRepository;

    @BeforeEach
    void setUp() {
        materiaPrimaRepository.deleteAll();
        tipoPelletRepository.deleteAll();
    }

    @Test
    void somaStocksDePelletsEMateriasPrimas() {
        tipoPelletRepository.save(new TipoPellet("ENplus A1", 6.0, 4.8, 1200.0, 500.0, BigDecimal.ONE, null));
        tipoPelletRepository.save(new TipoPellet("Industrial", 8.0, 4.2, 800.0, 250.0, BigDecimal.ONE, null));
        materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 300.0, 100.0));
        materiaPrimaRepository.save(new MateriaPrima("Madeira reciclada", "kg", 700.0, 150.0));

        assertThat(stockService.calcularStockPelletsAtual()).isEqualTo(2000.0);
        assertThat(stockService.calcularStockPelletsMinimo()).isEqualTo(750.0);
        assertThat(stockService.calcularStockMateriasPrimasKg()).isEqualTo(1000.0);
    }

    @Test
    void naoPermiteStockNegativoAoSubtrairMateriaPrima() {
        MateriaPrima materiaPrima = materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 20.0, 10.0));

        assertThatThrownBy(() -> stockService.subtrairStockMateriaPrima(materiaPrima.getId(), 25.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void contaAlertasQuandoStockFicaAbaixoDoMinimo() {
        materiaPrimaRepository.save(new MateriaPrima("Serradura", "kg", 20.0, 50.0));
        tipoPelletRepository.save(new TipoPellet("ENplus A1", 6.0, 4.8, 100.0, 200.0, BigDecimal.ONE, null));

        assertThat(stockService.verificarAlertasStock()).isEqualTo(2);
    }

    @Test
    void naoPermiteRegistarStockInicialInvalido() {
        assertThatThrownBy(() -> stockService.registarMateriaPrima(
                new MateriaPrima("Serradura", "kg", Double.NaN, 10.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock atual");
    }

    @Test
    void alertaDePelletNaoEEscondidoPeloExcessoDeOutroTipo() {
        tipoPelletRepository.save(new TipoPellet("Em falta", 6.0, 4.8, 10.0, 50.0, BigDecimal.ONE, null));
        tipoPelletRepository.save(new TipoPellet("Com excesso", 8.0, 4.2, 1000.0, 10.0, BigDecimal.ONE, null));

        assertThat(stockService.existemPelletsAbaixoMinimo()).isTrue();
    }

    @Test
    void dtoRejeitaValoresNumericosNaoFinitos() {
        assertThatThrownBy(() -> new MateriaPrimaRequestDTO("Serradura", "kg", Double.NaN, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paginacaoRejeitaPaginaZero() {
        assertThatThrownBy(() -> PageableUtils.create(0, 10, "nome", "ASC", "nome"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Página");
    }

    @Test
    void conversaoMonetariaRejeitaValorNaoFinito() {
        assertThatThrownBy(() -> DecimalUtils.fromDouble(Double.POSITIVE_INFINITY, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monetário");
    }
}
