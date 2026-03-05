package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.FormulaProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FormulaProducaoRepository extends JpaRepository<FormulaProducao, UUID> {
}
