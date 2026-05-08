package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.NotificacaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoResponseDTO;
import com.pelletsfactory.stock_manager.common.entities.Notificacao;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.mapper.NotificacaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.NotificacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service para gerenciamento de Notificações
 * Operações de leitura/escrita para todos os cargos
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepo;
    private final NotificacaoMapper mapper;

    public NotificacaoService(NotificacaoRepository notificacaoRepo, NotificacaoMapper mapper) {
        this.notificacaoRepo = notificacaoRepo;
        this.mapper = mapper;
    }

    /**
     * Criar nova notificação
     */
    @Transactional
    public NotificacaoResponseDTO criarNotificacao(NotificacaoRequestDTO dto) {
        Notificacao notificacao = mapper.toEntity(dto);
        Notificacao saved = notificacaoRepo.save(notificacao);
        return mapper.toResponseDTO(saved);
    }

    /**
     * Marcar como lida
     */
    @Transactional
    public NotificacaoResponseDTO marcarComoLida(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        notificacao.setLida(true);
        Notificacao updated = notificacaoRepo.save(notificacao);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Eliminar notificação
     */
    @Transactional
    public void apagarNotificacao(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        notificacaoRepo.delete(notificacao);
    }

    /**
     * Listar notificações não lidas do utilizador
     */
    public Page<NotificacaoResponseDTO> listarNotLidas(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<Notificacao> notificacoesPage = notificacaoRepo.findNotLidas(pageable);
        return notificacoesPage.map(mapper::toResponseDTO);
    }

    /**
     * Listar notificações não lidas para um cargo específico
     */
    public Page<NotificacaoResponseDTO> listarNotLidasParaCargo(Cargo cargo, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<Notificacao> notificacoesPage = notificacaoRepo.findNotLidasParaCargo(cargo, pageable);
        return notificacoesPage.map(mapper::toResponseDTO);
    }

    /**
     * Listar com filtros
     */
    public Page<NotificacaoResponseDTO> listarNotificacoesComFiltros(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Cargo cargoAlvo,
            Boolean lida,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        Page<Notificacao> notificacoesPage = notificacaoRepo.findByFiltros(tipoEvento, cargoAlvo, lida, pageable);

        return notificacoesPage.map(mapper::toResponseDTO);
    }

    /**
     * Obter por ID
     */
    public NotificacaoResponseDTO buscarPorId(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(notificacao);
    }

    /**
     * Buscar entidade ou falhar
     */
    public Notificacao buscarPorIdOuFalhar(UUID id) {
        return notificacaoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notificação não encontrada com ID: " + id
                ));
    }

    /**
     * Contar notificações não lidas
     */
    public long contarNotLidas() {
        return notificacaoRepo.countByLida(false);
    }
}

