package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import jakarta.persistence.LockModeType;

public interface ItemEncomendaClienteRepository extends JpaRepository<ItemEncomendaCliente, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ItemEncomendaCliente i WHERE i.id = :id")
    Optional<ItemEncomendaCliente> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT i FROM ItemEncomendaCliente i WHERE i.encomenda.id = :encomendaId")
    List<ItemEncomendaCliente> findByEncomendaId(@Param("encomendaId") UUID encomendaId);

    @Query("""
            SELECT i FROM ItemEncomendaCliente i
            JOIN FETCH i.encomenda e
            JOIN FETCH e.cliente
            JOIN FETCH i.tipoPellet t
            WHERE (:tipoPelletId IS NULL OR t.id = :tipoPelletId)
              AND e.estado NOT IN :estadosExcluidos
            """)
    List<ItemEncomendaCliente> findAllocationCandidates(
            @Param("tipoPelletId") UUID tipoPelletId,
            @Param("estadosExcluidos") Set<EstadoEncomendaCliente> estadosExcluidos);
}
