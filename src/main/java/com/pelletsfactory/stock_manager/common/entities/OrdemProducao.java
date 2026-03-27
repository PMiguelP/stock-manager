package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "ordens_producao")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "quantidade_maxima", nullable = false)
    private Double quantidadeMaxima;

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
    @JsonManagedReference
    private List<ConsumoProducao> consumos;

    @OneToMany(mappedBy = "ordem")
    @JsonManagedReference
    private List<LotePellet> lotes;

    @OneToMany(mappedBy = "ordemProducao", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ItemEncomendaCliente> itensAlocados;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OrdemProducao() {
    }

    public OrdemProducao(TipoPellet tipoPellet, Funcionario funcionario, FormulaProducao formula, Double quantidadeMaxima
            ,
                         LocalDate dataInicio, EstadoOrdemProducao estado) {
        this.tipoPellet = tipoPellet;
        this.funcionario = funcionario;
        this.formula = formula;
        this.quantidadeMaxima = quantidadeMaxima;
        this.dataInicio = dataInicio;
        this.estado = estado;
    }

    @Transient
    public List<EncomendaCliente> getEncomendasAssociadas() {
        if (itensAlocados == null || itensAlocados.isEmpty()) {
            return List.of();
        }

        return itensAlocados.stream()
                .map(ItemEncomendaCliente::getEncomenda)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transient
    public List<Cliente> getClientesAssociados() {
        return getEncomendasAssociadas().stream()
                .map(EncomendaCliente::getCliente)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transient
    public Map<UUID, Double> getQuantidadePorEncomenda() {
        if (itensAlocados == null || itensAlocados.isEmpty()) {
            return Map.of();
        }

        return itensAlocados.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getEncomenda().getId(),
                        Collectors.summingDouble(ItemEncomendaCliente::getQuantidadeKg)
                ));
    }

    @Transient
    public Double getQuantidadeTotalAlocada() {
        if (itensAlocados == null || itensAlocados.isEmpty()) {
            return 0.0;
        }

        return itensAlocados.stream()
                .mapToDouble(ItemEncomendaCliente::getQuantidadeKg)
                .sum();
    }

    @Transient
    public Double getCapacidadeDisponivel() {
        return quantidadeMaxima - getQuantidadeTotalAlocada();
    }

    @Transient
    public boolean isProducaoParaStock() {
        return itensAlocados == null || itensAlocados.isEmpty();
    }

    @Transient
    public boolean temCapacidadeDisponivel(Double quantidadeRequerida) {
        return getCapacidadeDisponivel() >= quantidadeRequerida;
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

    public Double getQuantidadeMaxima() {
        return quantidadeMaxima;
    }

    public void setQuantidadeMaxima(Double quantidadeMaxima) {
        this.quantidadeMaxima = quantidadeMaxima;
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

    public List<ItemEncomendaCliente> getItensAlocados() {
        return itensAlocados;
    }

    public void setItensAlocados(List<ItemEncomendaCliente> itensAlocados) {
        this.itensAlocados = itensAlocados;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}