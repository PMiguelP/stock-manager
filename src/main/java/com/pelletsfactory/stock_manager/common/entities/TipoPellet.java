package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tipo_pellet")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TipoPellet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(name = "diametro_mm")
    private Double diametroMm;

    @Column(name = "poder_calorifico")
    private Double poderCalorifico;

    @Column(name = "stock_atual", nullable = false)
    private Double stockAtual = 0.0;

    @Column(name = "stock_minimo", nullable = false)
    private Double stockMinimo = 0.0;

    @Column(name = "custo_atual_por_kg")
    private BigDecimal custoAtualPorKg;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TipoPellet() {
    }

    public TipoPellet(String nome, Double diametroMm, Double poderCalorifico, Double stockAtual, Double stockMinimo, BigDecimal custoAtualPorKg) {
        this.nome = nome;
        this.diametroMm = diametroMm;
        this.poderCalorifico = poderCalorifico;
        this.stockAtual = stockAtual;
        this.stockMinimo = stockMinimo;
        this.custoAtualPorKg = custoAtualPorKg;
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

    public Double getDiametroMm() {
        return diametroMm;
    }

    public void setDiametroMm(Double diametroMm) {
        this.diametroMm = diametroMm;
    }

    public Double getPoderCalorifico() {
        return poderCalorifico;
    }

    public void setPoderCalorifico(Double poderCalorifico) {
        this.poderCalorifico = poderCalorifico;
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

    public BigDecimal getCustoAtualPorKg() {
        return custoAtualPorKg;
    }

    public void setCustoAtualPorKg(BigDecimal custoAtualPorKg) {
        this.custoAtualPorKg = custoAtualPorKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
