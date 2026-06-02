package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import com.pelletsfactory.stock_manager.common.utils.DecimalUtils;

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

    @Column(name = "preco_unit_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal precoUnitarioNet;

    @Column(name = "taxa_iva", nullable = false)
    private Double taxaIva;

    @Column(name = "valor_iva_calculado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorIvaCalculado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public ItemEncomendaCliente() {
    }

    public ItemEncomendaCliente(EncomendaCliente encomenda, TipoPellet tipoPellet, Double quantidadeKg,
                                Double precoUnitarioNet, Double taxaIva, Double valorIvaCalculado) {
        this.encomenda = encomenda;
        this.tipoPellet = tipoPellet;
        this.quantidadeKg = quantidadeKg;
        setPrecoUnitarioNet(precoUnitarioNet);
        this.taxaIva = taxaIva;
        setValorIvaCalculado(valorIvaCalculado);
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

    public Double getPrecoUnitarioNet() {
        return DecimalUtils.toDouble(precoUnitarioNet);
    }

    public void setPrecoUnitarioNet(Double precoUnitarioNet) {
        this.precoUnitarioNet = DecimalUtils.fromDouble(precoUnitarioNet, 4);
    }

    public Double getTaxaIva() {
        return taxaIva;
    }

    public void setTaxaIva(Double taxaIva) {
        this.taxaIva = taxaIva;
    }

    public Double getValorIvaCalculado() {
        return DecimalUtils.toDouble(valorIvaCalculado);
    }

    public void setValorIvaCalculado(Double valorIvaCalculado) {
        this.valorIvaCalculado = DecimalUtils.fromDouble(valorIvaCalculado, 2);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
