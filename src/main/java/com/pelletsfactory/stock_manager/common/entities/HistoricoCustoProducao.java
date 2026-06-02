package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import com.pelletsfactory.stock_manager.common.utils.DecimalUtils;

@Entity
@Table(name = "historico_custo_producao")
public class HistoricoCustoProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tipo_pellet_id", nullable = false)
    private TipoPellet tipoPellet;

    @Column(name = "custo_base_por_kg", nullable = false, precision = 19, scale = 4)
    private BigDecimal custoBasePorKg;

    @CreationTimestamp
    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_fim")
    private Instant dataFim;

    public HistoricoCustoProducao() {
    }

    public HistoricoCustoProducao(UUID id, TipoPellet tipoPellet, Double custoBasePorKg) {
        this.id = id;
        this.tipoPellet = tipoPellet;
        setCustoBasePorKg(custoBasePorKg);
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

    public Double getCustoBasePorKg() {
        return DecimalUtils.toDouble(custoBasePorKg);
    }

    public void setCustoBasePorKg(Double custoBasePorKg) {
        this.custoBasePorKg = DecimalUtils.fromDouble(custoBasePorKg, 4);
    }

    public Instant getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(Instant dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Instant getDataFim() {
        return dataFim;
    }

    public void setDataFim(Instant dataFim) {
        this.dataFim = dataFim;
    }
}
