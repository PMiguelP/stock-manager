package com.pelletsfactory.stock_manager.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "encomendas_fornecedor")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EncomendaFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoEncomendaFornecedor estado;

    @Column(name = "total_encomenda")
    private Double totalEncomenda;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<ItemEncomendaFornecedor> itens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EncomendaFornecedor() {
    }

    public EncomendaFornecedor(Fornecedor fornecedor, LocalDate data, Double totalEncomenda, List<ItemEncomendaFornecedor> itens) {
        this.fornecedor = fornecedor;
        this.data = data;
        this.totalEncomenda = totalEncomenda;
        this.itens = itens; //TODO: check this
    }

    public UUID getId() {
        return id;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public EstadoEncomendaFornecedor getEstado() {
        return estado;
    }

    public void setEstado(EstadoEncomendaFornecedor estado) {
        this.estado = estado;
    }

    public Double getTotalEncomenda() {
        return totalEncomenda;
    }

    public void setTotalEncomenda(Double totalEncomenda) {
        this.totalEncomenda = totalEncomenda;
    }

    public List<ItemEncomendaFornecedor> getItens() {
        return itens;
    }

    public void setItens(List<ItemEncomendaFornecedor> itens) {
        this.itens = itens;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}