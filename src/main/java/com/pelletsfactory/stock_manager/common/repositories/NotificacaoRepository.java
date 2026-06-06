package com.pelletsfactory.stock_manager.common.repositories;

import com.pelletsfactory.stock_manager.common.entities.Notificacao;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    @Query("SELECT n FROM Notificacao n WHERE n.linkReferencia = :linkReferencia")
    List<Notificacao> findByLinkReferencia(@Param("linkReferencia") UUID linkReferencia);

    @Query("SELECT n FROM Notificacao n WHERE " +
            "(:tipoEvento IS NULL OR n.tipoEvento = :tipoEvento) AND " +
            "(:cargoAlvo IS NULL OR n.cargoAlvo IS NULL OR n.cargoAlvo = :cargoAlvo) AND " +
            "(:concluida IS NULL OR " +
            "(:concluida = true AND n.concluida = true) OR " +
            "(:concluida = false AND (n.concluida = false OR n.concluida IS NULL))) AND " +
            "(:lida IS NULL OR " +
            "(:lida = true AND EXISTS (SELECT nl.id FROM NotificacaoLeitura nl WHERE nl.notificacao = n AND nl.funcionario.id = :funcionarioId)) OR " +
            "(:lida = false AND NOT EXISTS (SELECT nl.id FROM NotificacaoLeitura nl WHERE nl.notificacao = n AND nl.funcionario.id = :funcionarioId)))")
    Page<Notificacao> findByFiltrosParaFuncionario(
            @Param("tipoEvento") TipoEventoNotificacao tipoEvento,
            @Param("cargoAlvo") Cargo cargoAlvo,
            @Param("lida") Boolean lida,
            @Param("concluida") Boolean concluida,
            @Param("funcionarioId") UUID funcionarioId,
            Pageable pageable
    );

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE " +
            "(:cargoAlvo IS NULL OR n.cargoAlvo IS NULL OR n.cargoAlvo = :cargoAlvo) AND " +
            "NOT EXISTS (SELECT nl.id FROM NotificacaoLeitura nl WHERE nl.notificacao = n AND nl.funcionario.id = :funcionarioId)")
    long countNotLidasParaFuncionario(
            @Param("cargoAlvo") Cargo cargoAlvo,
            @Param("funcionarioId") UUID funcionarioId
    );

    @Query("SELECT n FROM Notificacao n WHERE " +
            "(:cargoAlvo IS NULL OR n.cargoAlvo IS NULL OR n.cargoAlvo = :cargoAlvo) AND " +
            "NOT EXISTS (SELECT nl.id FROM NotificacaoLeitura nl WHERE nl.notificacao = n AND nl.funcionario.id = :funcionarioId)")
    List<Notificacao> findNotLidasParaFuncionario(
            @Param("cargoAlvo") Cargo cargoAlvo,
            @Param("funcionarioId") UUID funcionarioId
    );

}
