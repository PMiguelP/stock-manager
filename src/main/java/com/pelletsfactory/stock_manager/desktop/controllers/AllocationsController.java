package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.AlocacaoLoteEncomendaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.AlocacaoLoteEncomendaResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaPendenteAlocacaoDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LoteDisponivelAlocacaoDTO;
import com.pelletsfactory.stock_manager.common.services.AlocacaoLoteEncomendaService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class AllocationsController {

    private static final String CARD_STYLE =
            "-fx-padding: 15; -fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; "
                    + "-fx-border-radius: 6; -fx-background-radius: 6;";
    private static final String SELECTED_CARD_STYLE =
            "-fx-padding: 15; -fx-background-color: rgba(59, 130, 246, 0.10); -fx-border-color: -color-accent-emphasis; "
                    + "-fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;";
    private static final String UNAVAILABLE_CARD_STYLE =
            "-fx-padding: 15; -fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; "
                    + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-opacity: 0.48;";
    private static final String GREEN = "#22c55e";
    private static final String YELLOW = "#eab308";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private final AlocacaoLoteEncomendaService service;
    private final I18nService i18n;
    private final ToastService toastService;

    @FXML private ComboBox<PelletFilter> cmbTipoPellet;
    @FXML private VBox pendingContainer;
    @FXML private VBox batchContainer;
    @FXML private Label lblPendingSummary;
    @FXML private Label lblBatchSummary;
    @FXML private VBox allocationPanel;
    @FXML private Label lblSelectedOrder;
    @FXML private Label lblSelectedOrderDetails;
    @FXML private Label lblSelectedBatch;
    @FXML private Label lblSelectedBatchDetails;
    @FXML private HBox compatibilityBanner;
    @FXML private Label lblCompatibility;
    @FXML private Spinner<Double> spnQuantidade;
    @FXML private Label lblQuantityHint;
    @FXML private Button btnCreateAllocation;

    private List<ItemEncomendaPendenteAlocacaoDTO> allPendingItems = List.of();
    private List<LoteDisponivelAlocacaoDTO> allAvailableBatches = List.of();
    private List<ItemEncomendaPendenteAlocacaoDTO> pendingItems = List.of();
    private List<LoteDisponivelAlocacaoDTO> availableBatches = List.of();
    private ItemEncomendaPendenteAlocacaoDTO selectedOrder;
    private LoteDisponivelAlocacaoDTO selectedBatch;
    private boolean updatingFilters;

    public AllocationsController(AlocacaoLoteEncomendaService service, I18nService i18n, ToastService toastService) {
        this.service = service;
        this.i18n = i18n;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        spnQuantidade.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0, 1));
        spnQuantidade.valueProperty().addListener((observable, oldValue, newValue) -> atualizarPainelAcao());
        carregarDados();
    }

    @FXML
    private void handleFiltroAlterado() {
        if (!updatingFilters) {
            aplicarFiltro();
            renderizar();
        }
    }

    @FXML
    private void handleLimparSelecao() {
        selectedOrder = null;
        selectedBatch = null;
        renderizar();
    }

    @FXML
    private void handleCriarAlocacao() {
        if (!selecaoCompativel()) {
            mostrarErro(i18n.translate("allocations.incompatible"));
            return;
        }
        try {
            double quantidade = lerQuantidade();
            double limite = limiteSelecionado();
            if (quantidade <= 0 || quantidade > limite) {
                mostrarErro(i18n.translate("allocations.invalidQuantity"));
                return;
            }
            service.criarAlocacao(new AlocacaoLoteEncomendaRequestDTO(selectedBatch.loteId(), selectedOrder.itemEncomendaId(), quantidade));
            mostrarSucesso(i18n.translate("allocations.created"));
            selectedOrder = null;
            selectedBatch = null;
            carregarDados();
        } catch (NumberFormatException exception) {
            mostrarErro(i18n.translate("allocations.invalidQuantity"));
        } catch (RuntimeException exception) {
            mostrarErro(exception.getMessage());
        }
    }

    private void carregarDados() {
        allPendingItems = service.listarItensPendentes(null);
        allAvailableBatches = service.listarLotesDisponiveis(null);
        atualizarFiltros();
        aplicarFiltro();
        renderizar();
    }

    private void atualizarFiltros() {
        PelletFilter selecionado = cmbTipoPellet.getValue();
        Map<UUID, String> tipos = new LinkedHashMap<>();
        allPendingItems.forEach(item -> tipos.put(item.tipoPelletId(), item.tipoPelletNome()));
        allAvailableBatches.forEach(batch -> tipos.put(batch.tipoPelletId(), batch.tipoPelletNome()));

        List<PelletFilter> filtros = new ArrayList<>();
        filtros.add(new PelletFilter(null, i18n.translate("allocations.allTypes")));
        tipos.forEach((id, nome) -> filtros.add(new PelletFilter(id, nome)));

        updatingFilters = true;
        try {
            cmbTipoPellet.getItems().setAll(filtros);
            cmbTipoPellet.setValue(filtros.stream()
                    .filter(filtro -> selecionado != null && filtro.idEquals(selecionado.id()))
                    .findFirst()
                    .orElse(filtros.getFirst()));
        } finally {
            updatingFilters = false;
        }
    }

    private void aplicarFiltro() {
        UUID tipoPelletId = cmbTipoPellet.getValue() == null ? null : cmbTipoPellet.getValue().id();
        pendingItems = allPendingItems.stream()
                .filter(item -> tipoPelletId == null || tipoPelletId.equals(item.tipoPelletId()))
                .toList();
        availableBatches = allAvailableBatches.stream()
                .filter(batch -> tipoPelletId == null || tipoPelletId.equals(batch.tipoPelletId()))
                .toList();
    }

    private void renderizar() {
        lblPendingSummary.setText(traduzir("allocations.pendingSummary", pendingItems.size()));
        lblBatchSummary.setText(traduzir("allocations.batchSummary", availableBatches.size()));

        pendingContainer.getChildren().clear();
        pendingItems.forEach(item -> pendingContainer.getChildren().add(criarCardEncomenda(item)));
        if (pendingItems.isEmpty()) {
            pendingContainer.getChildren().add(criarEstadoVazio("allocations.noPending"));
        }

        batchContainer.getChildren().clear();
        availableBatches.forEach(batch -> batchContainer.getChildren().add(criarCardLote(batch)));
        if (availableBatches.isEmpty()) {
            batchContainer.getChildren().add(criarEstadoVazio("allocations.noBatches"));
        }

        atualizarPainelAcao();
    }

    private VBox criarCardEncomenda(ItemEncomendaPendenteAlocacaoDTO item) {
        boolean selected = selectedOrder != null && selectedOrder.itemEncomendaId().equals(item.itemEncomendaId());
        boolean compatible = selectedBatch == null || selectedBatch.tipoPelletId().equals(item.tipoPelletId());
        VBox card = criarCardBase(selected, compatible);
        card.setOnMouseClicked(event -> {
            if (!compatible) {
                mostrarErro(i18n.translate("allocations.incompatible"));
                return;
            }
            selectedOrder = item;
            sugerirQuantidade();
            renderizar();
        });

        HBox title = new HBox(8, titulo("#" + item.encomendaId()), spacer(), badge(i18n.translate("allocations.incomplete"), YELLOW));
        Label cliente = texto(item.clienteNome());
        Label pellet = texto(item.tipoPelletNome());
        ProgressBar progresso = progresso(item.quantidadeAlocada(), item.quantidadePedida());
        card.getChildren().addAll(
                title,
                cliente,
                pellet,
                detalhe(i18n.translate("allocations.requested"), kg(item.quantidadePedida())),
                detalhe(i18n.translate("allocations.allocated"), kgCor(item.quantidadeAlocada(), GREEN)),
                detalhe(i18n.translate("allocations.missing"), kgCor(item.quantidadeEmFalta(), YELLOW)),
                progresso
        );
        if (!item.alocacoes().isEmpty()) {
            card.getChildren().add(tituloPequeno(i18n.translate("allocations.existing")));
            item.alocacoes().forEach(alocacao -> card.getChildren().add(criarLinhaAlocacao(alocacao)));
        }
        return card;
    }

    private VBox criarCardLote(LoteDisponivelAlocacaoDTO batch) {
        boolean selected = selectedBatch != null && selectedBatch.loteId().equals(batch.loteId());
        boolean compatible = selectedOrder == null || selectedOrder.tipoPelletId().equals(batch.tipoPelletId());
        VBox card = criarCardBase(selected, compatible);
        card.setOnMouseClicked(event -> {
            if (!compatible) {
                mostrarErro(i18n.translate("allocations.incompatible"));
                return;
            }
            selectedBatch = batch;
            sugerirQuantidade();
            renderizar();
        });

        HBox title = new HBox(8, titulo(batch.codigoLote()), spacer(), badge(batch.localizacaoArmazem(), "#64748b"));
        Label pellet = texto(batch.tipoPelletNome());
        ProgressBar progresso = progresso(batch.quantidadeReservada(), batch.quantidadeTotal());
        card.getChildren().addAll(
                title,
                pellet,
                detalhe(i18n.translate("allocations.produced"), DATE_FORMAT.format(batch.dataProducao())),
                detalhe(i18n.translate("allocations.total"), kg(batch.quantidadeTotal())),
                detalhe(i18n.translate("allocations.reserved"), kgCor(batch.quantidadeReservada(), YELLOW)),
                detalhe(i18n.translate("allocations.available"), kgCor(batch.quantidadeDisponivel(), GREEN)),
                progresso
        );
        return card;
    }

    private VBox criarCardBase(boolean selected, boolean compatible) {
        VBox card = new VBox(7);
        card.setCursor(Cursor.HAND);
        card.setStyle(selected ? SELECTED_CARD_STYLE : compatible ? CARD_STYLE : UNAVAILABLE_CARD_STYLE);
        return card;
    }

    private HBox criarLinhaAlocacao(AlocacaoLoteEncomendaResponseDTO alocacao) {
        Button apagar = new Button();
        apagar.getStyleClass().add("icon-button");
        apagar.setGraphic(new FontIcon("mdi2c-close"));
        apagar.setOnAction(event -> {
            event.consume();
            service.apagarAlocacao(alocacao.id());
            mostrarSucesso(i18n.translate("allocations.deleted"));
            carregarDados();
        });
        return new HBox(8, texto(alocacao.codigoLote()), spacer(), texto(kg(alocacao.quantidadeReservada())), apagar);
    }

    private void atualizarPainelAcao() {
        boolean temSelecao = selectedOrder != null || selectedBatch != null;
        allocationPanel.setManaged(temSelecao);
        allocationPanel.setVisible(temSelecao);
        if (!temSelecao) {
            return;
        }

        lblSelectedOrder.setText(selectedOrder == null ? i18n.translate("allocations.orderPlaceholder") : "#" + selectedOrder.encomendaId());
        lblSelectedOrderDetails.setText(selectedOrder == null ? "" : traduzir("allocations.orderNeed", kg(selectedOrder.quantidadeEmFalta())));
        lblSelectedBatch.setText(selectedBatch == null ? i18n.translate("allocations.batchPlaceholder") : selectedBatch.codigoLote());
        lblSelectedBatchDetails.setText(selectedBatch == null ? "" : traduzir("allocations.batchBalance", kg(selectedBatch.quantidadeDisponivel())));

        boolean incompatible = selectedOrder != null && selectedBatch != null && !selecaoCompativel();
        compatibilityBanner.setManaged(incompatible);
        compatibilityBanner.setVisible(incompatible);
        lblCompatibility.setText(i18n.translate("allocations.incompatible"));

        double limite = limiteSelecionado();
        lblQuantityHint.setText(traduzir("allocations.maxQuantity", kg(limite)));
        btnCreateAllocation.setDisable(!selecaoCompativel() || limite <= 0);
        btnCreateAllocation.setText(traduzir("allocations.allocateAmount", kg(valorSpinner())));
    }

    private void sugerirQuantidade() {
        double limite = limiteSelecionado();
        spnQuantidade.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Math.max(1, limite), limite, 1));
    }

    private boolean selecaoCompativel() {
        return selectedOrder != null && selectedBatch != null
                && selectedOrder.tipoPelletId().equals(selectedBatch.tipoPelletId());
    }

    private double limiteSelecionado() {
        if (!selecaoCompativel()) {
            return 0;
        }
        return Math.min(selectedOrder.quantidadeEmFalta(), selectedBatch.quantidadeDisponivel());
    }

    private double lerQuantidade() {
        String value = spnQuantidade.getEditor().getText().trim().replace(',', '.');
        double quantidade = Double.parseDouble(value);
        if (!Double.isFinite(quantidade)) {
            throw new NumberFormatException();
        }
        spnQuantidade.getValueFactory().setValue(quantidade);
        return quantidade;
    }

    private double valorSpinner() {
        return spnQuantidade.getValue() == null ? 0 : spnQuantidade.getValue();
    }

    private VBox criarEstadoVazio(String key) {
        VBox box = new VBox(6, new FontIcon("mdi2p-package-variant"), texto(i18n.translate(key)));
        box.setStyle("-fx-padding: 26; -fx-alignment: center; -fx-opacity: 0.75;");
        return box;
    }

    private HBox detalhe(String nome, String valor) {
        return new HBox(8, texto(nome), spacer(), texto(valor));
    }

    private HBox detalhe(String nome, Label valor) {
        return new HBox(8, texto(nome), spacer(), valor);
    }

    private Label kgCor(Double valor, String cor) {
        Label label = texto(kg(valor));
        label.setStyle("-fx-text-fill: " + cor + ";");
        return label;
    }

    private Label badge(String valor, String cor) {
        Label label = texto(valor);
        label.setStyle("-fx-padding: 2 7 2 7; -fx-text-fill: " + cor + "; -fx-border-color: " + cor
                + "; -fx-border-radius: 4; -fx-background-radius: 4;");
        return label;
    }

    private ProgressBar progresso(Double atual, Double total) {
        ProgressBar bar = new ProgressBar(total == null || total <= 0 ? 0 : atual / total);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(4);
        return bar;
    }

    private Label titulo(String valor) {
        Label label = texto(valor);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label tituloPequeno(String valor) {
        Label label = texto(valor);
        label.getStyleClass().add("text-muted");
        label.setStyle("-fx-padding: 4 0 0 0;");
        return label;
    }

    private Label texto(String valor) {
        return new Label(valor == null ? "" : valor);
    }

    private javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region region = new javafx.scene.layout.Region();
        HBox.setHgrow(region, javafx.scene.layout.Priority.ALWAYS);
        return region;
    }

    private String kg(Double valor) {
        return String.format(Locale.getDefault(), "%.2f kg", valor == null ? 0 : valor);
    }

    private String traduzir(String key, Object... argumentos) {
        return String.format(i18n.translate(key), argumentos);
    }

    private void mostrarSucesso(String mensagem) {
        toastService.showSuccess(i18n.translate("common.success"), mensagem);
    }

    private void mostrarErro(String mensagem) {
        toastService.showError(i18n.translate("common.error"), mensagem);
    }

    private record PelletFilter(UUID id, String nome) {
        private boolean idEquals(UUID otherId) {
            return id == null ? otherId == null : id.equals(otherId);
        }

        @Override
        public String toString() {
            return nome;
        }
    }
}
