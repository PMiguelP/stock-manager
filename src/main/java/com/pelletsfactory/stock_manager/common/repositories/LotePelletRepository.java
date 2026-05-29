package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LotePelletRepository extends JpaRepository<LotePellet, UUID> {

    @Query("SELECT l FROM LotePellet l WHERE l.ordem.id = :ordemId")
    Page<LotePellet> findByOrdemId(@Param("ordemId") UUID ordemId, Pageable pageable);

    @Query("SELECT l FROM LotePellet l WHERE l.tipoPellet.id = :tipoPelletId")
    Page<LotePellet> findByTipoPelletId(@Param("tipoPelletId") UUID tipoPelletId, Pageable pageable);

    boolean existsByCodigoLote(String codigoLote);

    @Query(value = "SELECT * FROM lotes_pellet l WHERE " +
            "(:codigoLote IS NULL OR :codigoLote = '' OR LOWER(l.codigo_lote) LIKE LOWER(CONCAT('%', :codigoLote, '%'))) AND " +
            "(:tipoPelletId IS NULL OR l.tipo_pellet_id = :tipoPelletId) AND " +
            "(:ordemId IS NULL OR l.ordem_id = :ordemId)",
            countQuery = "SELECT COUNT(*) FROM lotes_pellet l WHERE " +
                    "(:codigoLote IS NULL OR :codigoLote = '' OR LOWER(l.codigo_lote) LIKE LOWER(CONCAT('%', :codigoLote, '%'))) AND " +
                    "(:tipoPelletId IS NULL OR l.tipo_pellet_id = :tipoPelletId) AND " +
                    "(:ordemId IS NULL OR l.ordem_id = :ordemId)",
            nativeQuery = true)
    Page<LotePellet> findByFiltros(
            @Param("codigoLote") String codigoLote,
            @Param("tipoPelletId") UUID tipoPelletId,
            @Param("ordemId") UUID ordemId,
            Pageable pageable
    );
}