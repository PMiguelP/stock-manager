package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import jakarta.persistence.LockModeType;

public interface LotePelletRepository extends JpaRepository<LotePellet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LotePellet l WHERE l.id = :id")
    Optional<LotePellet> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT l FROM LotePellet l WHERE l.ordem.id = :ordemId")
    Page<LotePellet> findByOrdemId(@Param("ordemId") UUID ordemId, Pageable pageable);

    @Query("SELECT l FROM LotePellet l WHERE l.ordem.id = :ordemId")
    List<LotePellet> findByOrdemId(@Param("ordemId") UUID ordemId);

    @Query("SELECT l FROM LotePellet l WHERE l.tipoPellet.id = :tipoPelletId")
    Page<LotePellet> findByTipoPelletId(@Param("tipoPelletId") UUID tipoPelletId, Pageable pageable);

    @Query("""
            SELECT l FROM LotePellet l
            JOIN FETCH l.tipoPellet t
            WHERE (:tipoPelletId IS NULL OR t.id = :tipoPelletId)
            """)
    List<LotePellet> findAllocationCandidates(@Param("tipoPelletId") UUID tipoPelletId);

    boolean existsByCodigoLote(String codigoLote);

    @Query("SELECT l.codigoLote FROM LotePellet l WHERE l.codigoLote LIKE CONCAT(:prefix, '%') ORDER BY l.codigoLote DESC")
    List<String> findTopCodigosComPrefix(@Param("prefix") String prefix, Pageable pageable);

    List<LotePellet> findByDataProducaoGreaterThanEqualAndDataProducaoLessThan(Instant inicio, Instant fim);

    @Query("SELECT l FROM LotePellet l WHERE " +
            "(:codigoLote IS NULL OR :codigoLote = '' OR LOWER(l.codigoLote) LIKE LOWER(CONCAT('%', :codigoLote, '%'))) AND " +
            "(:tipoPelletId IS NULL OR l.tipoPellet.id = :tipoPelletId) AND " +
            "(:ordemId IS NULL OR l.ordem.id = :ordemId)")
    Page<LotePellet> findByFiltros(
            @Param("codigoLote") String codigoLote,
            @Param("tipoPelletId") UUID tipoPelletId,
            @Param("ordemId") UUID ordemId,
            Pageable pageable
    );
}
