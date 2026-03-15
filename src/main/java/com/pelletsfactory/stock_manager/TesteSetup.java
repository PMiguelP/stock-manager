package com.pelletsfactory.stock_manager;

import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.repositories.FuncionarioRepository;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class TesteSetup {

    @Bean
    CommandLineRunner testeCriacao(FuncionarioService funcionarioService, FuncionarioRepository repo) {
        return args -> {
            System.out.println("\n==========================================");
            System.out.println("   EXECUTANDO TESTE DE BASE DE DADOS");
            System.out.println("==========================================");

            try {
                // 1. Limpar registos
                repo.deleteAll();
                System.out.println("-> Base de dados limpa.");

                // 2. Criar Admin (O "Primeiro" - Numero 999)
                Funcionario admin = new Funcionario();
                admin.setNome("Miguel Admin");
                admin.setNif("999999999");
                admin.setContacto("910000000");
                admin.setCargo(Cargo.ADMINISTRADOR);
                admin.setDataAdmissao(LocalDate.now());
                admin.setNumeroFuncionario(999); // IMPORTANTE: Define manual para o admin inicial
                admin.setPinHash(org.mindrot.jbcrypt.BCrypt.hashpw("1234", org.mindrot.jbcrypt.BCrypt.gensalt()));

                repo.save(admin);
                SessaoFuncionario.login(admin);
                System.out.println("-> Admin (999) criado e logado.");

                Funcionario novo = new Funcionario();
                novo.setNome("Joao Silva");
                novo.setNif("123456789");
                novo.setContacto("912345678");
                novo.setCargo(Cargo.OPERADOR_PRODUCAO);

                Funcionario criado = funcionarioService.adicionarFuncionario(novo);

                System.out.println("-> SUCESSO!");
                System.out.println("-> Criado: " + criado.getNome() + " | Nº: " + criado.getNumeroFuncionario());

                List<Funcionario> todos = funcionarioService.listarTodos();
                System.out.println("-> Total na BD: " + todos.size());
                System.out.println("==========================================\n");

            } catch (Exception e) {
                System.err.println("-> ERRO NO TESTE: " + e.getMessage());
            }
        };
    }
}