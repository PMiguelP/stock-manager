package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Ticket;
import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByEncomendaIdOrderByUltimaMensagemEmDesc(UUID encomendaId);
    List<Ticket> findByEncomendaId(UUID encomendaId);
    List<Ticket> findByEstadoInOrderByUltimaMensagemEmDesc(Collection<EstadoTicket> estados);
    List<Ticket> findAllByOrderByUltimaMensagemEmDesc();
}
