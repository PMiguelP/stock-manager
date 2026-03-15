package com.pelletsfactory.stock_manager.desktop.controllers;

import atlantafx.base.controls.ModalPane;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
@Component
public class FuncionarioController {
    private final FuncionarioService funcionarioService;

    @FXML private ComboBox<Cargo> cmbFiltroCargo;
    @FXML private ModalPane modalPane;
    @FXML private TableView<Funcionario> tblFuncionarios;
    @FXML private VBox vboxContainer;

    @FXML private TableColumn<Funcionario, String> colNome, colNif, colContacto;
    @FXML private TableColumn<Funcionario, Integer> colNumero;
    @FXML private TableColumn<Funcionario, Cargo> colCargo;
    @FXML private TableColumn<Funcionario, LocalDate> colDataAdmissao;

    //TODO atualizar os campos para a criacao do funcionario temos que retirar password vai gerar automaticamente
    private TextField txtNome, txtNif, txtContacto, txtNumeroFuncionario;
    private ComboBox<Cargo> cmbCargo;
    private PasswordField txtPin;
    private VBox drawerRoot;

    // Paginação
    private Pagination pagination;
    private ComboBox<Integer> cmbItemsPerPage;
    private Label lblPaginaStatus;
    private int itemsPerPage = 10;
    private ObservableList<Funcionario> todosFuncionarios = FXCollections.observableArrayList();
    private ObservableList<Funcionario> funcionarios = FXCollections.observableArrayList();

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarComboBoxes();
        configurarDrawerLateral();
        carregarFuncionarios();
    }

    private void configurarTabela() {
        if (colNome == null || colNumero == null) return;
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroFuncionario"));
        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        colDataAdmissao.setCellValueFactory(new PropertyValueFactory<>("dataAdmissao"));
        tblFuncionarios.setItems(funcionarios);
    }

    private void configurarPaginacao(VBox container) {
        HBox paginationBox = new HBox(20);
        paginationBox.setAlignment(Pos.CENTER_LEFT);
        paginationBox.setPadding(new Insets(20, 0, 10, 0));

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setPrefWidth(100);
        cmbItemsPerPage.setOnAction(e -> {
            itemsPerPage = cmbItemsPerPage.getValue();
            atualizarPaginacao();
        });

        Label lblPerPage = new Label("Por página");
        lblPerPage.getStyleClass().add("text-muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        pagination = new Pagination();
        pagination.setMaxPageIndicatorCount(5);
        pagination.setPageFactory(pageIndex -> {
            atualizarTabelaPaginada(pageIndex);
            return new StackPane();
        });

        HBox itemsPerPageBox = new HBox(10, cmbItemsPerPage, lblPerPage);
        itemsPerPageBox.setAlignment(Pos.CENTER_LEFT);

        paginationBox.getChildren().addAll(lblPaginaStatus, spacer, itemsPerPageBox, pagination);

        pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            atualizarLabelStatus();
        });

        container.getChildren().add(paginationBox);
    }

    private void atualizarLabelStatus() {
        if (lblPaginaStatus != null && pagination != null) {
            int inicio = pagination.getCurrentPageIndex() * itemsPerPage + 1;
            int fim = Math.min((pagination.getCurrentPageIndex() + 1) * itemsPerPage, todosFuncionarios.size());
            lblPaginaStatus.setText(String.format("Showing %d to %d of %d results", inicio, fim, todosFuncionarios.size()));
        }
    }

    private void atualizarPaginacao() {
        int totalPages = (int) Math.ceil((double) todosFuncionarios.size() / itemsPerPage);
        pagination.setPageCount(Math.max(1, totalPages));
        pagination.setCurrentPageIndex(0);
        atualizarLabelStatus();
    }

    private void atualizarTabelaPaginada(int pageIndex) {
        int fromIndex = pageIndex * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, todosFuncionarios.size());

        if (fromIndex <= todosFuncionarios.size()) {
            funcionarios.setAll(todosFuncionarios.subList(fromIndex, toIndex));
        }
        atualizarLabelStatus();
    }

    private void configurarDrawerLateral() {
        drawerRoot = new VBox(0);
        drawerRoot.setPrefWidth(450);
        drawerRoot.setMinWidth(450);
        drawerRoot.setMaxWidth(450);
        drawerRoot.setMaxHeight(Double.MAX_VALUE);
        drawerRoot.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(25));
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Adicionar Funcionário");
        titulo.getStyleClass().add("title-3");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnFechar = new Button();
        btnFechar.getStyleClass().addAll("button-icon", "button-flat");
        btnFechar.setGraphic(new FontIcon("mdi2c-close:22"));
        btnFechar.setOnAction(e -> modalPane.hide(true));
        header.getChildren().addAll(titulo, spacer, btnFechar);

        VBox formContent = new VBox(20);
        formContent.setPadding(new Insets(30));
        txtNome = new TextField();
        cmbCargo = new ComboBox<>(FXCollections.observableArrayList(Cargo.values()));
        cmbCargo.setMaxWidth(Double.MAX_VALUE);
        txtNif = new TextField();
        txtContacto = new TextField();
        txtNumeroFuncionario = new TextField();
        txtPin = new PasswordField();

        formContent.getChildren().addAll(
                new VBox(8, new Label("Nome Completo"), txtNome),
                new VBox(8, new Label("Cargo"), cmbCargo),
                new VBox(8, new Label("NIF"), txtNif),
                new VBox(8, new Label("Telemóvel"), txtContacto),
                new Separator(),
                new VBox(8, new Label("Número Interno"), txtNumeroFuncionario),
                new VBox(8, new Label("PIN de Segurança"), txtPin)
        );

        ScrollPane scroll = new ScrollPane(formContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox footer = new HBox(15);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnSalvar = new Button("Guardar Funcionário");
        btnSalvar.getStyleClass().addAll("accent", "success");
        btnSalvar.setPrefHeight(40);
        btnSalvar.setOnAction(e -> handleAdicionar());
        footer.getChildren().add(btnSalvar);

        drawerRoot.getChildren().addAll(header, scroll, footer);
    }

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        modalPane.setAlignment(Pos.CENTER_RIGHT);
        modalPane.usePredefinedTransitionFactories(Side.RIGHT);
        modalPane.show(drawerRoot);
    }

    private void handleAdicionar() {
        if (txtNome.getText().isEmpty() || cmbCargo.getValue() == null) {
            mostrarErro("Preencha os campos obrigatórios!");
            return;
        }
        try {
            Funcionario f = new Funcionario();
            f.setNome(txtNome.getText());
            f.setCargo(cmbCargo.getValue());
            f.setNif(txtNif.getText());
            f.setContacto(txtContacto.getText());
            f.setNumeroFuncionario(Integer.parseInt(txtNumeroFuncionario.getText()));
            f.setDataAdmissao(LocalDate.now());

            funcionarioService.adicionarFuncionario(f);
            carregarFuncionarios();
            modalPane.hide(true);
            mostrarSucesso("Funcionário salvo!");
        } catch (Exception e) {
            mostrarErro("Erro: " + e.getMessage());
        }
    }

    private void carregarFuncionarios() {
        try {
            List<Funcionario> lista = funcionarioService.listarTodos();
            todosFuncionarios.setAll(lista);

            if (pagination == null) {
                configurarPaginacao(vboxContainer);
            }

            atualizarPaginacao();

        } catch (Exception e) {
            System.err.println("Erro ao carregar lista: " + e.getMessage());
        }
    }

    private void configurarComboBoxes() {
        if (cmbFiltroCargo != null) {
            cmbFiltroCargo.setItems(FXCollections.observableArrayList(Cargo.values()));
        }
    }

    private void limparFormulario() {
        txtNome.clear();
        txtNif.clear();
        txtContacto.clear();
        txtNumeroFuncionario.clear();
        txtPin.clear();
        cmbCargo.setValue(null);
    }

    private void mostrarSucesso(String m) {
        if (lblPaginaStatus != null) {
            String oldText = lblPaginaStatus.getText();
            lblPaginaStatus.setText(m);
            lblPaginaStatus.setStyle("-fx-text-fill: -color-success-fg;");

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        lblPaginaStatus.setText(oldText);
                        lblPaginaStatus.setStyle("");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void mostrarErro(String m) {
        if (lblPaginaStatus != null) {
            String oldText = lblPaginaStatus.getText();
            lblPaginaStatus.setText(m);
            lblPaginaStatus.setStyle("-fx-text-fill: -color-danger-fg;");

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        lblPaginaStatus.setText(oldText);
                        lblPaginaStatus.setStyle("");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleFiltrar() {
        // TODO: Implementar filtro por cargo
        //TODO: Implementar outros filtros
    }

    @FXML
    private void handleMostrarTodos() {
        carregarFuncionarios();
    }
}
