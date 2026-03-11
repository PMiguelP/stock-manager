package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "composicao_pellet")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ComposicaoPellet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "formula_id", nullable = false)
    @JsonBackReference
    private FormulaProducao formulaProducao;

    @ManyToOne
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(name = "quantidade_por_kg", nullable = false)
    private Double quantidadePorKg;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ComposicaoPellet() {
    }

    public ComposicaoPellet(FormulaProducao formulaProducao, MateriaPrima materiaPrima, Double quantidadePorKg) {
        this.formulaProducao = formulaProducao;
        this.materiaPrima = materiaPrima;
        this.quantidadePorKg = quantidadePorKg;
    }

    public UUID getId() {
        return id;
    }

    public FormulaProducao getFormulaProducao() {
        return formulaProducao;
    }

    public void setFormulaProducao(FormulaProducao formulaProducao) {
        this.formulaProducao = formulaProducao;
    }

    public MateriaPrima getMateriaPrima() {
        return materiaPrima;
    }

    public void setMateriaPrima(MateriaPrima materiaPrima) {
        this.materiaPrima = materiaPrima;
    }

    public Double getQuantidadePorKg() {
        return quantidadePorKg;
    }

    public void setQuantidadePorKg(Double quantidadePorKg) {
        this.quantidadePorKg = quantidadePorKg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
