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

    @Query("SELECT n FROM Notificacao n WHERE n.lida = false ORDER BY n.createdAt DESC")
    Page<Notificacao> findNotLidas(Pageable pageable);

    @Query("SELECT n FROM Notificacao n WHERE n.lida = false AND (:cargoAlvo IS NULL OR n.cargoAlvo = :cargoAlvo) ORDER BY n.createdAt DESC")
    Page<Notificacao> findNotLidasParaCargo(@Param("cargoAlvo") Cargo cargoAlvo, Pageable pageable);

    @Query("SELECT n FROM Notificacao n WHERE " +
            "(:tipoEvento IS NULL OR n.tipoEvento = :tipoEvento) AND " +
            "(:cargoAlvo IS NULL OR n.cargoAlvo = :cargoAlvo) AND " +
            "(:lida IS NULL OR n.lida = :lida)")
    Page<Notificacao> findByFiltros(
            @Param("tipoEvento") TipoEventoNotificacao tipoEvento,
            @Param("cargoAlvo") Cargo cargoAlvo,
            @Param("lida") Boolean lida,
            Pageable pageable
    );

    @Query("SELECT n FROM Notificacao n WHERE n.linkReferencia = :linkReferencia")
    List<Notificacao> findByLinkReferencia(@Param("linkReferencia") UUID linkReferencia);

    long countByLida(Boolean lida);
}

