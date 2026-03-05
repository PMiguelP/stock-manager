package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ConsumoProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsumoProducaoRepository extends JpaRepository<ConsumoProducao, UUID> {
}
