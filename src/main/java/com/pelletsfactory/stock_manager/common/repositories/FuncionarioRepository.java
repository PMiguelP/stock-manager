package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    @Query("SELECT max(f.numeroFuncionario) FROM Funcionario f")
    Integer findMaxNumeroFuncionario();

    Optional<Funcionario> findByNumeroFuncionario(Integer numeroFuncionario);

    boolean existsByNif(String nif);

    @Query("SELECT f FROM Funcionario f WHERE " +
            "(:nome IS NULL OR :nome = '' OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:nif IS NULL OR :nif = '' OR f.nif = :nif) AND " +
            "(:cargo IS NULL OR f.cargo = :cargo) AND " +
            "(:numeroFuncionario IS NULL OR f.numeroFuncionario = :numeroFuncionario)")
    Page<Funcionario> findByFiltros(@Param("nome") String nome,
                                    @Param("nif") String nif,
                                    @Param("cargo") Cargo cargo,
                                    @Param("numeroFuncionario") Integer numeroFuncionario,
                                    Pageable pageable);
}
