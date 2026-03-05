package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordens_producao")
public class OrdemProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tipo_pellet_id", nullable = false)
    private TipoPellet tipoPellet;

    @ManyToOne
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne
    @JoinColumn(name = "formula_id", nullable = false)
    private FormulaProducao formula;

    @Column(name = "quantidade_pedida_kg", nullable = false)
    private Double quantidadePedidaKg;

    @Column(name = "quantidade_produzida_kg")
    private Double quantidadeProduzidaKg;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoOrdemProducao estado;

    @OneToMany(mappedBy = "ordem")
    private List<ConsumoProducao> consumos;

    @OneToMany(mappedBy = "ordem")
    private List<LotePellet> lotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OrdemProducao() {
    }

    public OrdemProducao(TipoPellet tipoPellet, Funcionario funcionario, FormulaProducao formula, Double quantidadePedidaKg, LocalDate dataInicio, EstadoOrdemProducao estado) {
        this.tipoPellet = tipoPellet;
        this.funcionario = funcionario;
        this.formula = formula;
        this.quantidadePedidaKg = quantidadePedidaKg;
        this.dataInicio = dataInicio;
        this.estado = estado;
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

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public FormulaProducao getFormula() {
        return formula;
    }

    public void setFormula(FormulaProducao formula) {
        this.formula = formula;
    }

    public Double getQuantidadePedidaKg() {
        return quantidadePedidaKg;
    }

    public void setQuantidadePedidaKg(Double quantidadePedidaKg) {
        this.quantidadePedidaKg = quantidadePedidaKg;
    }

    public Double getQuantidadeProduzidaKg() {
        return quantidadeProduzidaKg;
    }

    public void setQuantidadeProduzidaKg(Double quantidadeProduzidaKg) {
        this.quantidadeProduzidaKg = quantidadeProduzidaKg;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public EstadoOrdemProducao getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdemProducao estado) {
        this.estado = estado;
    }

    public List<ConsumoProducao> getConsumos() {
        return consumos;
    }

    public void setConsumos(List<ConsumoProducao> consumos) {
        this.consumos = consumos;
    }

    public List<LotePellet> getLotes() {
        return lotes;
    }

    public void setLotes(List<LotePellet> lotes) {
        this.lotes = lotes;
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