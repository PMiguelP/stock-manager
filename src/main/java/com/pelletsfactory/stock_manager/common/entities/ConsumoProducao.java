package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consumo_producao")
public class ConsumoProducao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ordem_id", nullable = false)
    private OrdemProducao ordem;

    @ManyToOne
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(name = "quantidade_consumida_real", nullable = false)
    private Double quantidadeConsumidaReal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    public ConsumoProducao(){
    }

    public ConsumoProducao(OrdemProducao ordem, MateriaPrima materiaPrima, Double quantidadeConsumidaReal) {
        this.ordem = ordem;
        this.materiaPrima = materiaPrima;
        this.quantidadeConsumidaReal = quantidadeConsumidaReal;
    }

    public UUID getId() {
        return id;
    }

    public OrdemProducao getOrdem() {
        return ordem;
    }

    public void setOrdem(OrdemProducao ordem) {
        this.ordem = ordem;
    }

    public MateriaPrima getMateriaPrima() {
        return materiaPrima;
    }

    public void setMateriaPrima(MateriaPrima materiaPrima) {
        this.materiaPrima = materiaPrima;
    }

    public Double getQuantidadeConsumidaReal() {
        return quantidadeConsumidaReal;
    }

    public void setQuantidadeConsumidaReal(Double quantidadeConsumidaReal) {
        this.quantidadeConsumidaReal = quantidadeConsumidaReal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}