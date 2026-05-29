package com.pelletsfactory.stock_manager.common.services;

import com.pelletsfactory.stock_manager.common.dto.response.ClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.entities.Cliente;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.mapper.ClienteMapper;
import com.pelletsfactory.stock_manager.common.mapper.EncomendaClienteMapper;
import com.pelletsfactory.stock_manager.common.repositories.ClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.utils.SecurityUtils;
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
public class ClienteService {

    private final ClienteRepository clienteRepo;
    private final EncomendaClienteRepository encomendaClienteRepo;
    private final ClienteMapper clienteMapper;
    private final EncomendaClienteMapper encomendaClienteMapper;

    public ClienteService(ClienteRepository clienteRepo,
                          EncomendaClienteRepository encomendaClienteRepo,
                          ClienteMapper clienteMapper,
                          EncomendaClienteMapper encomendaClienteMapper) {
        this.clienteRepo = clienteRepo;
        this.encomendaClienteRepo = encomendaClienteRepo;
        this.clienteMapper = clienteMapper;
        this.encomendaClienteMapper = encomendaClienteMapper;
    }

    @Transactional
    public ClienteResponseDTO registarCliente(String nome, String nif, String contacto, String email) {
        SecurityUtils.checkPermission(Cargo.ASSISTENTE_COMERCIAL, Cargo.ADMINISTRADOR);

        if (clienteRepo.existsByNif(nif)) {
            throw new IllegalArgumentException("Já existe cliente com este NIF: " + nif);
        }

        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setNif(nif);
        cliente.setContacto(contacto);
        cliente.setEmail(email);

        return clienteMapper.toResponseDTO(clienteRepo.save(cliente));
    }

    public Cliente buscarClientePorId(UUID id) {
        return clienteRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
    }

    public Page<ClienteSimpleDTO> listarClientesComFiltros(
            int page,
            int pageSize,
            String nome,
            String nif,
            String sortBy,
            String direction) {

        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "nome";
        }

        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(dir, sortBy));

        return clienteRepo.findByFiltros(nome, nif, pageable)
                .map(clienteMapper::toSimpleDTO);
    }

    public List<ClienteSimpleDTO> listarTodosClientesSimples() {
        return clienteRepo.findAll().stream()
                .map(clienteMapper::toSimpleDTO)
                .toList();
    }

    public ClienteDetailsDTO obterDetalhesCliente(UUID clienteId) {
        Cliente cliente = buscarClientePorId(clienteId);

        List<EncomendaClienteSimpleDTO> encomendas = encomendaClienteRepo.findByClienteId(clienteId).stream()
                .map(encomendaClienteMapper::toSimpleDTO)
                .toList();

        return new ClienteDetailsDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getNif(),
                cliente.getContacto(),
                cliente.getEmail(),
                encomendas,
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }
}
