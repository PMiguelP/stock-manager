package com.pelletsfactory.stock_manager.common.entities;

import com.pelletsfactory.stock_manager.common.enums.Cargo;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
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

    @Column(nullable = false, length = 20)
    private String contacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Cargo cargo;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroFuncionario;

    @Column(nullable = false)
    private Instant dataAdmissao;

    @Column(nullable = false, length = 60)
    private String pinHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Funcionario() {
    }

    public Funcionario(UUID id, String nome, String nif, String contacto, Cargo cargo, String numeroFuncionario, Instant dataAdmissao, String pinHash, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.nome = nome;
        this.nif = nif;
        this.contacto = contacto;
        this.cargo = cargo;
        this.numeroFuncionario = numeroFuncionario;
        this.dataAdmissao = dataAdmissao;
        this.pinHash = pinHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public String getNumeroFuncionario() {
        return numeroFuncionario;
    }

    public void setNumeroFuncionario(String numeroFuncionario) {
        this.numeroFuncionario = numeroFuncionario;
    }

    public Instant getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(Instant dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}