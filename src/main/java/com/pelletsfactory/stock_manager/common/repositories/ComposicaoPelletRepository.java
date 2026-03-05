package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ComposicaoPellet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComposicaoPelletRepository extends JpaRepository<ComposicaoPellet, UUID> {
}
