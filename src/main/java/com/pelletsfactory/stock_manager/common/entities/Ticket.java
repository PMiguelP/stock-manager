package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private EncomendaCliente encomenda;

    @Column(nullable = false, length = 150)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTicket estado = EstadoTicket.AGUARDA_EQUIPE;

    @ManyToOne
    @JoinColumn(name = "responsavel_id")
    private Funcionario responsavel;

    @Column(name = "ultima_mensagem_em", nullable = false)
    private Instant ultimaMensagemEm;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TicketMensagem> mensagens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Ticket() {
    }

    public Ticket(EncomendaCliente encomenda, String assunto, Instant ultimaMensagemEm) {
        this.encomenda = encomenda;
        this.assunto = assunto;
        this.ultimaMensagemEm = ultimaMensagemEm;
    }

    public UUID getId() { return id; }
    public EncomendaCliente getEncomenda() { return encomenda; }
    public void setEncomenda(EncomendaCliente encomenda) { this.encomenda = encomenda; }
    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }
    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }
    public Funcionario getResponsavel() { return responsavel; }
    public void setResponsavel(Funcionario responsavel) { this.responsavel = responsavel; }
    public Instant getUltimaMensagemEm() { return ultimaMensagemEm; }
    public void setUltimaMensagemEm(Instant ultimaMensagemEm) { this.ultimaMensagemEm = ultimaMensagemEm; }
    public List<TicketMensagem> getMensagens() { return mensagens; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
