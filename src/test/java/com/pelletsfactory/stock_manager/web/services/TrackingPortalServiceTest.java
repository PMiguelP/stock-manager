package com.pelletsfactory.stock_manager.web.services;

import com.pelletsfactory.stock_manager.common.entities.Cliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.Moeda;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.repositories.ClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.MoedaRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrackingPortalServiceTest {

    @Autowired
    private TrackingPortalService trackingPortalService;

    @Autowired
    private ClienteRepository clientRepository;

    @Autowired
    private MoedaRepository currencyRepository;

    @Autowired
    private EncomendaClienteRepository orderRepository;

    @Autowired
    private TipoPelletRepository pelletTypeRepository;

    @Autowired
    private ItemEncomendaClienteRepository itemRepository;

    @Test
    void normalizesCodeAndBuildsShippedOrderView() {
        Moeda currency = currencyRepository.save(new Moeda("EUR", "€"));
        Cliente client = clientRepository.save(
                new Cliente("ABC Industries", "123456789", "912345678", "client@example.com")
        );
        TipoPellet pelletType = pelletTypeRepository.save(
                new TipoPellet("ENplus A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, currency)
        );
        EncomendaCliente order = orderRepository.save(order(client, currency));

        itemRepository.save(item(order, pelletType, 750.0));
        itemRepository.save(item(order, pelletType, 1250.0));

        var result = trackingPortalService.findByTrackingCode("  pel-2026-000123  ");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().clientName()).isEqualTo("ABC Industries");
        assertThat(result.orElseThrow().quantityKg()).isEqualTo(2000.0);
        assertThat(result.orElseThrow().status()).isEqualTo("Shipped");
        assertThat(result.orElseThrow().stages())
                .extracting(stage -> stage.state())
                .containsExactly("complete", "complete", "complete", "current");
    }

    @Test
    void returnsEmptyForBlankCode() {
        assertThat(trackingPortalService.findByTrackingCode("  ")).isEmpty();
    }

    private EncomendaCliente order(Cliente client, Moeda currency) {
        EncomendaCliente order = new EncomendaCliente();
        order.setCliente(client);
        order.setMoeda(currency);
        order.setData(LocalDate.of(2026, 4, 15));
        order.setEstado(EstadoEncomendaCliente.EXPEDIDA);
        order.setTotalNet(0.0);
        order.setTotalIva(0.0);
        order.setTotalFinal(0.0);
        order.setCodigoTracking("PEL-2026-000123");
        return order;
    }

    private ItemEncomendaCliente item(EncomendaCliente order, TipoPellet pelletType, double quantityKg) {
        ItemEncomendaCliente item = new ItemEncomendaCliente();
        item.setEncomenda(order);
        item.setTipoPellet(pelletType);
        item.setQuantidadeKg(quantityKg);
        item.setPrecoUnitarioNet(1.0);
        item.setTaxaIva(23.0);
        item.setValorIvaCalculado(quantityKg * 0.23);
        return item;
    }
}
