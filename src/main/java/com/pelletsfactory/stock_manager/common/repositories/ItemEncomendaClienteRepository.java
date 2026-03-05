package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemEncomendaClienteRepository extends JpaRepository<ItemEncomendaCliente, UUID> {
}
