package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimentos_financeiros")
public class MovimentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 20)
    private TipoMovimento tipoMovimento; // E ou S

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal;

    @ManyToOne
    @JoinColumn(name = "moeda_id", nullable = false)
    private Moeda moeda;

    @ManyToOne
    @JoinColumn(name = "id_encomenda_cliente")
    private EncomendaCliente encomendaCliente;

    @ManyToOne
    @JoinColumn(name = "id_encomenda_fornecedor")
    private EncomendaFornecedor encomendaFornecedor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public MovimentoFinanceiro() {
    }

    public MovimentoFinanceiro(UUID id, TipoMovimento tipoMovimento, Double valorTotal, Moeda moeda,
                               EncomendaCliente encomendaCliente, EncomendaFornecedor encomendaFornecedor) {
        this.id = id;
        this.tipoMovimento = tipoMovimento;
        this.valorTotal = valorTotal;
        this.moeda = moeda;
        this.encomendaCliente = encomendaCliente;
        this.encomendaFornecedor = encomendaFornecedor;
    }

    public UUID getId() {
        return id;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Moeda getMoeda() {
        return moeda;
    }

    public void setMoeda(Moeda moeda) {
        this.moeda = moeda;
    }

    public EncomendaCliente getEncomendaCliente() {
        return encomendaCliente;
    }

    public void setEncomendaCliente(EncomendaCliente encomendaCliente) {
        this.encomendaCliente = encomendaCliente;
    }

    public EncomendaFornecedor getEncomendaFornecedor() {
        return encomendaFornecedor;
    }

    public void setEncomendaFornecedor(EncomendaFornecedor encomendaFornecedor) {
        this.encomendaFornecedor = encomendaFornecedor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}