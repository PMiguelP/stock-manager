package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.ClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.VendaService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
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

import java.util.UUID;

@Component
public class ClientsController {

    private final VendaService vendaService;
    private final NavigationService navigationService;
    private final ToastService toastService;

    @FXML private TableView<ClienteSimpleDTO> tblClients;
    @FXML private TableColumn<ClienteSimpleDTO, String> colId, colNome, colNif, colContacto;
    @FXML private TableColumn<ClienteSimpleDTO, Void> colAcoes;

    @FXML private TextField txtFiltroNome;
    @FXML private TextField txtFiltroNif;
    @FXML private VBox vboxContainer;

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<ClienteSimpleDTO> clientes = FXCollections.observableArrayList();

    public ClientsController(VendaService vendaService, NavigationService navigationService, ToastService toastService) {
        this.vendaService = vendaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        carregarDados();
    }

    private void configurarTabela() {
        colId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty("CLI-" + cd.getValue().id().toString().substring(0, 4).toUpperCase()));
        configurarColunaTexto(colId);

        colNome.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colNome);

        colNif.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().nif()));
        configurarColunaTexto(colNif);

        colContacto.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().contacto()));
        configurarColunaTexto(colContacto);

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button();
            {
                btnVer.getStyleClass().addAll("button-icon", "flat");
                btnVer.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnVer.setTooltip(new Tooltip("Ver Detalhes"));
                btnVer.setOnAction(e -> {
                    ClienteSimpleDTO dto = getTableView().getItems().get(getIndex());
                    handleVerDetalhes(dto.id());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnVer);
                setAlignment(Pos.CENTER);
            }
        });

        tblClients.setFixedCellSize(48);
        tblClients.setItems(clientes);
    }

    private <T> void configurarColunaTexto(TableColumn<ClienteSimpleDTO, T> coluna) {
        coluna.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private void carregarDados() {
        try {
            String nome = (txtFiltroNome != null && !txtFiltroNome.getText().isEmpty()) ? txtFiltroNome.getText() : null;
            String nif = (txtFiltroNif != null && !txtFiltroNif.getText().isEmpty()) ? txtFiltroNif.getText() : null;

            Page<ClienteSimpleDTO> page = vendaService.listarClientesComFiltros(
                    paginaAtual + 1, itemsPerPage, nome, nif, "nome", "ASC"
            );

            clientes.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            toastService.showError("Erro", "Não foi possível carregar clientes: " + e.getMessage());
        }
    }

    private void handleVerDetalhes(UUID clienteId) {
        try {
            ClienteDetailsDTO detalhes = vendaService.obterDetalhesCliente(clienteId);
            VBox drawer = criarDrawerDetalhes(detalhes);
            navigationService.showModal(drawer);
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao obter detalhes: " + e.getMessage());
        }
    }

    @FXML
    private void handleNovoCliente() {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Novo Cliente");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button close = new Button(); close.setGraphic(new FontIcon("mdi2c-close:22"));
        close.getStyleClass().addAll("button-icon", "flat");
        close.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, close);

        TextField txtNome = new TextField(); txtNome.setPromptText("Company Name");
        TextField txtNif = new TextField(); txtNif.setPromptText("NIF / VAT Number");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email Address");
        TextField txtPhone = new TextField(); txtPhone.setPromptText("Contact Phone");

        VBox form = new VBox(20,
                new VBox(6, new Label("Name"), txtNome),
                new VBox(6, new Label("NIF"), txtNif),
                new VBox(6, new Label("Email"), txtEmail),
                new VBox(6, new Label("Phone"), txtPhone)
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

        Button btnSave = new Button("Guardar Cliente");
        btnSave.setPrefHeight(44);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.getStyleClass().add("accent");
        HBox.setHgrow(btnSave, Priority.ALWAYS);

        btnSave.setOnAction(e -> {
            try {
                vendaService.registarCliente(txtNome.getText(), txtNif.getText(), txtPhone.getText(), txtEmail.getText());
                toastService.showSuccess("Sucesso", "Cliente registado!");
                navigationService.hideModal();
                carregarDados();
            } catch (Exception ex) {
                toastService.showError("Erro", ex.getMessage());
            }
        });
        footer.getChildren().add(btnSave);

        root.getChildren().addAll(header, scrollPane, footer);
        navigationService.showModal(root);
    }

    private VBox criarDrawerDetalhes(ClienteDetailsDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label title = new Label("Detalhes do Cliente"); title.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button close = new Button(); close.setGraphic(new FontIcon("mdi2c-close:22"));
        close.getStyleClass().addAll("button-icon", "flat");
        close.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(title, sp, close);

        VBox content = new VBox(15);
        content.setPadding(new Insets(30));
        content.getChildren().addAll(
                new Label("Company: " + d.nome()),
                new Label("NIF: " + d.nif()),
                new Label("Contact: " + d.contacto()),
                new Label("Email: " + d.email()),
                new Separator(),
                new Label("Total Orders: " + (d.encomendas() != null ? d.encomendas().size() : 0))
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private void configurarPaginacao(VBox container) {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        lblPaginaStatus = new Label();
        lblPaginaStatus.getStyleClass().add("text-muted");
        HBox left = new HBox(lblPaginaStatus); left.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(left, Priority.ALWAYS);

        cmbItemsPerPage = new ComboBox<>(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbItemsPerPage.setValue(itemsPerPage);
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarDados(); });
        HBox center = new HBox(10, new Label("Por página"), cmbItemsPerPage); center.setAlignment(Pos.CENTER); HBox.setHgrow(center, Priority.ALWAYS);

        paginationButtons = new HBox(5);
        HBox right = new HBox(paginationButtons); right.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(right, Priority.ALWAYS);

        nav.getChildren().addAll(left, center, right);
        container.getChildren().add(nav);
    }

    private void atualizarBotoesPaginacao() {
        paginationButtons.getChildren().clear();
        Button prev = new Button(); prev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        prev.setDisable(paginaAtual == 0);
        prev.setOnAction(e -> { paginaAtual--; carregarDados(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button b = new Button(String.valueOf(i + 1));
                b.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int finalI = i;
                b.setOnAction(e -> { paginaAtual = finalI; carregarDados(); });
                paginationButtons.getChildren().add(b);
            }
        }

        Button next = new Button(); next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarDados(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<?> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }

    @FXML private void handleFiltrar() { paginaAtual = 0; carregarDados(); }
    @FXML private void handleLimpar() {
        txtFiltroNome.clear();
        txtFiltroNif.clear();
        handleFiltrar();
    }
}

