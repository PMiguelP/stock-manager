package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import com.pelletsfactory.stock_manager.common.utils.DecimalUtils;

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
    private EstadoEncomendaCliente estado = EstadoEncomendaCliente.PENDENTE;

    @Column(name = "total_net", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "total_iva", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIva;

    @Column(name = "total_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFinal;

    @ManyToOne
    @JoinColumn(name = "moeda_id", nullable = false)
    private Moeda moeda;

    @Column(name = "codigo_tracking", length = 50, unique = true)
    private String codigoTracking;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL)
    private List<ItemEncomendaCliente> itens;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public EncomendaCliente() {
    }

    public EncomendaCliente(Cliente cliente, LocalDate data, EstadoEncomendaCliente estado, Double totalNet,
                            Double totalIva, Double totalFinal, Moeda moeda, List<ItemEncomendaCliente> itens) {
        this.cliente = cliente;
        this.data = data;
        this.estado = estado;
        setTotalNet(totalNet);
        setTotalIva(totalIva);
        setTotalFinal(totalFinal);
        this.moeda = moeda;
        this.itens = itens;
    }


    public UUID getId() {
        return id;
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

    public Double getTotalNet() {
        return DecimalUtils.toDouble(totalNet);
    }

    public void setTotalNet(Double totalNet) {
        this.totalNet = DecimalUtils.fromDouble(totalNet, 2);
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
