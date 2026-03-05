package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "formula_producao")
public class FormulaProducao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tipo_pellet_id", nullable = false)
    private TipoPellet tipoPellet;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Boolean ativa;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL)
    private List<ComposicaoPellet> composicao;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FormulaProducao() {
    }

    public FormulaProducao(TipoPellet tipoPellet, String nome, Boolean ativa, List<ComposicaoPellet> composicao) {
        this.tipoPellet = tipoPellet;
        this.nome = nome;
        this.ativa = ativa;
        this.composicao = composicao;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TipoPellet getTipoPellet() {
        return tipoPellet;
    }

    public void setTipoPellet(TipoPellet tipoPellet) {
        this.tipoPellet = tipoPellet;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public List<ComposicaoPellet> getComposicao() {
        return composicao;
    }

    public void setComposicao(List<ComposicaoPellet> composicao) {
        this.composicao = composicao;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
