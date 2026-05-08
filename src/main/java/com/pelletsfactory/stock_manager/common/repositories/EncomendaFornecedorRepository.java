package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EncomendaFornecedorRepository extends JpaRepository<EncomendaFornecedor, UUID> {
    Page<EncomendaFornecedor> findByFornecedorId(UUID fornecedorId, Pageable pageable);

    java.util.List<EncomendaFornecedor> findByFornecedorId(UUID fornecedorId);

    @Query("SELECT e FROM EncomendaFornecedor e WHERE " +
            "(:fornecedorId IS NULL OR e.fornecedor.id = :fornecedorId) AND " +
            "(:estado IS NULL OR e.estado = :estado)")
    Page<EncomendaFornecedor> findByFiltros(@Param("fornecedorId") UUID fornecedorId,
                                           @Param("estado") EstadoEncomendaFornecedor estado,
                                           Pageable pageable);
}
