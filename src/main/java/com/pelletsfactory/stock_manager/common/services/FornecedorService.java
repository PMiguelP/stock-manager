package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.request.FornecedorRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Fornecedor;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.EncomendaFornecedorMapper;
import com.pelletsfactory.stock_manager.common.mapper.FornecedorMapper;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaFornecedorRepository;
import com.pelletsfactory.stock_manager.common.repositories.FornecedorRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
import com.pelletsfactory.stock_manager.common.utils.NifUtils;
import com.pelletsfactory.stock_manager.common.utils.PageableUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepo;
    private final EncomendaFornecedorRepository encomendaFornecedorRepo;
    private final FornecedorMapper fornecedorMapper;
    private final EncomendaFornecedorMapper encomendaFornecedorMapper;

    public FornecedorService(FornecedorRepository fornecedorRepo,
                             EncomendaFornecedorRepository encomendaFornecedorRepo,
                             FornecedorMapper fornecedorMapper,
                             EncomendaFornecedorMapper encomendaFornecedorMapper) {
        this.fornecedorRepo = fornecedorRepo;
        this.encomendaFornecedorRepo = encomendaFornecedorRepo;
        this.fornecedorMapper = fornecedorMapper;
        this.encomendaFornecedorMapper = encomendaFornecedorMapper;
    }

    @Transactional
    public FornecedorResponseDTO registarFornecedor(String nome, String nif, String contacto, String email) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.ASSISTENTE_COMERCIAL);

        Fornecedor fornecedor = new Fornecedor();
        String nifNormalizado = NifUtils.normalizeRequired(nif);
        FornecedorRequestDTO dados = new FornecedorRequestDTO(
                nome == null ? null : nome.trim(),
                nifNormalizado,
                contacto == null ? null : contacto.trim(),
                email == null ? null : email.trim()
        );
        if (fornecedorRepo.existsByNif(nifNormalizado)) {
            throw new IllegalArgumentException("Já existe fornecedor com este NIF: " + nifNormalizado);
        }

        fornecedor.setNome(dados.nome());
        fornecedor.setNif(nifNormalizado);
        fornecedor.setContacto(dados.contacto());
        fornecedor.setEmail(dados.email());

        return fornecedorMapper.toResponseDTO(fornecedorRepo.save(fornecedor));
    }

    public Fornecedor buscarFornecedorPorId(UUID id) {
        return fornecedorRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));
    }

    public Page<FornecedorSimpleDTO> listarFornecedoresComFiltros(
            int page,
            int pageSize,
            String nome,
            String nif,
            String sortBy,
            String direction) {

        Pageable pageable = PageableUtils.create(page, pageSize, sortBy, direction, "nome");

        return fornecedorRepo.findByFiltros(nome, NifUtils.normalizeNullable(nif), pageable)
                .map(fornecedorMapper::toSimpleDTO);
    }

    public List<FornecedorSimpleDTO> listarTodosFornecedoresSimples() {
        return fornecedorRepo.findAll().stream()
                .map(fornecedorMapper::toSimpleDTO)
                .toList();
    }

    @Transactional
    public FornecedorDetailsDTO atualizarFornecedor(UUID id, String nome, String nif, String contacto, String email) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR, Cargo.ASSISTENTE_COMERCIAL);
        Fornecedor fornecedor = buscarFornecedorPorId(id);
        String nifNormalizado = NifUtils.normalizeRequired(nif);
        if (!nifNormalizado.equals(fornecedor.getNif()) && fornecedorRepo.existsByNif(nifNormalizado)) {
            throw new IllegalArgumentException("Já existe fornecedor com este NIF: " + nifNormalizado);
        }
        fornecedor.setNome(nome == null ? null : nome.trim());
        fornecedor.setNif(nifNormalizado);
        fornecedor.setContacto(contacto == null ? null : contacto.trim());
        fornecedor.setEmail(email == null ? null : email.trim());
        fornecedorRepo.save(fornecedor);
        return obterDetalhesFornecedor(id);
    }

    @Transactional
    public void apagarFornecedor(UUID id) {
        SecurityUtils.checkPermission(Cargo.ADMINISTRADOR);
        if (!fornecedorRepo.existsById(id)) {
            throw new EntityNotFoundException("Fornecedor não encontrado");
        }
        fornecedorRepo.deleteById(id);
    }

    public FornecedorDetailsDTO obterDetalhesFornecedor(UUID fornecedorId) {
        Fornecedor fornecedor = buscarFornecedorPorId(fornecedorId);

        List<EncomendaFornecedorSimpleDTO> encomendas = encomendaFornecedorRepo.findByFornecedorId(fornecedorId).stream()
                .map(encomendaFornecedorMapper::toSimpleDTO)
                .toList();

        return new FornecedorDetailsDTO(
                fornecedor.getId(),
                fornecedor.getNome(),
                fornecedor.getNif(),
                fornecedor.getContacto(),
                fornecedor.getEmail(),
                encomendas,
                fornecedor.getCreatedAt(),
                fornecedor.getUpdatedAt()
        );
    }

}
