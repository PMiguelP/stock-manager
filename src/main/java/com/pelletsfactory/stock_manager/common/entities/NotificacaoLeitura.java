package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notificacoes_leituras",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notificacao_id", "funcionario_id"})
)
public class NotificacaoLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "notificacao_id", nullable = false)
    private Notificacao notificacao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @CreationTimestamp
    @Column(name = "lida_em", updatable = false, nullable = false)
    private Instant lidaEm;

    public NotificacaoLeitura() {
    }

    public NotificacaoLeitura(Notificacao notificacao, Funcionario funcionario) {
        this.notificacao = notificacao;
        this.funcionario = funcionario;
    }

    public UUID getId() { return id; }

    public Notificacao getNotificacao() { return notificacao; }
    public void setNotificacao(Notificacao notificacao) { this.notificacao = notificacao; }

    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }

    public Instant getLidaEm() { return lidaEm; }
}
