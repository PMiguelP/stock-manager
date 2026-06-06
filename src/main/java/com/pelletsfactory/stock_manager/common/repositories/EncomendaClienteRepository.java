package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;

public interface EncomendaClienteRepository extends JpaRepository<EncomendaCliente, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EncomendaCliente e WHERE e.id = :id")
    Optional<EncomendaCliente> findByIdForUpdate(@Param("id") UUID id);

    Page<EncomendaCliente> findByClienteId(UUID clienteId, Pageable pageable);

    java.util.List<EncomendaCliente> findByClienteId(UUID clienteId);

    Optional<EncomendaCliente> findByCodigoTrackingIgnoreCase(String codigoTracking);

    boolean existsByCodigoTrackingIgnoreCase(String codigoTracking);

    long countByEstado(EstadoEncomendaCliente estado);

    java.util.List<EncomendaCliente> findByDataGreaterThanEqualAndDataLessThan(LocalDate inicio, LocalDate fim);

    @Query("SELECT e FROM EncomendaCliente e WHERE " +
            "(:clienteId IS NULL OR e.cliente.id = :clienteId) AND " +
            "(:estado IS NULL OR e.estado = :estado)")
    Page<EncomendaCliente> findByFiltros(@Param("clienteId") UUID clienteId,
                                        @Param("estado") EstadoEncomendaCliente estado,
                                        Pageable pageable);

    @Query("SELECT e FROM EncomendaCliente e WHERE " +
            "LOWER(e.cliente.nome) LIKE LOWER(CONCAT('%', :clienteNome, '%')) AND " +
            "(:estado IS NULL OR e.estado = :estado)")
    Page<EncomendaCliente> findByFiltrosPesquisa(@Param("clienteNome") String clienteNome,
                                                 @Param("estado") EstadoEncomendaCliente estado,
                                                 Pageable pageable);
}
