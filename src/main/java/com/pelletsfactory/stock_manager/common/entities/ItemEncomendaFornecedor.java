package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "itens_encomenda_fornecedor")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ItemEncomendaFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "encomenda_id", nullable = false)
    @JsonBackReference
    private EncomendaFornecedor encomenda;

    @ManyToOne
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(nullable = false)
    private Double quantidade;

    @Column(name = "preco_unitario", nullable = false)
    private Double precoUnitario;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ItemEncomendaFornecedor() {
    }

    public ItemEncomendaFornecedor(EncomendaFornecedor encomenda, MateriaPrima materiaPrima, Double quantidade, Double precoUnitario) {
        this.encomenda = encomenda;
        this.materiaPrima = materiaPrima;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
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

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}