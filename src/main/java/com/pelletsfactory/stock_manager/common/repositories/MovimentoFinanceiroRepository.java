package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, UUID> {
    @Query("SELECT m FROM MovimentoFinanceiro m WHERE " +
            "(:tipoMovimento IS NULL OR m.tipoMovimento = :tipoMovimento) AND " +
            "(:moedaId IS NULL OR m.moeda.id = :moedaId)")
    Page<MovimentoFinanceiro> findByFiltros(@Param("tipoMovimento") TipoMovimento tipoMovimento,
                                           @Param("moedaId") UUID moedaId,
                                           Pageable pageable);
}
