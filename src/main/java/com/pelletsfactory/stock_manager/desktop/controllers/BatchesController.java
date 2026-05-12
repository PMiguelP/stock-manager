package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.LotePelletRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.LotePelletSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.OrdemProducaoResponseDTO;
import com.pelletsfactory.stock_manager.common.services.LotePelletService;
import com.pelletsfactory.stock_manager.common.services.OrdemProducaoService;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import javafx.beans.property.SimpleStringProperty;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BatchesController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LotePelletService loteService;
    private final OrdemProducaoService ordemService;
    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;

    @FXML private VBox vboxContainer;
    @FXML private TextField txtFiltroCodigo;
    @FXML private TableView<LoteRow> tblBatches;
    @FXML private TableColumn<LoteRow, String> colCodigo;
    @FXML private TableColumn<LoteRow, String> colTipoPellet;
    @FXML private TableColumn<LoteRow, String> colData;
    @FXML private TableColumn<LoteRow, String> colQuantidade;
    @FXML private TableColumn<LoteRow, String> colLocalizacao;
    @FXML private TableColumn<LoteRow, Void> colAcoes;

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private VBox drawerRoot;
    private ComboBox<OrdemItem> cmbOrdem;
    private ComboBox<TipoPelletItem> cmbTipoPellet;
    private TextField txtCodigo;
    private TextField txtQuantidade;
    private TextField txtLocalizacao;

    private final ObservableList<LoteRow> data = FXCollections.observableArrayList();

    public BatchesController(LotePelletService loteService,
                             OrdemProducaoService ordemService,
                             StockService stockService,
                             NavigationService navigationService,
                             ToastService toastService) {
        this.loteService = loteService;
        this.ordemService = ordemService;
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarDrawerAdicionar();
        carregarLotes();
    }

    @FXML
    private void handleRefresh() {
        carregarLotes();
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

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerRoot);
    }

    private void configurarTabela() {
        colCodigo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().codigo()));
        configurarColunaTexto(colCodigo);

        colTipoPellet.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().tipoPellet()));
        configurarColunaTexto(colTipoPellet);

        colData.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().dataProducao()));
        configurarColunaTexto(colData);

        colQuantidade.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().quantidade()));
        configurarColunaTexto(colQuantidade);

        colLocalizacao.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().localizacao()));
        configurarColunaTexto(colLocalizacao);

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setOnAction(event -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblBatches.setFixedCellSize(48);
        tblBatches.setItems(data);
    }

    private <T> void configurarColunaTexto(TableColumn<LoteRow, T> coluna) {
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

    private void carregarLotes() {
        try {
            String codigo = (txtFiltroCodigo != null && !txtFiltroCodigo.getText().isBlank())
                    ? txtFiltroCodigo.getText()
                    : null;

            Page<LotePelletSimpleDTO> page = loteService.listarLotesComFiltros(
                    paginaAtual + 1, itemsPerPage, codigo, null, null, "dataProducao", "DESC"
            );

            List<LoteRow> rows = new ArrayList<>();
            for (LotePelletSimpleDTO lote : page.getContent()) {
                rows.add(LoteRow.from(lote));
            }
            data.setAll(rows);

            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro("Erro ao carregar lotes: " + e.getMessage());
        }
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
        cmbItemsPerPage.setOnAction(e -> { itemsPerPage = cmbItemsPerPage.getValue(); paginaAtual = 0; carregarLotes(); });
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
        prev.setOnAction(e -> { paginaAtual--; carregarLotes(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int idx = i; p.setOnAction(e -> { paginaAtual = idx; carregarLotes(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button(); next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarLotes(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<?> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }

    private void configurarDrawerAdicionar() {
        drawerRoot = new VBox(0);
        drawerRoot.setMinWidth(550);
        drawerRoot.setPrefWidth(550);
        drawerRoot.setMaxWidth(550);
        drawerRoot.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Create Batch");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        cmbOrdem = new ComboBox<>();
        cmbOrdem.setMaxWidth(Double.MAX_VALUE);
        cmbOrdem.setPromptText("Select production order");
        carregarOrdens();

        cmbTipoPellet = new ComboBox<>();
        cmbTipoPellet.setMaxWidth(Double.MAX_VALUE);
        cmbTipoPellet.setPromptText("Select pellet type");
        carregarTiposPellet();

        cmbOrdem.setOnAction(e -> aplicarTipoPelletDaOrdem());

        txtCodigo = new TextField();
        txtCodigo.setPromptText("Batch code");

        txtQuantidade = new TextField();
        txtQuantidade.setPromptText("Quantity (kg)");

        txtLocalizacao = new TextField();
        txtLocalizacao.setPromptText("Warehouse location (optional)");

        VBox form = new VBox(20,
                criarCampoFormulario("Production Order", cmbOrdem),
                criarCampoFormulario("Pellet Type", cmbTipoPellet),
                criarCampoFormulario("Batch Code", txtCodigo),
                criarCampoFormulario("Quantity (kg)", txtQuantidade),
                criarCampoFormulario("Warehouse Location", txtLocalizacao)
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
        Button btnSalvar = new Button("Create Batch");
        btnSalvar.getStyleClass().add("accent");
        btnSalvar.setPrefHeight(44);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnSalvar, Priority.ALWAYS);
        btnSalvar.setOnAction(e -> handleAdicionar());
        footer.getChildren().add(btnSalvar);

        drawerRoot.getChildren().addAll(header, scrollPane, footer);
    }

    private void carregarOrdens() {
        List<OrdemItem> items = ordemService.listarTodosSimples().stream()
                .map(o -> new OrdemItem(o.id(), o.tipoPelletNome(), o.estado().name()))
                .toList();
        cmbOrdem.setItems(FXCollections.observableArrayList(items));
    }

    private void carregarTiposPellet() {
        var page = stockService.listarTiposPelletComFiltros(1, 1000, null, null, "nome", "ASC");
        List<TipoPelletItem> items = page.getContent().stream()
                .map(tp -> new TipoPelletItem(tp.id(), tp.nome()))
                .toList();
        cmbTipoPellet.setItems(FXCollections.observableArrayList(items));
    }

    private void aplicarTipoPelletDaOrdem() {
        OrdemItem selected = cmbOrdem.getValue();
        if (selected == null) {
            return;
        }

        try {
            OrdemProducaoResponseDTO ordem = ordemService.buscarPorId(selected.id());
            if (ordem.tipoPelletId() != null) {
                cmbTipoPellet.getItems().stream()
                        .filter(item -> item.id().equals(ordem.tipoPelletId()))
                        .findFirst()
                        .ifPresent(cmbTipoPellet::setValue);
            }
        } catch (Exception e) {
            mostrarErro("Erro ao carregar tipo de pellet da ordem: " + e.getMessage());
        }
    }

    private VBox criarCampoFormulario(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private void limparFormulario() {
        cmbOrdem.setValue(null);
        cmbTipoPellet.setValue(null);
        txtCodigo.clear();
        txtQuantidade.clear();
        txtLocalizacao.clear();
    }

    private void handleAdicionar() {
        if (cmbOrdem.getValue() == null) {
            mostrarErro("Selecione uma ordem de produção");
            return;
        }
        if (cmbTipoPellet.getValue() == null) {
            mostrarErro("Selecione um tipo de pellet");
            return;
        }
        if (txtCodigo.getText().isBlank()) {
            mostrarErro("Informe o código do lote");
            return;
        }

        double quantidade;
        try {
            quantidade = Double.parseDouble(txtQuantidade.getText().replace(",", "."));
        } catch (NumberFormatException e) {
            mostrarErro("Quantidade inválida");
            return;
        }

        try {
            LotePelletRequestDTO dto = new LotePelletRequestDTO(
                    cmbOrdem.getValue().id(),
                    cmbTipoPellet.getValue().id(),
                    txtCodigo.getText().trim(),
                    quantidade,
                    txtLocalizacao.getText().isBlank() ? null : txtLocalizacao.getText().trim()
            );
            loteService.criarLote(dto);
            paginaAtual = 0;
            carregarLotes();
            navigationService.hideModal();
            mostrarSucesso("Lote criado com sucesso!");
        } catch (Exception e) {
            mostrarErro("Erro ao criar lote: " + e.getMessage());
        }
    }

    private void handleAbrirDetalhes(LoteRow row) {
        try {
            LotePelletResponseDTO d = loteService.buscarPorId(row.id());
            VBox drawer = criarDrawerDetalhes(d);
            navigationService.showModal(drawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(LotePelletResponseDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Batch Details");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);
        addDetalhe(grid, 0, "Batch Code", d.codigoLote());
        addDetalhe(grid, 1, "Pellet Type", d.tipoPelletNome() != null ? d.tipoPelletNome() : "-");
        addDetalhe(grid, 2, "Quantity", d.quantidadeKg() != null ? d.quantidadeKg() + " kg" : "-");
        addDetalhe(grid, 3, "Production Date", d.dataProducao() != null ? d.dataProducao().format(DATETIME_FORMATTER) : "-");
        addDetalhe(grid, 4, "Warehouse", d.localizacaoArmazem() != null ? d.localizacaoArmazem() : "-");

        VBox body = new VBox(20, grid);
        body.setPadding(new Insets(25));
        root.getChildren().addAll(header, body);
        return root;
    }

    private void addDetalhe(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        Label val = new Label(value);
        val.getStyleClass().add("text-strong");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private void mostrarErro(String m) {
        toastService.showError("Erro", m);
    }

    private void mostrarSucesso(String m) {
        toastService.showSuccess("Sucesso", m);
    }

    private record LoteRow(UUID id, String codigo, String tipoPellet, String dataProducao, String quantidade, String localizacao) {
        static LoteRow from(LotePelletSimpleDTO dto) {
            String data = dto.dataProducao() != null ? dto.dataProducao().format(DATETIME_FORMATTER) : "-";
            String qtd = dto.quantidadeKg() != null ? String.format("%.2f kg", dto.quantidadeKg()) : "-";
            return new LoteRow(
                    dto.id(),
                    dto.codigoLote() != null ? dto.codigoLote() : "-",
                    dto.tipoPelletNome() != null ? dto.tipoPelletNome() : "-",
                    data,
                    qtd,
                    dto.localizacaoArmazem() != null ? dto.localizacaoArmazem() : "-"
            );
        }
    }

    private record OrdemItem(UUID id, String tipoPellet, String estado) {
        @Override
        public String toString() { return tipoPellet + " (" + estado + ")"; }
    }

    private record TipoPelletItem(UUID id, String nome) {
        @Override
        public String toString() { return nome; }
    }
}

