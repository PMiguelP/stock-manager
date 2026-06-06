package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.TicketMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketMensagemRepository extends JpaRepository<TicketMensagem, UUID> {
    List<TicketMensagem> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
