package com.pelletsfactory.stock_manager.common.utils;

import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;

public class SecurityUtils {

    public static void checkPermission(Cargo... authorizedRoles) {
        Funcionario logged = SessaoFuncionario.getFuncionarioLogado();

        if (logged == null) {
            throw new SecurityException("Sessão expirada. Faça login novamente.");
        }

        if (logged.getCargo() == Cargo.ADMINISTRADOR) {
            return;
        }

        boolean hasAccess = false;
        for (Cargo role : authorizedRoles) {
            if (logged.getCargo() == role) {
                hasAccess = true;
                break;
            }
        }

        if (!hasAccess) {
            throw new SecurityException(
                    "O seu cargo (" + logged.getCargo() + ") não tem permissão para esta ação."
            );
        }
    }
}