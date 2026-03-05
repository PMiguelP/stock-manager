package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EncomendaClienteRepository extends JpaRepository<EncomendaCliente, UUID> {
}
