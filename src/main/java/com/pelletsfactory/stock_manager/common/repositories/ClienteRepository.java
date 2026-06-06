package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    boolean existsByNif(String nif);

    @Query("SELECT c FROM Cliente c WHERE " +
            "(:nome IS NULL OR :nome = '' OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:nif IS NULL OR :nif = '' OR c.nif = :nif)")
    Page<Cliente> findByFiltros(@Param("nome") String nome,
                               @Param("nif") String nif,
                               Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE " +
            "(:pesquisa IS NULL OR :pesquisa = '' " +
            "OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :pesquisa, '%')) " +
            "OR LOWER(c.nif) LIKE LOWER(CONCAT('%', :pesquisa, '%')))")
    Page<Cliente> findByPesquisa(@Param("pesquisa") String pesquisa, Pageable pageable);
}
