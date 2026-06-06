package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.AbrirTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.NotificacaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.ResponderTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketMensagemDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketOrderItemDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.entities.Ticket;
import com.pelletsfactory.stock_manager.common.entities.TicketMensagem;
import com.pelletsfactory.stock_manager.common.enums.AutorMensagemTicket;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.TicketMensagemRepository;
import com.pelletsfactory.stock_manager.common.repositories.TicketRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketMensagemRepository mensagemRepo;
    private final EncomendaClienteRepository encomendaRepo;
    private final ItemEncomendaClienteRepository itemEncomendaRepo;
    private final NotificacaoService notificacaoService;

    public TicketService(TicketRepository ticketRepo,
                         TicketMensagemRepository mensagemRepo,
                         EncomendaClienteRepository encomendaRepo,
                         ItemEncomendaClienteRepository itemEncomendaRepo,
                         NotificacaoService notificacaoService) {
        this.ticketRepo = ticketRepo;
        this.mensagemRepo = mensagemRepo;
        this.encomendaRepo = encomendaRepo;
        this.itemEncomendaRepo = itemEncomendaRepo;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public TicketDetailsDTO abrirTicketComoCliente(String codigoTracking, AbrirTicketRequestDTO dto) {
        EncomendaCliente encomenda = buscarEncomendaPorTracking(codigoTracking);
        Instant agora = Instant.now();
        Ticket ticket = ticketRepo.save(new Ticket(encomenda, dto.assunto(), agora));
        mensagemRepo.save(new TicketMensagem(ticket, AutorMensagemTicket.CLIENTE, null, dto.mensagem()));
        notificarEquipa(ticket, TipoEventoNotificacao.NOVO_TICKET,
                "Novo pedido de suporte",
                "Novo pedido sobre a encomenda " + ticket.getEncomenda().getCodigoTracking() + ".");
        return toDetails(ticket);
    }

    @Transactional
    public TicketDetailsDTO responderComoCliente(String codigoTracking, UUID ticketId, ResponderTicketRequestDTO dto) {
        Ticket ticket = buscarTicketDaEncomenda(codigoTracking, ticketId);
        guardarMensagem(ticket, AutorMensagemTicket.CLIENTE, null, dto.mensagem());
        ticket.setEstado(EstadoTicket.AGUARDA_EQUIPE);
        ticketRepo.save(ticket);
        notificarEquipa(ticket, TipoEventoNotificacao.NOVA_MENSAGEM_TICKET,
                "Nova mensagem de suporte",
                "O cliente respondeu ao ticket " + referencia(ticket) + ".");
        return toDetails(ticket);
    }

    @Transactional
    public TicketDetailsDTO responderComoFuncionario(UUID ticketId, ResponderTicketRequestDTO dto) {
        Funcionario funcionario = funcionarioComercialAutenticado();
        Ticket ticket = buscarPorIdOuFalhar(ticketId);
        guardarMensagem(ticket, AutorMensagemTicket.FUNCIONARIO, funcionario, dto.mensagem());
        if (ticket.getResponsavel() == null) {
            ticket.setResponsavel(funcionario);
        }
        ticket.setEstado(EstadoTicket.AGUARDA_CLIENTE);
        return toDetails(ticketRepo.save(ticket));
    }

    @Transactional
    public TicketDetailsDTO atribuirAoFuncionarioAtual(UUID ticketId) {
        Funcionario funcionario = funcionarioComercialAutenticado();
        Ticket ticket = buscarPorIdOuFalhar(ticketId);
        ticket.setResponsavel(funcionario);
        return toDetails(ticketRepo.save(ticket));
    }

    @Transactional
    public TicketDetailsDTO resolverTicket(UUID ticketId) {
        funcionarioComercialAutenticado();
        Ticket ticket = buscarPorIdOuFalhar(ticketId);
        ticket.setEstado(EstadoTicket.RESOLVIDO);
        return toDetails(ticketRepo.save(ticket));
    }

    public List<TicketSimpleDTO> listarTicketsDaEncomenda(String codigoTracking) {
        EncomendaCliente encomenda = buscarEncomendaPorTracking(codigoTracking);
        return ticketRepo.findByEncomendaIdOrderByUltimaMensagemEmDesc(encomenda.getId()).stream()
                .map(this::toSimple)
                .toList();
    }

    public List<TicketSimpleDTO> listarInboxComercial() {
        funcionarioComercialAutenticado();
        return ticketRepo.findAllByOrderByUltimaMensagemEmDesc().stream()
                .map(this::toSimple)
                .toList();
    }

    public TicketDetailsDTO obterConversaCliente(String codigoTracking, UUID ticketId) {
        return toDetails(buscarTicketDaEncomenda(codigoTracking, ticketId));
    }

    public TicketDetailsDTO obterConversaFuncionario(UUID ticketId) {
        funcionarioComercialAutenticado();
        return toDetails(buscarPorIdOuFalhar(ticketId));
    }

    private void guardarMensagem(Ticket ticket, AutorMensagemTicket autor, Funcionario funcionario, String mensagem) {
        mensagemRepo.save(new TicketMensagem(ticket, autor, funcionario, mensagem));
        ticket.setUltimaMensagemEm(Instant.now());
    }

    private void notificarEquipa(Ticket ticket, TipoEventoNotificacao tipo, String titulo, String mensagem) {
        List.of(Cargo.ASSISTENTE_COMERCIAL, Cargo.ADMINISTRADOR).forEach(cargo ->
                notificacaoService.criarNotificacaoAutomatica(new NotificacaoRequestDTO(
                        titulo, mensagem, tipo, cargo, false, ticket.getId()
                ))
        );
    }

    private EncomendaCliente buscarEncomendaPorTracking(String codigoTracking) {
        if (codigoTracking == null || codigoTracking.isBlank()) {
            throw new IllegalArgumentException("O código de tracking é obrigatório.");
        }
        return encomendaRepo.findByCodigoTrackingIgnoreCase(codigoTracking.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada."));
    }

    private Ticket buscarTicketDaEncomenda(String codigoTracking, UUID ticketId) {
        EncomendaCliente encomenda = buscarEncomendaPorTracking(codigoTracking);
        Ticket ticket = buscarPorIdOuFalhar(ticketId);
        if (!ticket.getEncomenda().getId().equals(encomenda.getId())) {
            throw new EntityNotFoundException("Ticket não encontrado para esta encomenda.");
        }
        return ticket;
    }

    private Ticket buscarPorIdOuFalhar(UUID ticketId) {
        return ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket não encontrado."));
    }

    private Funcionario funcionarioComercialAutenticado() {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL, Cargo.ADMINISTRADOR);
        return SessaoFuncionario.getFuncionarioLogado();
    }

    private TicketSimpleDTO toSimple(Ticket ticket) {
        EncomendaCliente encomenda = ticket.getEncomenda();
        return new TicketSimpleDTO(
                ticket.getId(),
                encomenda.getId(),
                encomenda.getCodigoTracking(),
                encomenda.getCliente().getNome(),
                ticket.getAssunto(),
                ticket.getEstado(),
                ticket.getResponsavel() != null ? ticket.getResponsavel().getId() : null,
                ticket.getResponsavel() != null ? ticket.getResponsavel().getNome() : null,
                ticket.getUltimaMensagemEm()
        );
    }

    private TicketDetailsDTO toDetails(Ticket ticket) {
        EncomendaCliente encomenda = ticket.getEncomenda();
        List<TicketOrderItemDTO> itens = itemEncomendaRepo.findByEncomendaId(encomenda.getId()).stream()
                .map(item -> new TicketOrderItemDTO(
                        item.getTipoPellet() != null ? item.getTipoPellet().getNome() : "Pellet",
                        item.getQuantidadeKg()
                ))
                .toList();
        double quantidadeTotalKg = itens.stream()
                .map(TicketOrderItemDTO::quantidadeKg)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        List<TicketMensagemDTO> mensagens = mensagemRepo.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(this::toMensagem)
                .toList();
        return new TicketDetailsDTO(
                ticket.getId(),
                encomenda.getId(),
                encomenda.getCodigoTracking(),
                encomenda.getCliente().getNome(),
                ticket.getAssunto(),
                ticket.getEstado(),
                ticket.getResponsavel() != null ? ticket.getResponsavel().getNome() : null,
                encomenda.getData(),
                encomenda.getEstado(),
                quantidadeTotalKg,
                encomenda.getTotalFinal(),
                encomenda.getMoeda() != null ? encomenda.getMoeda().getSimbolo() : "",
                ticket.getUltimaMensagemEm(),
                itens,
                mensagens
        );
    }

    private TicketMensagemDTO toMensagem(TicketMensagem mensagem) {
        return new TicketMensagemDTO(
                mensagem.getId(),
                mensagem.getAutorTipo(),
                mensagem.getFuncionario() != null ? mensagem.getFuncionario().getNome() : "Cliente",
                mensagem.getMensagem(),
                mensagem.getCreatedAt()
        );
    }

    private String referencia(Ticket ticket) {
        return "#" + ticket.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
