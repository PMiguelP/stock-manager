package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materias_primas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MateriaPrima {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 20)
    private String unidade;  // Ex: "kg", "ton", "m³"

    @Column(name = "stock_atual", nullable = false)
    private Double stockAtual;

    @Column(name = "stock_minimo", nullable = false)
    private Double stockMinimo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MateriaPrima() {
    }

    public MateriaPrima(String nome, String unidade, Double stockAtual, Double stockMinimo) {
        this.nome = nome;
        this.unidade = unidade;
        this.stockAtual = stockAtual;
        this.stockMinimo = stockMinimo;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public Double getStockAtual() {
        return stockAtual;
    }

    public void setStockAtual(Double stockAtual) {
        this.stockAtual = stockAtual;
    }

    public Double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}