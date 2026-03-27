package com.pelletsfactory.stock_manager.common.utils;

import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;

public class FuncoesAuxiliares {

    private FuncoesAuxiliares() {}

    public static void validarPermissaoAdmin() {
        Funcionario userLogado = SessaoFuncionario.getFuncionarioLogado();

        if (userLogado == null) {
            throw new RuntimeException("Utilizador não autenticado. Por favor, faça login.");
        }

        if (userLogado.getCargo() != Cargo.ADMINISTRADOR) {
            throw new RuntimeException("Acesso negado: Operação exclusiva para Administradores.");
        }
    }

    public static void validarPermissaoTratarFornecedores() {
        Funcionario userLogado = SessaoFuncionario.getFuncionarioLogado();

        if (userLogado == null) {
            throw new RuntimeException("Utilizador não autenticado. Por favor, faça login.");
        }

        if (userLogado.getCargo() != Cargo.ADMINISTRADOR && userLogado.getCargo() != Cargo.RESPONSAVEL_LOGISTICA) {
            throw new RuntimeException("Acesso negado: Operação exclusiva para Administradores e Gestores de Fornecedores.");
        }
    }
}
