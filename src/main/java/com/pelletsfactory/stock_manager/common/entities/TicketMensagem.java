package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.AutorMensagemTicket;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets_mensagens")
public class TicketMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "autor_tipo", nullable = false, length = 20)
    private AutorMensagemTicket autorTipo;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public TicketMensagem() {
    }

    public TicketMensagem(Ticket ticket, AutorMensagemTicket autorTipo, Funcionario funcionario, String mensagem) {
        this.ticket = ticket;
        this.autorTipo = autorTipo;
        this.funcionario = funcionario;
        this.mensagem = mensagem;
    }

    public UUID getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public AutorMensagemTicket getAutorTipo() { return autorTipo; }
    public Funcionario getFuncionario() { return funcionario; }
    public String getMensagem() { return mensagem; }
    public Instant getCreatedAt() { return createdAt; }
}
