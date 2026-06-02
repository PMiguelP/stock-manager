package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, UUID> {
    boolean existsByEncomendaClienteId(UUID encomendaClienteId);

    boolean existsByEncomendaFornecedorId(UUID encomendaFornecedorId);

    @Query("SELECT m FROM MovimentoFinanceiro m WHERE " +
            "(:tipoMovimento IS NULL OR m.tipoMovimento = :tipoMovimento) AND " +
            "(:moedaId IS NULL OR m.moeda.id = :moedaId)")
    Page<MovimentoFinanceiro> findByFiltros(@Param("tipoMovimento") TipoMovimento tipoMovimento,
                                           @Param("moedaId") UUID moedaId,
                                           Pageable pageable);

    @Query("SELECT COUNT(DISTINCT m.moeda.id) FROM MovimentoFinanceiro m")
    long countDistinctCurrencies();

    @Query("SELECT COALESCE(SUM(m.valorTotal), 0) FROM MovimentoFinanceiro m WHERE m.tipoMovimento = :tipoMovimento")
    Double sumByTipoMovimento(@Param("tipoMovimento") TipoMovimento tipoMovimento);

    List<MovimentoFinanceiro> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant inicio, Instant fim);
}
