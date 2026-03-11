package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consumo_producao")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConsumoProducao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ordem_id", nullable = false)
    @JsonBackReference
    private OrdemProducao ordem;

    @ManyToOne
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(name = "quantidade_consumida", nullable = false)
    private Double quantidadeConsumida;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    public ConsumoProducao(){
    }

    public ConsumoProducao(OrdemProducao ordem, MateriaPrima materiaPrima, Double quantidadeConsumida) {
        this.ordem = ordem;
        this.materiaPrima = materiaPrima;
        this.quantidadeConsumida = quantidadeConsumida;
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

    public Double getQuantidadeConsumida() {
        return quantidadeConsumida;
    }

    public void setQuantidadeConsumida(Double quantidadeConsumida) {
        this.quantidadeConsumida = quantidadeConsumida;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}