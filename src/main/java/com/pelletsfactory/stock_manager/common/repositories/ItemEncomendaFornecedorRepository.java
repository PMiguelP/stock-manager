package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemEncomendaFornecedorRepository extends JpaRepository<ItemEncomendaFornecedor, UUID> {
}
