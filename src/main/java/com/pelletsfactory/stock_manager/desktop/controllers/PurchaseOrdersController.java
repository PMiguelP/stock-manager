package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.*;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaFornecedor;
import com.pelletsfactory.stock_manager.common.services.CompraService;
import com.pelletsfactory.stock_manager.common.services.FornecedorService;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
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
    private Label lblCriarSubtotal;
    private Label lblCriarVat;
    private Label lblCriarGrandTotal;
    private ComboBox<FornecedorSimpleDTO> cmbFornecedorCriar;
    private final List<ItemRow> itemRows = new ArrayList<>();

    private final ObservableList<EncomendaFornecedorSimpleDTO> encomendas = FXCollections.observableArrayList();

    public PurchaseOrdersController(CompraService compraService,
                                    FornecedorService fornecedorService,
                                    StockService stockService,
                                    MoedaService moedaService,
                                    NavigationService navigationService,
                                    ToastService toastService,
                                    I18nService i18nService) {
        this.compraService = compraService;
        this.fornecedorService = fornecedorService;
        this.stockService = stockService;
        this.moedaService = moedaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarEncomendas, i18nService);
        configurarFiltroStatus();
        carregarDadosAuxiliares();
        configurarTabela();
        configurarDrawerCriar();
        carregarEncomendas();
    }

    private void configurarFiltroStatus() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(EstadoEncomendaFornecedor.values()));
        cmbFiltroStatus.setConverter(new StringConverter<>() {
            @Override
            public String toString(EstadoEncomendaFornecedor e) {
                if (e == null) return "";
                return switch (e) {
                    case RASCUNHO -> "Draft";
                    case EFETIVA  -> "Confirmed";
                    case RECEBIDA -> "Received";
                    case ANULADA  -> "Cancelled";
                };
            }
            @Override
            public EstadoEncomendaFornecedor fromString(String s) { return null; }
        });
    }

    private void carregarDadosAuxiliares() {
        try {
            List<MoedaSimpleDTO> moedas = moedaService.listarTodosSimplesDTO();
            if (!moedas.isEmpty()) moedaPadraoId = moedas.get(0).id();
        } catch (Exception e) {
            toastService.showError("Erro", "Não foi possível carregar a moeda padrão.");
        }
        try {
            materiais = stockService.listarMateriasPrimasComFiltros(1, 200, null, null, "nome", "ASC").getContent();
        } catch (Exception e) {
            toastService.showError("Erro", "Não foi possível carregar matérias-primas.");
        }
        try {
            fornecedores = fornecedorService.listarTodosFornecedoresSimples();
        } catch (Exception e) {
            toastService.showError("Erro", "Não foi possível carregar fornecedores.");
        }
    }

    private void configurarTabela() {
        colPoNumber.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
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
            @Override
            protected void updateItem(Void item, boolean empty) {
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
                btnDetails.setTooltip(new Tooltip("View Details"));
                btnDetails.setOnAction(event -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        EncomendaFornecedorSimpleDTO dto = getTableView().getItems().get(idx);
                        handleAbrirDetalhes(dto, pagination.currentPage() * pagination.pageSize() + idx + 1);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblOrders.setFixedCellSize(48);
        tblOrders.setItems(encomendas);
    }

    private <T> void configurarColunaTexto(TableColumn<EncomendaFornecedorSimpleDTO, T> coluna) {
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

    private HBox criarBadgeEstado(EstadoEncomendaFornecedor estado) {
        if (estado == null) return new HBox();
        String color, icon, label;
        switch (estado) {
            case RASCUNHO -> { color = "#6b7280"; icon = "mdi2c-circle-outline";       label = "DRAFT";      }
            case EFETIVA  -> { color = "#3b82f6"; icon = "mdi2c-check-circle-outline"; label = "CONFIRMED";  }
            case RECEBIDA -> { color = "#22c55e"; icon = "mdi2c-check-circle";         label = "RECEIVED";   }
            case ANULADA  -> { color = "#ef4444"; icon = "mdi2c-close-circle";         label = "CANCELLED";  }
            default       -> { color = "#6b7280"; icon = "mdi2c-circle-outline";       label = estado.getDisplayName().toUpperCase(); }
        }
        HBox b = new HBox(6);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle(String.format(
                "-fx-background-color: %s22; -fx-border-color: %s; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5;",
                color.replace("#", ""), color));
        FontIcon ic = new FontIcon(icon + ":14");
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 600; -fx-font-size: 12px;");
        b.getChildren().addAll(ic, lbl);
        return b;
    }

    private void carregarEncomendas() {
        try {
            EstadoEncomendaFornecedor estado = cmbFiltroStatus.getValue();
            Page<EncomendaFornecedorSimpleDTO> page = compraService.listarEncomendasComFiltrosSimples(
                    null, estado, pagination.pageNumberForService(), pagination.pageSize(), "data", "DESC");

            List<EncomendaFornecedorSimpleDTO> content = new ArrayList<>(page.getContent());
            String search = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
            if (!search.isEmpty()) {
                content.removeIf(dto -> dto.fornecedorNome() == null
                        || !dto.fornecedorNome().toLowerCase().contains(search));
            }

            encomendas.setAll(content);
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao carregar encomendas: " + e.getMessage());
        }
    }

    @FXML
    private void handleAbrirModal() {
        if (itemsContainerRef != null) itemsContainerRef.getChildren().clear();
        if (lblCriarSubtotal != null)   lblCriarSubtotal.setText("Subtotal: €0.00");
        if (lblCriarVat != null)        lblCriarVat.setText("VAT (23%): €0.00");
        if (lblCriarGrandTotal != null) lblCriarGrandTotal.setText("Grand Total: €0.00");
        if (cmbFornecedorCriar != null) cmbFornecedorCriar.setValue(null);
        itemRows.clear();
        navigationService.showModal(drawerCriar);
    }

    private void handleAbrirDetalhes(EncomendaFornecedorSimpleDTO dto, int poSeq) {
        try {
            EncomendaFornecedorDetailsDTO details = compraService.obterDetalhesEncomendaFornecedor(dto.id());
            navigationService.showModal(criarDrawerDetalhes(details, poSeq));
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao carregar detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(EncomendaFornecedorDetailsDTO d, int poSeq) {
        VBox root = new VBox(0);
        root.setMinWidth(550); root.setPrefWidth(550); root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Purchase Order Details");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button(); btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));

        VBox secInfo = criarSecao("Order Information");
        secInfo.getChildren().addAll(
                criarLinhaDetalhe("PO Number",   String.format("PO-%03d", poSeq), true),
                criarLinhaDetalhe("Supplier",    d.fornecedorNome() != null ? d.fornecedorNome() : "—", false),
                criarLinhaDetalhe("Order Date",  d.data() != null ? d.data().toString() : "—", false),
                criarLinhaDetalheComBadge("Status", criarBadgeEstado(d.estado()))
        );

        VBox secTotal = criarSecao("Order Total");
        secTotal.getChildren().addAll(
                criarLinhaDetalhe("Subtotal",    d.totalLiquido() != null ? String.format("€%.2f", d.totalLiquido()) : "—", false),
                criarLinhaDetalhe("VAT (23%)",   d.totalIva()     != null ? String.format("€%.2f", d.totalIva())     : "—", false),
                criarLinhaDetalhe("Grand Total", d.totalFinal()   != null ? String.format("€%.2f", d.totalFinal())   : "—", true)
        );

        content.getChildren().addAll(secInfo, secTotal);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");
        Button btnFechar = new Button("Close");
        btnFechar.getStyleClass().add("button-outlined");
        btnFechar.setPrefHeight(44);
        btnFechar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnFechar, Priority.ALWAYS);
        btnFechar.setOnAction(e -> navigationService.hideModal());
        footer.getChildren().add(btnFechar);

        root.getChildren().addAll(header, scroll, footer);
        return root;
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

    private void configurarDrawerCriar() {
        drawerCriar = new VBox(0);
        drawerCriar.setMinWidth(550); drawerCriar.setPrefWidth(550); drawerCriar.setMaxWidth(550);
        drawerCriar.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Create Purchase Order");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button(); btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));

        cmbFornecedorCriar = new ComboBox<>(FXCollections.observableArrayList(fornecedores));
        cmbFornecedorCriar.setMaxWidth(Double.MAX_VALUE);
        cmbFornecedorCriar.setPromptText("Select supplier...");
        cmbFornecedorCriar.setConverter(new StringConverter<>() {
            @Override public String toString(FornecedorSimpleDTO f) { return f != null ? f.nome() : ""; }
            @Override public FornecedorSimpleDTO fromString(String s) { return null; }
        });

        itemsContainerRef = new VBox(10);
        Button btnAddItem = new Button("+ Add Item");
        btnAddItem.getStyleClass().add("button-outlined");
        btnAddItem.setOnAction(e -> adicionarItemRow());

        Label itemsLabel = new Label("Order Items");
        itemsLabel.getStyleClass().add("text-muted");
        itemsLabel.setStyle("-fx-font-weight: 600;");
        VBox itemsSection = new VBox(10, itemsLabel, itemsContainerRef, btnAddItem);

        lblCriarSubtotal   = new Label("Subtotal: €0.00");
        lblCriarVat        = new Label("VAT (23%): €0.00");
        lblCriarGrandTotal = new Label("Grand Total: €0.00");
        lblCriarGrandTotal.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
        VBox summary = new VBox(8, lblCriarSubtotal, lblCriarVat, lblCriarGrandTotal);
        summary.setPadding(new Insets(16));
        summary.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");

        form.getChildren().addAll(
                criarCampoFormulario("Supplier",   cmbFornecedorCriar),
                itemsSection,
                summary
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        Button btnDraft = new Button("Save as Draft");
        btnDraft.getStyleClass().add("button-outlined");
        btnDraft.setPrefHeight(44);
        btnDraft.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDraft, Priority.ALWAYS);
        btnDraft.setOnAction(e -> handleGuardarRascunho());

        Button btnConfirm = new Button("Confirm Order");
        btnConfirm.getStyleClass().add("accent");
        btnConfirm.setPrefHeight(44);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfirm, Priority.ALWAYS);
        btnConfirm.setOnAction(e -> handleConfirmarEncomenda());

        footer.getChildren().addAll(btnDraft, btnConfirm);
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
        double vat      = subtotal * 0.23;
        double grand    = subtotal + vat;
        lblCriarSubtotal.setText(String.format("Subtotal: €%.2f", subtotal));
        lblCriarVat.setText(String.format("VAT (23%): €%.2f", vat));
        lblCriarGrandTotal.setText(String.format("Grand Total: €%.2f", grand));
    }

    private void handleGuardarRascunho() {
        if (!validarFormularioCriar()) return;
        try {
            criarRascunho();
            navigationService.hideModal();
            pagination.resetPage();
            carregarEncomendas();
            toastService.showSuccess("Sucesso", "Purchase order saved as draft.");
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao guardar: " + e.getMessage());
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
            toastService.showSuccess("Sucesso", "Purchase order confirmed.");
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao confirmar: " + e.getMessage());
        }
    }

    private boolean validarFormularioCriar() {
        if (cmbFornecedorCriar.getValue() == null) {
            toastService.showError("Validação", "Selecione um fornecedor.");
            return false;
        }
        if (itemRows.isEmpty()) {
            toastService.showError("Validação", "Adicione pelo menos um item.");
            return false;
        }
        for (ItemRow row : itemRows) {
            if (row.getMaterial() == null
                    || !Double.isFinite(row.getParsedQty()) || row.getParsedQty() <= 0
                    || !Double.isFinite(row.getParsedPrice()) || row.getParsedPrice() <= 0) {
                toastService.showError("Validação", "Preencha todos os itens corretamente.");
                return false;
            }
        }
        if (moedaPadraoId == null) {
            toastService.showError("Erro", "Moeda padrão não disponível.");
            return false;
        }
        return true;
    }

    private UUID criarRascunho() {
        FornecedorSimpleDTO fornecedor = cmbFornecedorCriar.getValue();
        EncomendaFornecedorResponseDTO draft = compraService.gerarEncomendaRascunho(fornecedor.id(), moedaPadraoId);
        for (ItemRow row : itemRows) {
            compraService.adicionarItemEncomenda(draft.id(), row.getMaterial().id(), row.getParsedQty(), row.getParsedPrice(), 23.0);
        }
        return draft.id();
    }

    private VBox criarCampoFormulario(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarEncomendas(); }

    @FXML
    private void handleLimpar() {
        txtSearch.clear();
        cmbFiltroStatus.setValue(null);
        handleFiltrar();
    }

    private static class ItemRow {
        private final ComboBox<MateriaPrimaSimpleDTO> cmbMaterial;
        private final TextField txtQty;
        private final TextField txtPrice;
        private final Label lblTotal;
        private VBox card;

        ItemRow(List<MateriaPrimaSimpleDTO> materiais, Runnable onTotalChanged) {
            cmbMaterial = new ComboBox<>(FXCollections.observableArrayList(materiais));
            cmbMaterial.setMaxWidth(Double.MAX_VALUE);
            cmbMaterial.setPromptText("Select raw material...");
            cmbMaterial.setConverter(new StringConverter<>() {
                @Override public String toString(MateriaPrimaSimpleDTO m) { return m != null ? m.nome() : ""; }
                @Override public MateriaPrimaSimpleDTO fromString(String s) { return null; }
            });

            txtQty   = new TextField("0");
            txtPrice = new TextField("0.00");
            lblTotal = new Label("Total: €0.00");
            lblTotal.setStyle("-fx-font-weight: 600;");

            txtQty.textProperty().addListener((obs, o, n)   -> { recalc(); onTotalChanged.run(); });
            txtPrice.textProperty().addListener((obs, o, n) -> { recalc(); onTotalChanged.run(); });
        }

        private void recalc() {
            lblTotal.setText(String.format("Total: €%.2f", getTotal()));
        }

        VBox buildCard(Runnable onRemove) {
            Label matLabel   = new Label("Raw Material");   matLabel.getStyleClass().add("text-muted");
            Label qtyLabel   = new Label("Quantity (kg)");  qtyLabel.getStyleClass().add("text-muted");
            Label priceLabel = new Label("Unit Price (€)"); priceLabel.getStyleClass().add("text-muted");

            VBox qtyField   = new VBox(6, qtyLabel,   txtQty);
            VBox priceField = new VBox(6, priceLabel, txtPrice);
            HBox.setHgrow(qtyField,   Priority.ALWAYS);
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

        VBox getCard()               { return card; }
        MateriaPrimaSimpleDTO getMaterial() { return cmbMaterial.getValue(); }
        double getParsedQty() {
            try { return Double.parseDouble(txtQty.getText().replace(",", ".")); } catch (Exception e) { return 0; }
        }
        double getParsedPrice() {
            try { return Double.parseDouble(txtPrice.getText().replace(",", ".")); } catch (Exception e) { return 0; }
        }
        double getTotal() {
            double total = getParsedQty() * getParsedPrice();
            return Double.isFinite(total) ? total : 0;
        }
    }
}
