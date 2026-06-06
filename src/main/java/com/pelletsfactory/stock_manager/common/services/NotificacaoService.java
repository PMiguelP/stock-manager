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
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;

import java.time.Instant;
import java.util.List;
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
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);
        return guardarNotificacao(dto);
    }

    /**
     * Regista avisos gerados por regras internas, sem depender de uma sessão desktop.
     */
    public NotificacaoResponseDTO criarNotificacaoAutomatica(NotificacaoRequestDTO dto) {
        return guardarNotificacao(dto);
    }

    private NotificacaoResponseDTO guardarNotificacao(NotificacaoRequestDTO dto) {
        Notificacao notificacao = mapper.toEntity(dto);
        Notificacao saved = notificacaoRepo.save(notificacao);
        return mapper.toResponseDTO(saved);
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        Funcionario funcionario = obterFuncionarioAutenticado();

        if (!leituraRepo.existsByNotificacaoIdAndFuncionarioId(id, funcionario.getId())) {
            leituraRepo.save(new NotificacaoLeitura(notificacao, funcionario));
        }

        return mapper.toResponseDTO(notificacao, true);
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoConcluida(UUID id) {
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();

        if (!Boolean.TRUE.equals(notificacao.getRequerAcao())) {
            throw new IllegalArgumentException("Esta notificação não requer ação.");
        }
        if (Boolean.TRUE.equals(notificacao.getConcluida())) {
            throw new IllegalStateException("Esta notificação já foi concluída.");
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
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);
        Notificacao notificacao = buscarPorIdOuFalhar(id);
        notificacaoRepo.delete(notificacao);
    }

    public Page<NotificacaoResponseDTO> listarNotLidas(int page, int pageSize) {
        return listarParaUtilizadorAtual(page, pageSize, null, false, null, "createdAt", "DESC");
    }

    public Page<NotificacaoResponseDTO> listarParaUtilizadorAtual(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Boolean lida,
            Boolean concluida,
            String sortBy,
            String direction) {

        Funcionario funcionario = obterFuncionarioAutenticado();

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
        Funcionario funcionario = obterFuncionarioAutenticado();
        Boolean lida = isLidaPorFuncionario(notificacao, funcionario);
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
            return 0;
        }
        return notificacaoRepo.countNotLidasParaFuncionario(funcionario.getCargo(), funcionario.getId());
    }

    @Transactional
    public void marcarTodasComoLidas() {
        Funcionario funcionario = obterFuncionarioAutenticado();
        List<NotificacaoLeitura> leituras = notificacaoRepo
                .findNotLidasParaFuncionario(funcionario.getCargo(), funcionario.getId())
                .stream()
                .map(notificacao -> new NotificacaoLeitura(notificacao, funcionario))
                .toList();
        leituraRepo.saveAll(leituras);
    }

    public Page<NotificacaoSimpleDTO> listarNotLidasSimples(int page, int pageSize) {
        return listarParaUtilizadorAtualSimples(page, pageSize, null, false, null, "createdAt", "DESC");
    }

    public Page<NotificacaoSimpleDTO> listarParaUtilizadorAtualSimples(
            int page,
            int pageSize,
            TipoEventoNotificacao tipoEvento,
            Boolean lida,
            Boolean concluida,
            String sortBy,
            String direction) {

        Funcionario funcionario = obterFuncionarioAutenticado();

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
        return PageableUtils.create(page, pageSize, sortBy, direction, "createdAt");
    }

    private boolean isLidaPorFuncionario(Notificacao notificacao, Funcionario funcionario) {
        return leituraRepo.existsByNotificacaoIdAndFuncionarioId(notificacao.getId(), funcionario.getId());
    }

    private Funcionario obterFuncionarioAutenticado() {
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();
        if (funcionario == null) {
            throw new SecurityException("Sessão expirada. Faça login novamente.");
        }
        return funcionario;
    }
}
