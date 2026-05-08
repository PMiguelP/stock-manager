package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemEncomendaFornecedorRepository extends JpaRepository<ItemEncomendaFornecedor, UUID> {
    @Query("SELECT i FROM ItemEncomendaFornecedor i WHERE i.encomenda.id = :encomendaId")
    List<ItemEncomendaFornecedor> findByEncomendaId(@Param("encomendaId") UUID encomendaId);
}
