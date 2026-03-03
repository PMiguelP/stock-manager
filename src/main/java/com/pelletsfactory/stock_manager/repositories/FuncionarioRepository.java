package com.pelletsfactory.stock_manager.repositories;

import com.pelletsfactory.stock_manager.entities.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
}
