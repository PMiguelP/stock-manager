package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.theme.*;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class FuncionarioController {
    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @FXML private TextField txtNome, txtNif, txtContacto, txtNumeroFuncionario;
    @FXML private ComboBox<Cargo> cmbCargo, cmbFiltroCargo;
    @FXML private PasswordField txtPin;
    @FXML private Label lblStatus;
    @FXML private Button btnAtualizar, btnApagar, btnAdicionar;

    @FXML private TableView<Funcionario> tblFuncionarios;
    @FXML private TableColumn<Funcionario, String> colNome, colNif, colContacto;
    @FXML private TableColumn<Funcionario, Integer> colNumero;
    @FXML private TableColumn<Funcionario, Cargo> colCargo;
    @FXML private TableColumn<Funcionario, LocalDate> colDataAdmissao;

    private ObservableList<Funcionario> funcionarios = FXCollections.observableArrayList();
    private Funcionario funcionarioSelecionado;

    @FXML
    public void initialize() {
        configurarComboBoxes();
        configurarTabela();
        carregarFuncionarios();

        btnAtualizar.setDisable(true);
        btnApagar.setDisable(true);

        tblFuncionarios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                funcionarioSelecionado = newVal;
                preencherFormulario(newVal);
                btnAtualizar.setDisable(false);
                btnApagar.setDisable(false);
            }
        });
    }

    // ========== MÉTODOS DE TEMA ATLANTAFX ==========
    @FXML private void changeToPrimerLight() { Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()); }
    @FXML private void changeToPrimerDark() { Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet()); }
    @FXML private void changeToNordLight() { Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet()); }
    @FXML private void changeToNordDark() { Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet()); }
    @FXML private void changeToCupertinoLight() { Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet()); }
    @FXML private void changeToCupertinoDark() { Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet()); }
    @FXML private void changeToDracula() { Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet()); }

    // ========== RESTO DA LÓGICA ==========
    private void configurarComboBoxes() {
        cmbCargo.setItems(FXCollections.observableArrayList(Cargo.values()));
        cmbFiltroCargo.setItems(FXCollections.observableArrayList(Cargo.values()));
    }

    private void configurarTabela() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroFuncionario"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colDataAdmissao.setCellValueFactory(new PropertyValueFactory<>("dataAdmissao"));
        tblFuncionarios.setItems(funcionarios);
    }

    private void carregarFuncionarios() {
        try {
            List<Funcionario> lista = funcionarioService.listarOperadores();
            funcionarios.setAll(lista);
            lblStatus.setText("Total: " + funcionarios.size() + " funcionários");
        } catch (Exception e) {
            lblStatus.setText("Erro ao carregar dados");
        }
    }

    @FXML private void handleAdicionar() {
        if (!validarCampos()) return;
        try {
            Funcionario f = new Funcionario();
            f.setNome(txtNome.getText());
            f.setNif(txtNif.getText());
            f.setContacto(txtContacto.getText());
            f.setNumeroFuncionario(Integer.parseInt(txtNumeroFuncionario.getText()));
            f.setCargo(cmbCargo.getValue());
            f.setDataAdmissao(LocalDate.now());

            funcionarioService.adicionarFuncionario(f);
            carregarFuncionarios();
            limparFormulario();
            mostrarSucesso("Adicionado!");
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    @FXML private void handleAtualizar() { /* Tua lógica de atualizar */ }
    @FXML private void handleApagar() { /* Tua lógica de apagar */ }
    @FXML private void handleLimpar() { limparFormulario(); }
    @FXML private void handleFiltrar() { /* Filtro */ }
    @FXML private void handleMostrarTodos() { carregarFuncionarios(); }

    private void preencherFormulario(Funcionario f) {
        txtNome.setText(f.getNome());
        txtNif.setText(f.getNif());
        txtContacto.setText(f.getContacto());
        txtNumeroFuncionario.setText(String.valueOf(f.getNumeroFuncionario()));
        cmbCargo.setValue(f.getCargo());
    }

    private void limparFormulario() {
        txtNome.clear(); txtNif.clear(); txtContacto.clear(); txtNumeroFuncionario.clear(); txtPin.clear();
        tblFuncionarios.getSelectionModel().clearSelection();
        btnAtualizar.setDisable(true); btnApagar.setDisable(true);
    }

    private boolean validarCampos() { return !txtNome.getText().isEmpty() && cmbCargo.getValue() != null; }
    private void mostrarSucesso(String m) { lblStatus.setText(m); lblStatus.setStyle("-fx-text-fill: green;"); }
    private void mostrarErro(String m) { lblStatus.setText(m); lblStatus.setStyle("-fx-text-fill: red;"); }
}