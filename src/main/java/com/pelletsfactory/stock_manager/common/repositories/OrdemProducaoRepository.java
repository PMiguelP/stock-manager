package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import jakarta.persistence.LockModeType;

public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, UUID> {
    boolean existsByFormulaId(UUID formulaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT op FROM OrdemProducao op WHERE op.id = :id")
    Optional<OrdemProducao> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT DISTINCT op FROM OrdemProducao op " +
            "LEFT JOIN FETCH op.lotes " +
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

    @Query("SELECT COALESCE(SUM(op.quantidadeProduzidaReal), 0) FROM OrdemProducao op " +
            "WHERE op.dataInicio >= :inicio AND op.dataInicio < :fim")
    Double sumQuantidadeProduzidaBetween(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    List<OrdemProducao> findByDataInicioGreaterThanEqualAndDataInicioLessThan(Instant inicio, Instant fim);

    @Query("""
            SELECT (cp.quantidadePorKg * op.quantidadePlaneada) AS quantidadeEsperada,
                   COALESCE((
                       SELECT SUM(c.quantidadeConsumidaReal)
                       FROM ConsumoProducao c
                       WHERE c.ordem = op AND c.materiaPrima.id = :materiaPrimaId
                   ), 0) AS quantidadeConsumida
            FROM OrdemProducao op
            JOIN ComposicaoPellet cp ON cp.formulaProducao = op.formula
            WHERE cp.materiaPrima.id = :materiaPrimaId
              AND op.estado IN :estados
              AND (:ordemIgnoradaId IS NULL OR op.id <> :ordemIgnoradaId)
            """)
    List<CompromissoMateriaPrimaProjection> findCompromissosMateriaPrima(
            @Param("materiaPrimaId") UUID materiaPrimaId,
            @Param("ordemIgnoradaId") UUID ordemIgnoradaId,
            @Param("estados") Set<EstadoOrdemProducao> estados);

    interface CompromissoMateriaPrimaProjection {
        Double getQuantidadeEsperada();
        Double getQuantidadeConsumida();
    }
}
