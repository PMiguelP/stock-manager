package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Alocação de Ordens de Produção a Encomendas de Cliente
 * Realiza a ponte entre produção e vendas
 * 
 * Uma ordem pode produzir para múltiplas encomendas
 * Uma encomenda pode ser atendida por múltiplas ordens
 */
@Entity
@Table(name = "alocacao_ordem_encomenda")
public class AlocacaoOrdemEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ordem_id", nullable = false)
    private OrdemProducao ordem;

    @ManyToOne
    @JoinColumn(name = "encomenda_cliente_id", nullable = false)
    private EncomendaCliente encomendaCliente;

    @Column(name = "quantidade_reservada", nullable = false)
    private Double quantidadeReservada;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public AlocacaoOrdemEncomenda() {
    }

    public AlocacaoOrdemEncomenda(OrdemProducao ordem, EncomendaCliente encomendaCliente, Double quantidadeReservada) {
        this.ordem = ordem;
        this.encomendaCliente = encomendaCliente;
        this.quantidadeReservada = quantidadeReservada;
    }

    public UUID getId() { return id; }

    public OrdemProducao getOrdem() { return ordem; }
    public void setOrdem(OrdemProducao ordem) { this.ordem = ordem; }

    public EncomendaCliente getEncomendaCliente() { return encomendaCliente; }
    public void setEncomendaCliente(EncomendaCliente encomendaCliente) { this.encomendaCliente = encomendaCliente; }

    public Double getQuantidadeReservada() { return quantidadeReservada; }
    public void setQuantidadeReservada(Double quantidadeReservada) { this.quantidadeReservada = quantidadeReservada; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
