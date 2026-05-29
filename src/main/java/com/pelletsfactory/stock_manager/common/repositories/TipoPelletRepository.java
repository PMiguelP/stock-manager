package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TipoPelletRepository extends JpaRepository<TipoPellet, UUID> {
    @Query("SELECT t FROM TipoPellet t WHERE " +
            "(:nome IS NULL OR :nome = '' OR LOWER(t.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:diametroMm IS NULL OR t.diametroMm = :diametroMm)")
    Page<TipoPellet> findByFiltros(@Param("nome") String nome,
                                  @Param("diametroMm") Double diametroMm,
                                  Pageable pageable);

    @Query("SELECT COUNT(t) FROM TipoPellet t WHERE t.stockAtual < t.stockMinimo")
    long countBelowMinimumStock();
}
