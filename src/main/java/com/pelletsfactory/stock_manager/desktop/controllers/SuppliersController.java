package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.FornecedorDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.FornecedorSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.CompraService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
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

    private final CompraService compraService;
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

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<FornecedorSimpleDTO> fornecedores = FXCollections.observableArrayList();

    public SuppliersController(CompraService compraService, NavigationService navigationService,
                               ToastService toastService) {
        this.compraService = compraService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        resetPaginationControls();
        configurarTabela();
        configurarDrawerAdicionar();
        carregarFornecedores();
    }

    private void resetPaginationControls() {
        lblPaginaStatus = null;
        cmbItemsPerPage = null;
        paginationButtons = null;
    }

    private void configurarTabela() {
        colSupplierID.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setText(null);
                } else {
                    int seq = paginaAtual * itemsPerPage + getIndex() + 1;
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
            Page<FornecedorSimpleDTO> page = compraService.listarFornecedoresComFiltros(
                    paginaAtual + 1, itemsPerPage, nome, null, "nome", "ASC");
            fornecedores.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
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
        paginaAtual = 0;
        carregarFornecedores();
    }

    @FXML
    private void handleLimpar() {
        txtSearch.clear();
        paginaAtual = 0;
        carregarFornecedores();
    }

    private void handleAbrirDetalhes(FornecedorSimpleDTO dto) {
        try {
            FornecedorDetailsDTO details = compraService.obterDetalhesFornecedor(dto.id());
            navigationService.showModal(criarDrawerDetalhes(details));
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(FornecedorDetailsDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Supplier Details");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button(); btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

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

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

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
        drawerAdicionar = new VBox(0);
        drawerAdicionar.setMinWidth(550);
        drawerAdicionar.setPrefWidth(550);
        drawerAdicionar.setMaxWidth(550);
        drawerAdicionar.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("New Supplier");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button(); btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

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

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnCreate = new Button("Create Supplier");
        btnCreate.getStyleClass().add("accent");
        btnCreate.setPrefHeight(44);
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        btnCreate.setOnAction(e -> toastService.showSuccess("Em breve", "Funcionalidade disponível em breve"));
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

    private String valorOuVazio(String s) { return s != null ? s : ""; }

    private void configurarPaginacao(VBox container) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox left = new HBox(lblPaginaStatus);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarFornecedores(); });
        HBox center = new HBox(10, new Label("Por página"), cmbItemsPerPage);
        center.setAlignment(Pos.CENTER);
        HBox.setHgrow(center, Priority.ALWAYS);

        paginationButtons = new HBox(5);
        HBox right = new HBox(paginationButtons);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(right, Priority.ALWAYS);

        nav.getChildren().addAll(left, center, right);
        container.getChildren().add(nav);
    }

    private void atualizarBotoesPaginacao() {
        paginationButtons.getChildren().clear();
        Button prev = new Button();
        prev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        prev.setDisable(paginaAtual == 0);
        prev.setOnAction(e -> { paginaAtual--; carregarFornecedores(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int finalI = i;
                p.setOnAction(e -> { paginaAtual = finalI; carregarFornecedores(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarFornecedores(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<FornecedorSimpleDTO> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }
}
