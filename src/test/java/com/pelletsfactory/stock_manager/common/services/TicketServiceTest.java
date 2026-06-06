package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.AbrirTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.ResponderTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.entities.Cliente;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.Moeda;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.repositories.ClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.MoedaRepository;
import com.pelletsfactory.stock_manager.common.repositories.NotificacaoRepository;
import com.pelletsfactory.stock_manager.common.repositories.TipoPelletRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    @Autowired private TicketService ticketService;
    @Autowired private NotificacaoService notificacaoService;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private EncomendaClienteRepository encomendaRepo;
    @Autowired private FuncionarioRepository funcionarioRepo;
    @Autowired private MoedaRepository moedaRepo;
    @Autowired private NotificacaoRepository notificacaoRepo;
    @Autowired private TipoPelletRepository tipoPelletRepo;
    @Autowired private ItemEncomendaClienteRepository itemEncomendaRepo;

    @AfterEach
    void logout() {
        SessaoFuncionario.logout();
    }

    @Test
    void supportsCompleteCustomerAndCommercialConversationFlow() {
        EncomendaCliente encomenda = order("PEL-2026-SUPPORT");
        TipoPellet pellet = tipoPelletRepo.save(new TipoPellet(
                "ENplus A1", 6.0, 4.8, 0.0, 0.0, BigDecimal.ONE, encomenda.getMoeda()
        ));
        itemEncomendaRepo.save(new ItemEncomendaCliente(encomenda, pellet, 2500.0, 0.20, 23.0, 115.0));

        var opened = ticketService.abrirTicketComoCliente(
                encomenda.getCodigoTracking(),
                new AbrirTicketRequestDTO("Entrega", "Quando será expedida?")
        );

        assertThat(opened.estado()).isEqualTo(EstadoTicket.AGUARDA_EQUIPE);
        assertThat(opened.quantidadeTotalKg()).isEqualTo(2500.0);
        assertThat(opened.itens()).singleElement()
                .satisfies(item -> {
                    assertThat(item.tipoPelletNome()).isEqualTo("ENplus A1");
                    assertThat(item.quantidadeKg()).isEqualTo(2500.0);
                });
        assertThat(opened.mensagens()).hasSize(1);
        assertThat(notificacaoRepo.findByLinkReferencia(opened.id()))
                .extracting(notification -> notification.getTipoEvento())
                .containsExactly(TipoEventoNotificacao.NOVO_TICKET, TipoEventoNotificacao.NOVO_TICKET);

        Funcionario admin = employee(Cargo.ADMINISTRADOR);
        SessaoFuncionario.login(admin);
        assertThat(ticketService.listarInboxComercial()).hasSize(1);
        assertThat(notificacaoRepo.findByLinkReferencia(opened.id()))
                .extracting(notification -> notification.getCargoAlvo())
                .containsExactlyInAnyOrder(Cargo.ASSISTENTE_COMERCIAL, Cargo.ADMINISTRADOR);
        assertThat(notificacaoRepo.count()).isEqualTo(2);
        assertThat(ticketService.obterConversaFuncionario(opened.id()).id()).isEqualTo(opened.id());
        assertThat(notificacaoRepo.findByLinkReferencia(opened.id())).hasSize(2);
        assertThat(notificacaoService.contarNotLidas()).isEqualTo(1);
        assertThat(notificacaoService.listarParaUtilizadorAtualSimples(
                1, 100, null, null, null, "createdAt", "DESC"
        ).getContent()).anySatisfy(notification -> {
            assertThat(notification.tipoEvento()).isEqualTo(TipoEventoNotificacao.NOVO_TICKET);
            assertThat(notification.linkReferencia()).isEqualTo(opened.id());
            assertThat(notification.mensagem()).contains(encomenda.getCodigoTracking());
        });
        notificacaoService.marcarTodasComoLidas();
        assertThat(notificacaoService.contarNotLidas()).isZero();

        Funcionario commercial = employee(Cargo.ASSISTENTE_COMERCIAL);
        SessaoFuncionario.login(commercial);
        var answered = ticketService.responderComoFuncionario(
                opened.id(),
                new ResponderTicketRequestDTO("A expedição está prevista para amanhã.")
        );

        assertThat(answered.estado()).isEqualTo(EstadoTicket.AGUARDA_CLIENTE);
        assertThat(answered.responsavelNome()).isEqualTo(commercial.getNome());
        assertThat(answered.mensagens()).hasSize(2);

        SessaoFuncionario.logout();
        var reopened = ticketService.responderComoCliente(
                encomenda.getCodigoTracking(),
                opened.id(),
                new ResponderTicketRequestDTO("Obrigado. Podem confirmar quando sair?")
        );

        assertThat(reopened.estado()).isEqualTo(EstadoTicket.AGUARDA_EQUIPE);
        assertThat(reopened.mensagens()).hasSize(3);
        assertThat(notificacaoRepo.findByLinkReferencia(opened.id()))
                .extracting(notification -> notification.getTipoEvento())
                .containsExactlyInAnyOrder(
                        TipoEventoNotificacao.NOVO_TICKET,
                        TipoEventoNotificacao.NOVO_TICKET,
                        TipoEventoNotificacao.NOVA_MENSAGEM_TICKET,
                        TipoEventoNotificacao.NOVA_MENSAGEM_TICKET
                );

        SessaoFuncionario.login(admin);
        assertThat(ticketService.listarInboxComercial()).hasSize(1);

        SessaoFuncionario.login(commercial);
        assertThat(ticketService.resolverTicket(opened.id()).estado()).isEqualTo(EstadoTicket.RESOLVIDO);
    }

    private EncomendaCliente order(String tracking) {
        Moeda moeda = moedaRepo.save(new Moeda("EUR", "€"));
        Cliente cliente = clienteRepo.save(new Cliente("Cliente Suporte", "123456789", "910000000", "cliente@example.com"));
        EncomendaCliente encomenda = new EncomendaCliente();
        encomenda.setCliente(cliente);
        encomenda.setMoeda(moeda);
        encomenda.setData(LocalDate.of(2026, 6, 2));
        encomenda.setEstado(EstadoEncomendaCliente.CONFIRMADA);
        encomenda.setTotalNet(100.0);
        encomenda.setTotalIva(23.0);
        encomenda.setTotalFinal(123.0);
        encomenda.setCodigoTracking(tracking);
        return encomendaRepo.save(encomenda);
    }

    private Funcionario employee(Cargo cargo) {
        boolean admin = cargo == Cargo.ADMINISTRADOR;
        return funcionarioRepo.save(new Funcionario(
                admin ? "Administrador" : "Assistente Comercial",
                admin ? "987654322" : "987654321",
                admin ? "910000002" : "910000001",
                cargo,
                admin ? 7002 : 7001,
                LocalDate.of(2026, 1, 1),
                "unused"
        ));
    }
}
