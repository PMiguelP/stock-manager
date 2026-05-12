package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MateriaPrimaRepository extends JpaRepository<MateriaPrima, UUID> {
    @Query("SELECT m FROM MateriaPrima m WHERE " +
            "(:nome IS NULL OR :nome = '' OR LOWER(m.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:unidade IS NULL OR :unidade = '' OR LOWER(m.unidade) LIKE LOWER(CONCAT('%', :unidade, '%')))")
    Page<MateriaPrima> findByFiltros(@Param("nome") String nome,
                                    @Param("unidade") String unidade,
                                    Pageable pageable);

    @Query("SELECT COUNT(m) FROM MateriaPrima m WHERE m.stockAtual < m.stockMinimo")
    long countBelowMinimumStock();
}
