package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "varchar(50)")
    private TipoEventoNotificacao tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_alvo", length = 50)
    private Cargo cargoAlvo; // null = para todos

    @Column(nullable = false)
    private Boolean lida = false;

    @Column(name = "requer_acao")
    private Boolean requerAcao;

    @Column
    private Boolean concluida = false;

    @ManyToOne
    @JoinColumn(name = "concluida_por_id")
    private Funcionario concluidaPor;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    @Column(name = "link_referencia")
    private UUID linkReferencia;

    @OneToMany(mappedBy = "notificacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NotificacaoLeitura> leituras = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Notificacao() {
    }

    public Notificacao(String titulo, String mensagem, TipoEventoNotificacao tipoEvento) {
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.tipoEvento = tipoEvento;
        this.lida = false;
    }

    // Getters e Setters
    public UUID getId() { return id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public TipoEventoNotificacao getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoNotificacao tipoEvento) { this.tipoEvento = tipoEvento; }

    public Cargo getCargoAlvo() { return cargoAlvo; }
    public void setCargoAlvo(Cargo cargoAlvo) { this.cargoAlvo = cargoAlvo; }

    public Boolean getLida() { return lida; }
    public void setLida(Boolean lida) { this.lida = lida; }

    public Boolean getRequerAcao() {
        if (requerAcao != null) {
            return requerAcao;
        }
        return switch (tipoEvento) {
            case STOCK_BAIXO, NOVA_ORDEM_PRODUCAO, ERRO_PRODUCAO -> true;
            default -> false;
        };
    }
    public void setRequerAcao(Boolean requerAcao) { this.requerAcao = requerAcao; }

    public Boolean getConcluida() { return concluida; }
    public void setConcluida(Boolean concluida) { this.concluida = concluida; }

    public Funcionario getConcluidaPor() { return concluidaPor; }
    public void setConcluidaPor(Funcionario concluidaPor) { this.concluidaPor = concluidaPor; }

    public Instant getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(Instant concluidaEm) { this.concluidaEm = concluidaEm; }

    public UUID getLinkReferencia() { return linkReferencia; }
    public void setLinkReferencia(UUID linkReferencia) { this.linkReferencia = linkReferencia; }

    public Set<NotificacaoLeitura> getLeituras() { return leituras; }
    public void setLeituras(Set<NotificacaoLeitura> leituras) { this.leituras = leituras; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
