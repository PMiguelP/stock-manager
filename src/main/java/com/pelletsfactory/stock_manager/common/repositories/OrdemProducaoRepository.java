package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, UUID> {
}
