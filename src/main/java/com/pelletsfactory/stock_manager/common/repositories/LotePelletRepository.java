package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LotePelletRepository extends JpaRepository<LotePellet, UUID> {
}
