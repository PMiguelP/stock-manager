package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.AlocacaoLoteEncomenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlocacaoLoteEncomendaRepository extends JpaRepository<AlocacaoLoteEncomenda, UUID> {

    boolean existsByLoteIdAndItemEncomendaId(UUID loteId, UUID itemEncomendaId);

    List<AlocacaoLoteEncomenda> findByLoteId(UUID loteId);

    List<AlocacaoLoteEncomenda> findByItemEncomendaId(UUID itemEncomendaId);

    List<AlocacaoLoteEncomenda> findByItemEncomendaEncomendaId(UUID encomendaId);

    @Query("""
            SELECT a FROM AlocacaoLoteEncomenda a
            JOIN FETCH a.lote l
            JOIN FETCH a.itemEncomenda i
            JOIN FETCH i.encomenda e
            JOIN FETCH e.cliente
            JOIN FETCH i.tipoPellet
            """)
    List<AlocacaoLoteEncomenda> findAllWithDetails();

    @Query("SELECT COALESCE(SUM(a.quantidadeReservada), 0) FROM AlocacaoLoteEncomenda a WHERE a.lote.id = :loteId")
    Double sumQuantidadeReservadaByLoteId(@Param("loteId") UUID loteId);

    @Query("SELECT COALESCE(SUM(a.quantidadeReservada), 0) FROM AlocacaoLoteEncomenda a WHERE a.itemEncomenda.id = :itemId")
    Double sumQuantidadeReservadaByItemId(@Param("itemId") UUID itemId);
}
