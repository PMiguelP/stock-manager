package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.AlocacaoOrdemEncomenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlocacaoOrdemEncomendaRepository extends JpaRepository<AlocacaoOrdemEncomenda, UUID> {

    @Query("SELECT a FROM AlocacaoOrdemEncomenda a WHERE a.ordem.id = :ordemId")
    List<AlocacaoOrdemEncomenda> findByOrdemId(@Param("ordemId") UUID ordemId);

    @Query("SELECT a FROM AlocacaoOrdemEncomenda a WHERE a.ordem.id = :ordemId")
    Page<AlocacaoOrdemEncomenda> findByOrdemId(@Param("ordemId") UUID ordemId, Pageable pageable);

    @Query("SELECT a FROM AlocacaoOrdemEncomenda a WHERE a.encomendaCliente.id = :encomendaClienteId")
    List<AlocacaoOrdemEncomenda> findByEncomendaClienteId(@Param("encomendaClienteId") UUID encomendaClienteId);

    @Query("SELECT a FROM AlocacaoOrdemEncomenda a WHERE a.encomendaCliente.id = :encomendaClienteId")
    Page<AlocacaoOrdemEncomenda> findByEncomendaClienteId(@Param("encomendaClienteId") UUID encomendaClienteId, Pageable pageable);

    @Query("SELECT SUM(a.quantidadeReservada) FROM AlocacaoOrdemEncomenda a WHERE a.ordem.id = :ordemId")
    Double sumQuantidadeReservadaByOrdemId(@Param("ordemId") UUID ordemId);

    @Query("SELECT SUM(a.quantidadeReservada) FROM AlocacaoOrdemEncomenda a WHERE a.encomendaCliente.id = :encomendaClienteId")
    Double sumQuantidadeReservadaByEncomendaClienteId(@Param("encomendaClienteId") UUID encomendaClienteId);
}

