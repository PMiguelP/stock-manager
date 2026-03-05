package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.MateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MateriaPrimaRepository extends JpaRepository<MateriaPrima, UUID> {
}
