package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.OrdemProducaoRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.*;
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

    private PaginationControls pagination;

    private final ObservableList<OrdemProducaoSimpleDTO> ordens = FXCollections.observableArrayList();
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
        configurarTabela();
        configurarDrawerCriarOrdem();
        carregarOrdens();
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void configurarFiltroEstado() {
        cmbEstadoFiltro.setItems(FXCollections.observableArrayList(EstadoOrdemProducao.values()));
        cmbEstadoFiltro.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoOrdemProducao e) {
                if (e == null) return "Todos";
                return switch (e) {
                    case PENDENTE -> "Pendente";
                    case EM_PRODUCAO -> "Em Produção";
                    case CONCLUIDA -> "Concluída";
                    case PAUSADA -> "Pausada";
                    case ANULADA -> "Anulada";
                };
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
            Page<OrdemProducaoSimpleDTO> page = ordemService.listarOrdensComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), estado, null, null, "dataInicio", "DESC");

            ordens.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao carregar ordens: " + e.getMessage());
        }
    }

    // ── Details drawer ────────────────────────────────────────────────────────

    private void handleAbrirDetalhes(OrdemProducaoSimpleDTO ordem) {
        try {
            OrdemProducaoDetailsDTO d = ordemService.obterDetalhes(ordem.id());
            navigationService.showModal(criarDrawerDetalhes(d));
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(OrdemProducaoDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(580);
        HBox header = UiFactory.drawerHeader("Detalhes da Ordem", navigationService::hideModal);

        // ── Section: Informação Geral ─────────────────────────────────────────
        VBox secaoGeral = criarSecao("Informação Geral");
        VBox camposGeral = new VBox(15);
        camposGeral.setPadding(new Insets(15));
        HBox estadoRow = new HBox(10);
        estadoRow.setAlignment(Pos.CENTER_LEFT);
        Label lblEstadoKey = new Label("Estado");
        lblEstadoKey.getStyleClass().add("text-muted");
        lblEstadoKey.setPrefWidth(140);
        estadoRow.getChildren().addAll(lblEstadoKey, criarBadgeEstado(d.estado()));
        camposGeral.getChildren().addAll(
                estadoRow,
                criarCampoLeitura("Tipo de Pellet", d.tipoPelletNome()),
                criarCampoLeitura("Fórmula", d.formulaNome()),
                criarCampoLeitura("Operador", d.funcionarioNome())
        );
        secaoGeral.getChildren().add(camposGeral);

        // ── Section: Produção ─────────────────────────────────────────────────
        VBox secaoProducao = criarSecao("Produção");
        VBox camposProducao = new VBox(15);
        camposProducao.setPadding(new Insets(15));
        camposProducao.getChildren().addAll(
                criarCampoLeitura("Qtd. Planeada (kg)",
                        d.quantidadePlaneada() != null ? String.format("%.2f", d.quantidadePlaneada()) : "—"),
                criarCampoLeitura("Qtd. Produzida (kg)",
                        d.quantidadeProduzidaReal() != null ? String.format("%.2f", d.quantidadeProduzidaReal()) : "—"),
                criarCampoLeitura("Data Início",
                        d.dataInicio() != null ? d.dataInicio().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT) : "—"),
                criarCampoLeitura("Data Fim",
                        d.dataFim() != null ? d.dataFim().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT) : "—")
        );
        secaoProducao.getChildren().add(camposProducao);

        // ── Section: Encomendas Associadas ────────────────────────────────────
        int numEncomendas = d.encomendas() != null ? d.encomendas().size() : 0;
        VBox secaoEncomendas = criarSecao("Encomendas Associadas (" + numEncomendas + ")");
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
                Label tracking = new Label(a.codigoTracking() != null ? a.codigoTracking() : "Sem tracking");
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
        VBox secaoConsumos = criarSecao("Consumos de Matérias-Primas (" + numConsumos + ")");
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
        VBox secaoLotes = criarSecao("Lotes Produzidos (" + numLotes + ")");
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
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(20, 25, 20, 25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        boolean podeEditar = d.estado() != EstadoOrdemProducao.CONCLUIDA && d.estado() != EstadoOrdemProducao.ANULADA;
        boolean podeDeletar = d.estado() == EstadoOrdemProducao.PENDENTE;

        if (podeDeletar) {
            Button btnDeletar = new Button("Eliminar");
            btnDeletar.getStyleClass().addAll("button-outlined", "danger");
            btnDeletar.setGraphic(new FontIcon("mdi2t-trash-can-outline:16"));
            btnDeletar.setPrefHeight(40);
            btnDeletar.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Eliminar a ordem de produção para \"" + d.tipoPelletNome() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Confirmar eliminação");
                confirm.setHeaderText(null);
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        try {
                            ordemService.apagarOrdem(d.id());
                            carregarOrdens();
                            navigationService.hideModal();
                            toastService.showSuccess("Sucesso", "Ordem eliminada.");
                        } catch (Exception ex) {
                            toastService.showError("Erro", "Não foi possível eliminar: " + ex.getMessage());
                        }
                    }
                });
            });
            footer.getChildren().add(btnDeletar);
        }

        Region footerSp = new Region();
        HBox.setHgrow(footerSp, Priority.ALWAYS);
        footer.getChildren().add(footerSp);

        if (podeEditar) {
            Button btnEditar = new Button("Editar");
            btnEditar.getStyleClass().add("accent");
            btnEditar.setGraphic(new FontIcon("mdi2p-pencil-outline:16"));
            btnEditar.setPrefHeight(40);
            btnEditar.setOnAction(e -> navigationService.showModal(criarDrawerEditar(d)));
            footer.getChildren().add(btnEditar);
        }

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
        Label titulo = new Label("Editar Ordem");
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
        VBox secaoGeral = criarSecao("Informação Geral");
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
        txtQtdPlaneada.setPromptText("ex: 500.00");
        Label lblErroQtdPlaneada = criarErroLabel();

        TextField txtQtdProduzida = new TextField(d.quantidadeProduzidaReal() != null ? String.format("%.2f", d.quantidadeProduzidaReal()) : "");
        txtQtdProduzida.setMaxWidth(Double.MAX_VALUE);
        txtQtdProduzida.setPromptText("ex: 450.00");
        Label lblErroQtdProduzida = criarErroLabel();

        DatePicker dpDataInicio = new DatePicker(d.dataInicio() != null
                ? d.dataInicio().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now());
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        Label lblErroDataInicio = criarErroLabel();

        HBox estadoAtualRow = new HBox(10);
        estadoAtualRow.setAlignment(Pos.CENTER_LEFT);
        Label lblAtual = new Label("Estado Atual");
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
                return switch (e) {
                    case PENDENTE -> "Pendente";
                    case EM_PRODUCAO -> "Em Produção";
                    case CONCLUIDA -> "Concluída";
                    case PAUSADA -> "Pausada";
                    case ANULADA -> "Anulada";
                };
            }
            @Override public EstadoOrdemProducao fromString(String s) { return null; }
        });
        cmbNovoEstado.setValue(d.estado());
        Label lblErroEstado = criarErroLabel();

        camposGeral.getChildren().addAll(
                criarCampoFormulario("Tipo de Pellet *", cmbTipoPelletEdicao, lblErroTipoPellet),
                criarCampoFormulario("Fórmula de Produção *", cmbFormulaEdicao, lblErroFormula),
                criarCampoFormulario("Operador *", cmbFuncionarioEdicao, lblErroFuncionario),
                criarCampoFormulario("Estado *", cmbNovoEstado, lblErroEstado)
        );
        secaoGeral.getChildren().add(camposGeral);

        VBox secaoProducao = criarSecao("Produção");
        VBox camposProducao = new VBox(15);
        camposProducao.setPadding(new Insets(15));
        camposProducao.getChildren().addAll(
                criarCampoFormulario("Quantidade Planeada (kg) *", txtQtdPlaneada, lblErroQtdPlaneada),
                criarCampoFormulario("Quantidade Produzida Real (kg)", txtQtdProduzida, lblErroQtdProduzida),
                criarCampoFormulario("Data de Início *", dpDataInicio, lblErroDataInicio)
        );
        secaoProducao.getChildren().add(camposProducao);

        form.getChildren().addAll(estadoAtualRow, secaoGeral, secaoProducao);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(20, 25, 20, 25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnCancelar = new Button("Voltar");
        btnCancelar.getStyleClass().add("button-outlined");
        btnCancelar.setPrefHeight(40);
        btnCancelar.setOnAction(e -> {
            try {
                OrdemProducaoDetailsDTO fresh = ordemService.obterDetalhes(d.id());
                navigationService.showModal(criarDrawerDetalhes(fresh));
            } catch (Exception ex) { navigationService.hideModal(); }
        });
        Button btnGuardar = new Button("Guardar");
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(40);
        HBox.setHgrow(btnGuardar, Priority.ALWAYS);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        btnGuardar.setOnAction(e -> {
            boolean valido = true;

            if (cmbTipoPelletEdicao.getValue() == null) {
                mostrarErroLabel(lblErroTipoPellet, cmbTipoPelletEdicao, "Tipo de pellet é obrigatório");
                valido = false;
            }
            if (cmbFormulaEdicao.getValue() == null) {
                mostrarErroLabel(lblErroFormula, cmbFormulaEdicao, "Fórmula é obrigatória");
                valido = false;
            }
            if (cmbFuncionarioEdicao.getValue() == null) {
                mostrarErroLabel(lblErroFuncionario, cmbFuncionarioEdicao, "Operador é obrigatório");
                valido = false;
            }
            if (cmbNovoEstado.getValue() == null) {
                mostrarErroLabel(lblErroEstado, cmbNovoEstado, "Selecione o estado");
                valido = false;
            }

            Double qtdPlaneada;
            try {
                qtdPlaneada = Double.parseDouble(txtQtdPlaneada.getText().replace(",", ".").trim());
                if (!Double.isFinite(qtdPlaneada) || qtdPlaneada <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                mostrarErroLabel(lblErroQtdPlaneada, txtQtdPlaneada, "Quantidade inválida (> 0)");
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
                    mostrarErroLabel(lblErroQtdProduzida, txtQtdProduzida, "Quantidade produzida inválida");
                    valido = false;
                }
            }

            if (dpDataInicio.getValue() == null) {
                mostrarErroLabel(lblErroDataInicio, dpDataInicio, "Data de início é obrigatória");
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
                toastService.showSuccess("Sucesso", "Ordem atualizada com sucesso.");
            } catch (Exception ex) {
                toastService.showError("Erro", ex.getMessage());
            }
        });
        footer.getChildren().addAll(btnCancelar, btnGuardar);

        root.getChildren().addAll(header, scroll, footer);
        return root;
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    private void configurarDrawerCriarOrdem() {
        criarOrdemDrawer = new VBox(0);
        criarOrdemDrawer.setMinWidth(560);
        criarOrdemDrawer.setPrefWidth(560);
        criarOrdemDrawer.setMaxWidth(560);
        criarOrdemDrawer.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Nova Ordem de Produção");
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
        cmbFormula.setPromptText("Selecione primeiro o tipo de pellet");
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
        txtQuantidade.setPromptText("ex: 500.00");
        txtQuantidade.textProperty().addListener((obs, ov, nv) -> esconderErro(lblErroQuantidade, txtQuantidade));
        lblErroQuantidade = criarErroLabel();

        dpDataInicio = new DatePicker(LocalDate.now());
        dpDataInicio.setMaxWidth(Double.MAX_VALUE);
        dpDataInicio.valueProperty().addListener((obs, ov, nv) -> esconderErro(lblErroData, dpDataInicio));
        lblErroData = criarErroLabel();

        form.getChildren().addAll(
                criarCampoFormulario("Tipo de Pellet *", cmbTipoPellet, lblErroTipoPellet),
                criarCampoFormulario("Fórmula de Produção *", cmbFormula, lblErroFormula),
                criarCampoFormulario("Operador *", cmbFuncionario, lblErroFuncionario),
                criarCampoFormulario("Quantidade Planeada (kg) *", txtQuantidade, lblErroQuantidade),
                criarCampoFormulario("Data de Início *", dpDataInicio, lblErroData)
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(20, 25, 20, 25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("button-outlined");
        btnCancelar.setPrefHeight(40);
        btnCancelar.setOnAction(e -> navigationService.hideModal());
        Button btnCriar = new Button("Criar Ordem");
        btnCriar.getStyleClass().add("accent");
        btnCriar.setPrefHeight(40);
        HBox.setHgrow(btnCriar, Priority.ALWAYS);
        btnCriar.setMaxWidth(Double.MAX_VALUE);
        btnCriar.setOnAction(e -> handleCriarOrdem());
        footer.getChildren().addAll(btnCancelar, btnCriar);

        criarOrdemDrawer.getChildren().addAll(header, scroll, footer);
    }

    private void recarregarFormulas(TipoPelletSimpleDTO tipoPellet) {
        cmbFormula.setValue(null);
        if (tipoPellet == null) {
            cmbFormula.setItems(FXCollections.emptyObservableList());
            cmbFormula.setPromptText("Selecione primeiro o tipo de pellet");
            return;
        }
        try {
            List<FormulaSimpleDTO> formulas = formulaService
                    .listarFormulasPorTipoPellet(tipoPellet.id(), 1, 100)
                    .getContent();
            cmbFormula.setItems(FXCollections.observableArrayList(formulas));
            cmbFormula.setPromptText(formulas.isEmpty() ? "Sem fórmulas disponíveis" : "Selecionar fórmula");
        } catch (Exception e) {
            cmbFormula.setItems(FXCollections.emptyObservableList());
        }
    }

    private void handleCriarOrdem() {
        boolean valido = true;

        if (cmbTipoPellet.getValue() == null) {
            mostrarErroLabel(lblErroTipoPellet, cmbTipoPellet, "Tipo de pellet é obrigatório");
            valido = false;
        }
        if (cmbFormula.getValue() == null) {
            mostrarErroLabel(lblErroFormula, cmbFormula, "Fórmula é obrigatória");
            valido = false;
        }
        if (cmbFuncionario.getValue() == null) {
            mostrarErroLabel(lblErroFuncionario, cmbFuncionario, "Operador é obrigatório");
            valido = false;
        }

        Double quantidade = null;
        try {
            quantidade = Double.parseDouble(txtQuantidade.getText().replace(",", ".").trim());
            if (!Double.isFinite(quantidade) || quantidade <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            mostrarErroLabel(lblErroQuantidade, txtQuantidade, "Insira uma quantidade válida (> 0)");
            valido = false;
        }

        if (dpDataInicio.getValue() == null) {
            mostrarErroLabel(lblErroData, dpDataInicio, "Data de início é obrigatória");
            valido = false;
        }

        if (!valido) return;

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
            toastService.showSuccess("Sucesso", "Ordem de produção criada com sucesso!");
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao criar ordem: " + e.getMessage());
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarOrdens(); }
    @FXML private void handleLimpar() { cmbEstadoFiltro.setValue(null); pagination.resetPage(); carregarOrdens(); }

    @FXML
    private void handleAbrirModal() {
        try {
            cmbTipoPellet.setItems(FXCollections.observableArrayList(
                    stockService.listarTiposPelletComFiltros(1, 500, null, null, "nome", "ASC").getContent()));
        } catch (Exception e) { cmbTipoPellet.setItems(FXCollections.emptyObservableList()); }
        try {
            cmbFuncionario.setItems(FXCollections.observableArrayList(
                    funcionarioService.listarFuncionarios(1, 500, null, null, null, null, "nome", "ASC").getContent()));
        } catch (Exception e) { cmbFuncionario.setItems(FXCollections.emptyObservableList()); }

        cmbTipoPellet.setValue(null);
        cmbFormula.setItems(FXCollections.emptyObservableList());
        cmbFormula.setValue(null);
        cmbFormula.setPromptText("Selecione primeiro o tipo de pellet");
        cmbFuncionario.setValue(null);
        txtQuantidade.clear();
        dpDataInicio.setValue(LocalDate.now());
        esconderErro(lblErroTipoPellet, cmbTipoPellet);
        esconderErro(lblErroFormula, cmbFormula);
        esconderErro(lblErroFuncionario, cmbFuncionario);
        esconderErro(lblErroQuantidade, txtQuantidade);
        esconderErro(lblErroData, dpDataInicio);

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
            case PENDENTE -> "Pendente";
            case EM_PRODUCAO -> "Em Produção";
            case CONCLUIDA -> "Concluída";
            case PAUSADA -> "Pausada";
            case ANULADA -> "Anulada";
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
                    .listarTiposPelletComFiltros(1, 500, null, null, "nome", "ASC")
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
                    .listarFuncionarios(1, 500, null, null, null, null, "nome", "ASC")
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
            combo.setPromptText("Selecione primeiro o tipo de pellet");
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
            combo.setPromptText(formulas.isEmpty() ? "Sem fórmulas disponíveis" : "Selecionar fórmula");
        } catch (Exception e) {
            combo.setItems(FXCollections.emptyObservableList());
            combo.setValue(null);
        }
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
