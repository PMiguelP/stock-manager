package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import com.pelletsfactory.stock_manager.common.utils.DecimalUtils;

@Entity
@Table(name = "itens_encomenda_fornecedor")
public class ItemEncomendaFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "encomenda_id", nullable = false)
    private EncomendaFornecedor encomenda;

    @ManyToOne
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Column(nullable = false)
    private Double quantidade;

    @Column(name = "preco_unitario_net", nullable = false, precision = 19, scale = 4)
    private BigDecimal precoUnitarioNet;

    @Column(name = "taxa_iva", nullable = false)
    private Double taxaIva;

    @Column(name = "valor_iva_calculado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorIvaCalculado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public ItemEncomendaFornecedor() {
    }

    public ItemEncomendaFornecedor(EncomendaFornecedor encomenda, MateriaPrima materiaPrima, Double quantidade,
                                   Double precoUnitarioNet, Double taxaIva, Double valorIvaCalculado) {
        this.encomenda = encomenda;
        this.materiaPrima = materiaPrima;
        this.quantidade = quantidade;
        setPrecoUnitarioNet(precoUnitarioNet);
        this.taxaIva = taxaIva;
        setValorIvaCalculado(valorIvaCalculado);
    }

    public UUID getId() {
        return id;
    }

    public EncomendaFornecedor getEncomenda() {
        return encomenda;
    }

    public void setEncomenda(EncomendaFornecedor encomenda) {
        this.encomenda = encomenda;
    }

    public MateriaPrima getMateriaPrima() {
        return materiaPrima;
    }

    public void setMateriaPrima(MateriaPrima materiaPrima) {
        this.materiaPrima = materiaPrima;
    }

    public Double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Double quantidade) {
        this.quantidade = quantidade;
    }

    public Double getPrecoUnitarioNet() {
        return DecimalUtils.toDouble(precoUnitarioNet);
    }

    public void setPrecoUnitarioNet(Double precoUnitarioNet) {
        this.precoUnitarioNet = DecimalUtils.fromDouble(precoUnitarioNet, 4);
    }

    public Double getTaxaIva() {
        return taxaIva;
    }

    public void setTaxaIva(Double taxaIva) {
        this.taxaIva = taxaIva;
    }

    public Double getValorIvaCalculado() {
        return DecimalUtils.toDouble(valorIvaCalculado);
    }

    public void setValorIvaCalculado(Double valorIvaCalculado) {
        this.valorIvaCalculado = DecimalUtils.fromDouble(valorIvaCalculado, 2);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
