package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.*;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.services.CompraService;
import com.pelletsfactory.stock_manager.common.services.FornecedorService;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.common.utils.CalculationUtils;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.PurchaseOrderPdfService;
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
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PurchaseOrdersController {

    private final CompraService compraService;
    private final FornecedorService fornecedorService;
    private final StockService stockService;
    private final MoedaService moedaService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;
    private final PurchaseOrderPdfService pdfService;

    @FXML private VBox vboxContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<EstadoEncomendaFornecedor> cmbFiltroStatus;
    @FXML private TableView<EncomendaFornecedorSimpleDTO> tblOrders;
    @FXML private TableColumn<EncomendaFornecedorSimpleDTO, Void> colPoNumber;
    @FXML private TableColumn<EncomendaFornecedorSimpleDTO, String> colSupplier;
    @FXML private TableColumn<EncomendaFornecedorSimpleDTO, String> colOrderDate;
    @FXML private TableColumn<EncomendaFornecedorSimpleDTO, String> colGrandTotal;
    @FXML private TableColumn<EncomendaFornecedorSimpleDTO, Void> colStatus;
    @FXML private TableColumn<EncomendaFornecedorSimpleDTO, Void> colActions;

    private UUID moedaPadraoId;
    private List<MateriaPrimaSimpleDTO> materiais = new ArrayList<>();
    private List<FornecedorSimpleDTO> fornecedores = new ArrayList<>();
    private VBox drawerCriar;

    private PaginationControls pagination;
    private VBox itemsContainerRef;
    private Label lblCriarSubtotal, lblCriarVat, lblCriarGrandTotal;
    private ComboBox<FornecedorSimpleDTO> cmbFornecedorCriar;
    private final List<ItemRow> itemRows = new ArrayList<>();
    private final ObservableList<EncomendaFornecedorSimpleDTO> encomendas = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double DEFAULT_VAT_RATE = 23.0;

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private boolean updatingSearch;

    public PurchaseOrdersController(CompraService compraService,
                                    FornecedorService fornecedorService,
                                    StockService stockService,
                                    MoedaService moedaService,
                                    NavigationService navigationService,
                                    ToastService toastService,
                                    I18nService i18nService,
                                    PurchaseOrderPdfService pdfService) {
        this.compraService = compraService;
        this.fornecedorService = fornecedorService;
        this.stockService = stockService;
        this.moedaService = moedaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
        this.pdfService = pdfService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarEncomendas, i18nService);
        configurarFiltroStatus();
        configurarPesquisaDinamica();
        carregarDadosAuxiliares();
        configurarTabela();
        configurarDrawerCriar();
        carregarEncomendas();
    }

    // ── Filters & search ─────────────────────────────────────────────────────

    private void configurarFiltroStatus() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(EstadoEncomendaFornecedor.values()));
        cmbFiltroStatus.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoEncomendaFornecedor e) {
                if (e == null) return "";
                return estadoLabel(e);
            }
            @Override public EstadoEncomendaFornecedor fromString(String s) { return null; }
        });
    }

    private void configurarPesquisaDinamica() {
        searchDebounce.setOnFinished(e -> aplicarPesquisa());
        txtSearch.textProperty().addListener((obs, o, n) -> {
            if (!updatingSearch) searchDebounce.playFromStart();
        });
        cmbFiltroStatus.valueProperty().addListener((obs, o, n) -> {
            if (!updatingSearch) aplicarPesquisa();
        });
    }

    private void aplicarPesquisa() {
        pagination.resetPage();
        carregarEncomendas();
    }

    @FXML
    private void handleLimpar() {
        updatingSearch = true;
        searchDebounce.stop();
        txtSearch.clear();
        cmbFiltroStatus.setValue(null);
        updatingSearch = false;
        aplicarPesquisa();
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void carregarDadosAuxiliares() {
        try {
            List<MoedaSimpleDTO> moedas = moedaService.listarTodosSimplesDTO();
            if (!moedas.isEmpty()) moedaPadraoId = moedas.get(0).id();
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), i18nService.translate("purchaseOrders.loadCurrencyError"));
        }
        try {
            materiais = stockService.listarMateriasPrimasComFiltros(1, 100, null, null, null, "nome", "ASC").getContent();
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), i18nService.translate("rawMaterials.loadError"));
        }
        try {
            fornecedores = fornecedorService.listarTodosFornecedoresSimples();
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), i18nService.translate("suppliers.loadError"));
        }
    }

    private void carregarEncomendas() {
        try {
            EstadoEncomendaFornecedor estado = cmbFiltroStatus.getValue();
            Page<EncomendaFornecedorSimpleDTO> page = compraService.listarEncomendasComFiltrosSimples(
                    null, estado, pagination.pageNumberForService(), pagination.pageSize(), "data", "DESC");

            String search = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
            List<EncomendaFornecedorSimpleDTO> content = new ArrayList<>(page.getContent());
            if (!search.isEmpty()) {
                content.removeIf(dto -> dto.fornecedorNome() == null
                        || !dto.fornecedorNome().toLowerCase().contains(search));
            }
            encomendas.setAll(content);
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void configurarTabela() {
        colPoNumber.setCellFactory(param -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                int seq = pagination.currentPage() * pagination.pageSize() + getIndex() + 1;
                Label lbl = new Label(String.format("PO-%03d", seq));
                lbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                setGraphic(lbl);
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colSupplier.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().fornecedorNome()));
        configurarColunaTexto(colSupplier);

        colOrderDate.setCellValueFactory(cd -> {
            LocalDate d = cd.getValue().data();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "—");
        });
        configurarColunaTexto(colOrderDate);

        colGrandTotal.setCellValueFactory(cd -> {
            Double total = cd.getValue().totalFinal();
            String moeda = cd.getValue().moedaCodigo();
            String symbol = (moeda != null && moeda.equals("EUR")) ? "€" : (moeda != null ? moeda + " " : "");
            return new javafx.beans.property.SimpleStringProperty(
                    total != null ? String.format("%s%.2f", symbol, total) : "—");
        });
        configurarColunaTexto(colGrandTotal);

        colStatus.setCellFactory(param -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                EncomendaFornecedorSimpleDTO dto = getTableView().getItems().get(getIndex());
                setGraphic(criarBadgeEstado(dto.estado()));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setTooltip(new Tooltip(i18nService.translate("common.view")));
                btnDetails.setOnAction(event -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        EncomendaFornecedorSimpleDTO dto = getTableView().getItems().get(idx);
                        handleAbrirDetalhes(dto, pagination.currentPage() * pagination.pageSize() + idx + 1);
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblOrders.setFixedCellSize(48);
        tblOrders.setItems(encomendas);
    }

    private <T> void configurarColunaTexto(TableColumn<EncomendaFornecedorSimpleDTO, T> coluna) {
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    // ── Details drawer ────────────────────────────────────────────────────────

    @FXML
    private void handleAbrirModal() {
        if (itemsContainerRef != null) itemsContainerRef.getChildren().clear();
        atualizarResumoCriacao(0, 0, 0);
        if (cmbFornecedorCriar != null) cmbFornecedorCriar.setValue(null);
        itemRows.clear();
        navigationService.showModal(drawerCriar);
    }

    private void handleAbrirDetalhes(EncomendaFornecedorSimpleDTO dto, int poSeq) {
        try {
            EncomendaFornecedorDetailsDTO details = compraService.obterDetalhesEncomendaFornecedor(dto.id());
            navigationService.showModal(criarDrawerDetalhes(details, poSeq));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(EncomendaFornecedorDetailsDTO d, int poSeq) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(
                i18nService.translate("purchaseOrders.detailsTitle"), navigationService::hideModal);

        // ── Secção view (read-only) ───────────────────────────────────────────
        VBox secInfoView = criarSecao(i18nService.translate("purchaseOrders.orderInformation"));
        secInfoView.getChildren().addAll(
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.poNumber"), String.format("PO-%03d", poSeq), true),
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.supplier"), valorOuTraco(d.fornecedorNome()), false),
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.orderDate"), d.data() != null ? d.data().format(DATE_FMT) : "—", false),
                criarLinhaDetalheComBadge(i18nService.translate("purchaseOrders.status"), criarBadgeEstado(d.estado()))
        );

        // ── Secção edit (editável, RASCUNHO) ─────────────────────────────────
        ComboBox<FornecedorSimpleDTO> cmbFornecedorEdit = new ComboBox<>(FXCollections.observableArrayList(fornecedores));
        cmbFornecedorEdit.setMaxWidth(Double.MAX_VALUE);
        cmbFornecedorEdit.setConverter(new StringConverter<>() {
            @Override public String toString(FornecedorSimpleDTO f) { return f != null ? f.nome() : ""; }
            @Override public FornecedorSimpleDTO fromString(String s) { return null; }
        });
        fornecedores.stream().filter(f -> f.id().equals(d.fornecedorId())).findFirst()
                .ifPresent(cmbFornecedorEdit::setValue);

        TextField txtDataEdit = new TextField(d.data() != null ? d.data().format(DATE_FMT) : "");
        txtDataEdit.setPromptText("dd/MM/yyyy");
        txtDataEdit.setMaxWidth(Double.MAX_VALUE);

        ComboBox<EstadoEncomendaFornecedor> cmbStatusEdit = new ComboBox<>(FXCollections.observableArrayList(
                EstadoEncomendaFornecedor.RASCUNHO, EstadoEncomendaFornecedor.EFETIVA, EstadoEncomendaFornecedor.ANULADA));
        cmbStatusEdit.setConverter(new StringConverter<>() {
            @Override public String toString(EstadoEncomendaFornecedor e) {
                if (e == null) return "";
                return estadoLabel(e);
            }
            @Override public EstadoEncomendaFornecedor fromString(String s) { return null; }
        });
        cmbStatusEdit.setValue(d.estado());
        cmbStatusEdit.setMaxWidth(Double.MAX_VALUE);

        Label lblErroEdit = new Label();
        lblErroEdit.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-color: #ef444418; -fx-background-radius: 6;");
        lblErroEdit.setWrapText(true);
        lblErroEdit.setMaxWidth(Double.MAX_VALUE);
        lblErroEdit.setVisible(false); lblErroEdit.setManaged(false);

        VBox secInfoEdit = criarSecao(i18nService.translate("purchaseOrders.orderInformation"));
        secInfoEdit.getChildren().addAll(
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.poNumber"), String.format("PO-%03d", poSeq), true),
                criarCampoFormulario(i18nService.translate("purchaseOrders.supplier"), cmbFornecedorEdit),
                criarCampoFormulario(i18nService.translate("purchaseOrders.orderDateWithFormat"), txtDataEdit),
                criarCampoFormulario(i18nService.translate("purchaseOrders.status"), cmbStatusEdit),
                lblErroEdit
        );
        secInfoEdit.setVisible(false);
        secInfoEdit.setManaged(false);

        // ── Itens (sempre read-only) ──────────────────────────────────────────
        VBox secItems = criarSecao(i18nService.translate("purchaseOrders.orderItems"));
        if (d.itens() != null && !d.itens().isEmpty()) {
            for (ItemEncomendaFornecedorResponseDTO item : d.itens()) {
                double qty   = item.quantidade()       != null ? item.quantidade()       : 0;
                double price = item.precoUnitarioNet() != null ? item.precoUnitarioNet() : 0;
                Label lbl = new Label(String.format("%s  ×  %.2f %s  @  €%.2f",
                        valorOuTraco(item.materiaPrimaNome()), qty, valorOuTraco(item.unidade()), price));
                lbl.setStyle("-fx-font-size: 12px;");
                secItems.getChildren().add(lbl);
            }
        } else {
            Label no = new Label("—"); no.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-muted;");
            secItems.getChildren().add(no);
        }

        VBox secTotal = criarSecao(i18nService.translate("purchaseOrders.orderTotal"));
        secTotal.getChildren().addAll(
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.subtotal"), d.totalLiquido() != null ? formatMoney(d.totalLiquido()) : "—", false),
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.vat"), d.totalIva() != null ? formatMoney(d.totalIva()) : "—", false),
                criarLinhaDetalhe(i18nService.translate("purchaseOrders.grandTotal"), d.totalFinal() != null ? formatMoney(d.totalFinal()) : "—", true)
        );

        VBox content = new VBox(20, secInfoView, secInfoEdit, secItems, secTotal);
        content.setPadding(new Insets(30));
        ScrollPane scroll = UiFactory.transparentScroll(content);

        // ── Footer por estado ─────────────────────────────────────────────────
        EstadoEncomendaFornecedor estado = d.estado();
        HBox footer;

        if (estado == EstadoEncomendaFornecedor.RASCUNHO) {
            Button btnApagar   = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
            Button btnCancelar = UiFactory.drawerNeutralAction(i18nService.translate("common.cancel"), "mdi2c-close");
            Button btnEditar   = UiFactory.drawerSecondaryAction(i18nService.translate("common.edit"), "mdi2p-pencil-outline");
            Button btnGuardar  = UiFactory.drawerPrimaryAction(i18nService.translate("common.save"), "mdi2c-content-save-outline");

            btnCancelar.setVisible(false); btnCancelar.setManaged(false);
            btnGuardar.setDisable(true);
            footer = UiFactory.drawerActionFooter(btnApagar, btnCancelar, btnEditar, btnGuardar);

            btnEditar.setOnAction(e -> {
                secInfoView.setVisible(false); secInfoView.setManaged(false);
                secInfoEdit.setVisible(true);  secInfoEdit.setManaged(true);
                btnGuardar.setDisable(false);
                btnEditar.setVisible(false);   btnEditar.setManaged(false);
                btnCancelar.setVisible(true);  btnCancelar.setManaged(true);
            });

            btnCancelar.setOnAction(e -> {
                secInfoEdit.setVisible(false); secInfoEdit.setManaged(false);
                secInfoView.setVisible(true);  secInfoView.setManaged(true);
                lblErroEdit.setVisible(false); lblErroEdit.setManaged(false);
                btnGuardar.setDisable(true);
                btnCancelar.setVisible(false); btnCancelar.setManaged(false);
                btnEditar.setVisible(true);    btnEditar.setManaged(true);
                // reset edit fields to original values
                fornecedores.stream().filter(f -> f.id().equals(d.fornecedorId())).findFirst()
                        .ifPresent(cmbFornecedorEdit::setValue);
                txtDataEdit.setText(d.data() != null ? d.data().format(DATE_FMT) : "");
                cmbStatusEdit.setValue(d.estado());
            });

            btnGuardar.setOnAction(e -> {
                lblErroEdit.setVisible(false); lblErroEdit.setManaged(false);
                FornecedorSimpleDTO selectedF = cmbFornecedorEdit.getValue();
                EstadoEncomendaFornecedor selectedStatus = cmbStatusEdit.getValue();
                if (selectedF == null) {
                    lblErroEdit.setText(i18nService.translate("purchaseOrders.supplierRequired"));
                    lblErroEdit.setVisible(true); lblErroEdit.setManaged(true); return;
                }
                LocalDate selectedDate = null;
                try { selectedDate = LocalDate.parse(txtDataEdit.getText(), DATE_FMT); }
                catch (Exception ignored) {}
                try {
                    compraService.atualizarEncomenda(d.id(), selectedF.id(), selectedDate);
                    if (selectedStatus != null && selectedStatus != EstadoEncomendaFornecedor.RASCUNHO) {
                        if (selectedStatus == EstadoEncomendaFornecedor.EFETIVA) {
                            compraService.confirmarEncomenda(d.id());
                        } else if (selectedStatus == EstadoEncomendaFornecedor.ANULADA) {
                            compraService.anularEncomenda(d.id());
                        }
                    }
                    toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("purchaseOrders.statusUpdated"));
                    navigationService.hideModal();
                    carregarEncomendas();
                } catch (Exception ex) {
                    lblErroEdit.setText(ex.getMessage() != null ? ex.getMessage() : i18nService.translate("common.saveError"));
                    lblErroEdit.setVisible(true); lblErroEdit.setManaged(true);
                }
            });

            btnApagar.setOnAction(e -> handleConfirmarApagar(d.id(), String.format("PO-%03d", poSeq),
                    () -> compraService.apagarEncomendaRascunho(d.id()),
                    i18nService.translate("purchaseOrders.deleted")));

        } else if (estado == EstadoEncomendaFornecedor.EFETIVA) {
            Button btnAnular = UiFactory.drawerDangerAction(i18nService.translate("purchaseOrders.cancel"), "mdi2c-close-circle-outline");
            Button btnGerarPDF = UiFactory.drawerSecondaryAction(i18nService.translate("purchaseOrders.generatePdf"), "mdi2f-file-pdf-box");
            Button btnReceber = UiFactory.drawerPrimaryAction(i18nService.translate("purchaseOrders.markReceived"), "mdi2c-check-circle-outline");

            footer = UiFactory.drawerActionFooter(btnAnular, btnGerarPDF, btnReceber);

            btnAnular.setOnAction(e -> handleConfirmarApagar(d.id(), String.format("PO-%03d", poSeq),
                    () -> compraService.anularEncomenda(d.id()),
                    i18nService.translate("purchaseOrders.statusUpdated")));

            btnGerarPDF.setOnAction(e -> handleGerarPDF(d, poSeq));

            btnReceber.setOnAction(e -> {
                try {
                    compraService.confirmarRecebimento(d.id());
                    toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("purchaseOrders.statusUpdated"));
                    navigationService.hideModal();
                    carregarEncomendas();
                } catch (Exception ex) {
                    toastService.showError(i18nService.translate("common.error"), ex.getMessage());
                }
            });

        } else if (estado == EstadoEncomendaFornecedor.RECEBIDA) {
            Button btnGerarPDF = UiFactory.drawerSecondaryAction(i18nService.translate("purchaseOrders.generatePdf"), "mdi2f-file-pdf-box");
            footer = UiFactory.drawerActionFooter(null, btnGerarPDF);
            btnGerarPDF.setOnAction(e -> handleGerarPDF(d, poSeq));

        } else if (estado == EstadoEncomendaFornecedor.ANULADA) {
            Button btnApagar = UiFactory.drawerDangerAction(i18nService.translate("common.delete"), "mdi2d-delete-outline");
            footer = UiFactory.drawerActionFooter(btnApagar);
            btnApagar.setOnAction(e -> handleConfirmarApagar(d.id(), String.format("PO-%03d", poSeq),
                    () -> compraService.apagarEncomenda(d.id()),
                    i18nService.translate("purchaseOrders.deleted")));

        } else {
            footer = UiFactory.drawerFooter();
        }

        root.getChildren().addAll(header, scroll, footer);
        return root;
    }

    private void handleConfirmarApagar(UUID id, String label, Runnable action, String successMsg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(i18nService.translate("common.delete"));
        confirm.setHeaderText(i18nService.translate("common.confirmDelete"));
        confirm.setContentText(label);
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            action.run();
            toastService.showSuccess(i18nService.translate("common.success"), successMsg);
            navigationService.hideModal();
            carregarEncomendas();
        } catch (Exception ex) {
            toastService.showError(i18nService.translate("common.error"), ex.getMessage());
        }
    }

    private void handleGerarPDF(EncomendaFornecedorDetailsDTO d, int poSeq) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18nService.translate("purchaseOrders.savePdf"));
        chooser.setInitialFileName(String.format("PO-%03d.pdf", poSeq));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            pdfService.generate(d, poSeq, fos);
            toastService.showSuccess(i18nService.translate("common.success"),
                    i18nService.translate("purchaseOrders.pdfGenerated") + ": " + file.getName());
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            toastService.showError(i18nService.translate("common.error"), ex.getMessage());
        }
    }

    // ── Create drawer ─────────────────────────────────────────────────────────

    private void configurarDrawerCriar() {
        drawerCriar = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("purchaseOrders.createTitle"), navigationService::hideModal);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));

        cmbFornecedorCriar = new ComboBox<>(FXCollections.observableArrayList(fornecedores));
        cmbFornecedorCriar.setMaxWidth(Double.MAX_VALUE);
        cmbFornecedorCriar.setPromptText(i18nService.translate("purchaseOrders.selectSupplier"));
        cmbFornecedorCriar.setConverter(new StringConverter<>() {
            @Override public String toString(FornecedorSimpleDTO f) { return f != null ? f.nome() : ""; }
            @Override public FornecedorSimpleDTO fromString(String s) { return null; }
        });

        itemsContainerRef = new VBox(10);
        Button btnAddItem = UiFactory.drawerSecondaryAction(i18nService.translate("purchaseOrders.addItem"), "mdi2p-plus-circle-outline");
        btnAddItem.setOnAction(e -> adicionarItemRow());

        Label itemsLabel = new Label(i18nService.translate("purchaseOrders.orderItems"));
        itemsLabel.getStyleClass().add("text-muted");
        itemsLabel.setStyle("-fx-font-weight: 600;");
        VBox itemsSection = new VBox(10, itemsLabel, itemsContainerRef, btnAddItem);

        lblCriarSubtotal = new Label();
        lblCriarVat = new Label();
        lblCriarGrandTotal = new Label();
        lblCriarGrandTotal.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
        VBox summary = new VBox(8, lblCriarSubtotal, lblCriarVat, lblCriarGrandTotal);
        summary.setPadding(new Insets(16));
        summary.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");

        atualizarResumoCriacao(0, 0, 0);
        form.getChildren().addAll(criarCampoFormulario(i18nService.translate("purchaseOrders.supplier"), cmbFornecedorCriar), itemsSection, summary);

        ScrollPane scroll = UiFactory.transparentScroll(form);

        Button btnDraft = UiFactory.drawerSecondaryAction(i18nService.translate("purchaseOrders.saveDraft"), "mdi2c-content-save-outline");
        btnDraft.setOnAction(e -> handleGuardarRascunho());

        Button btnConfirm = UiFactory.drawerPrimaryAction(i18nService.translate("purchaseOrders.confirmOrder"), "mdi2c-check-circle-outline");
        btnConfirm.setOnAction(e -> handleConfirmarEncomenda());

        HBox footer = UiFactory.drawerActionFooter(null, btnDraft, btnConfirm);
        drawerCriar.getChildren().addAll(header, scroll, footer);
    }

    private void adicionarItemRow() {
        ItemRow row = new ItemRow(materiais, this::recalcularTotais);
        itemRows.add(row);
        VBox card = row.buildCard(() -> {
            itemRows.remove(row);
            itemsContainerRef.getChildren().remove(row.getCard());
            recalcularTotais();
        });
        itemsContainerRef.getChildren().add(card);
    }

    private void recalcularTotais() {
        double subtotal = itemRows.stream().mapToDouble(ItemRow::getTotal).sum();
        double vat = CalculationUtils.vat(subtotal, DEFAULT_VAT_RATE);
        double grand = CalculationUtils.total(subtotal, vat);
        atualizarResumoCriacao(subtotal, vat, grand);
    }

    private void handleGuardarRascunho() {
        if (!validarFormularioCriar()) return;
        try {
            criarRascunho();
            navigationService.hideModal();
            pagination.resetPage();
            carregarEncomendas();
            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("purchaseOrders.savedDraft"));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private void handleConfirmarEncomenda() {
        if (!validarFormularioCriar()) return;
        try {
            UUID encomendaId = criarRascunho();
            compraService.confirmarEncomenda(encomendaId);
            navigationService.hideModal();
            pagination.resetPage();
            carregarEncomendas();
            toastService.showSuccess(i18nService.translate("common.success"), i18nService.translate("purchaseOrders.confirmed"));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private boolean validarFormularioCriar() {
        if (cmbFornecedorCriar.getValue() == null) {
            toastService.showError(i18nService.translate("common.error"), i18nService.translate("purchaseOrders.supplierRequired"));
            return false;
        }
        if (itemRows.isEmpty()) {
            toastService.showError(i18nService.translate("common.error"), i18nService.translate("purchaseOrders.itemsRequired"));
            return false;
        }
        for (ItemRow row : itemRows) {
            if (row.getMaterial() == null
                    || !Double.isFinite(row.getParsedQty()) || row.getParsedQty() <= 0
                    || !Double.isFinite(row.getParsedPrice()) || row.getParsedPrice() <= 0) {
                toastService.showError(i18nService.translate("common.error"), i18nService.translate("purchaseOrders.itemsInvalid"));
                return false;
            }
        }
        if (moedaPadraoId == null) {
            toastService.showError(i18nService.translate("common.error"), i18nService.translate("purchaseOrders.noCurrency"));
            return false;
        }
        return true;
    }

    private UUID criarRascunho() {
        FornecedorSimpleDTO fornecedor = cmbFornecedorCriar.getValue();
        EncomendaFornecedorResponseDTO draft = compraService.gerarEncomendaRascunho(fornecedor.id(), moedaPadraoId);
        for (ItemRow row : itemRows) {
            compraService.adicionarItemEncomenda(draft.id(), row.getMaterial().id(), row.getParsedQty(), row.getParsedPrice(), DEFAULT_VAT_RATE);
        }
        return draft.id();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private HBox criarBadgeEstado(EstadoEncomendaFornecedor estado) {
        if (estado == null) return new HBox();
        String color, icon, label;
        switch (estado) {
            case RASCUNHO -> { color = "#6b7280"; icon = "mdi2c-circle-outline"; }
            case EFETIVA -> { color = "#3b82f6"; icon = "mdi2c-check-circle-outline"; }
            case RECEBIDA -> { color = "#22c55e"; icon = "mdi2c-check-circle"; }
            case ANULADA -> { color = "#ef4444"; icon = "mdi2c-close-circle"; }
            default -> { color = "#6b7280"; icon = "mdi2c-circle-outline"; }
        }
        return UiFactory.statusBadge(estadoLabel(estado), icon, color);
    }

    private String estadoLabel(EstadoEncomendaFornecedor estado) {
        if (estado == null) return "";
        return i18nService.translate("purchaseOrders.status." + estado.name());
    }

    private void atualizarResumoCriacao(double subtotal, double vat, double grand) {
        if (lblCriarSubtotal != null) lblCriarSubtotal.setText(i18nService.translate("purchaseOrders.subtotal") + ": " + formatMoney(subtotal));
        if (lblCriarVat != null) lblCriarVat.setText(i18nService.translate("purchaseOrders.vat") + ": " + formatMoney(vat));
        if (lblCriarGrandTotal != null) lblCriarGrandTotal.setText(i18nService.translate("purchaseOrders.grandTotal") + ": " + formatMoney(grand));
    }

    private String formatMoney(double value) {
        return String.format("€%.2f", CalculationUtils.money(value));
    }

    private VBox criarSecao(String titulo) {
        VBox sec = new VBox(12);
        sec.setPadding(new Insets(16));
        sec.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-border-radius: 8;");
        Label lbl = new Label(titulo);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");
        lbl.getStyleClass().add("text-muted");
        sec.getChildren().add(lbl);
        return sec;
    }

    private HBox criarLinhaDetalhe(String key, String value, boolean bold) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: -color-fg-muted; -fx-min-width: 120; -fx-font-size: 13px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px;" + (bold ? " -fx-font-weight: 700;" : ""));
        HBox row = new HBox(10, k, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox criarLinhaDetalheComBadge(String key, HBox badge) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: -color-fg-muted; -fx-min-width: 120; -fx-font-size: 13px;");
        HBox row = new HBox(10, k, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox criarCampoFormulario(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private String valorOuTraco(String s) { return s != null && !s.isBlank() ? s : "—"; }

    // ── ItemRow ───────────────────────────────────────────────────────────────

    private class ItemRow {
        private final ComboBox<MateriaPrimaSimpleDTO> cmbMaterial;
        private final TextField txtQty;
        private final TextField txtPrice;
        private final Label lblTotal;
        private VBox card;

        ItemRow(List<MateriaPrimaSimpleDTO> materiais, Runnable onTotalChanged) {
            cmbMaterial = new ComboBox<>(FXCollections.observableArrayList(materiais));
            cmbMaterial.setMaxWidth(Double.MAX_VALUE);
            cmbMaterial.setPromptText(i18nService.translate("purchaseOrders.selectRawMaterial"));
            cmbMaterial.setConverter(new StringConverter<>() {
                @Override public String toString(MateriaPrimaSimpleDTO m) { return m != null ? m.nome() : ""; }
                @Override public MateriaPrimaSimpleDTO fromString(String s) { return null; }
            });
            txtQty   = new TextField("0");
            txtPrice = new TextField("0.00");
            lblTotal = new Label(totalLabel(0));
            lblTotal.setStyle("-fx-font-weight: 600;");
            txtQty.textProperty().addListener((obs, o, n)   -> { recalc(); onTotalChanged.run(); });
            txtPrice.textProperty().addListener((obs, o, n) -> { recalc(); onTotalChanged.run(); });
        }

        private void recalc() {
            lblTotal.setText(totalLabel(getTotal()));
        }

        VBox buildCard(Runnable onRemove) {
            Label matLabel = new Label(i18nService.translate("purchaseOrders.rawMaterial"));
            matLabel.getStyleClass().add("text-muted");
            Label qtyLabel = new Label(i18nService.translate("purchaseOrders.quantityKg"));
            qtyLabel.getStyleClass().add("text-muted");
            Label priceLabel = new Label(i18nService.translate("purchaseOrders.unitPrice"));
            priceLabel.getStyleClass().add("text-muted");

            VBox qtyField   = new VBox(6, qtyLabel,   txtQty);
            VBox priceField = new VBox(6, priceLabel, txtPrice);
            HBox.setHgrow(qtyField, Priority.ALWAYS);
            HBox.setHgrow(priceField, Priority.ALWAYS);
            HBox qtyPriceRow = new HBox(12, qtyField, priceField);

            Button btnRemove = new Button();
            btnRemove.setGraphic(new FontIcon("mdi2t-trash-can-outline:16"));
            btnRemove.getStyleClass().addAll("button-icon", "flat");
            btnRemove.setOnAction(e -> onRemove.run());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox totalRow = new HBox(lblTotal, spacer, btnRemove);
            totalRow.setAlignment(Pos.CENTER_LEFT);

            card = new VBox(10, new VBox(6, matLabel, cmbMaterial), qtyPriceRow, totalRow);
            card.setPadding(new Insets(14));
            card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-border-radius: 8;");
            return card;
        }

        VBox getCard()                     { return card; }
        MateriaPrimaSimpleDTO getMaterial() { return cmbMaterial.getValue(); }
        double getParsedQty()   { try { return Double.parseDouble(txtQty.getText().replace(",", ".")); }   catch (Exception e) { return 0; } }
        double getParsedPrice() { try { return Double.parseDouble(txtPrice.getText().replace(",", ".")); } catch (Exception e) { return 0; } }
        double getTotal()       { double t = getParsedQty() * getParsedPrice(); return Double.isFinite(t) ? t : 0; }

        private String totalLabel(double value) {
            return i18nService.translate("purchaseOrders.itemTotal") + ": " + formatMoney(value);
        }
    }
}
