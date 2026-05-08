package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {
    @Query("SELECT f FROM Fornecedor f WHERE " +
            "(:nome IS NULL OR :nome = '' OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:nif IS NULL OR :nif = '' OR f.nif = :nif)")
    Page<Fornecedor> findByFiltros(@Param("nome") String nome,
                                  @Param("nif") String nif,
                                  Pageable pageable);
}
