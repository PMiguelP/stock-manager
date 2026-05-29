package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.FornecedorDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.FornecedorService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class SuppliersController {

    private final FornecedorService fornecedorService;
    private final NavigationService navigationService;
    private final ToastService toastService;

    @FXML private VBox vboxContainer;
    @FXML private TextField txtSearch;
    @FXML private TableView<FornecedorSimpleDTO> tblSuppliers;
    @FXML private TableColumn<FornecedorSimpleDTO, Void> colSupplierID;
    @FXML private TableColumn<FornecedorSimpleDTO, String> colName;
    @FXML private TableColumn<FornecedorSimpleDTO, String> colTaxId;
    @FXML private TableColumn<FornecedorSimpleDTO, String> colContactPerson;
    @FXML private TableColumn<FornecedorSimpleDTO, Void> colActions;

    private VBox drawerAdicionar;
    private TextField txtNomeAdicionar, txtNifAdicionar, txtContactoAdicionar, txtEmailAdicionar;

    private PaginationControls pagination;

    private final ObservableList<FornecedorSimpleDTO> fornecedores = FXCollections.observableArrayList();

    public SuppliersController(FornecedorService fornecedorService, NavigationService navigationService,
                               ToastService toastService) {
        this.fornecedorService = fornecedorService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarFornecedores);
        configurarTabela();
        configurarDrawerAdicionar();
        carregarFornecedores();
    }

    private void configurarTabela() {
        colSupplierID.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setText(null);
                } else {
                    int seq = pagination.currentPage() * pagination.pageSize() + getIndex() + 1;
                    setText(String.format("SUP-%03d", seq));
                }
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
                setStyle("-fx-font-weight: bold;");
            }
        });

        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colName);

        colTaxId.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().nif() != null ? "PT" + cd.getValue().nif() : "—"));
        configurarColunaTexto(colTaxId);

        colContactPerson.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().contacto() != null ? cd.getValue().contacto() : "—"));
        configurarColunaTexto(colContactPerson);

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().addAll("button-icon", "flat");
                btn.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btn.setTooltip(new Tooltip("Ver Detalhes"));
                btn.setOnAction(e -> {
                    FornecedorSimpleDTO dto = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(dto);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });

        tblSuppliers.setFixedCellSize(48);
        tblSuppliers.setItems(fornecedores);
    }

    private <T> void configurarColunaTexto(TableColumn<FornecedorSimpleDTO, T> coluna) {
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private void carregarFornecedores() {
        try {
            String nome = (txtSearch != null && !txtSearch.getText().isEmpty()) ? txtSearch.getText() : null;
            Page<FornecedorSimpleDTO> page = fornecedorService.listarFornecedoresComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, "nome", "ASC");
            fornecedores.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao carregar fornecedores: " + e.getMessage());
        }
    }

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerAdicionar);
    }

    @FXML
    private void handleFiltrar() {
        pagination.resetPage();
        carregarFornecedores();
    }

    @FXML
    private void handleLimpar() {
        txtSearch.clear();
        pagination.resetPage();
        carregarFornecedores();
    }

    private void handleAbrirDetalhes(FornecedorSimpleDTO dto) {
        try {
            FornecedorDetailsDTO details = fornecedorService.obterDetalhesFornecedor(dto.id());
            navigationService.showModal(criarDrawerDetalhes(details));
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(FornecedorDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Supplier Details", navigationService::hideModal);

        TextField txtNome = new TextField(valorOuVazio(d.nome()));
        TextField txtNif = new TextField(d.nif() != null ? "PT" + d.nif() : "");
        TextField txtContacto = new TextField(valorOuVazio(d.contacto()));
        TextField txtEmail = new TextField(valorOuVazio(d.email()));

        txtNome.setEditable(false);
        txtNif.setEditable(false);
        txtContacto.setEditable(false);
        txtEmail.setEditable(false);

        VBox form = new VBox(20,
                criarCampo("Supplier Name", txtNome),
                criarCampo("Tax ID / VAT Number", txtNif),
                criarCampo("Contact Person", txtContacto),
                criarCampo("Email", txtEmail)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();
        footer.setSpacing(12);

        Button btnUpdate = new Button("Update Supplier");
        btnUpdate.getStyleClass().add("accent");
        btnUpdate.setPrefHeight(44);
        btnUpdate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnUpdate, Priority.ALWAYS);
        btnUpdate.setOnAction(e -> toastService.showSuccess("Em breve", "Funcionalidade disponível em breve"));

        Button btnDelete = new Button("Delete");
        btnDelete.setPrefHeight(44);
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");
        HBox.setHgrow(btnDelete, Priority.ALWAYS);
        btnDelete.setOnAction(e -> toastService.showSuccess("Em breve", "Funcionalidade disponível em breve"));

        footer.getChildren().addAll(btnUpdate, btnDelete);
        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void configurarDrawerAdicionar() {
        drawerAdicionar = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("New Supplier", navigationService::hideModal);

        txtNomeAdicionar = new TextField(); txtNomeAdicionar.setPromptText("Enter supplier name");
        txtNifAdicionar = new TextField(); txtNifAdicionar.setPromptText("PT123456789");
        txtContactoAdicionar = new TextField(); txtContactoAdicionar.setPromptText("Contact person name");
        txtEmailAdicionar = new TextField(); txtEmailAdicionar.setPromptText("supplier@example.com");

        VBox form = new VBox(20,
                criarCampo("Supplier Name", txtNomeAdicionar),
                criarCampo("Tax ID / VAT Number", txtNifAdicionar),
                criarCampo("Contact Person", txtContactoAdicionar),
                criarCampo("Email", txtEmailAdicionar)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();
        Button btnCreate = new Button("Create Supplier");
        btnCreate.getStyleClass().add("accent");
        btnCreate.setPrefHeight(44);
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        btnCreate.setOnAction(e -> handleCriarFornecedor());
        footer.getChildren().add(btnCreate);

        drawerAdicionar.getChildren().addAll(header, scrollPane, footer);
    }

    private VBox criarCampo(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private void limparFormulario() {
        txtNomeAdicionar.clear();
        txtNifAdicionar.clear();
        txtContactoAdicionar.clear();
        txtEmailAdicionar.clear();
    }

    private void handleCriarFornecedor() {
        try {
            fornecedorService.registarFornecedor(
                    txtNomeAdicionar.getText(),
                    txtNifAdicionar.getText(),
                    txtContactoAdicionar.getText(),
                    txtEmailAdicionar.getText()
            );
            toastService.showSuccess("Sucesso", "Fornecedor registado!");
            navigationService.hideModal();
            carregarFornecedores();
        } catch (Exception e) {
            toastService.showError("Erro", e.getMessage());
        }
    }

    private String valorOuVazio(String s) { return s != null ? s : ""; }

}
