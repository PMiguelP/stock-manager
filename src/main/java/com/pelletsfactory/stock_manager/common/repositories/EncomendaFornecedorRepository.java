package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.EncomendaFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EncomendaFornecedorRepository extends JpaRepository<EncomendaFornecedor, UUID> {
    org.springframework.data.domain.Page<EncomendaFornecedor> findByFornecedorId(UUID fornecedorId,
            org.springframework.data.domain.Pageable pageable);
}
