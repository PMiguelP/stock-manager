package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lotes_pellet")
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
    private OrdemProducao ordem;

    @Column(name = "codigo_lote", nullable = false, unique = true, length = 50)
    private String codigoLote;

    @Column(name = "quantidade_kg", nullable = false)
    private Double quantidadeKg;

    @Column(name = "data_producao", nullable = false)
    private Instant dataProducao;

    @Column(name = "localizacao_armazem", length = 100)
    private String localizacaoArmazem;

    @OneToMany(mappedBy = "lote")
    private List<AlocacaoLoteEncomenda> alocacoes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public LotePellet() {
    }

    public LotePellet(TipoPellet tipoPellet, OrdemProducao ordem, String codigoLote, Double quantidadeKg,
                      Instant dataProducao, String localizacaoArmazem) {
        this.tipoPellet = tipoPellet;
        this.ordem = ordem;
        this.codigoLote = codigoLote;
        this.quantidadeKg = quantidadeKg;
        this.dataProducao = dataProducao;
        this.localizacaoArmazem = localizacaoArmazem;
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

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public Double getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(Double quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public Instant getDataProducao() {
        return dataProducao;
    }

    public void setDataProducao(Instant dataProducao) {
        this.dataProducao = dataProducao;
    }

    public String getLocalizacaoArmazem() {
        return localizacaoArmazem;
    }

    public void setLocalizacaoArmazem(String localizacaoArmazem) {
        this.localizacaoArmazem = localizacaoArmazem;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<AlocacaoLoteEncomenda> getAlocacoes() {
        return alocacoes;
    }

    public void setAlocacoes(List<AlocacaoLoteEncomenda> alocacoes) {
        this.alocacoes = alocacoes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
