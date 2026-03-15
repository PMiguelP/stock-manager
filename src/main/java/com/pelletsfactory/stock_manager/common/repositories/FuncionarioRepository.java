package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    List<Funcionario> findByCargo(Cargo cargo);

    //DEixa ver se isto vai funcionar tiago que senao nao da
    @Query("SELECT max(f.numeroFuncionario) FROM Funcionario f")
    Integer findMaxNumeroFuncionario();

    Optional<Funcionario> findByNumeroFuncionario(Integer numeroFuncionario);

    boolean existsByNif(String nif);
}
