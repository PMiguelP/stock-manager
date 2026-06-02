package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.ClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.ClienteService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
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

    private final ClienteService clienteService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private TableView<ClienteSimpleDTO> tblClients;
    @FXML private TableColumn<ClienteSimpleDTO, String> colId, colNome, colNif, colContacto;
    @FXML private TableColumn<ClienteSimpleDTO, Void> colAcoes;

    @FXML private TextField txtFiltroNome;
    @FXML private TextField txtFiltroNif;
    @FXML private VBox vboxContainer;

    private PaginationControls pagination;

    private final ObservableList<ClienteSimpleDTO> clientes = FXCollections.observableArrayList();

    public ClientsController(ClienteService clienteService, NavigationService navigationService,
                             ToastService toastService, I18nService i18nService) {
        this.clienteService = clienteService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarDados, i18nService);
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

            Page<ClienteSimpleDTO> page = clienteService.listarClientesComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, nif, "nome", "ASC"
            );

            clientes.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError("Erro", "Não foi possível carregar clientes: " + e.getMessage());
        }
    }

    private void handleVerDetalhes(UUID clienteId) {
        try {
            ClienteDetailsDTO detalhes = clienteService.obterDetalhesCliente(clienteId);
            VBox drawer = criarDrawerDetalhes(detalhes);
            navigationService.showModal(drawer);
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao obter detalhes: " + e.getMessage());
        }
    }

    @FXML
    private void handleNovoCliente() {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Novo Cliente", navigationService::hideModal);

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

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();

        Button btnSave = new Button("Guardar Cliente");
        btnSave.setPrefHeight(44);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.getStyleClass().add("accent");
        HBox.setHgrow(btnSave, Priority.ALWAYS);

        btnSave.setOnAction(e -> {
            try {
                clienteService.registarCliente(txtNome.getText(), txtNif.getText(), txtPhone.getText(), txtEmail.getText());
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
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Detalhes do Cliente", navigationService::hideModal);

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

        ScrollPane scrollPane = UiFactory.transparentScroll(content);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarDados(); }
    @FXML private void handleLimpar() {
        txtFiltroNome.clear();
        txtFiltroNif.clear();
        handleFiltrar();
    }
}
