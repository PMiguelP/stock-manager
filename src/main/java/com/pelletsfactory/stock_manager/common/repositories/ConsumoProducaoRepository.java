package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ConsumoProducao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConsumoProducaoRepository extends JpaRepository<ConsumoProducao, UUID> {
    
    @Query("SELECT c FROM ConsumoProducao c WHERE c.ordem.id = :ordemId")
    List<ConsumoProducao> findByOrdemId(@Param("ordemId") UUID ordemId);

    @Query("SELECT c FROM ConsumoProducao c WHERE c.ordem.id = :ordemId")
    Page<ConsumoProducao> findByOrdemId(@Param("ordemId") UUID ordemId, Pageable pageable);

    @Query("SELECT c FROM ConsumoProducao c WHERE c.materiaPrima.id = :materiaPrimaId")
    List<ConsumoProducao> findByMateriaPrimaId(@Param("materiaPrimaId") UUID materiaPrimaId);
}
