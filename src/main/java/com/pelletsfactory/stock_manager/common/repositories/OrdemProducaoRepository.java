package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, UUID> {
    @Query("SELECT DISTINCT op FROM OrdemProducao op " +
            "LEFT JOIN FETCH op.itensAlocados ia " +
            "LEFT JOIN FETCH ia.encomenda e " +
            "LEFT JOIN FETCH e.cliente c " +
            "WHERE op.id = :id")
    Optional<OrdemProducao> findByIdWithDetalhes(@Param("id") UUID id);

    long countByFuncionarioIdAndEstado(UUID funcionarioId, EstadoOrdemProducao estado);
}

