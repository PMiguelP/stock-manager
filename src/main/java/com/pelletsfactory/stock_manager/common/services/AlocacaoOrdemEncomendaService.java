package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.AlocacaoOrdemEncomendaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoOrdemEncomendaResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoOrdemEncomendaSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.AlocacaoOrdemEncomenda;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.OrdemProducao;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.AlocacaoOrdemEncomendaMapper;
import com.pelletsfactory.stock_manager.common.repositories.AlocacaoOrdemEncomendaRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.OrdemProducaoRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de Alocações de Ordens a Encomendas
 * Realiza a ponte entre produção e vendas
 */
@Service
public class AlocacaoOrdemEncomendaService {

    private final AlocacaoOrdemEncomendaRepository alocacaoRepo;
    private final OrdemProducaoRepository ordemRepo;
    private final EncomendaClienteRepository encomendaRepo;
    private final AlocacaoOrdemEncomendaMapper mapper;

    public AlocacaoOrdemEncomendaService(
            AlocacaoOrdemEncomendaRepository alocacaoRepo,
            OrdemProducaoRepository ordemRepo,
            EncomendaClienteRepository encomendaRepo,
            AlocacaoOrdemEncomendaMapper mapper) {
        this.alocacaoRepo = alocacaoRepo;
        this.ordemRepo = ordemRepo;
        this.encomendaRepo = encomendaRepo;
        this.mapper = mapper;
    }

    /**
     * Criar alocação (reservar quantidade de ordem para encomenda)
     */
    @Transactional
    public AlocacaoOrdemEncomendaResponseDTO criarAlocacao(AlocacaoOrdemEncomendaRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA, Cargo.RESPONSAVEL_PRODUCAO);

        // Buscar ordem e encomenda
        OrdemProducao ordem = ordemRepo.findById(dto.ordemId())
                .orElseThrow(() -> new EntityNotFoundException("Ordem não encontrada"));

        EncomendaCliente encomenda = encomendaRepo.findById(dto.encomendaClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada"));

        // Validar que os tipos de pellet correspondem
        if (!ordem.getTipoPellet().getId().equals(encomenda.getItens().stream()
                .findFirst()
                .map(item -> item.getTipoPellet().getId())
                .orElse(null))) {
            throw new RuntimeException(
                    "Tipo de pellet da ordem não corresponde aos tipos da encomenda"
            );
        }

        // Validar quantidade disponível
        Double quantidadeDisponivel = ordem.getQuantidadeProduzidaReal() != null ?
                ordem.getQuantidadeProduzidaReal() : 0.0;

        Double quantidadeReservada = alocacaoRepo.sumQuantidadeReservadaByOrdemId(dto.ordemId());
        if (quantidadeReservada == null) quantidadeReservada = 0.0;

        if (quantidadeDisponivel - quantidadeReservada < dto.quantidadeReservada()) {
            throw new RuntimeException(
                    "Quantidade insuficiente. Disponível: " + (quantidadeDisponivel - quantidadeReservada) + "kg"
            );
        }

        // Criar alocação
        AlocacaoOrdemEncomenda alocacao = mapper.toEntity(dto);
        alocacao.setOrdem(ordem);
        alocacao.setEncomendaCliente(encomenda);

        AlocacaoOrdemEncomenda saved = alocacaoRepo.save(alocacao);
        return mapper.toResponseDTO(saved);
    }

    /**
     * Atualizar alocação (mudar quantidade reservada)
     */
    @Transactional
    public AlocacaoOrdemEncomendaResponseDTO atualizarAlocacao(UUID id, AlocacaoOrdemEncomendaRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        AlocacaoOrdemEncomenda alocacao = buscarPorIdOuFalhar(id);

        mapper.updateEntityFromDTO(dto, alocacao);

        AlocacaoOrdemEncomenda updated = alocacaoRepo.save(alocacao);
        return mapper.toResponseDTO(updated);
    }

    /**
     * Eliminar alocação (liberar quantidade)
     */
    @Transactional
    public void apagarAlocacao(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        AlocacaoOrdemEncomenda alocacao = buscarPorIdOuFalhar(id);
        alocacaoRepo.delete(alocacao);
    }

    /**
     * Listar alocações de uma ordem
     */
    public List<AlocacaoOrdemEncomendaResponseDTO> listarAlocacoesDeOrdem(UUID ordemId) {
        return alocacaoRepo.findByOrdemId(ordemId).stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar alocações de uma ordem (paginado)
     */
    public Page<AlocacaoOrdemEncomendaResponseDTO> listarAlocacoesDeOrdemPaginado(UUID ordemId, int page, int pageSize) {
        return listarAlocacoesDeOrdemPaginado(ordemId, page, pageSize, "createdAt", "DESC");
    }

    /**
     * Listar alocações de uma ordem (paginado e ordenado)
     */
    public Page<AlocacaoOrdemEncomendaResponseDTO> listarAlocacoesDeOrdemPaginado(
            UUID ordemId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
        Page<AlocacaoOrdemEncomenda> alocacoesPage = alocacaoRepo.findByOrdemId(ordemId, pageable);
        return alocacoesPage.map(mapper::toResponseDTO);
    }

    /**
     * Listar alocações de uma encomenda
     */
    public List<AlocacaoOrdemEncomendaResponseDTO> listarAlocacoesDeEncomenda(UUID encomendaClienteId) {
        return alocacaoRepo.findByEncomendaClienteId(encomendaClienteId).stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar alocações de uma encomenda (paginado)
     */
    public Page<AlocacaoOrdemEncomendaResponseDTO> listarAlocacoesDeEncomendaPaginado(UUID encomendaClienteId, int page, int pageSize) {
        return listarAlocacoesDeEncomendaPaginado(encomendaClienteId, page, pageSize, "createdAt", "DESC");
    }

    /**
     * Listar alocações de uma encomenda (paginado e ordenado)
     */
    public Page<AlocacaoOrdemEncomendaResponseDTO> listarAlocacoesDeEncomendaPaginado(
            UUID encomendaClienteId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
        Page<AlocacaoOrdemEncomenda> alocacoesPage = alocacaoRepo.findByEncomendaClienteId(encomendaClienteId, pageable);
        return alocacoesPage.map(mapper::toResponseDTO);
    }

    /**
     * Listar alocações de uma ordem (paginado e simples)
     */
    public Page<AlocacaoOrdemEncomendaSimpleDTO> listarAlocacoesDeOrdemSimples(UUID ordemId, int page, int pageSize) {
        return listarAlocacoesDeOrdemSimples(ordemId, page, pageSize, "createdAt", "DESC");
    }

    /**
     * Listar alocações de uma ordem (paginado, ordenado e simples)
     */
    public Page<AlocacaoOrdemEncomendaSimpleDTO> listarAlocacoesDeOrdemSimples(
            UUID ordemId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
        Page<AlocacaoOrdemEncomenda> alocacoesPage = alocacaoRepo.findByOrdemId(ordemId, pageable);
        return alocacoesPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar alocações de uma encomenda (paginado e simples)
     */
    public Page<AlocacaoOrdemEncomendaSimpleDTO> listarAlocacoesDeEncomendaSimples(UUID encomendaClienteId, int page, int pageSize) {
        return listarAlocacoesDeEncomendaSimples(encomendaClienteId, page, pageSize, "createdAt", "DESC");
    }

    /**
     * Listar alocações de uma encomenda (paginado, ordenado e simples)
     */
    public Page<AlocacaoOrdemEncomendaSimpleDTO> listarAlocacoesDeEncomendaSimples(
            UUID encomendaClienteId,
            int page,
            int pageSize,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "createdAt";
        }

        org.springframework.data.domain.Sort.Direction dir = "ASC".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(dir, sortBy));
        Page<AlocacaoOrdemEncomenda> alocacoesPage = alocacaoRepo.findByEncomendaClienteId(encomendaClienteId, pageable);
        return alocacoesPage.map(mapper::toSimpleDTO);
    }

    /**
     * Obter quantidade total reservada para uma ordem
     */
    public Double obterQuantidadeReservadaDeOrdem(UUID ordemId) {
        Double total = alocacaoRepo.sumQuantidadeReservadaByOrdemId(ordemId);
        return total != null ? total : 0.0;
    }

    /**
     * Obter quantidade total reservada de uma encomenda
     */
    public Double obterQuantidadeReservadaDeEncomenda(UUID encomendaClienteId) {
        Double total = alocacaoRepo.sumQuantidadeReservadaByEncomendaClienteId(encomendaClienteId);
        return total != null ? total : 0.0;
    }

    /**
     * Contar alocações de uma ordem
     */
    public long contarAlocacoesDeOrdem(UUID ordemId) {
        return alocacaoRepo.findByOrdemId(ordemId).size();
    }

    /**
     * Obter por ID
     */
    public AlocacaoOrdemEncomendaResponseDTO buscarPorId(UUID id) {
        AlocacaoOrdemEncomenda alocacao = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(alocacao);
    }

    /**
     * Buscar entidade ou falhar
     */
    public AlocacaoOrdemEncomenda buscarPorIdOuFalhar(UUID id) {
        return alocacaoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Alocação não encontrada com ID: " + id
                ));
    }
}
