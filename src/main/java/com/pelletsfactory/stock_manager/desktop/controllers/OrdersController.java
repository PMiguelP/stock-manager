package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TipoPelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
import com.pelletsfactory.stock_manager.common.services.ClienteService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.common.services.VendaService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class OrdersController {
    private final VendaService vendaService;
    private final ClienteService clienteService;
    private final MoedaService moedaService;
    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private ComboBox<EstadoEncomendaCliente> cmbFiltroEstado;
    @FXML private TextField txtFiltroCliente;
    @FXML private TableView<EncomendaClienteSimpleDTO> tblEncomendas;
    @FXML private VBox vboxContainer;

    @FXML private TableColumn<EncomendaClienteSimpleDTO, String> colCliente;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, LocalDate> colData;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, EstadoEncomendaCliente> colEstado;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, Double> colTotal;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, String> colMoeda;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, String> colTracking;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, Void> colAcoes;

    // Create order drawer fields
    private VBox criarEncomendaDrawer;
    private ComboBox<ClienteSimpleDTO> cmbCliente;
    private ComboBox<MoedaSimpleDTO> cmbMoeda;
    private Label lblErroCliente;
    private Label lblErroMoeda;
    private VBox itensContainer;
    private Label lblErroItens;

    private PaginationControls pagination;

    private final ObservableList<EncomendaClienteSimpleDTO> encomendas = FXCollections.observableArrayList();
    private final ObservableList<TipoPelletSimpleDTO> tiposPellet = FXCollections.observableArrayList();
    private final List<OrderItemRow> itemRows = new ArrayList<>();
    private final PauseTransition searchDebounce = new PauseTransition(javafx.util.Duration.millis(300));
    private boolean updatingFilters;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public OrdersController(VendaService vendaService,
                            ClienteService clienteService,
                            MoedaService moedaService,
                            StockService stockService,
                            NavigationService navigationService,
                            ToastService toastService,
                            I18nService i18nService) {
        this.vendaService = vendaService;
        this.clienteService = clienteService;
        this.moedaService = moedaService;
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarEncomendas, i18nService);
        configurarTabela();
        configurarComboBoxes();
        configurarPesquisaDinamica();
        configurarDrawerCriarEncomenda();
        carregarEncomendas();
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colCliente.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().clienteNome()));
        configurarColunaTexto(colCliente);

        colData.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().data()));
        colData.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.format(DATE_FORMATTER));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colEstado.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().estado()));
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoEncomendaCliente estado, boolean empty) {
                super.updateItem(estado, empty);
                setGraphic(empty || estado == null ? null : criarBadgeEstado(estado));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colTotal.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().totalFinal()));
        colTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f", item));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colMoeda.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().moedaCodigo()));
        configurarColunaTexto(colMoeda);

        colTracking.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().codigoTracking()));
        configurarColunaTexto(colTracking);

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setTooltip(new Tooltip("Ver Detalhes"));
                btnDetails.setOnAction(event -> {
                    EncomendaClienteSimpleDTO enc = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(enc);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblEncomendas.setFixedCellSize(48);
        tblEncomendas.setItems(encomendas);
    }

    private <T> void configurarColunaTexto(TableColumn<EncomendaClienteSimpleDTO, T> coluna) {
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

    // ── Data loading ─────────────────────────────────────────────────────────

    private void carregarEncomendas() {
        try {
            EstadoEncomendaCliente estado = (cmbFiltroEstado != null) ? cmbFiltroEstado.getValue() : null;
            String cliente = txtFiltroCliente != null ? txtFiltroCliente.getText() : null;

            Page<EncomendaClienteSimpleDTO> page = vendaService.listarEncomendasComPesquisaSimples(
                    cliente, estado, pagination.pageNumberForService(), pagination.pageSize(), "data", "DESC"
            );

            encomendas.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro("Erro ao carregar: " + e.getMessage());
        }
    }

    // ── Create order drawer ───────────────────────────────────────────────────

    private void configurarDrawerCriarEncomenda() {
        criarEncomendaDrawer = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("orders.new"), navigationService::hideModal);

        // Form
        VBox form = new VBox(20);
        form.setPadding(new Insets(30));

        cmbCliente = new ComboBox<>();
        cmbCliente.setMaxWidth(Double.MAX_VALUE);
        cmbCliente.setConverter(new StringConverter<>() {
            @Override public String toString(ClienteSimpleDTO c) { return c == null ? "" : c.nome() + " (" + c.nif() + ")"; }
            @Override public ClienteSimpleDTO fromString(String s) { return null; }
        });
        lblErroCliente = criarErroLabel();

        cmbMoeda = new ComboBox<>();
        cmbMoeda.setMaxWidth(Double.MAX_VALUE);
        cmbMoeda.setConverter(new StringConverter<>() {
            @Override public String toString(MoedaSimpleDTO m) { return m == null ? "" : m.codigo() + " (" + m.simbolo() + ")"; }
            @Override public MoedaSimpleDTO fromString(String s) { return null; }
        });
        lblErroMoeda = criarErroLabel();

        cmbCliente.focusedProperty().addListener((o, ov, nv) -> { if (nv) esconderErro(lblErroCliente, cmbCliente); });
        cmbMoeda.focusedProperty().addListener((o, ov, nv) -> { if (nv) esconderErro(lblErroMoeda, cmbMoeda); });

        form.getChildren().addAll(
                criarCampoFormulario("Cliente *", cmbCliente, lblErroCliente),
                criarCampoFormulario("Moeda *", cmbMoeda, lblErroMoeda)
        );

        Label lblItensTitulo = new Label(i18nService.translate("orders.items"));
        lblItensTitulo.getStyleClass().add("title-4");

        itensContainer = new VBox(12);
        lblErroItens = criarErroLabel();

        Button btnAdicionarItem = UiFactory.drawerSecondaryAction(i18nService.translate("orders.addItem"), "mdi2p-plus-circle-outline");
        btnAdicionarItem.setOnAction(e -> adicionarLinhaItem());

        form.getChildren().addAll(lblItensTitulo, itensContainer, lblErroItens, btnAdicionarItem);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        btnCancelar.setOnAction(e -> navigationService.hideModal());

        Button btnCriar = UiFactory.drawerPrimaryAction(i18nService.translate("orders.create"), "mdi2c-check-circle-outline");
        btnCriar.setOnAction(e -> handleCriarEncomenda());

        HBox footer = UiFactory.drawerActionFooter(null, btnCancelar, btnCriar);

        criarEncomendaDrawer.getChildren().addAll(header, scroll, footer);
    }

    private void handleCriarEncomenda() {
        boolean valido = true;
        if (cmbCliente.getValue() == null) {
            mostrarErroLabel(lblErroCliente, cmbCliente, i18nService.translate("orders.customerRequired"));
            valido = false;
        }
        if (cmbMoeda.getValue() == null) {
            mostrarErroLabel(lblErroMoeda, cmbMoeda, i18nService.translate("orders.currencyRequired"));
            valido = false;
        }
        if (itemRows.isEmpty()) {
            mostrarErroItens(i18nService.translate("orders.itemsRequired"));
            valido = false;
        }
        for (OrderItemRow row : itemRows) {
            valido = row.validar() && valido;
        }
        if (!valido) return;

        try {
            var encomenda = vendaService.criarPedidoVenda(cmbCliente.getValue().id(), cmbMoeda.getValue().id());
            for (OrderItemRow row : itemRows) {
                vendaService.adicionarItemEncomenda(
                        encomenda.id(),
                        row.tipoPelletId(),
                        row.quantidadeKg(),
                        row.precoUnitario(),
                        row.taxaIva()
                );
            }
            pagination.resetPage();
            carregarEncomendas();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("orders.created"));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("orders.createError") + ": " + e.getMessage());
        }
    }

    public void openCreateModal() {
        handleAbrirModal();
    }

    @FXML
    private void handleAbrirModal() {
        // Reload dropdowns fresh on each open
        recarregarOpcoesEncomenda();

        cmbCliente.setValue(null);
        cmbMoeda.setValue(null);
        itemRows.clear();
        itensContainer.getChildren().clear();
        adicionarLinhaItem();
        esconderErro(lblErroCliente, cmbCliente);
        esconderErro(lblErroMoeda, cmbMoeda);
        esconderErroItens();

        navigationService.showModal(criarEncomendaDrawer);
    }

    private void recarregarOpcoesEncomenda() {
        try {
            cmbCliente.setItems(FXCollections.observableArrayList(clienteService.listarTodosClientesSimples()));
        } catch (Exception e) { cmbCliente.setItems(FXCollections.emptyObservableList()); }
        try {
            cmbMoeda.setItems(FXCollections.observableArrayList(moedaService.listarTodosSimplesDTO()));
        } catch (Exception e) { cmbMoeda.setItems(FXCollections.emptyObservableList()); }
        try {
            tiposPellet.setAll(stockService.listarTiposPelletComFiltros(1, 100, null, null, "nome", "ASC").getContent());
        } catch (Exception e) {
            tiposPellet.clear();
            mostrarErro("Erro ao carregar produtos: " + e.getMessage());
        }
    }

    private void adicionarLinhaItem() {
        OrderItemRow row = new OrderItemRow();
        itemRows.add(row);
        itensContainer.getChildren().add(row.root);
        atualizarBotoesRemoverItens();
        esconderErroItens();
    }

    private void removerLinhaItem(OrderItemRow row) {
        itemRows.remove(row);
        itensContainer.getChildren().remove(row.root);
        atualizarBotoesRemoverItens();
    }

    private void atualizarBotoesRemoverItens() {
        boolean podeRemover = itemRows.size() > 1;
        itemRows.forEach(row -> row.btnRemover.setDisable(!podeRemover));
    }

    private class OrderItemRow {
        private final VBox root = new VBox(8);
        private final ComboBox<TipoPelletSimpleDTO> cmbTipoPellet = new ComboBox<>(tiposPellet);
        private final TextField txtQuantidade = new TextField();
        private final TextField txtPreco = new TextField();
        private final TextField txtIva = new TextField("23");
        private final Button btnRemover = UiFactory.iconButton("mdi2d-delete-outline", 18);
        private final Label lblErro = criarErroLabel();

        private OrderItemRow() {
            root.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-muted; " +
                    "-fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 14;");

            cmbTipoPellet.setMaxWidth(Double.MAX_VALUE);
            cmbTipoPellet.setConverter(new StringConverter<>() {
                @Override public String toString(TipoPelletSimpleDTO t) { return t == null ? "" : t.nome(); }
                @Override public TipoPelletSimpleDTO fromString(String s) { return null; }
            });

            txtQuantidade.setPromptText(i18nService.translate("orders.quantityKg"));
            txtPreco.setPromptText(i18nService.translate("orders.unitPrice"));
            txtIva.setPromptText(i18nService.translate("orders.vatRate"));

            btnRemover.setMinWidth(44);
            btnRemover.setPrefWidth(44);
            btnRemover.setStyle("-fx-background-color: rgba(239, 68, 68, 0.10); -fx-border-color: rgba(239, 68, 68, 0.70); -fx-border-radius: 7; -fx-background-radius: 7;");
            btnRemover.setOnAction(e -> removerLinhaItem(this));

            HBox linhaTopo = new HBox(10, cmbTipoPellet, btnRemover);
            HBox.setHgrow(cmbTipoPellet, Priority.ALWAYS);

            HBox linhaValores = new HBox(10, txtQuantidade, txtPreco, txtIva);
            HBox.setHgrow(txtQuantidade, Priority.ALWAYS);
            HBox.setHgrow(txtPreco, Priority.ALWAYS);
            HBox.setHgrow(txtIva, Priority.ALWAYS);

            cmbTipoPellet.valueProperty().addListener((o, ov, nv) -> esconderErro(lblErro, cmbTipoPellet));
            txtQuantidade.textProperty().addListener((o, ov, nv) -> esconderErro(lblErro, txtQuantidade));
            txtPreco.textProperty().addListener((o, ov, nv) -> esconderErro(lblErro, txtPreco));
            txtIva.textProperty().addListener((o, ov, nv) -> esconderErro(lblErro, txtIva));

            root.getChildren().addAll(linhaTopo, linhaValores, lblErro);
        }

        private boolean validar() {
            if (cmbTipoPellet.getValue() == null) {
                mostrarErroLabel(lblErro, cmbTipoPellet, i18nService.translate("orders.pelletRequired"));
                return false;
            }
            if (quantidadeKg() <= 0) {
                mostrarErroLabel(lblErro, txtQuantidade, i18nService.translate("orders.quantityInvalid"));
                return false;
            }
            if (precoUnitario() <= 0) {
                mostrarErroLabel(lblErro, txtPreco, i18nService.translate("orders.priceInvalid"));
                return false;
            }
            double iva = taxaIva();
            if (iva < 0 || iva > 100) {
                mostrarErroLabel(lblErro, txtIva, i18nService.translate("orders.vatInvalid"));
                return false;
            }
            esconderErro(lblErro, cmbTipoPellet);
            return true;
        }

        private UUID tipoPelletId() {
            return cmbTipoPellet.getValue().id();
        }

        private double quantidadeKg() {
            return parseDouble(txtQuantidade.getText());
        }

        private double precoUnitario() {
            return parseDouble(txtPreco.getText());
        }

        private double taxaIva() {
            return parseDouble(txtIva.getText());
        }

        private void preencher(ItemEncomendaClienteResponseDTO item) {
            tiposPellet.stream()
                    .filter(tipo -> tipo.id().equals(item.tipoPelletId()))
                    .findFirst()
                    .ifPresent(cmbTipoPellet::setValue);
            txtQuantidade.setText(formatarNumero(item.quantidadeKg()));
            txtPreco.setText(formatarNumero(item.precoUnitarioNet()));
            txtIva.setText(formatarNumero(item.taxaIva()));
        }

        private void setEditavel(boolean editavel) {
            cmbTipoPellet.setMouseTransparent(!editavel);
            cmbTipoPellet.setFocusTraversable(editavel);
            txtQuantidade.setEditable(editavel);
            txtPreco.setEditable(editavel);
            txtIva.setEditable(editavel);
            btnRemover.setVisible(editavel);
            btnRemover.setManaged(editavel);
        }
    }

    // ── View order details drawer ─────────────────────────────────────────────

    private void handleAbrirDetalhes(EncomendaClienteSimpleDTO enc) {
        try {
            recarregarOpcoesEncomenda();
            EncomendaClienteDetailsDTO d = vendaService.obterDetalhesEncomendaCliente(enc.id());
            navigationService.showModal(criarDrawerVisualizacao(d));
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerVisualizacao(EncomendaClienteDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Detalhes da Encomenda", navigationService::hideModal);
        boolean encomendaCancelada = EstadoEncomendaCliente.CANCELADA.equals(d.estado());
        boolean encomendaExpedida = EstadoEncomendaCliente.EXPEDIDA.equals(d.estado());
        boolean podeEditarItens = EstadoEncomendaCliente.PENDENTE.equals(d.estado());
        boolean podeEditarEstado = !encomendaExpedida && !encomendaCancelada;

        ComboBox<ClienteSimpleDTO> cmbClienteDetalhes = new ComboBox<>();
        cmbClienteDetalhes.setItems(cmbCliente.getItems());
        cmbClienteDetalhes.setMaxWidth(Double.MAX_VALUE);
        cmbClienteDetalhes.setConverter(cmbCliente.getConverter());
        selecionarCliente(cmbClienteDetalhes, d.clienteId());

        ComboBox<MoedaSimpleDTO> cmbMoedaDetalhes = new ComboBox<>();
        cmbMoedaDetalhes.setItems(cmbMoeda.getItems());
        cmbMoedaDetalhes.setMaxWidth(Double.MAX_VALUE);
        cmbMoedaDetalhes.setConverter(cmbMoeda.getConverter());
        selecionarMoeda(cmbMoedaDetalhes, d.moedaId());

        TextField txtTracking = new TextField(valorOuVazio(d.codigoTracking()));
        txtTracking.setEditable(false);
        txtTracking.setFocusTraversable(false);

        ComboBox<EstadoEncomendaCliente> cmbEstadoDetalhes = new ComboBox<>(
                FXCollections.observableArrayList(EstadoEncomendaCliente.values())
        );
        cmbEstadoDetalhes.setValue(d.estado());
        cmbEstadoDetalhes.setMaxWidth(Double.MAX_VALUE);
        cmbEstadoDetalhes.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoEncomendaCliente estado) {
                return estado == null ? "" : estado.getDisplayName();
            }
            @Override public EstadoEncomendaCliente fromString(String value) { return null; }
        });

        VBox itensEdicao = new VBox(12);
        List<OrderItemRow> editRows = new ArrayList<>();
        if (d.itens() != null) {
            for (ItemEncomendaClienteResponseDTO item : d.itens()) {
                OrderItemRow row = new OrderItemRow();
                row.preencher(item);
                editRows.add(row);
                itensEdicao.getChildren().add(row.root);
            }
        }
        if (editRows.isEmpty()) {
            OrderItemRow row = new OrderItemRow();
            editRows.add(row);
            itensEdicao.getChildren().add(row.root);
        }
        editRows.forEach(row -> {
            row.btnRemover.setOnAction(e -> {
                if (editRows.size() <= 1) {
                    return;
                }
                editRows.remove(row);
                itensEdicao.getChildren().remove(row.root);
                atualizarBotoesRemoverEditRows(editRows);
            });
        });
        atualizarBotoesRemoverEditRows(editRows);

        Button btnAdicionarItem = UiFactory.drawerSecondaryAction(i18nService.translate("orders.addItem"), "mdi2p-plus-circle-outline");
        btnAdicionarItem.setOnAction(e -> {
            OrderItemRow row = new OrderItemRow();
            row.btnRemover.setOnAction(event -> {
                if (editRows.size() <= 1) {
                    return;
                }
                editRows.remove(row);
                itensEdicao.getChildren().remove(row.root);
                atualizarBotoesRemoverEditRows(editRows);
            });
            editRows.add(row);
            itensEdicao.getChildren().add(row.root);
            atualizarBotoesRemoverEditRows(editRows);
        });

        VBox resumo = criarResumoEncomenda(d);
        VBox form = new VBox(18,
                resumo,
                criarSecaoEdicao("Cliente", cmbClienteDetalhes),
                criarSecaoEdicao("Moeda", cmbMoedaDetalhes),
                criarSecaoEdicao("Estado", cmbEstadoDetalhes),
                criarSecaoEdicao("Tracking ID", txtTracking),
                criarSecaoItensEdicao(editRows, itensEdicao, btnAdicionarItem)
        );
        form.setPadding(new Insets(30));

        ScrollPane scroll = UiFactory.transparentScroll(form);

        Button btnAcaoDestrutiva = encomendaCancelada
                ? UiFactory.drawerDangerAction(i18nService.translate("orders.deleteOrder"), "mdi2d-delete-outline")
                : UiFactory.drawerDangerAction(i18nService.translate("orders.cancelOrder"), "mdi2c-close-circle-outline");
        btnAcaoDestrutiva.setDisable(encomendaExpedida);
        btnAcaoDestrutiva.setOnAction(e -> {
            if (encomendaCancelada) {
                handleEliminarEncomenda(d);
            } else {
                handleCancelarEncomenda(d);
            }
        });

        Button btnGuardar = UiFactory.drawerPrimaryAction(i18nService.translate("common.save"), "mdi2c-content-save-outline");
        btnGuardar.setDisable(true);

        Button btnEditar = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
        btnEditar.setDisable(!podeEditarItens && !podeEditarEstado);

        Button btnCancelarEdicao = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        btnCancelarEdicao.setVisible(false);
        btnCancelarEdicao.setManaged(false);

        setModoEdicaoEncomenda(false, podeEditarItens, cmbClienteDetalhes, cmbMoedaDetalhes, cmbEstadoDetalhes, editRows, btnAdicionarItem);
        btnEditar.setOnAction(e -> {
            setModoEdicaoEncomenda(true, podeEditarItens, cmbClienteDetalhes, cmbMoedaDetalhes, cmbEstadoDetalhes, editRows, btnAdicionarItem);
            btnGuardar.setDisable(false);
            btnEditar.setVisible(false);
            btnEditar.setManaged(false);
            btnCancelarEdicao.setVisible(true);
            btnCancelarEdicao.setManaged(true);
        });
        btnCancelarEdicao.setOnAction(e -> navigationService.showModal(criarDrawerVisualizacao(d)));
        btnGuardar.setOnAction(e -> handleGuardarEdicaoEncomenda(d, cmbClienteDetalhes, cmbMoedaDetalhes, cmbEstadoDetalhes, editRows));

        HBox footer = UiFactory.drawerActionFooter(btnAcaoDestrutiva, btnCancelarEdicao, btnEditar, btnGuardar);

        root.getChildren().addAll(header, scroll, footer);
        return root;
    }

    private void handleCancelarEncomenda(EncomendaClienteDetailsDTO d) {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle(i18nService.translate("orders.cancelOrder"));
        confirmacao.setHeaderText(i18nService.translate("orders.cancelConfirm"));
        confirmacao.setContentText(valorOuVazio(d.clienteNome()));
        confirmacao.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            vendaService.cancelarEncomenda(d.id());
            carregarEncomendas();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("orders.cancelled"));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("orders.cancelError") + ": " + e.getMessage());
        }
    }

    private void handleEliminarEncomenda(EncomendaClienteDetailsDTO d) {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle(i18nService.translate("orders.deleteOrder"));
        confirmacao.setHeaderText(i18nService.translate("orders.deleteConfirm"));
        confirmacao.setContentText(valorOuVazio(d.clienteNome()));
        confirmacao.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            vendaService.eliminarEncomendaCancelada(d.id());
            carregarEncomendas();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("orders.deleted"));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("orders.deleteError") + ": " + e.getMessage());
        }
    }

    private void handleGuardarEdicaoEncomenda(EncomendaClienteDetailsDTO d,
                                              ComboBox<ClienteSimpleDTO> cliente,
                                              ComboBox<MoedaSimpleDTO> moeda,
                                              ComboBox<EstadoEncomendaCliente> estado,
                                              List<OrderItemRow> rows) {
        boolean podeEditarItens = EstadoEncomendaCliente.PENDENTE.equals(d.estado());
        if (podeEditarItens && (cliente.getValue() == null || moeda.getValue() == null)) {
            mostrarErro("Cliente e moeda são obrigatórios");
            return;
        }
        if (estado.getValue() == null) {
            mostrarErro("Estado é obrigatório");
            return;
        }

        if (podeEditarItens) {
            boolean valido = true;
            for (OrderItemRow row : rows) {
                valido = row.validar() && valido;
            }
            if (!valido) {
                return;
            }
        }

        try {
            if (podeEditarItens) {
                vendaService.atualizarPedidoVenda(
                        d.id(),
                        cliente.getValue().id(),
                        moeda.getValue().id(),
                        rows.stream()
                                .map(row -> new VendaService.ItemPedidoVendaInput(
                                        row.tipoPelletId(),
                                        row.quantidadeKg(),
                                        row.precoUnitario(),
                                        row.taxaIva()
                                ))
                                .toList()
                );
            }

            if (estado.getValue() != d.estado()) {
                vendaService.alterarEstadoEncomenda(d.id(), estado.getValue());
            }

            carregarEncomendas();
            EncomendaClienteDetailsDTO atualizado = vendaService.obterDetalhesEncomendaCliente(d.id());
            navigationService.showModal(criarDrawerVisualizacao(atualizado));
            mostrarSucesso(i18nService.translate("orders.updated"));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("orders.updateError") + ": " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox criarResumoEncomenda(EncomendaClienteDetailsDTO d) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-muted; " +
                "-fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 18;");

        HBox topo = new HBox(10);
        topo.setAlignment(Pos.CENTER_LEFT);
        Label cliente = new Label(valorOuVazio(d.clienteNome()));
        cliente.getStyleClass().add("title-4");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topo.getChildren().addAll(cliente, spacer, criarBadgeEstado(d.estado()));

        HBox valores = new HBox(12,
                criarResumoValor("Data", d.data() != null ? d.data().format(DATE_FORMATTER) : "—"),
                criarResumoValor("Total", String.format("%.2f %s", d.totalFinal(), d.moedaCodigo())),
                criarResumoValor("Tracking", valorOuVazio(d.codigoTracking()))
        );
        valores.setFillHeight(true);
        card.getChildren().addAll(topo, valores);
        return card;
    }

    private VBox criarResumoValor(String label, String valor) {
        VBox box = new VBox(4);
        box.setMinWidth(135);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        Label val = new Label(valor);
        val.setStyle("-fx-font-weight: 700; -fx-font-size: 15px;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private VBox criarSecaoEdicao(String label, Control input) {
        VBox box = new VBox(8);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        box.getChildren().addAll(lbl, input);
        return box;
    }

    private VBox criarSecaoItensEdicao(List<OrderItemRow> rows, VBox itensEdicao, Button btnAdicionarItem) {
        VBox box = new VBox(12);
        Label lbl = new Label(i18nService.translate("orders.items") + " (" + rows.size() + ")");
        lbl.getStyleClass().add("title-4");
        box.getChildren().addAll(lbl, itensEdicao, btnAdicionarItem);
        return box;
    }

    private void setModoEdicaoEncomenda(boolean editavel,
                                        boolean podeEditarItens,
                                        ComboBox<ClienteSimpleDTO> cliente,
                                        ComboBox<MoedaSimpleDTO> moeda,
                                        ComboBox<EstadoEncomendaCliente> estado,
                                        List<OrderItemRow> rows,
                                        Button btnAdicionarItem) {
        boolean editarItens = editavel && podeEditarItens;
        cliente.setMouseTransparent(!editarItens);
        cliente.setFocusTraversable(editarItens);
        moeda.setMouseTransparent(!editarItens);
        moeda.setFocusTraversable(editarItens);
        estado.setMouseTransparent(!editavel);
        estado.setFocusTraversable(editavel);
        rows.forEach(row -> row.setEditavel(editarItens));
        btnAdicionarItem.setVisible(editarItens);
        btnAdicionarItem.setManaged(editarItens);
    }

    private void atualizarBotoesRemoverEditRows(List<OrderItemRow> rows) {
        boolean podeRemover = rows.size() > 1;
        rows.forEach(row -> row.btnRemover.setDisable(!podeRemover));
    }

    private void selecionarCliente(ComboBox<ClienteSimpleDTO> combo, UUID clienteId) {
        combo.getItems().stream()
                .filter(cliente -> cliente.id().equals(clienteId))
                .findFirst()
                .ifPresent(combo::setValue);
    }

    private void selecionarMoeda(ComboBox<MoedaSimpleDTO> combo, UUID moedaId) {
        combo.getItems().stream()
                .filter(moeda -> moeda.id().equals(moedaId))
                .findFirst()
                .ifPresent(combo::setValue);
    }

    private VBox criarCampoFormulario(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private Label criarErroLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void mostrarErroLabel(Label lbl, Control ctrl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        ctrl.setStyle("-fx-border-color: #ef4444;");
    }

    private void esconderErro(Label lbl, Control ctrl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
        ctrl.setStyle("");
    }

    private void mostrarErroItens(String msg) {
        lblErroItens.setText(msg);
        lblErroItens.setVisible(true);
        lblErroItens.setManaged(true);
    }

    private void esconderErroItens() {
        lblErroItens.setVisible(false);
        lblErroItens.setManaged(false);
    }

    private VBox criarSecao(String titulo) {
        VBox secao = new VBox(0);
        secao.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("title-4");
        lblTitulo.setPadding(new Insets(15));
        lblTitulo.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0;");
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        secao.getChildren().add(lblTitulo);
        return secao;
    }

    private HBox criarCampoLeitura(String label, String valor) {
        HBox campo = new HBox(10);
        campo.setAlignment(Pos.CENTER_LEFT);
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("text-muted");
        lblLabel.setPrefWidth(130);
        Label lblValor = new Label(valorOuVazio(valor));
        lblValor.setStyle("-fx-font-weight: 500;");
        campo.getChildren().addAll(lblLabel, lblValor);
        return campo;
    }

    private String valorOuVazio(String v) { return v != null && !v.isEmpty() ? v : "—"; }

    private HBox criarBadgeEstado(EstadoEncomendaCliente estado) {
        String color = switch (estado) {
            case PENDENTE -> "#eab308";
            case CONFIRMADA -> "#3b82f6";
            case EM_PRODUCAO -> "#8b5cf6";
            case PRONTA -> "#06b6d4";
            case EXPEDIDA -> "#f97316";
            case CANCELADA -> "#ef4444";
        };
        String icon = switch (estado) {
            case PENDENTE -> "mdi2c-clock-outline";
            case CONFIRMADA -> "mdi2c-check-circle-outline";
            case EM_PRODUCAO -> "mdi2c-cog-outline";
            case PRONTA -> "mdi2p-package-variant";
            case EXPEDIDA -> "mdi2t-truck-delivery-outline";
            case CANCELADA -> "mdi2c-close-circle-outline";
        };
        return UiFactory.statusBadge(estado.name(), icon, color);
    }

    private void configurarComboBoxes() {
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(EstadoEncomendaCliente.values()));
    }

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> aplicarFiltrosDinamicos());
        txtFiltroCliente.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                searchDebounce.playFromStart();
            }
        });
        cmbFiltroEstado.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                aplicarFiltrosDinamicos();
            }
        });
    }

    private void aplicarFiltrosDinamicos() {
        pagination.resetPage();
        carregarEncomendas();
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatarNumero(Double value) {
        if (value == null) {
            return "";
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private void mostrarSucesso(String m) { toastService.showSuccess(i18nService.translate("common.success"), m); }
    private void mostrarErro(String m) { toastService.showError(i18nService.translate("common.error"), m); }

    @FXML
    private void handleMostrarTodos() {
        updatingFilters = true;
        searchDebounce.stop();
        txtFiltroCliente.clear();
        cmbFiltroEstado.setValue(null);
        updatingFilters = false;
        aplicarFiltrosDinamicos();
    }
}
