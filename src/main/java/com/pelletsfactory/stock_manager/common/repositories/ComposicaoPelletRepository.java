package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ComposicaoPellet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ComposicaoPelletRepository extends JpaRepository<ComposicaoPellet, UUID> {
    
    @Query("SELECT c FROM ComposicaoPellet c WHERE c.formulaProducao.id = :formulaId")
    List<ComposicaoPellet> findByFormulaId(@Param("formulaId") UUID formulaId);

    @Query("SELECT c FROM ComposicaoPellet c WHERE c.formulaProducao.id = :formulaId")
    Page<ComposicaoPellet> findByFormulaId(@Param("formulaId") UUID formulaId, Pageable pageable);

    @Query("SELECT c FROM ComposicaoPellet c WHERE c.materiaPrima.id = :materiaPrimaId")
    List<ComposicaoPellet> findByMateriaPrimaId(@Param("materiaPrimaId") UUID materiaPrimaId);
}
