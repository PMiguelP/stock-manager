package com.pelletsfactory.stock_manager.common.entities;

public class SessaoFuncionario {

    private static Funcionario funcionarioLogado;

    private SessaoFuncionario() {
    }

    public static void login(Funcionario funcionario) {
        funcionarioLogado = funcionario;
    }

    public static void logout() {
        funcionarioLogado = null;
    }

    public static Funcionario getFuncionarioLogado() {
        return funcionarioLogado;
    }

    public static boolean isAutenticado() {
        return funcionarioLogado != null;
    }
}
