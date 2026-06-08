package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.OrdemProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.*;
import com.pelletsfactory.stock_manager.common.enums.Cargo;
import com.pelletsfactory.stock_manager.common.enums.EstadoOrdemProducao;
import com.pelletsfactory.stock_manager.common.services.FormulaProducaoService;
import com.pelletsfactory.stock_manager.common.services.FuncionarioService;
import com.pelletsfactory.stock_manager.common.services.OrdemProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class ProductionController {

    private final OrdemProducaoService ordemService;
    private final FormulaProducaoService formulaService;
    private final FuncionarioService funcionarioService;
    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private VBox vboxContainer;
    @FXML private ComboBox<EstadoOrdemProducao> cmbEstadoFiltro;
    @FXML private TextField txtPesquisa;

    @FXML private TableView<OrdemProducaoSimpleDTO> tblOrdens;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, String> colTipoPellet;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, String> colFuncionario;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, String> colDataInicio;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, Double> colQtdPlaneada;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, Double> colQtdProduzida;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, EstadoOrdemProducao> colEstado;
    @FXML private TableColumn<OrdemProducaoSimpleDTO, Void> colAcoes;

    // Drawer – create
    private VBox criarOrdemDrawer;
    private ComboBox<TipoPelletSimpleDTO> cmbTipoPellet;
    private ComboBox<FormulaSimpleDTO> cmbFormula;
    private ComboBox<FuncionarioSimpleDTO> cmbFuncionario;
    private TextField txtQuantidade;
    private DatePicker dpDataInicio;
    private Label lblErroTipoPellet;
    private Label lblErroFormula;
    private Label lblErroFuncionario;
    private Label lblErroQuantidade;
    private Label lblErroData;
    private Label lblErroGeral;

    private PaginationControls pagination;

    private final ObservableList<OrdemProducaoSimpleDTO> ordens = FXCollections.observableArrayList();
    private final PauseTransition searchDebounce = new PauseTransition(javafx.util.Duration.millis(300));
    private boolean updatingFilters;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ProductionController(OrdemProducaoService ordemService,
                                FormulaProducaoService formulaService,
                                FuncionarioService funcionarioService,
                                StockService stockService,
                                NavigationService navigationService,
                                ToastService toastService,
                                I18nService i18nService) {
        this.ordemService = ordemService;
        this.formulaService = formulaService;
        this.funcionarioService = funcionarioService;
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarOrdens, i18nService);
        configurarFiltroEstado();
        configurarPesquisaDinamica();
        configurarTabela();
        configurarDrawerCriarOrdem();
        carregarOrdens();
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void configurarFiltroEstado() {
        cmbEstadoFiltro.setItems(FXCollections.observableArrayList(EstadoOrdemProducao.values()));
        cmbEstadoFiltro.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoOrdemProducao e) {
                return e == null ? i18nService.translate("common.all") : estadoLabel(e);
            }
            @Override public EstadoOrdemProducao fromString(String s) { return null; }
        });
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colTipoPellet.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().tipoPelletNome()));
        configurarColunaTexto(colTipoPellet);

        colFuncionario.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().funcionarioNome()));
        configurarColunaTexto(colFuncionario);

        colDataInicio.setCellValueFactory(cd -> {
            if (cd.getValue().dataInicio() == null)
                return new javafx.beans.property.SimpleStringProperty("—");
            return new javafx.beans.property.SimpleStringProperty(
                    cd.getValue().dataInicio().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT));
        });
        configurarColunaTexto(colDataInicio);

        colQtdPlaneada.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().quantidadePlaneada()));
        colQtdPlaneada.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colQtdProduzida.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().quantidadeProduzidaReal()));
        colQtdProduzida.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (v == null ? "—" : String.format("%.2f", v)));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colEstado.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().estado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(EstadoOrdemProducao e, boolean empty) {
                super.updateItem(e, empty);
                setGraphic(empty || e == null ? null : criarBadgeEstado(e));
                setPadding(new Insets(6, 10, 6, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetalhes = new Button();
            {
                btnDetalhes.getStyleClass().addAll("button-icon", "flat");
                btnDetalhes.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetalhes.setTooltip(new Tooltip("Ver Detalhes"));
                btnDetalhes.setOnAction(e -> {
                    OrdemProducaoSimpleDTO ordem = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(ordem);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDetalhes);
                setAlignment(Pos.CENTER);
            }
        });

        tblOrdens.setFixedCellSize(48);
        tblOrdens.setItems(ordens);
    }

    private <T> void configurarColunaTexto(TableColumn<OrdemProducaoSimpleDTO, T> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void carregarOrdens() {
        try {
            EstadoOrdemProducao estado = cmbEstadoFiltro.getValue();
            Page<OrdemProducaoSimpleDTO> page = ordemService.listarOrdensComPesquisa(
                    pagination.pageNumberForService(), pagination.pageSize(), estado,
                    txtPesquisa != null ? txtPesquisa.getText() : null,
                    "dataInicio", "DESC");

            ordens.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    // ── Details drawer ────────────────────────────────────────────────────────

    private void handleAbrirDetalhes(OrdemProducaoSimpleDTO ordem) {
        try {
            OrdemProducaoDetailsDTO d = ordemService.obterDetalhes(ordem.id());
            navigationService.showModal(criarDrawerDetalhes(d));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(OrdemProducaoDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(580);
        HBox header = UiFactory.drawerHeader(i18nService.translate("production.detailsTitle"), navigationService::hideModal);

        // ── Section: Informação Geral ─────────────────────────────────────────
        VBox secaoGeral = criarSecao(i18nService.translate("production.generalInfo"));
        VBox camposGeral = new VBox(15);
        camposGeral.setPadding(new Insets(15));
        HBox estadoRow = new HBox(10);
        estadoRow.setAlignment(Pos.CENTER_LEFT);
        Label lblEstadoKey = new Label(i18nService.translate("common.status"));
        lblEstadoKey.getStyleClass().add("text-muted");
        lblEstadoKey.setPrefWidth(140);
        estadoRow.getChildren().addAll(lblEstadoKey, criarBadgeEstado(d.estado()));
        camposGeral.getChildren().addAll(
                estadoRow,
                criarCampoLeitura(i18nService.translate("production.pelletType"), d.tipoPelletNome()),
                criarCampoLeitura(i18nService.translate("production.formula"), d.formulaNome()),
                criarCampoLeitura(i18nService.translate("production.operator"), d.funcionarioNome())
        );
        secaoGeral.getChildren().add(camposGeral);

        // ── Section: Produção ─────────────────────────────────────────────────
        VBox secaoProducao = criarSecao(i18nService.translate("production.production"));
        VBox camposProducao = new VBox(15);
        camposProducao.setPadding(new Insets(15));
        camposProducao.getChildren().addAll(
                criarCampoLeitura(i18nService.translate("production.plannedQuantityKg"),
                        d.quantidadePlaneada() != null ? String.format("%.2f", d.quantidadePlaneada()) : "—"),
                criarCampoLeitura(i18nService.translate("production.realProducedQuantityKg"),
                        d.quantidadeProduzidaReal() != null ? String.format("%.2f", d.quantidadeProduzidaReal()) : "—"),
                criarCampoLeitura(i18nService.translate("production.startDate"),
                        d.dataInicio() != null ? d.dataInicio().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT) : "—"),
                criarCampoLeitura(i18nService.translate("production.endDate"),
                        d.dataFim() != null ? d.dataFim().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT) : "—")
        );
        secaoProducao.getChildren().add(camposProducao);

        // ── Section: Encomendas Associadas ────────────────────────────────────
        int numEncomendas = d.encomendas() != null ? d.encomendas().size() : 0;
        VBox secaoEncomendas = criarSecao(i18nService.translate("production.associatedOrders") + " (" + numEncomendas + ")");
        if (d.encomendas() != null && !d.encomendas().isEmpty()) {
            VBox lista = new VBox(8);
            lista.setPadding(new Insets(15));
            for (AlocacaoSimpleDTO a : d.encomendas()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 12; -fx-background-radius: 6;");
                VBox info = new VBox(3);
                Label cliente = new Label(a.clienteNome());
                cliente.setStyle("-fx-font-weight: 500;");
                Label tracking = new Label(a.codigoTracking() != null ? a.codigoTracking() : i18nService.translate("production.noTracking"));
                tracking.getStyleClass().add("text-muted");
                tracking.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
                info.getChildren().addAll(cliente, tracking);
                HBox.setHgrow(info, Priority.ALWAYS);
                VBox rightCol = new VBox(3);
                rightCol.setAlignment(Pos.CENTER_RIGHT);
                Label qtdRes = new Label(String.format("%.2f kg", a.quantidadeReservada()));
                qtdRes.setStyle("-fx-font-weight: 500;");
                Label estadoEnc = new Label(a.estadoEncomenda() != null ? a.estadoEncomenda().name() : "");
                estadoEnc.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
                rightCol.getChildren().addAll(qtdRes, estadoEnc);
                row.getChildren().addAll(info, rightCol);
                lista.getChildren().add(row);
            }
            secaoEncomendas.getChildren().add(lista);
        }

        // ── Section: Consumos ─────────────────────────────────────────────────
        int numConsumos = d.consumos() != null ? d.consumos().size() : 0;
        VBox secaoConsumos = criarSecao(i18nService.translate("production.rawMaterialConsumptions") + " (" + numConsumos + ")");
        if (d.consumos() != null && !d.consumos().isEmpty()) {
            VBox lista = new VBox(8);
            lista.setPadding(new Insets(15));
            for (ConsumoResponseDTO c : d.consumos()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 10; -fx-background-radius: 6;");
                Label nome = new Label(c.materiaPrimaNome());
                nome.setStyle("-fx-font-weight: 500;");
                HBox.setHgrow(nome, Priority.ALWAYS);
                Label qtd = new Label(String.format("%.2f %s", c.quantidadeConsumidaReal(), c.unidade()));
                qtd.getStyleClass().add("text-muted");
                row.getChildren().addAll(nome, qtd);
                lista.getChildren().add(row);
            }
            secaoConsumos.getChildren().add(lista);
        }

        // ── Section: Lotes ────────────────────────────────────────────────────
        int numLotes = d.lotes() != null ? d.lotes().size() : 0;
        VBox secaoLotes = criarSecao(i18nService.translate("production.producedBatches") + " (" + numLotes + ")");
        if (d.lotes() != null && !d.lotes().isEmpty()) {
            VBox lista = new VBox(8);
            lista.setPadding(new Insets(15));
            for (LotePelletSimpleDTO l : d.lotes()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 10; -fx-background-radius: 6;");
                Label codigo = new Label(l.codigoLote());
                codigo.setStyle("-fx-font-weight: 500;");
                HBox.setHgrow(codigo, Priority.ALWAYS);
                Label qtd = new Label(String.format("%.2f kg", l.quantidadeKg()));
                qtd.getStyleClass().add("text-muted");
                row.getChildren().addAll(codigo, qtd);
                lista.getChildren().add(row);
            }
            secaoLotes.getChildren().add(lista);
        }

        VBox form = new VBox(20, secaoGeral, secaoProducao, secaoEncomendas, secaoConsumos, secaoLotes);
        form.setPadding(new Insets(30));

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Footer: Edit + Delete ─────────────────────────────────────────────
        Button btnDeletar = null;

        boolean podeEditar = d.estado() != EstadoOrdemProducao.CONCLUIDA && d.estado() != EstadoOrdemProducao.ANULADA;
        boolean podeDeletar = d.estado() == EstadoOrdemProducao.PENDENTE;

        if (podeDeletar) {
            btnDeletar = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
            btnDeletar.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        i18nService.translate("production.deleteQuestion") + " \"" + d.tipoPelletNome() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle(i18nService.translate("common.confirmDelete"));
                confirm.setHeaderText(null);
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        try {
                            ordemService.apagarOrdem(d.id());
                            carregarOrdens();
                            navigationService.hideModal();
                            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("production.deleted"));
                        } catch (Exception ex) {
                            toastService.showError(i18nService.translate("common.error"), ex.getMessage());
                        }
                    }
                });
            });
        }

        Button btnEditar = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
        btnEditar.setDisable(!podeEditar);
        if (podeEditar) {
            btnEditar.setOnAction(e -> navigationService.showModal(criarDrawerEditar(d)));
        }

        Button btnFechar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        btnFechar.setOnAction(e -> navigationService.hideModal());

        HBox footer = UiFactory.drawerActionFooter(btnDeletar, btnFechar, btnEditar);

        root.getChildren().addAll(header, scroll, footer);
        return root;
    }

    private VBox criarDrawerEditar(OrdemProducaoDetailsDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(560);
        root.setPrefWidth(560);
        root.setMaxWidth(560);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        VBox headerText = new VBox(4);
        Label titulo = new Label(i18nService.translate("production.editTitle"));
        titulo.getStyleClass().add("title-3");
        Label subtitulo = new Label(d.tipoPelletNome());
        subtitulo.getStyleClass().add("text-muted");
        headerText.getChildren().addAll(titulo, subtitulo);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> {
            try {
                OrdemProducaoDetailsDTO fresh = ordemService.obterDetalhes(d.id());
                navigationService.showModal(criarDrawerDetalhes(fresh));
            } catch (Exception ex) { navigationService.hideModal(); }
        });
        header.getChildren().addAll(headerText, sp, btnClose);

        // Form
        VBox form = new VBox(20);
        form.setPadding(new Insets(30));

        // General info
        VBox secaoGeral = criarSecao(i18nService.translate("production.generalInfo"));
        VBox camposGeral = new VBox(15);
        camposGeral.setPadding(new Insets(15));

        ComboBox<TipoPelletSimpleDTO> cmbTipoPelletEdicao = new ComboBox<>();
        cmbTipoPelletEdicao.setMaxWidth(Double.MAX_VALUE);
        cmbTipoPelletEdicao.setConverter(new StringConverter<>() {
            @Override public String toString(TipoPelletSimpleDTO t) { return t == null ? "" : t.nome(); }
            @Override public TipoPelletSimpleDTO fromString(String s) { return null; }
        });
        Label lblErroTipoPellet = criarErroLabel();

        ComboBox<FormulaSimpleDTO> cmbFormulaEdicao = new ComboBox<>();
        cmbFormulaEdicao.setMaxWidth(Double.MAX_VALUE);
        cmbFormulaEdicao.setConverter(new StringConverter<>() {
            @Override public String toString(FormulaSimpleDTO f) { return f == null ? "" : f.nome(); }
            @Override public FormulaSimpleDTO fromString(String s) { return null; }
        });
        Label lblErroFormula = criarErroLabel();

        ComboBox<FuncionarioSimpleDTO> cmbFuncionarioEdicao = new ComboBox<>();
        cmbFuncionarioEdicao.setMaxWidth(Double.MAX_VALUE);
        cmbFuncionarioEdicao.setConverter(new StringConverter<>() {
            @Override public String toString(FuncionarioSimpleDTO f) { return f == null ? "" : f.nome(); }
            @Override public FuncionarioSimpleDTO fromString(String s) { return null; }
        });
        Label lblErroFuncionario = criarErroLabel();

        List<TipoPelletSimpleDTO> tiposPellet = carregarTiposPelletParaEdicao(cmbTipoPelletEdicao, d.tipoPelletId());
        List<FuncionarioSimpleDTO> funcionarios = carregarFuncionariosParaEdicao(cmbFuncionarioEdicao, d.funcionarioId());
        cmbTipoPelletEdicao.valueProperty().addListener((obs, ov, nv) -> {
            esconderErro(lblErroTipoPellet, cmbTipoPelletEdicao);
            carregarFormulasParaEdicao(cmbFormulaEdicao, nv != null ? nv.id() : null, d.formulaId());
        });
        if (cmbTipoPelletEdicao.getValue() != null) {
            carregarFormulasParaEdicao(cmbFormulaEdicao, cmbTipoPelletEdicao.getValue().id(), d.formulaId());
        } else {
            carregarFormulasParaEdicao(cmbFormulaEdicao, null, d.formulaId());
        }

        if (tiposPellet != null) {
            tiposPellet.stream()
                    .filter(t -> t.id() != null && t.id().equals(d.tipoPelletId()))
                    .findFirst()
                    .ifPresent(cmbTipoPelletEdicao::setValue);
        }
        if (funcionarios != null) {
            funcionarios.stream()
                    .filter(f -> f.id() != null && f.id().equals(d.funcionarioId()))
                    .findFirst()
                    .ifPresent(cmbFuncionarioEdicao::setValue);
        }

        if (cmbTipoPelletEdicao.getValue() != null) {
            carregarFormulasParaEdicao(cmbFormulaEdicao, cmbTipoPelletEdicao.getValue().id(), d.formulaId());
        }

        TextField txtQtdPlaneada = new TextField(d.quantidadePlaneada() != null ? String.format("%.2f", d.quantidadePlaneada()) : "");
        txtQtdPlaneada.setMaxWidth(Double.MAX_VALUE);
        txtQtdPlaneada.setPromptText(i18nService.translate("production.quantityExample"));
        Label lblErroQtdPlaneada = criarErroLabel();

        TextField txtQtdProduzida = new TextField(d.quantidadeProduzidaReal() != null ? String.format("%.2f", d.quantidadeProduzidaReal()) : "");
        txtQtdProduzida.setMaxWidth(Double.MAX_VALUE);
        txtQtdProduzida.setPromptText(i18nService.translate("production.producedQuantityExample"));
        Label lblErroQtdProduzida = criarErroLabel();

        DatePicker dpDataInicio = new DatePicker(d.dataInicio() != null
                ? d.dataInicio().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now());
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        Label lblErroDataInicio = criarErroLabel();

        HBox estadoAtualRow = new HBox(10);
        estadoAtualRow.setAlignment(Pos.CENTER_LEFT);
        Label lblAtual = new Label(i18nService.translate("production.currentStatus"));
        lblAtual.getStyleClass().add("text-muted");
        lblAtual.setPrefWidth(140);
        estadoAtualRow.getChildren().addAll(lblAtual, criarBadgeEstado(d.estado()));

        List<EstadoOrdemProducao> estadosValidos = switch (d.estado()) {
            case PENDENTE -> List.of(EstadoOrdemProducao.EM_PRODUCAO, EstadoOrdemProducao.ANULADA);
            case EM_PRODUCAO -> List.of(EstadoOrdemProducao.CONCLUIDA, EstadoOrdemProducao.PAUSADA, EstadoOrdemProducao.ANULADA);
            case PAUSADA -> List.of(EstadoOrdemProducao.EM_PRODUCAO, EstadoOrdemProducao.ANULADA);
            default -> List.of();
        };

        ComboBox<EstadoOrdemProducao> cmbNovoEstado = new ComboBox<>(FXCollections.observableArrayList(estadosValidos));
        cmbNovoEstado.setMaxWidth(Double.MAX_VALUE);
        cmbNovoEstado.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoOrdemProducao e) {
                if (e == null) return "";
                return estadoLabel(e);
            }
            @Override public EstadoOrdemProducao fromString(String s) { return null; }
        });
        cmbNovoEstado.setValue(d.estado());
        Label lblErroEstado = criarErroLabel();

        camposGeral.getChildren().addAll(
                criarCampoFormulario(i18nService.translate("production.pelletType") + " *", cmbTipoPelletEdicao, lblErroTipoPellet),
                criarCampoFormulario(i18nService.translate("production.formula") + " *", cmbFormulaEdicao, lblErroFormula),
                criarCampoFormulario(i18nService.translate("production.operator") + " *", cmbFuncionarioEdicao, lblErroFuncionario),
                criarCampoFormulario(i18nService.translate("common.status") + " *", cmbNovoEstado, lblErroEstado)
        );
        secaoGeral.getChildren().add(camposGeral);

        VBox secaoProducao = criarSecao(i18nService.translate("production.production"));
        VBox camposProducao = new VBox(15);
        camposProducao.setPadding(new Insets(15));
        camposProducao.getChildren().addAll(
                criarCampoFormulario(i18nService.translate("production.plannedQuantityKg") + " *", txtQtdPlaneada, lblErroQtdPlaneada),
                criarCampoFormulario(i18nService.translate("production.realProducedQuantityKg"), txtQtdProduzida, lblErroQtdProduzida),
                criarCampoFormulario(i18nService.translate("production.startDate") + " *", dpDataInicio, lblErroDataInicio)
        );
        secaoProducao.getChildren().add(camposProducao);

        form.getChildren().addAll(estadoAtualRow, secaoGeral, secaoProducao);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        btnCancelar.setOnAction(e -> {
            try {
                OrdemProducaoDetailsDTO fresh = ordemService.obterDetalhes(d.id());
                navigationService.showModal(criarDrawerDetalhes(fresh));
            } catch (Exception ex) { navigationService.hideModal(); }
        });
        Button btnGuardar = UiFactory.drawerPrimaryAction(i18nService.translate("common.save"), "mdi2c-content-save-outline");
        btnGuardar.setOnAction(e -> {
            boolean valido = true;

            if (cmbTipoPelletEdicao.getValue() == null) {
                mostrarErroLabel(lblErroTipoPellet, cmbTipoPelletEdicao, i18nService.translate("production.pelletTypeRequired"));
                valido = false;
            }
            if (cmbFormulaEdicao.getValue() == null) {
                mostrarErroLabel(lblErroFormula, cmbFormulaEdicao, i18nService.translate("production.formulaRequired"));
                valido = false;
            }
            if (cmbFuncionarioEdicao.getValue() == null) {
                mostrarErroLabel(lblErroFuncionario, cmbFuncionarioEdicao, i18nService.translate("production.operatorRequired"));
                valido = false;
            }
            if (cmbNovoEstado.getValue() == null) {
                mostrarErroLabel(lblErroEstado, cmbNovoEstado, i18nService.translate("production.statusRequired"));
                valido = false;
            }

            Double qtdPlaneada;
            try {
                qtdPlaneada = Double.parseDouble(txtQtdPlaneada.getText().replace(",", ".").trim());
                if (!Double.isFinite(qtdPlaneada) || qtdPlaneada <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                mostrarErroLabel(lblErroQtdPlaneada, txtQtdPlaneada, i18nService.translate("production.quantityInvalid"));
                valido = false;
                qtdPlaneada = null;
            }

            Double qtdProduzida = null;
            String qtdProduzidaRaw = txtQtdProduzida.getText() != null ? txtQtdProduzida.getText().trim() : "";
            if (!qtdProduzidaRaw.isEmpty()) {
                try {
                    qtdProduzida = Double.parseDouble(qtdProduzidaRaw.replace(",", "."));
                    if (!Double.isFinite(qtdProduzida) || qtdProduzida < 0) throw new NumberFormatException();
                } catch (Exception ex) {
                    mostrarErroLabel(lblErroQtdProduzida, txtQtdProduzida, i18nService.translate("production.producedQuantityInvalid"));
                    valido = false;
                }
            }

            if (dpDataInicio.getValue() == null) {
                mostrarErroLabel(lblErroDataInicio, dpDataInicio, i18nService.translate("production.startDateRequired"));
                valido = false;
            }

            if (!valido) {
                return;
            }

            try {
                OrdemProducaoRequestDTO dto = new OrdemProducaoRequestDTO(
                        cmbTipoPelletEdicao.getValue().id(),
                        cmbFuncionarioEdicao.getValue().id(),
                        cmbFormulaEdicao.getValue().id(),
                        qtdPlaneada,
                        qtdProduzida,
                        dpDataInicio.getValue().atStartOfDay().toString(),
                        cmbNovoEstado.getValue().name()
                );
                ordemService.atualizarOrdem(d.id(), dto);
                carregarOrdens();
                OrdemProducaoDetailsDTO fresh = ordemService.obterDetalhes(d.id());
                navigationService.showModal(criarDrawerDetalhes(fresh));
                toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("production.updated"));
            } catch (Exception ex) {
                toastService.showError(i18nService.translate("common.error"), ex.getMessage());
            }
        });
        HBox footer = UiFactory.drawerActionFooter(null, btnCancelar, btnGuardar);

        root.getChildren().addAll(header, scroll, footer);
        return root;
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    private void configurarDrawerCriarOrdem() {
        criarOrdemDrawer = UiFactory.drawerRoot(560);
        HBox header = UiFactory.drawerHeader(i18nService.translate("production.newTitle"), navigationService::hideModal);

        // Form
        VBox form = new VBox(20);
        form.setPadding(new Insets(30));

        cmbTipoPellet = new ComboBox<>();
        cmbTipoPellet.setMaxWidth(Double.MAX_VALUE);
        cmbTipoPellet.setConverter(new StringConverter<>() {
            @Override public String toString(TipoPelletSimpleDTO t) { return t == null ? "" : t.nome(); }
            @Override public TipoPelletSimpleDTO fromString(String s) { return null; }
        });
        cmbTipoPellet.valueProperty().addListener((obs, ov, nv) -> {
            esconderErro(lblErroTipoPellet, cmbTipoPellet);
            recarregarFormulas(nv);
        });
        lblErroTipoPellet = criarErroLabel();

        cmbFormula = new ComboBox<>();
        cmbFormula.setMaxWidth(Double.MAX_VALUE);
        cmbFormula.setPromptText(i18nService.translate("production.selectPelletFirst"));
        cmbFormula.setConverter(new StringConverter<>() {
            @Override public String toString(FormulaSimpleDTO f) { return f == null ? "" : f.nome(); }
            @Override public FormulaSimpleDTO fromString(String s) { return null; }
        });
        cmbFormula.valueProperty().addListener((obs, ov, nv) -> esconderErro(lblErroFormula, cmbFormula));
        lblErroFormula = criarErroLabel();

        cmbFuncionario = new ComboBox<>();
        cmbFuncionario.setMaxWidth(Double.MAX_VALUE);
        cmbFuncionario.setConverter(new StringConverter<>() {
            @Override public String toString(FuncionarioSimpleDTO f) { return f == null ? "" : f.nome(); }
            @Override public FuncionarioSimpleDTO fromString(String s) { return null; }
        });
        cmbFuncionario.valueProperty().addListener((obs, ov, nv) -> esconderErro(lblErroFuncionario, cmbFuncionario));
        lblErroFuncionario = criarErroLabel();

        txtQuantidade = new TextField();
        txtQuantidade.setMaxWidth(Double.MAX_VALUE);
        txtQuantidade.setPromptText(i18nService.translate("production.quantityExample"));
        txtQuantidade.textProperty().addListener((obs, ov, nv) -> esconderErro(lblErroQuantidade, txtQuantidade));
        lblErroQuantidade = criarErroLabel();

        dpDataInicio = new DatePicker(LocalDate.now());
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        dpDataInicio.valueProperty().addListener((obs, ov, nv) -> esconderErro(lblErroData, dpDataInicio));
        lblErroData = criarErroLabel();

        lblErroGeral = new Label();
        lblErroGeral.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 8 12; -fx-background-color: #ef444420; -fx-background-radius: 6; -fx-border-color: #ef4444; -fx-border-radius: 6; -fx-border-width: 1;");
        lblErroGeral.setWrapText(true);
        lblErroGeral.setMaxWidth(Double.MAX_VALUE);
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);

        form.getChildren().addAll(
                criarCampoFormulario(i18nService.translate("production.pelletType") + " *", cmbTipoPellet, lblErroTipoPellet),
                criarCampoFormulario(i18nService.translate("production.formula") + " *", cmbFormula, lblErroFormula),
                criarCampoFormulario(i18nService.translate("production.operator") + " *", cmbFuncionario, lblErroFuncionario),
                criarCampoFormulario(i18nService.translate("production.plannedQuantityKg") + " *", txtQuantidade, lblErroQuantidade),
                criarCampoFormulario(i18nService.translate("production.startDate") + " *", dpDataInicio, lblErroData),
                lblErroGeral
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
        btnCancelar.setOnAction(e -> navigationService.hideModal());
        Button btnCriar = UiFactory.drawerPrimaryAction(i18nService.translate("production.createOrder"), "mdi2c-check-circle-outline");
        btnCriar.setOnAction(e -> handleCriarOrdem());
        HBox footer = UiFactory.drawerActionFooter(null, btnCancelar, btnCriar);

        criarOrdemDrawer.getChildren().addAll(header, scroll, footer);
    }

    private void recarregarFormulas(TipoPelletSimpleDTO tipoPellet) {
        cmbFormula.setValue(null);
        if (tipoPellet == null) {
            cmbFormula.setItems(FXCollections.emptyObservableList());
            cmbFormula.setPromptText(i18nService.translate("production.selectPelletFirst"));
            return;
        }
        try {
            List<FormulaSimpleDTO> formulas = formulaService
                    .listarFormulasPorTipoPellet(tipoPellet.id(), 1, 100)
                    .getContent();
            cmbFormula.setItems(FXCollections.observableArrayList(formulas));
            cmbFormula.setPromptText(formulas.isEmpty() ? i18nService.translate("production.noFormulasAvailable") : i18nService.translate("production.selectFormula"));
        } catch (Exception e) {
            cmbFormula.setItems(FXCollections.emptyObservableList());
        }
    }

    private void handleCriarOrdem() {
        boolean valido = true;

        if (cmbTipoPellet.getValue() == null) {
            mostrarErroLabel(lblErroTipoPellet, cmbTipoPellet, i18nService.translate("production.pelletTypeRequired"));
            valido = false;
        }
        if (cmbFormula.getValue() == null) {
            mostrarErroLabel(lblErroFormula, cmbFormula, i18nService.translate("production.formulaRequired"));
            valido = false;
        }
        if (cmbFuncionario.getValue() == null) {
            mostrarErroLabel(lblErroFuncionario, cmbFuncionario, i18nService.translate("production.operatorRequired"));
            valido = false;
        }

        Double quantidade = null;
        try {
            quantidade = Double.parseDouble(txtQuantidade.getText().replace(",", ".").trim());
            if (!Double.isFinite(quantidade) || quantidade <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            mostrarErroLabel(lblErroQuantidade, txtQuantidade, i18nService.translate("production.quantityInvalid"));
            valido = false;
        }

        if (dpDataInicio.getValue() == null) {
            mostrarErroLabel(lblErroData, dpDataInicio, i18nService.translate("production.startDateRequired"));
            valido = false;
        }

        if (!valido) return;

        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);
        try {
            OrdemProducaoRequestDTO dto = new OrdemProducaoRequestDTO(
                    cmbTipoPellet.getValue().id(),
                    cmbFuncionario.getValue().id(),
                    cmbFormula.getValue().id(),
                    quantidade,
                    null,
                    dpDataInicio.getValue().atStartOfDay().toString(),
                    "PENDENTE"
            );
            ordemService.criarOrdem(dto);
            pagination.resetPage();
            carregarOrdens();
            navigationService.hideModal();
            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("production.created"));
        } catch (Exception e) {
            lblErroGeral.setText(e.getMessage() != null ? e.getMessage() : "Erro ao criar ordem de produção.");
            lblErroGeral.setVisible(true);
            lblErroGeral.setManaged(true);
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(event -> aplicarFiltrosDinamicos());
        txtPesquisa.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                searchDebounce.playFromStart();
            }
        });
        cmbEstadoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                aplicarFiltrosDinamicos();
            }
        });
    }

    private void aplicarFiltrosDinamicos() {
        pagination.resetPage();
        carregarOrdens();
    }

    @FXML private void handleLimpar() {
        updatingFilters = true;
        searchDebounce.stop();
        txtPesquisa.clear();
        cmbEstadoFiltro.setValue(null);
        updatingFilters = false;
        aplicarFiltrosDinamicos();
    }

    @FXML
    private void handleAbrirModal() {
        try {
            cmbTipoPellet.setItems(FXCollections.observableArrayList(
                    stockService.listarTiposPelletComFiltros(1, 100, null, null, "nome", "ASC").getContent()));
        } catch (Exception e) {
            cmbTipoPellet.setItems(FXCollections.emptyObservableList());
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
        try {
            cmbFuncionario.setItems(FXCollections.observableArrayList(
                    funcionarioService.listarFuncionarios(1, 100, null, null, Cargo.OPERADOR_PRODUCAO, null, "nome", "ASC").getContent()));
        } catch (Exception e) {
            cmbFuncionario.setItems(FXCollections.emptyObservableList());
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }

        cmbTipoPellet.setValue(null);
        cmbFormula.setItems(FXCollections.emptyObservableList());
        cmbFormula.setValue(null);
        cmbFormula.setPromptText(i18nService.translate("production.selectPelletFirst"));
        cmbFuncionario.setValue(null);
        txtQuantidade.clear();
        dpDataInicio.setValue(LocalDate.now());
        esconderErro(lblErroTipoPellet, cmbTipoPellet);
        esconderErro(lblErroFormula, cmbFormula);
        esconderErro(lblErroFuncionario, cmbFuncionario);
        esconderErro(lblErroQuantidade, txtQuantidade);
        esconderErro(lblErroData, dpDataInicio);
        lblErroGeral.setVisible(false);
        lblErroGeral.setManaged(false);

        navigationService.showModal(criarOrdemDrawer);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HBox criarBadgeEstado(EstadoOrdemProducao estado) {
        String color = switch (estado) {
            case PENDENTE -> "#eab308";
            case EM_PRODUCAO -> "#3b82f6";
            case CONCLUIDA -> "#22c55e";
            case PAUSADA -> "#f97316";
            case ANULADA -> "#ef4444";
        };
        String icon = switch (estado) {
            case PENDENTE -> "mdi2c-clock-outline";
            case EM_PRODUCAO -> "mdi2c-cog-outline";
            case CONCLUIDA -> "mdi2c-check-circle-outline";
            case PAUSADA -> "mdi2p-pause-circle-outline";
            case ANULADA -> "mdi2c-close-circle-outline";
        };
        String label = switch (estado) {
            case PENDENTE -> i18nService.translate("production.status.PENDENTE");
            case EM_PRODUCAO -> i18nService.translate("production.status.EM_PRODUCAO");
            case CONCLUIDA -> i18nService.translate("production.status.CONCLUIDA");
            case PAUSADA -> i18nService.translate("production.status.PAUSADA");
            case ANULADA -> i18nService.translate("production.status.ANULADA");
        };
        return UiFactory.statusBadge(label, icon, color);
    }

    private VBox criarSecao(String tituloText) {
        VBox secao = new VBox(0);
        secao.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label lbl = new Label(tituloText);
        lbl.getStyleClass().add("title-4");
        lbl.setPadding(new Insets(15));
        lbl.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        secao.getChildren().add(lbl);
        return secao;
    }

    private HBox criarCampoLeitura(String labelText, String valor) {
        HBox campo = new HBox(10);
        campo.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("text-muted");
        lbl.setPrefWidth(140);
        Label val = new Label(valor != null && !valor.isEmpty() ? valor : "—");
        val.setStyle("-fx-font-weight: 500;");
        campo.getChildren().addAll(lbl, val);
        return campo;
    }

    private VBox criarCampoFormulario(String labelText, Control input, Label erroLabel) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private List<TipoPelletSimpleDTO> carregarTiposPelletParaEdicao(ComboBox<TipoPelletSimpleDTO> combo, UUID selectedId) {
        try {
            List<TipoPelletSimpleDTO> tipos = stockService
                    .listarTiposPelletComFiltros(1, 100, null, null, "nome", "ASC")
                    .getContent();
            combo.setItems(FXCollections.observableArrayList(tipos));
            if (selectedId != null) {
                tipos.stream()
                        .filter(t -> selectedId.equals(t.id()))
                        .findFirst()
                        .ifPresent(combo::setValue);
            }
            return tipos;
        } catch (Exception e) {
            combo.setItems(FXCollections.emptyObservableList());
            return List.of();
        }
    }

    private List<FuncionarioSimpleDTO> carregarFuncionariosParaEdicao(ComboBox<FuncionarioSimpleDTO> combo, UUID selectedId) {
        try {
            List<FuncionarioSimpleDTO> funcionarios = funcionarioService
                    .listarFuncionarios(1, 100, null, null, Cargo.OPERADOR_PRODUCAO, null, "nome", "ASC")
                    .getContent();
            combo.setItems(FXCollections.observableArrayList(funcionarios));
            if (selectedId != null) {
                funcionarios.stream()
                        .filter(f -> selectedId.equals(f.id()))
                        .findFirst()
                        .ifPresent(combo::setValue);
            }
            return funcionarios;
        } catch (Exception e) {
            combo.setItems(FXCollections.emptyObservableList());
            return List.of();
        }
    }

    private void carregarFormulasParaEdicao(ComboBox<FormulaSimpleDTO> combo, UUID tipoPelletId, UUID selectedFormulaId) {
        if (tipoPelletId == null) {
            combo.setItems(FXCollections.emptyObservableList());
            combo.setValue(null);
            combo.setPromptText(i18nService.translate("production.selectPelletFirst"));
            return;
        }

        try {
            List<FormulaSimpleDTO> formulas = formulaService
                    .listarFormulasPorTipoPellet(tipoPelletId, 1, 100)
                    .getContent();
            combo.setItems(FXCollections.observableArrayList(formulas));
            if (selectedFormulaId != null) {
                formulas.stream()
                        .filter(f -> selectedFormulaId.equals(f.id()))
                        .findFirst()
                        .ifPresent(combo::setValue);
            }
            combo.setPromptText(formulas.isEmpty() ? i18nService.translate("production.noFormulasAvailable") : i18nService.translate("production.selectFormula"));
        } catch (Exception e) {
            combo.setItems(FXCollections.emptyObservableList());
            combo.setValue(null);
        }
    }

    private String estadoLabel(EstadoOrdemProducao estado) {
        return estado == null ? "" : i18nService.translate("production.status." + estado.name());
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
}
