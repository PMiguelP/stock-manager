package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.AlocacaoLoteEncomendaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoLoteEncomendaResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaPendenteAlocacaoDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LoteDisponivelAlocacaoDTO;
import com.pelletsfactory.stock_manager.common.entities.AlocacaoLoteEncomenda;
import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.LotePellet;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.repositories.AlocacaoLoteEncomendaRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.LotePelletRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.ValidationUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AlocacaoLoteEncomendaService {

    private final AlocacaoLoteEncomendaRepository alocacaoRepo;
    private final ItemEncomendaClienteRepository itemRepo;
    private final LotePelletRepository loteRepo;

    public AlocacaoLoteEncomendaService(AlocacaoLoteEncomendaRepository alocacaoRepo,
                                        ItemEncomendaClienteRepository itemRepo,
                                        LotePelletRepository loteRepo) {
        this.alocacaoRepo = alocacaoRepo;
        this.itemRepo = itemRepo;
        this.loteRepo = loteRepo;
    }

    /**
     * Reserva uma quantidade física de um lote para um item da encomenda.
     */
    @Transactional
    public AlocacaoLoteEncomendaResponseDTO criarAlocacao(AlocacaoLoteEncomendaRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA, Cargo.RESPONSAVEL_PRODUCAO);

        LotePellet lote = buscarLote(dto.loteId());
        ItemEncomendaCliente item = buscarItem(dto.itemEncomendaId());
        if (alocacaoRepo.existsByLoteIdAndItemEncomendaId(lote.getId(), item.getId())) {
            throw new IllegalArgumentException("Já existe uma alocação deste lote para este item");
        }
        validarReserva(lote, item, dto.quantidadeReservada(), null);

        AlocacaoLoteEncomenda alocacao = new AlocacaoLoteEncomenda();
        alocacao.setLote(lote);
        alocacao.setItemEncomenda(item);
        alocacao.setQuantidadeReservada(dto.quantidadeReservada());
        return toResponse(alocacaoRepo.save(alocacao));
    }

    @Transactional
    public AlocacaoLoteEncomendaResponseDTO atualizarAlocacao(UUID id, Double quantidadeReservada) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        AlocacaoLoteEncomenda alocacao = buscarPorIdOuFalhar(id);
        LotePellet lote = buscarLote(alocacao.getLote().getId());
        ItemEncomendaCliente item = buscarItem(alocacao.getItemEncomenda().getId());
        validarReserva(lote, item, quantidadeReservada, id);
        alocacao.setQuantidadeReservada(quantidadeReservada);
        return toResponse(alocacaoRepo.save(alocacao));
    }

    @Transactional
    public void apagarAlocacao(UUID id) {
        SecurityUtils.checkPermission(Cargo.RESPONSAVEL_LOGISTICA);

        AlocacaoLoteEncomenda alocacao = buscarPorIdOuFalhar(id);
        validarEncomendaEditavel(alocacao.getItemEncomenda().getEncomenda());
        alocacaoRepo.delete(alocacao);
    }

    @Transactional
    public List<ItemEncomendaPendenteAlocacaoDTO> listarItensPendentes(UUID tipoPelletId) {
        Map<UUID, Double> reservadoPorItem = reservedTotals(alocacaoRepo.sumQuantidadeReservadaGroupedByItem());
        return itemRepo.findAllocationCandidates(tipoPelletId, estadosEncomendaSemAlocacao()).stream()
                .map(item -> toPendente(item, reservadoPorItem.getOrDefault(item.getId(), 0.0)))
                .filter(item -> item.quantidadeEmFalta() > 0.000001)
                .sorted(Comparator.comparing(item -> item.encomendaId().toString()))
                .toList();
    }

    @Transactional
    public List<LoteDisponivelAlocacaoDTO> listarLotesDisponiveis(UUID tipoPelletId) {
        Map<UUID, Double> reservadoPorLote = reservedTotals(alocacaoRepo.sumQuantidadeReservadaGroupedByLote());
        return loteRepo.findAllocationCandidates(tipoPelletId).stream()
                .map(lote -> toDisponivel(lote, reservadoPorLote.getOrDefault(lote.getId(), 0.0)))
                .filter(lote -> lote.quantidadeDisponivel() > 0.000001)
                .sorted(Comparator.comparing(LoteDisponivelAlocacaoDTO::dataProducao))
                .toList();
    }

    public List<AlocacaoLoteEncomendaResponseDTO> listarPorEncomenda(UUID encomendaId) {
        return alocacaoRepo.findByItemEncomendaEncomendaId(encomendaId).stream()
                .map(this::toResponse)
                .toList();
    }

    public double obterQuantidadeReservadaDoLote(UUID loteId) {
        return valor(alocacaoRepo.sumQuantidadeReservadaByLoteId(loteId));
    }

    public AlocacaoLoteEncomenda buscarPorIdOuFalhar(UUID id) {
        return alocacaoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alocação não encontrada"));
    }

    private void validarReserva(LotePellet lote,
                                ItemEncomendaCliente item,
                                Double quantidade,
                                UUID alocacaoIgnoradaId) {
        ValidationUtils.requirePositive(quantidade, "Quantidade reservada");
        validarEncomendaEditavel(item.getEncomenda());
        if (!lote.getTipoPellet().getId().equals(item.getTipoPellet().getId())) {
            throw new IllegalArgumentException("O lote e o item da encomenda devem ser do mesmo tipo de pellet");
        }

        double reservadoNoLote = excluirAlocacaoAtual(
                valor(alocacaoRepo.sumQuantidadeReservadaByLoteId(lote.getId())),
                alocacaoIgnoradaId
        );
        if (reservadoNoLote + quantidade > lote.getQuantidadeKg() + 0.000001) {
            throw new IllegalArgumentException("A quantidade reservada excede o saldo disponível do lote");
        }

        double reservadoNoItem = excluirAlocacaoAtual(
                valor(alocacaoRepo.sumQuantidadeReservadaByItemId(item.getId())),
                alocacaoIgnoradaId
        );
        if (reservadoNoItem + quantidade > item.getQuantidadeKg() + 0.000001) {
            throw new IllegalArgumentException("A quantidade reservada excede o que falta fornecer ao cliente");
        }
    }

    private void validarEncomendaEditavel(EncomendaCliente encomenda) {
        if (!encomendaPodeReceberAlocacao(encomenda)) {
            throw new IllegalStateException("A encomenda não pode receber alterações de alocação");
        }
    }

    private boolean encomendaPodeReceberAlocacao(EncomendaCliente encomenda) {
        return !estadosEncomendaSemAlocacao().contains(encomenda.getEstado());
    }

    private EnumSet<EstadoEncomendaCliente> estadosEncomendaSemAlocacao() {
        return EnumSet.of(
                EstadoEncomendaCliente.PENDENTE,
                EstadoEncomendaCliente.CANCELADA,
                EstadoEncomendaCliente.EXPEDIDA
        );
    }

    private ItemEncomendaPendenteAlocacaoDTO toPendente(
            ItemEncomendaCliente item,
            double alocado) {
        List<AlocacaoLoteEncomendaResponseDTO> alocacoes = alocacaoRepo.findByItemEncomendaId(item.getId()).stream()
                .map(this::toResponse)
                .toList();
        return new ItemEncomendaPendenteAlocacaoDTO(
                item.getId(),
                item.getEncomenda().getId(),
                item.getEncomenda().getCliente().getNome(),
                item.getTipoPellet().getId(),
                item.getTipoPellet().getNome(),
                item.getEncomenda().getEstado(),
                item.getQuantidadeKg(),
                alocado,
                item.getQuantidadeKg() - alocado,
                alocacoes
        );
    }

    private Map<UUID, Double> reservedTotals(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> ((Number) row[1]).doubleValue()
        ));
    }

    private LoteDisponivelAlocacaoDTO toDisponivel(LotePellet lote, double reservado) {
        return new LoteDisponivelAlocacaoDTO(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getTipoPellet().getId(),
                lote.getTipoPellet().getNome(),
                lote.getLocalizacaoArmazem(),
                lote.getDataProducao(),
                lote.getQuantidadeKg(),
                reservado,
                lote.getQuantidadeKg() - reservado
        );
    }

    private AlocacaoLoteEncomendaResponseDTO toResponse(AlocacaoLoteEncomenda alocacao) {
        ItemEncomendaCliente item = alocacao.getItemEncomenda();
        return new AlocacaoLoteEncomendaResponseDTO(
                alocacao.getId(),
                alocacao.getLote().getId(),
                alocacao.getLote().getCodigoLote(),
                item.getId(),
                item.getEncomenda().getId(),
                item.getEncomenda().getCliente().getNome(),
                item.getTipoPellet().getNome(),
                alocacao.getQuantidadeReservada()
        );
    }

    private LotePellet buscarLote(UUID id) {
        return loteRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado"));
    }

    private ItemEncomendaCliente buscarItem(UUID id) {
        return itemRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Item da encomenda não encontrado"));
    }

    private double valor(Double quantidade) {
        return quantidade != null ? quantidade : 0.0;
    }

    private double excluirAlocacaoAtual(double reservado, UUID alocacaoIgnoradaId) {
        if (alocacaoIgnoradaId == null) {
            return reservado;
        }
        return reservado - buscarPorIdOuFalhar(alocacaoIgnoradaId).getQuantidadeReservada();
    }
}
