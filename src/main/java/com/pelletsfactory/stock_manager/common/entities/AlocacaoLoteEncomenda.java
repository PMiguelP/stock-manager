package com.pelletsfactory.stock_manager.common.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Reserva stock físico de um lote para um item concreto de uma encomenda.
 * O lote identifica a origem da produção e o item identifica o produto pedido.
 */
@Entity
@Table(name = "alocacoes_lote_encomenda", uniqueConstraints = {
        @UniqueConstraint(name = "uk_alocacao_lote_item", columnNames = {"lote_id", "item_encomenda_id"})
})
public class AlocacaoLoteEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "lote_id", nullable = false)
    private LotePellet lote;

    @ManyToOne
    @JoinColumn(name = "item_encomenda_id", nullable = false)
    private ItemEncomendaCliente itemEncomenda;

    @Column(name = "quantidade_reservada", nullable = false)
    private Double quantidadeReservada;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public LotePellet getLote() { return lote; }
    public void setLote(LotePellet lote) { this.lote = lote; }
    public ItemEncomendaCliente getItemEncomenda() { return itemEncomenda; }
    public void setItemEncomenda(ItemEncomendaCliente itemEncomenda) { this.itemEncomenda = itemEncomenda; }
    public Double getQuantidadeReservada() { return quantidadeReservada; }
    public void setQuantidadeReservada(Double quantidadeReservada) { this.quantidadeReservada = quantidadeReservada; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
