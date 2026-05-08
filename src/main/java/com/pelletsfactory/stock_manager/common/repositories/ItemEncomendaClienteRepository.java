package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemEncomendaClienteRepository extends JpaRepository<ItemEncomendaCliente, UUID> {
    @Query("SELECT i FROM ItemEncomendaCliente i WHERE i.encomenda.id = :encomendaId")
    List<ItemEncomendaCliente> findByEncomendaId(@Param("encomendaId") UUID encomendaId);
}
