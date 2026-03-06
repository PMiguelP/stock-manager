package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "itens_encomenda_cliente")
public class ItemEncomendaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "encomenda_id", nullable = false)
    private EncomendaCliente encomenda;

    @ManyToOne
    @JoinColumn(name = "tipo_pellet_id", nullable = false)
    private TipoPellet tipoPellet;

    @Column(name = "quantidade_kg", nullable = false)
    private Double quantidadeKg;

    @Column(name = "preco_unitario_aplicado", nullable = false)
    private Double precoUnitarioAplicado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ItemEncomendaCliente() {
    }

    public ItemEncomendaCliente(EncomendaCliente encomenda, TipoPellet tipoPellet, Double quantidadeKg, Double precoUnitarioAplicado) {
        this.encomenda = encomenda;
        this.tipoPellet = tipoPellet;
        this.quantidadeKg = quantidadeKg;
        this.precoUnitarioAplicado = precoUnitarioAplicado;
    }

    public UUID getId() {
        return id;
    }

    public EncomendaCliente getEncomenda() {
        return encomenda;
    }

    public void setEncomenda(EncomendaCliente encomenda) {
        this.encomenda = encomenda;
    }

    public TipoPellet getTipoPellet() {
        return tipoPellet;
    }

    public void setTipoPellet(TipoPellet tipoPellet) {
        this.tipoPellet = tipoPellet;
    }

    public Double getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(Double quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public Double getPrecoUnitarioAplicado() {
        return precoUnitarioAplicado;
    }

    public void setPrecoUnitarioAplicado(Double precoUnitarioAplicado) {
        this.precoUnitarioAplicado = precoUnitarioAplicado;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}