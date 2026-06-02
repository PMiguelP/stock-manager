package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.request.MoedaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Moeda;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.MoedaMapper;
import com.pelletsfactory.stock_manager.common.repositories.MoedaRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service para gerenciamento de Moedas
 * Operações restritas a ADMINISTRADOR
 */
@Service
public class MoedaService {

    private final MoedaRepository moedaRepo;
    private final MoedaMapper mapper;

    public MoedaService(MoedaRepository moedaRepo, MoedaMapper mapper) {
        this.moedaRepo = moedaRepo;
        this.mapper = mapper;
    }

    @Transactional
    public MoedaResponseDTO criarMoeda(MoedaRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        if (moedaRepo.existsByCodigo(dto.codigo())) {
            throw new RuntimeException("Já existe uma moeda com o código: " + dto.codigo());
        }

        Moeda moeda = mapper.toEntity(dto);
        Moeda saved = moedaRepo.save(moeda);
        return mapper.toResponseDTO(saved);
    }

    @Transactional
    public MoedaResponseDTO atualizarMoeda(UUID id, MoedaRequestDTO dto) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        Moeda moeda = buscarPorIdOuFalhar(id);

        // Validar código único (se mudou)
        if (!moeda.getCodigo().equals(dto.codigo()) && moedaRepo.existsByCodigo(dto.codigo())) {
            throw new RuntimeException("Código já existe: " + dto.codigo());
        }

        mapper.updateEntityFromDTO(dto, moeda);
        Moeda updated = moedaRepo.save(moeda);
        return mapper.toResponseDTO(updated);
    }

    @Transactional
    public void apagarMoeda(UUID id) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);

        Moeda moeda = buscarPorIdOuFalhar(id);
        try {
            moedaRepo.delete(moeda);
            moedaRepo.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Não é possível eliminar uma moeda que está em utilização", exception);
        }
    }

    public Page<MoedaResponseDTO> listarMoedasComFiltros(
            int page,
            int pageSize,
            String codigo,
            String simbolo,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "codigo");

        Page<Moeda> moedasPage = moedaRepo.findByFiltros(codigo, simbolo, pageable);

        return moedasPage.map(mapper::toResponseDTO);
    }

    /**
     * Listar moedas com filtros (SimpleDTO)
     */
    public Page<MoedaSimpleDTO> listarMoedasComFiltrosSimples(
            int page,
            int pageSize,
            String codigo,
            String simbolo,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "codigo");

        Page<Moeda> moedasPage = moedaRepo.findByFiltros(codigo, simbolo, pageable);

        return moedasPage.map(mapper::toSimpleDTO);
    }

    /**
     * Listar todos (para combobox)
     */
    public java.util.List<MoedaResponseDTO> listarTodosSimples() {
        return moedaRepo.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Listar todos (SimpleDTO)
     */
    public java.util.List<MoedaSimpleDTO> listarTodosSimplesDTO() {
        return moedaRepo.findAll()
                .stream()
                .map(mapper::toSimpleDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Verifica se código já existe
     */
    public boolean existeByCodigo(String codigo) {
        return moedaRepo.existsByCodigo(codigo);
    }

    public MoedaResponseDTO buscarPorId(UUID id) {
        Moeda moeda = buscarPorIdOuFalhar(id);
        return mapper.toResponseDTO(moeda);
    }

    public MoedaResponseDTO buscarPorCodigo(String codigo) {
        Moeda moeda = moedaRepo.findByCodigo(codigo)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Moeda não encontrada com código: " + codigo
                ));
        return mapper.toResponseDTO(moeda);
    }

    public Moeda buscarPorIdOuFalhar(UUID id) {
        return moedaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Moeda não encontrada com ID: " + id
                ));
    }
}
