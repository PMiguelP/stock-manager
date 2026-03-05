package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "encomendas_cliente")
public class EncomendaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoEncomendaCliente estado;

    @Column(nullable = false)
    private Double total;

    @Column(name = "codigo_tracking", length = 50)
    private String codigoTracking;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL)
    private List<ItemEncomendaCliente> itens;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EncomendaCliente() {
    }

    public EncomendaCliente(Cliente cliente, LocalDate data, EstadoEncomendaCliente estado, Double total, List<ItemEncomendaCliente> itens) {
        this.cliente = cliente;
        this.data = data;
        this.estado = estado;
        this.total = total;
        this.itens = itens;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public EstadoEncomendaCliente getEstado() {
        return estado;
    }

    public void setEstado(EstadoEncomendaCliente estado) {
        this.estado = estado;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getCodigoTracking() {
        return codigoTracking;
    }

    public void setCodigoTracking(String codigoTracking) {
        this.codigoTracking = codigoTracking;
    }

    public List<ItemEncomendaCliente> getItens() {
        return itens;
    }

    public void setItens(List<ItemEncomendaCliente> itens) {
        this.itens = itens;
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