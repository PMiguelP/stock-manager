package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.NotificacaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.Notificacao;
import com.pelletsfactory.stock_manager.common.entities.NotificacaoLeitura;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.mapper.NotificacaoMapper;
import com.pelletsfactory.stock_manager.common.repositories.NotificacaoLeituraRepository;
import com.pelletsfactory.stock_manager.common.repositories.NotificacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepo;
    private final NotificacaoLeituraRepository leituraRepo;
    private final NotificacaoMapper mapper;

    public NotificacaoService(NotificacaoRepository notificacaoRepo,
                              NotificacaoLeituraRepository leituraRepo,
                              NotificacaoMapper mapper) {
        this.notificacaoRepo = notificacaoRepo;
        this.leituraRepo = leituraRepo;
        this.mapper = mapper;
    }

    @Transactional
    public NotificacaoResponseDTO criarNotificacao(NotificacaoRequestDTO dto) {
        Notificacao notificacao = mapper.toEntity(dto);
        Notificacao saved = notificacaoRepo.save(notificacao);
        return mapper.toResponseDTO(saved);
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();

        if (funcionario == null) {
            notificacao.setLida(true);
        } else if (!leituraRepo.existsByNotificacaoIdAndFuncionarioId(id, funcionario.getId())) {
            leituraRepo.save(new NotificacaoLeitura(notificacao, funcionario));
        }

        Notificacao updated = notificacaoRepo.save(notificacao);
        return mapper.toResponseDTO(updated, true);
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoConcluida(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();

        if (!Boolean.TRUE.equals(notificacao.getRequerAcao())) {
            throw new IllegalArgumentException("Esta notificação não requer ação.");
        }
        if (funcionario == null) {
            throw new SecurityException("Sessão expirada. Faça login novamente.");
        }

        notificacao.setConcluida(true);
        notificacao.setConcluidaPor(funcionario);
        notificacao.setConcluidaEm(Instant.now());

        if (!leituraRepo.existsByNotificacaoIdAndFuncionarioId(id, funcionario.getId())) {
            leituraRepo.save(new NotificacaoLeitura(notificacao, funcionario));
        }

        Notificacao updated = notificacaoRepo.save(notificacao);
        return mapper.toResponseDTO(updated, true);
    }

    @Transactional
    public void apagarNotificacao(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        notificacaoRepo.delete(notificacao);
    }

    public Page<NotificacaoResponseDTO> listarNotLidas(int page, int pageSize) {
        return listarParaUtilizadorAtual(page, pageSize, null, false, null, "createdAt", "DESC");
    }

    public Page<NotificacaoResponseDTO> listarNotLidasParaCargo(Cargo cargo, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<Notificacao> notificacoesPage = notificacaoRepo.findNotLidasParaCargo(cargo, pageable);
        return notificacoesPage.map(mapper::toResponseDTO);
    }

    public Page<NotificacaoResponseDTO> listarNotificacoesComFiltros(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Cargo cargoAlvo,
            Boolean lida,
            String sortBy,
            String direction) {

        Pageable pageable = criarPageable(page, pageSize, sortBy, direction);
        Page<Notificacao> notificacoesPage = notificacaoRepo.findByFiltros(tipoEvento, cargoAlvo, lida, pageable);
        return notificacoesPage.map(mapper::toResponseDTO);
    }

    public Page<NotificacaoResponseDTO> listarParaUtilizadorAtual(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Boolean lida,
            Boolean concluida,
            String sortBy,
            String direction) {

        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();
        if (funcionario == null) {
            return listarNotificacoesComFiltros(page, pageSize, tipoEvento, null, lida, sortBy, direction);
        }

        Pageable pageable = criarPageable(page, pageSize, sortBy, direction);
        Page<Notificacao> notificacoesPage = notificacaoRepo.findByFiltrosParaFuncionario(
                tipoEvento,
                funcionario.getCargo(),
                lida,
                concluida,
                funcionario.getId(),
                pageable
        );

        return notificacoesPage.map(n -> mapper.toResponseDTO(n, isLidaPorFuncionario(n, funcionario)));
    }

    public NotificacaoResponseDTO buscarPorId(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();
        Boolean lida = funcionario == null
                ? notificacao.getLida()
                : isLidaPorFuncionario(notificacao, funcionario);
        return mapper.toResponseDTO(notificacao, lida);
    }

    public Notificacao buscarPorIdOuFalhar(UUID id) {
        return notificacaoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notificação não encontrada com ID: " + id
                ));
    }

    public long contarNotLidas() {
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();
        if (funcionario == null) {
            return notificacaoRepo.countByLida(false);
        }
        return notificacaoRepo.countNotLidasParaFuncionario(funcionario.getCargo(), funcionario.getId());
    }

    public Page<NotificacaoSimpleDTO> listarNotLidasSimples(int page, int pageSize) {
        return listarParaUtilizadorAtualSimples(page, pageSize, null, false, null, "createdAt", "DESC");
    }

    public Page<NotificacaoSimpleDTO> listarNotLidasParaCargoSimples(Cargo cargo, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<Notificacao> notificacoesPage = notificacaoRepo.findNotLidasParaCargo(cargo, pageable);
        return notificacoesPage.map(mapper::toSimpleDTO);
    }

    public Page<NotificacaoSimpleDTO> listarNotificacoesComFiltrosSimples(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Cargo cargoAlvo,
            Boolean lida,
            String sortBy,
            String direction) {

        Pageable pageable = criarPageable(page, pageSize, sortBy, direction);
        Page<Notificacao> notificacoesPage = notificacaoRepo.findByFiltros(tipoEvento, cargoAlvo, lida, pageable);
        return notificacoesPage.map(mapper::toSimpleDTO);
    }

    public Page<NotificacaoSimpleDTO> listarParaUtilizadorAtualSimples(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Boolean lida,
            Boolean concluida,
            String sortBy,
            String direction) {

        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();
        if (funcionario == null) {
            return listarNotificacoesComFiltrosSimples(page, pageSize, tipoEvento, null, lida, sortBy, direction);
        }

        Pageable pageable = criarPageable(page, pageSize, sortBy, direction);
        Page<Notificacao> notificacoesPage = notificacaoRepo.findByFiltrosParaFuncionario(
                tipoEvento,
                funcionario.getCargo(),
                lida,
                concluida,
                funcionario.getId(),
                pageable
        );

        return notificacoesPage.map(n -> mapper.toSimpleDTO(n, isLidaPorFuncionario(n, funcionario)));
    }

    private Pageable criarPageable(int page, int pageSize, String sortBy, String direction) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));
    }

    private boolean isLidaPorFuncionario(Notificacao notificacao, Funcionario funcionario) {
        return leituraRepo.existsByNotificacaoIdAndFuncionarioId(notificacao.getId(), funcionario.getId());
    }
}
