package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "itens_encomenda_fornecedor")
public class ItemEncomendaFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "encomenda_id", nullable = false)
    private EncomendaFornecedor encomenda;

    @ManyToOne
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(nullable = false)
    private Double quantidade;

    @Column(name = "preco_unitario_net", nullable = false)
    private Double precoUnitarioNet;

    @Column(name = "taxa_iva", nullable = false)
    private Double taxaIva;

    @Column(name = "valor_iva_calculado", nullable = false)
    private Double valorIvaCalculado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public ItemEncomendaFornecedor() {
    }

    public ItemEncomendaFornecedor(EncomendaFornecedor encomenda, MateriaPrima materiaPrima, Double quantidade,
                                   Double precoUnitarioNet, Double taxaIva, Double valorIvaCalculado) {
        this.encomenda = encomenda;
        this.materiaPrima = materiaPrima;
        this.quantidade = quantidade;
        this.precoUnitarioNet = precoUnitarioNet;
        this.taxaIva = taxaIva;
        this.valorIvaCalculado = valorIvaCalculado;
    }

    public UUID getId() {
        return id;
    }

    public EncomendaFornecedor getEncomenda() {
        return encomenda;
    }

    public void setEncomenda(EncomendaFornecedor encomenda) {
        this.encomenda = encomenda;
    }

    public MateriaPrima getMateriaPrima() {
        return materiaPrima;
    }

    public void setMateriaPrima(MateriaPrima materiaPrima) {
        this.materiaPrima = materiaPrima;
    }

    public Double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Double quantidade) {
        this.quantidade = quantidade;
    }

    public Double getPrecoUnitarioNet() {
        return precoUnitarioNet;
    }

    public void setPrecoUnitarioNet(Double precoUnitarioNet) {
        this.precoUnitarioNet = precoUnitarioNet;
    }

    public Double getTaxaIva() {
        return taxaIva;
    }

    public void setTaxaIva(Double taxaIva) {
        this.taxaIva = taxaIva;
    }

    public Double getValorIvaCalculado() {
        return valorIvaCalculado;
    }

    public void setValorIvaCalculado(Double valorIvaCalculado) {
        this.valorIvaCalculado = valorIvaCalculado;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}