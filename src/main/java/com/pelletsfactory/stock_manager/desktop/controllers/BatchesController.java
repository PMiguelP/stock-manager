package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.LotePelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.LotePelletService;
import com.pelletsfactory.stock_manager.common.services.OrdemProducaoService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
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

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class BatchesController {

    private final LotePelletService lotePelletService;
    private final OrdemProducaoService ordemProducaoService;
    private final NavigationService navigationService;
    private final FormValidationService formValidationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private VBox vboxContainer;
    @FXML private TextField txtFiltroCodigo;
    @FXML private TableView<LotePelletSimpleDTO> tblLotes;
    @FXML private TableColumn<LotePelletSimpleDTO, String> colCodigo;
    @FXML private TableColumn<LotePelletSimpleDTO, String> colTipoPellet;
    @FXML private TableColumn<LotePelletSimpleDTO, Double> colQuantidade;
    @FXML private TableColumn<LotePelletSimpleDTO, String> colDataProducao;
    @FXML private TableColumn<LotePelletSimpleDTO, String> colLocalizacao;
    @FXML private TableColumn<LotePelletSimpleDTO, Void> colAcoes;

    // Drawer fields
    private VBox drawerRoot;
    private ComboBox<OrdemProducaoSimpleDTO> cmbOrdem;
    private Label lblTipoPelletNome;
    private TextField txtCodigoLote;
    private TextField txtQuantidadeKg;
    private TextField txtLocalizacao;
    private Label lblErroOrdem;
    private Label lblErroCodigo;
    private Label lblErroQuantidade;
    private UUID selectedTipoPelletId;

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<LotePelletSimpleDTO> lotes = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public BatchesController(LotePelletService lotePelletService,
                             OrdemProducaoService ordemProducaoService,
                             NavigationService navigationService,
                             FormValidationService formValidationService,
                             ToastService toastService,
                             I18nService i18nService) {
        this.lotePelletService = lotePelletService;
        this.ordemProducaoService = ordemProducaoService;
        this.navigationService = navigationService;
        this.formValidationService = formValidationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        resetPaginationControls();
        configurarTabela();
        configurarDrawer();
        carregarLotes();
    }

    private void resetPaginationControls() {
        lblPaginaStatus = null;
        cmbItemsPerPage = null;
        paginationButtons = null;
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colCodigo.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().codigoLote()));
        configurarColunaTexto(colCodigo);

        // tipoPellet not in SimpleDTO — leave blank; resolved in drawer
        colTipoPellet.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty("—"));
        configurarColunaTexto(colTipoPellet);

        colQuantidade.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().quantidadeKg()));
        colQuantidade.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f", item));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colDataProducao.setCellValueFactory(cd -> {
            var dt = cd.getValue().dataProducao();
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.format(DT_FORMATTER) : "—");
        });
        configurarColunaTexto(colDataProducao);

        colLocalizacao.setCellValueFactory(cd -> {
            String loc = cd.getValue().localizacaoArmazem();
            return new javafx.beans.property.SimpleStringProperty(loc != null ? loc : "—");
        });
        configurarColunaTexto(colLocalizacao);

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDel = new Button();
            {
                btnDel.getStyleClass().addAll("button-icon", "flat");
                btnDel.setGraphic(new FontIcon("mdi2t-trash-can-outline:18"));
                btnDel.setTooltip(new Tooltip(i18nService.translate("common.delete")));
                btnDel.setOnAction(ev -> {
                    LotePelletSimpleDTO lote = getTableView().getItems().get(getIndex());
                    handleEliminarLote(lote);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDel);
                setAlignment(Pos.CENTER);
            }
        });

        tblLotes.setFixedCellSize(48);
        tblLotes.setItems(lotes);
    }

    private <T> void configurarColunaTexto(TableColumn<LotePelletSimpleDTO, T> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void carregarLotes() {
        try {
            String codigo = (txtFiltroCodigo != null && !txtFiltroCodigo.getText().isEmpty())
                    ? txtFiltroCodigo.getText() : null;

            Page<LotePelletSimpleDTO> page = lotePelletService.listarLotesComFiltros(
                    paginaAtual + 1, itemsPerPage, codigo, null, null, "dataProducao", "DESC"
            );

            lotes.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    private void handleEliminarLote(LotePelletSimpleDTO lote) {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle(i18nService.translate("common.delete"));
        confirmacao.setHeaderText(i18nService.translate("common.confirmDelete"));
        confirmacao.setContentText(lote.codigoLote());
        confirmacao.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            lotePelletService.apagarLote(lote.id());
            paginaAtual = 0;
            carregarLotes();
            mostrarSucesso(i18nService.translate("batches.deleted"));
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    private void configurarDrawer() {
        drawerRoot = new VBox(0);
        drawerRoot.setMinWidth(550);
        drawerRoot.setPrefWidth(550);
        drawerRoot.setMaxWidth(550);
        drawerRoot.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label(i18nService.translate("batches.newTitle"));
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

        // Ordem de Produção
        cmbOrdem = new ComboBox<>();
        cmbOrdem.setMaxWidth(Double.MAX_VALUE);
        cmbOrdem.setConverter(new StringConverter<>() {
            @Override
            public String toString(OrdemProducaoSimpleDTO o) {
                if (o == null) return "";
                return o.tipoPelletNome() + " — " + String.format("%.0f kg planeados", o.quantidadePlaneada())
                        + " [" + o.estado().name() + "]";
            }
            @Override public OrdemProducaoSimpleDTO fromString(String s) { return null; }
        });
        lblErroOrdem = formValidationService.createErrorLabel();
        formValidationService.attachComboAutoClear(cmbOrdem, lblErroOrdem);

        // Auto-fill TipoPellet on order selection
        lblTipoPelletNome = new Label("—");
        lblTipoPelletNome.setStyle("-fx-font-weight: 500; -fx-text-fill: -color-fg-default;");
        cmbOrdem.setOnAction(e -> atualizarTipoPellet(cmbOrdem.getValue()));

        VBox campoTipoPellet = new VBox(6);
        Label lblTipoPelletLabel = new Label(i18nService.translate("batches.pelletTypeAuto"));
        lblTipoPelletLabel.getStyleClass().add("text-muted");
        HBox tipoPelletBox = new HBox(8);
        tipoPelletBox.setAlignment(Pos.CENTER_LEFT);
        tipoPelletBox.setPadding(new Insets(10, 12, 10, 12));
        tipoPelletBox.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 4; -fx-border-color: -color-border-default; -fx-border-radius: 4;");
        tipoPelletBox.getChildren().add(lblTipoPelletNome);
        campoTipoPellet.getChildren().addAll(lblTipoPelletLabel, tipoPelletBox);

        // Código do Lote
        txtCodigoLote = new TextField();
        txtCodigoLote.setPromptText("Ex: LOT-2026-001");
        lblErroCodigo = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtCodigoLote, lblErroCodigo);

        // Quantidade
        txtQuantidadeKg = new TextField();
        txtQuantidadeKg.setPromptText("Ex: 1500.00");
        lblErroQuantidade = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtQuantidadeKg, lblErroQuantidade);

        // Localização (optional)
        txtLocalizacao = new TextField();
        txtLocalizacao.setPromptText("Ex: Armazém A, Zona 3 (opcional)");

        form.getChildren().addAll(
                criarCampoFormulario(i18nService.translate("batches.productionOrder") + " *", cmbOrdem, lblErroOrdem),
                campoTipoPellet,
                criarCampoFormulario(i18nService.translate("batches.batchCode") + " *", txtCodigoLote, lblErroCodigo),
                criarCampoFormulario(i18nService.translate("batches.quantityKg") + " *", txtQuantidadeKg, lblErroQuantidade),
                criarCampoFormulario(i18nService.translate("batches.location"), txtLocalizacao)
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
        Button btnSalvar = new Button(i18nService.translate("batches.save"));
        btnSalvar.getStyleClass().add("accent");
        btnSalvar.setPrefHeight(44);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnSalvar, Priority.ALWAYS);
        btnSalvar.setOnAction(e -> handleRegistarLote());
        footer.getChildren().add(btnSalvar);

        drawerRoot.getChildren().addAll(header, scroll, footer);
    }

    private void atualizarTipoPellet(OrdemProducaoSimpleDTO ordem) {
        if (ordem == null) {
            lblTipoPelletNome.setText("—");
            selectedTipoPelletId = null;
            return;
        }
        try {
            var detalhes = ordemProducaoService.obterDetalhes(ordem.id());
            selectedTipoPelletId = detalhes.tipoPelletId();
            lblTipoPelletNome.setText(detalhes.tipoPelletNome() != null ? detalhes.tipoPelletNome() : "—");
        } catch (Exception e) {
            lblTipoPelletNome.setText("Erro ao carregar");
            selectedTipoPelletId = null;
        }
    }

    private void handleRegistarLote() {
        boolean valido = true;
        valido = formValidationService.validateRequiredCombo(cmbOrdem, lblErroOrdem, i18nService.translate("batches.orderRequired")) && valido;
        valido = formValidationService.validateRequiredText(txtCodigoLote, lblErroCodigo, i18nService.translate("batches.codeRequired")) && valido;
        valido = formValidationService.validateRequiredText(txtQuantidadeKg, lblErroQuantidade, i18nService.translate("batches.quantityRequired")) && valido;

        if (!valido) return;

        if (selectedTipoPelletId == null) {
            mostrarErro(i18nService.translate("batches.selectValidOrder"));
            return;
        }

        double quantidade;
        try {
            quantidade = Double.parseDouble(txtQuantidadeKg.getText().trim().replace(",", "."));
            if (!Double.isFinite(quantidade) || quantidade <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            formValidationService.validateRequiredText(txtQuantidadeKg, lblErroQuantidade, i18nService.translate("batches.quantityInvalid"));
            return;
        }

        String localizacao = txtLocalizacao.getText().trim().isEmpty() ? null : txtLocalizacao.getText().trim();

        try {
            lotePelletService.criarLote(new LotePelletRequestDTO(
                    cmbOrdem.getValue().id(),
                    selectedTipoPelletId,
                    txtCodigoLote.getText().trim(),
                    quantidade,
                    localizacao
            ));
            paginaAtual = 0;
            carregarLotes();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("batches.created"));
        } catch (Exception e) {
            mostrarErro(e.getMessage());
        }
    }

    public void openCreateModal() {
        handleAbrirModal();
    }

    @FXML
    private void handleAbrirModal() {
        // Refresh orders list on each open
        try {
            var ordens = ordemProducaoService.listarOrdensComFiltros(1, 200, null, null, null, "dataInicio", "DESC")
                    .getContent();
            cmbOrdem.setItems(FXCollections.observableArrayList(ordens));
        } catch (Exception e) {
            cmbOrdem.setItems(FXCollections.emptyObservableList());
        }

        // Reset form
        cmbOrdem.setValue(null);
        lblTipoPelletNome.setText("—");
        selectedTipoPelletId = null;
        txtCodigoLote.clear();
        txtQuantidadeKg.clear();
        txtLocalizacao.clear();
        formValidationService.clearError(cmbOrdem, lblErroOrdem);
        formValidationService.clearError(txtCodigoLote, lblErroCodigo);
        formValidationService.clearError(txtQuantidadeKg, lblErroQuantidade);

        navigationService.showModal(drawerRoot);
    }

    @FXML
    private void handleFiltrar() {
        paginaAtual = 0;
        carregarLotes();
    }

    @FXML
    private void handleMostrarTodos() {
        txtFiltroCodigo.clear();
        handleFiltrar();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox criarCampoFormulario(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private VBox criarCampoFormulario(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

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
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarLotes(); });
        HBox center = new HBox(10, new Label(i18nService.translate("common.perPage")), cmbItemsPerPage);
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
        prev.setOnAction(e -> { paginaAtual--; carregarLotes(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int fi = i;
                p.setOnAction(e -> { paginaAtual = fi; carregarLotes(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarLotes(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<LotePelletSimpleDTO> page) {
        if (page.getTotalElements() == 0) {
            lblPaginaStatus.setText(i18nService.translate("common.noResults"));
            return;
        }
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText(java.text.MessageFormat.format(
                i18nService.translate("common.showingRange"), start, end, page.getTotalElements()));
    }

    private void mostrarSucesso(String m) { toastService.showSuccess(i18nService.translate("common.success"), m); }
    private void mostrarErro(String m) { toastService.showError(i18nService.translate("common.error"), m); }
}
