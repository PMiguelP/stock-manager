package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.Cargo;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "funcionarios")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 9)
    private String nif;

    @Column(length = 20)
    private String contacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Cargo cargo;

    @Column(name = "numero_funcionario", unique = true, nullable = false)
    private Integer numeroFuncionario;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @Column(name = "hashed_pin", nullable = false)
    private String pinHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Funcionario() {}

    public Funcionario(String nome, String nif, String contacto, Cargo cargo,
                       Integer numeroFuncionario, LocalDate dataAdmissao, String pinHash) {
        this.nome = nome;
        this.nif = nif;
        this.contacto = contacto;
        this.cargo = cargo;
        this.numeroFuncionario = numeroFuncionario;
        this.dataAdmissao = dataAdmissao;
        this.pinHash = pinHash;
    }

    public UUID getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }

    public Integer getNumeroFuncionario() { return numeroFuncionario; }
    public void setNumeroFuncionario(Integer numeroFuncionario) { this.numeroFuncionario = numeroFuncionario; }

    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }

    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}