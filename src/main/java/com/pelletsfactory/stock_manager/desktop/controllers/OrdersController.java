package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.ClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
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
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OrdersController {
    private final VendaService vendaService;
    private final MoedaService moedaService;
    private final NavigationService navigationService;
    private final ToastService toastService;

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

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<EncomendaClienteSimpleDTO> encomendas = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public OrdersController(VendaService vendaService,
                            MoedaService moedaService,
                            NavigationService navigationService,
                            ToastService toastService) {
        this.vendaService = vendaService;
        this.moedaService = moedaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        resetPaginationControls();
        configurarTabela();
        configurarComboBoxes();
        configurarDrawerCriarEncomenda();
        carregarEncomendas();
    }

    private void resetPaginationControls() {
        lblPaginaStatus = null;
        cmbItemsPerPage = null;
        paginationButtons = null;
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

            Page<EncomendaClienteSimpleDTO> page = vendaService.listarEncomendasComFiltrosSimples(
                    null, estado, paginaAtual + 1, itemsPerPage, "data", "DESC"
            );

            encomendas.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro("Erro ao carregar: " + e.getMessage());
        }
    }

    // ── Create order drawer ───────────────────────────────────────────────────

    private void configurarDrawerCriarEncomenda() {
        criarEncomendaDrawer = new VBox(0);
        criarEncomendaDrawer.setMinWidth(550);
        criarEncomendaDrawer.setPrefWidth(550);
        criarEncomendaDrawer.setMaxWidth(550);
        criarEncomendaDrawer.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Nova Encomenda");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

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

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox();
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnCriar = new Button("Criar Encomenda");
        btnCriar.getStyleClass().add("accent");
        btnCriar.setPrefHeight(44);
        btnCriar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCriar, Priority.ALWAYS);
        btnCriar.setOnAction(e -> handleCriarEncomenda());
        footer.getChildren().add(btnCriar);

        criarEncomendaDrawer.getChildren().addAll(header, scroll, footer);
    }

    private void handleCriarEncomenda() {
        boolean valido = true;
        if (cmbCliente.getValue() == null) {
            mostrarErroLabel(lblErroCliente, cmbCliente, "Cliente é obrigatório");
            valido = false;
        }
        if (cmbMoeda.getValue() == null) {
            mostrarErroLabel(lblErroMoeda, cmbMoeda, "Moeda é obrigatória");
            valido = false;
        }
        if (!valido) return;

        try {
            vendaService.criarPedidoVenda(cmbCliente.getValue().id(), cmbMoeda.getValue().id());
            paginaAtual = 0;
            carregarEncomendas();
            navigationService.hideModal();
            mostrarSucesso("Encomenda criada com sucesso!");
        } catch (Exception e) {
            mostrarErro("Erro ao criar encomenda: " + e.getMessage());
        }
    }

    public void openCreateModal() {
        handleAbrirModal();
    }

    @FXML
    private void handleAbrirModal() {
        // Reload dropdowns fresh on each open
        try {
            cmbCliente.setItems(FXCollections.observableArrayList(vendaService.listarTodosClientesSimples()));
        } catch (Exception e) { cmbCliente.setItems(FXCollections.emptyObservableList()); }
        try {
            cmbMoeda.setItems(FXCollections.observableArrayList(moedaService.listarTodosSimplesDTO()));
        } catch (Exception e) { cmbMoeda.setItems(FXCollections.emptyObservableList()); }

        cmbCliente.setValue(null);
        cmbMoeda.setValue(null);
        esconderErro(lblErroCliente, cmbCliente);
        esconderErro(lblErroMoeda, cmbMoeda);

        navigationService.showModal(criarEncomendaDrawer);
    }

    // ── View order details drawer ─────────────────────────────────────────────

    private void handleAbrirDetalhes(EncomendaClienteSimpleDTO enc) {
        try {
            EncomendaClienteDetailsDTO d = vendaService.obterDetalhesEncomendaCliente(enc.id());
            navigationService.showModal(criarDrawerVisualizacao(d));
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerVisualizacao(EncomendaClienteDetailsDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Detalhes da Encomenda");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        VBox secaoCliente = criarSecao("Informação do Cliente");
        VBox camposCliente = new VBox(15);
        camposCliente.setPadding(new Insets(15));
        camposCliente.getChildren().addAll(
                criarCampoLeitura("Cliente", d.clienteNome()),
                criarCampoLeitura("Data", d.data() != null ? d.data().format(DATE_FORMATTER) : "")
        );
        secaoCliente.getChildren().add(camposCliente);

        VBox secaoDetalhes = criarSecao("Detalhes da Encomenda");
        VBox camposDetalhes = new VBox(15);
        camposDetalhes.setPadding(new Insets(15));
        HBox estadoBox = new HBox(10);
        estadoBox.setAlignment(Pos.CENTER_LEFT);
        Label lblEstadoLabel = new Label("Estado");
        lblEstadoLabel.getStyleClass().add("text-muted");
        lblEstadoLabel.setPrefWidth(120);
        estadoBox.getChildren().addAll(lblEstadoLabel, criarBadgeEstado(d.estado()));
        camposDetalhes.getChildren().addAll(
                estadoBox,
                criarCampoLeitura("Total Líquido", String.format("%.2f %s", d.totalNet(), d.moedaCodigo())),
                criarCampoLeitura("IVA", String.format("%.2f %s", d.totalIva(), d.moedaCodigo())),
                criarCampoLeitura("Total Final", String.format("%.2f %s", d.totalFinal(), d.moedaCodigo())),
                criarCampoLeitura("Código Tracking", valorOuVazio(d.codigoTracking()))
        );
        secaoDetalhes.getChildren().add(camposDetalhes);

        VBox secaoItens = criarSecao("Itens (" + (d.itens() != null ? d.itens().size() : 0) + ")");
        if (d.itens() != null && !d.itens().isEmpty()) {
            VBox lista = new VBox(10);
            lista.setPadding(new Insets(15));
            for (ItemEncomendaClienteResponseDTO item : d.itens()) {
                VBox itemBox = new VBox(4);
                itemBox.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 12; -fx-background-radius: 6;");
                Label lblTipo = new Label(item.tipoPelletNome());
                lblTipo.setStyle("-fx-font-weight: 500;");
                Label lblQtd = new Label(String.format("Quantidade: %.2f kg", item.quantidadeKg()));
                Label lblPreco = new Label(String.format("Preço Unit.: %.2f  |  IVA (%.1f%%): %.2f",
                        item.precoUnitarioNet(), item.taxaIva(), item.valorIvaCalculado()));
                itemBox.getChildren().addAll(lblTipo, lblQtd, lblPreco);
                lista.getChildren().add(itemBox);
            }
            secaoItens.getChildren().add(lista);
        }

        VBox form = new VBox(20, secaoCliente, secaoDetalhes, secaoItens);
        form.setPadding(new Insets(30));

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = switch (estado) {
            case PENDENTE -> "#eab308";
            case CONFIRMADA -> "#3b82f6";
            case EM_PRODUCAO -> "#8b5cf6";
            case PRONTA -> "#06b6d4";
            case EXPEDIDA -> "#f97316";
            case CANCELADA -> "#ef4444";
        };
        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s20; -fx-border-color: %s;",
                color.replace("#", ""), color));

        String icon = switch (estado) {
            case PENDENTE -> "mdi2c-clock-outline";
            case CONFIRMADA -> "mdi2c-check-circle-outline";
            case EM_PRODUCAO -> "mdi2c-cog-outline";
            case PRONTA -> "mdi2p-package-variant";
            case EXPEDIDA -> "mdi2t-truck-delivery-outline";
            case CANCELADA -> "mdi2c-close-circle-outline";
        };
        FontIcon ic = new FontIcon(icon);
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        Label l = new Label(estado.name());
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
        b.getChildren().addAll(ic, l);
        return b;
    }

    // ── Pagination ────────────────────────────────────────────────────────────

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
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarEncomendas(); });
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
        prev.setOnAction(e -> { paginaAtual--; carregarEncomendas(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int fi = i;
                p.setOnAction(e -> { paginaAtual = fi; carregarEncomendas(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarEncomendas(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<EncomendaClienteSimpleDTO> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }

    private void configurarComboBoxes() {
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(EstadoEncomendaCliente.values()));
    }

    private void mostrarSucesso(String m) { toastService.showSuccess("Sucesso", m); }
    private void mostrarErro(String m) { toastService.showError("Erro", m); }

    @FXML private void handleFiltrar() { paginaAtual = 0; carregarEncomendas(); }

    @FXML
    private void handleMostrarTodos() {
        txtFiltroCliente.clear();
        cmbFiltroEstado.setValue(null);
        handleFiltrar();
    }
}
