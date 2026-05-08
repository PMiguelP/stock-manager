package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Moeda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MoedaRepository extends JpaRepository<Moeda, UUID> {
    
    Optional<Moeda> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);

    @Query("SELECT m FROM Moeda m WHERE " +
            "(:codigo IS NULL OR :codigo = '' OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', :codigo, '%'))) OR " +
            "(:simbolo IS NULL OR :simbolo = '' OR LOWER(m.simbolo) LIKE LOWER(CONCAT('%', :simbolo, '%')))")
    Page<Moeda> findByFiltros(
            @Param("codigo") String codigo,
            @Param("simbolo") String simbolo,
            Pageable pageable
    );
}

