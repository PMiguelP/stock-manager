package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.MovimentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, UUID> {
}
