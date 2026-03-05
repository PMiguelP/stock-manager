package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.TipoPellet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TipoPelletRepository extends JpaRepository<TipoPellet, UUID> {
}
