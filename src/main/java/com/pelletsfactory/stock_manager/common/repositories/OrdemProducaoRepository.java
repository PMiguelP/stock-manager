package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, UUID> {
    @Query("SELECT DISTINCT op FROM OrdemProducao op " +
            "LEFT JOIN FETCH op.alocacoes a " +
            "LEFT JOIN FETCH a.encomendaCliente e " +
            "LEFT JOIN FETCH e.cliente c " +
            "WHERE op.id = :id")
    Optional<OrdemProducao> findByIdWithDetalhes(@Param("id") UUID id);

    long countByFuncionarioIdAndEstado(UUID funcionarioId, EstadoOrdemProducao estado);

    @Query("SELECT op FROM OrdemProducao op WHERE " +
            "(:estado IS NULL OR op.estado = :estado) AND " +
            "(:tipoPelletId IS NULL OR op.tipoPellet.id = :tipoPelletId) AND " +
            "(:funcionarioId IS NULL OR op.funcionario.id = :funcionarioId)")
    Page<OrdemProducao> findByFiltros(
            @Param("estado") EstadoOrdemProducao estado,
            @Param("tipoPelletId") UUID tipoPelletId,
            @Param("funcionarioId") UUID funcionarioId,
            Pageable pageable
    );

    @Query("SELECT COUNT(op) FROM OrdemProducao op WHERE op.estado = :estado")
    Long countByEstado(@Param("estado") EstadoOrdemProducao estado);

    @Query("SELECT op FROM OrdemProducao op WHERE op.funcionario.id = :funcionarioId ORDER BY op.dataInicio DESC")
    Page<OrdemProducao> findByFuncionarioId(@Param("funcionarioId") UUID funcionarioId, Pageable pageable);
}
