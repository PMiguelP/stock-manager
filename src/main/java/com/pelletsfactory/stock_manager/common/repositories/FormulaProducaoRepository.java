package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface FormulaProducaoRepository extends JpaRepository<FormulaProducao, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FormulaProducao f WHERE f.id = :id")
    Optional<FormulaProducao> findByIdForUpdate(@Param("id") UUID id);

    
    @Query("SELECT f FROM FormulaProducao f WHERE f.tipoPellet.id = :tipoPelletId")
    Page<FormulaProducao> findByTipoPelletId(@Param("tipoPelletId") UUID tipoPelletId, Pageable pageable);

    @Query("SELECT f FROM FormulaProducao f WHERE " +
            "(:nome IS NULL OR :nome = '' OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:ativa IS NULL OR f.ativa = :ativa) AND " +
            "(:tipoPelletId IS NULL OR f.tipoPellet.id = :tipoPelletId)")
    Page<FormulaProducao> findByFiltros(
            @Param("nome") String nome,
            @Param("ativa") Boolean ativa,
            @Param("tipoPelletId") UUID tipoPelletId,
            Pageable pageable
    );

    @Query("SELECT f FROM FormulaProducao f WHERE f.ativa = true")
    Page<FormulaProducao> findAllAtivas(Pageable pageable);

    @EntityGraph(attributePaths = {"tipoPellet", "composicao", "composicao.materiaPrima"})
    @Query("SELECT f FROM FormulaProducao f WHERE f.id = :id")
    Optional<FormulaProducao> findByIdWithComposicao(@Param("id") UUID id);
}
