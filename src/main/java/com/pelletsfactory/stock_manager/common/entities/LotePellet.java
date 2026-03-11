package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lotes_pellet")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LotePellet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tipo_pellet_id", nullable = false)
    private TipoPellet tipoPellet;

    @ManyToOne
    @JoinColumn(name = "ordem_id", nullable = false)
    @JsonBackReference
    private OrdemProducao ordem;

    @Column(name = "quantidade_kg", nullable = false)
    private Double quantidadeKg;

    @Column(name = "data_producao", nullable = false)
    private LocalDate dataProducao;

    @Column(length = 100)
    private String localizacao;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LotePellet() {
    }

    public LotePellet(TipoPellet tipoPellet, OrdemProducao ordem, Double quantidadeKg, LocalDate dataProducao, String localizacao) {
        this.tipoPellet = tipoPellet;
        this.ordem = ordem;
        this.quantidadeKg = quantidadeKg;
        this.dataProducao = dataProducao;
        this.localizacao = localizacao;
    }

    public UUID getId() {
        return id;
    }

    public TipoPellet getTipoPellet() {
        return tipoPellet;
    }

    public void setTipoPellet(TipoPellet tipoPellet) {
        this.tipoPellet = tipoPellet;
    }

    public OrdemProducao getOrdem() {
        return ordem;
    }

    public void setOrdem(OrdemProducao ordem) {
        this.ordem = ordem;
    }

    public Double getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(Double quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public LocalDate getDataProducao() {
        return dataProducao;
    }

    public void setDataProducao(LocalDate dataProducao) {
        this.dataProducao = dataProducao;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}