package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import com.pelletsfactory.stock_manager.common.utils.DecimalUtils;

@Entity
@Table(name = "encomendas_fornecedor")
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
    private EstadoEncomendaFornecedor estado = EstadoEncomendaFornecedor.RASCUNHO;

    @Column(name = "total_liquido", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalLiquido;

    @Column(name = "total_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIva;

    @Column(name = "total_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFinal;

    @ManyToOne
    @JoinColumn(name = "moeda_id", nullable = false)
    private Moeda moeda;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL)
    private List<ItemEncomendaFornecedor> itens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public EncomendaFornecedor() {
    }

    public EncomendaFornecedor(Fornecedor fornecedor, LocalDate data, Double totalLiquido, Double totalIva,
                               Double totalFinal, Moeda moeda, List<ItemEncomendaFornecedor> itens) {
        this.fornecedor = fornecedor;
        this.data = data;
        setTotalLiquido(totalLiquido);
        setTotalIva(totalIva);
        setTotalFinal(totalFinal);
        this.moeda = moeda;
        this.itens = itens;
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

    public Double getTotalLiquido() {
        return DecimalUtils.toDouble(totalLiquido);
    }

    public void setTotalLiquido(Double totalLiquido) {
        this.totalLiquido = DecimalUtils.fromDouble(totalLiquido, 2);
    }

    public Double getTotalIva() {
        return DecimalUtils.toDouble(totalIva);
    }

    public void setTotalIva(Double totalIva) {
        this.totalIva = DecimalUtils.fromDouble(totalIva, 2);
    }

    public Double getTotalFinal() {
        return DecimalUtils.toDouble(totalFinal);
    }

    public void setTotalFinal(Double totalFinal) {
        this.totalFinal = DecimalUtils.fromDouble(totalFinal, 2);
    }

    public Moeda getMoeda() {
        return moeda;
    }

    public void setMoeda(Moeda moeda) {
        this.moeda = moeda;
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
