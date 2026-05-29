package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.services.FinanceiroService;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
import com.pelletsfactory.stock_manager.common.services.StockService;
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
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class StockController {
    private final FinanceiroService financeiroService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final MoedaService moedaService;
    private final StockService stockService;

    // Elementos de UI dos Cards
    @FXML private Label lblCurrentStock, lblMinThreshold, lblAvailableStock, lblReservedStock;
    @FXML private StackPane iconCurrentStock, iconMinThreshold, iconAvailableStock, iconReservedStock;
    @FXML private Button btnAdjustStock, btnStockEntry, btnStockExit, btnFilter, btnClear;

    // Tabela e Filtros
    @FXML private ComboBox<TipoMovimento> cmbFiltroTipo;
    @FXML private ComboBox<MoedaSimpleDTO> cmbFiltroMoeda;
    @FXML private TableView<MovimentoFinanceiroSimpleDTO> tblMovimentos;
    @FXML private VBox vboxContainer;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, TipoMovimento> colTipo;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Double> colValor;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, String> colMoeda;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Instant> colData;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Void> colAcoes;

    private PaginationControls pagination;

    private final ObservableList<MovimentoFinanceiroSimpleDTO> movimentos = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StockController(FinanceiroService financeiroService,
                           NavigationService navigationService,
                           ToastService toastService,
                           MoedaService moedaService,
                           StockService stockService) {
        this.financeiroService = financeiroService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.moedaService = moedaService;
        this.stockService = stockService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarMovimentos);
        configurarIcones();
        configurarTabela();
        configurarComboBoxes();
        carregarDadosEstatisticos();
        carregarMovimentos();
    }

    private void carregarDadosEstatisticos() {
        double stockPellets = stockService.calcularStockPelletsAtual();
        double stockMinimo = stockService.calcularStockPelletsMinimo();
        double stockMaterias = stockService.calcularStockMateriasPrimasAtual();
        int alertas = stockService.verificarAlertasStock();

        lblCurrentStock.setText(String.format("%.0f kg", stockPellets));
        lblMinThreshold.setText(String.format("%.0f kg", stockMinimo));
        lblAvailableStock.setText(String.format("%.0f kg", stockMaterias));
        lblReservedStock.setText(String.valueOf(alertas));
    }

    private void configurarIcones() {
        setCardIcon(iconCurrentStock, "mdi2p-package-variant", "#4C7AF2");
        setCardIcon(iconMinThreshold, "mdi2a-alert-circle-outline", "#f59e0b");
        setCardIcon(iconAvailableStock, "mdi2c-check-circle-outline", "#22c55e");
        setCardIcon(iconReservedStock, "mdi2l-lock-outline", "#ef4444");

        setButtonIcon(btnAdjustStock, "mdi2t-tune-variant");
        setButtonIcon(btnStockEntry, "mdi2a-arrow-down-circle");
        setButtonIcon(btnStockExit, "mdi2a-arrow-up-circle");
        setButtonIcon(btnFilter, "mdi2f-filter-outline");
        setButtonIcon(btnClear, "mdi2c-close-circle-outline");
    }

    private void setCardIcon(StackPane container, String literal, String color) {
        if (container == null) {
            return;
        }

        FontIcon icon = new FontIcon();
        icon.setIconLiteral(literal);
        icon.setIconSize(20);
        icon.setIconColor(javafx.scene.paint.Color.web(color));

        container.setMinSize(36, 36);
        container.setPrefSize(36, 36);
        container.setMaxSize(36, 36);
        container.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 8;");
        container.getChildren().setAll(icon);
    }

    private void setButtonIcon(Button button, String literal) {
        if (button == null) {
            return;
        }

        FontIcon icon = new FontIcon();
        icon.setIconLiteral(literal);
        icon.setIconSize(16);
        button.setGraphic(icon);
    }

    // --- AÇÕES DOS BOTÕES COLORIDOS (CONFORME PRINTS) ---

    @FXML
    private void handleAdjustStock() {
        abrirFormularioMovimento("Adjust Stock", "Save Adjustment", "#3b82f6", false);
    }

    @FXML
    private void handleStockEntry() {
        abrirFormularioMovimento("Register Stock Entry", "Register Entry", "#10b981", false);
    }

    @FXML
    private void handleStockExit() {
        abrirFormularioMovimento("Register Stock Exit", "Register Exit", "#ef4444", true);
    }

    private void abrirFormularioMovimento(String titulo, String textoBotao, String corBotao, boolean mostrarRelatedOrder) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(titulo, navigationService::hideModal);

        VBox form = new VBox(20);
        form.setPadding(new Insets(30));
        form.getChildren().add(criarCampoInput("Movement ID (auto-generated)", "MOV-" + (movimentos.size() + 1), true));

        TextField txtQuantidade = new TextField("0");
        form.getChildren().add(new VBox(6, new Label("Quantity (tons)"), txtQuantidade));

        if (mostrarRelatedOrder) {
            ComboBox<String> cmbOrder = new ComboBox<>(FXCollections.observableArrayList("Select order..."));
            cmbOrder.setMaxWidth(Double.MAX_VALUE);
            form.getChildren().add(new VBox(6, new Label("Related Order"), cmbOrder));
        }

        DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().add(new VBox(6, new Label("Date"), datePicker));

        ComboBox<String> cmbUser = new ComboBox<>(FXCollections.observableArrayList("Select user..."));
        cmbUser.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().add(new VBox(6, new Label("Responsible User"), cmbUser));

        TextArea txtNotes = new TextArea();
        txtNotes.setPromptText("Additional notes...");
        txtNotes.setPrefHeight(100);
        form.getChildren().add(new VBox(6, new Label("Notes"), txtNotes));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();

        Button btnSubmit = new Button(textoBotao);
        btnSubmit.setPrefHeight(44);
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;", corBotao));
        HBox.setHgrow(btnSubmit, Priority.ALWAYS);
        btnSubmit.setOnAction(e -> {
            // Aqui chamaria o financeiroService.registarEntrada ou Saida
            mostrarSucesso("Movimento registado com sucesso!");
            navigationService.hideModal();
            carregarMovimentos();
            carregarDadosEstatisticos();
        });
        footer.getChildren().add(btnSubmit);

        root.getChildren().addAll(header, scrollPane, footer);
        navigationService.showModal(root);
    }

    private VBox criarCampoInput(String label, String valor, boolean disabled) {
        TextField tf = new TextField(valor);
        tf.setDisable(disabled);
        return new VBox(5, new Label(label), tf);
    }

    // --- LÓGICA DA TABELA (EXISTENTE) ---

    private void configurarTabela() {
        colTipo.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().tipoMovimento()));
        colTipo.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(TipoMovimento tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) setGraphic(null);
                else setGraphic(criarBadgeTipo(tipo));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colValor.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().valorTotal()));
        colValor.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText((empty || valor == null) ? null : String.format("%.2f", valor));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colMoeda.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().moedaCodigo()));
        configurarColunaTexto(colMoeda);

        colData.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().createdAt()));
        colData.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Instant data, boolean empty) {
                super.updateItem(data, empty);
                setText((empty || data == null) ? null : data.atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setOnAction(event -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblMovimentos.setFixedCellSize(48);
        tblMovimentos.setItems(movimentos);
    }

    private <T> void configurarColunaTexto(TableColumn<MovimentoFinanceiroSimpleDTO, T> coluna) {
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

    private void carregarMovimentos() {
        try {
            TipoMovimento tipo = cmbFiltroTipo.getValue();
            MoedaSimpleDTO moeda = cmbFiltroMoeda.getValue();
            Page<MovimentoFinanceiroSimpleDTO> page = financeiroService.listarMovimentosFinanceirosSimples(
                    pagination.pageNumberForService(), pagination.pageSize(), tipo, moeda != null ? moeda.id() : null, "createdAt", "DESC"
            );
            movimentos.setAll(page.getContent());
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro("Erro: " + e.getMessage());
        }
    }

    private HBox criarBadgeTipo(TipoMovimento tipo) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = (tipo == TipoMovimento.ENTRADA) ? "#22c55e" : "#ef4444";
        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s; -fx-border-color: %s;", color + "20", color));

        FontIcon ic = new FontIcon(tipo == TipoMovimento.ENTRADA ? "mdi2a-arrow-down-circle" : "mdi2a-arrow-up-circle");
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        Label l = new Label(tipo.name());
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");

        b.getChildren().addAll(ic, l);
        return b;
    }

    private void configurarComboBoxes() {
        cmbFiltroTipo.setItems(FXCollections.observableArrayList(TipoMovimento.values()));
        cmbFiltroMoeda.setItems(FXCollections.observableArrayList(moedaService.listarTodosSimplesDTO()));
        cmbFiltroMoeda.setConverter(new StringConverter<>() {
            @Override
            public String toString(MoedaSimpleDTO moeda) {
                return moeda == null ? "" : moeda.codigo();
            }

            @Override
            public MoedaSimpleDTO fromString(String codigo) {
                if (codigo == null || codigo.isBlank()) {
                    return null;
                }
                return cmbFiltroMoeda.getItems().stream()
                        .filter(m -> codigo.equalsIgnoreCase(m.codigo()))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void handleAbrirDetalhes(MovimentoFinanceiroSimpleDTO mov) {
        try {
            MovimentoFinanceiroResponseDTO detalhes = financeiroService.obterMovimentoFinanceiro(mov.id());
            VBox drawer = criarDrawerDetalhes(detalhes);
            navigationService.showModal(drawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(MovimentoFinanceiroResponseDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Detalhes do Movimento", navigationService::hideModal);

        VBox content = new VBox(16);
        content.setPadding(new Insets(30));

        HBox tipoBox = new HBox(10);
        tipoBox.setAlignment(Pos.CENTER_LEFT);
        Label lblTipo = new Label("Tipo");
        lblTipo.getStyleClass().add("text-muted");
        lblTipo.setPrefWidth(140);
        tipoBox.getChildren().addAll(lblTipo, d.tipoMovimento() != null ? criarBadgeTipo(d.tipoMovimento()) : new Label("-"));

        String dataFormatada = d.createdAt() != null
                ? d.createdAt().atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER)
                : "-";
        String encomendaRelacionada = d.idEncomendaCliente() != null
                ? d.idEncomendaCliente().toString()
                : (d.idEncomendaFornecedor() != null ? d.idEncomendaFornecedor().toString() : "-");

        content.getChildren().addAll(
                tipoBox,
                criarCampoLeitura("Quantidade/Valor", d.valorTotal() != null ? String.format("%.2f", d.valorTotal()) : "-"),
                criarCampoLeitura("Moeda/Unidade", valorOuVazio(d.moedaCodigo())),
                criarCampoLeitura("Data", dataFormatada),
                criarCampoLeitura("Encomenda Relacionada", encomendaRelacionada)
        );

        ScrollPane scrollPane = UiFactory.transparentScroll(content);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private HBox criarCampoLeitura(String label, String valor) {
        HBox campo = new HBox(10);
        campo.setAlignment(Pos.CENTER_LEFT);
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("text-muted");
        lblLabel.setPrefWidth(140);
        Label lblValor = new Label(valorOuVazio(valor));
        lblValor.setStyle("-fx-font-weight: 500;");
        campo.getChildren().addAll(lblLabel, lblValor);
        return campo;
    }

    private String valorOuVazio(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private void mostrarSucesso(String m) { toastService.showSuccess("Sucesso", m); }
    private void mostrarErro(String m) { toastService.showError("Erro", m); }

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarMovimentos(); }
    @FXML private void handleMostrarTodos() {
        cmbFiltroTipo.setValue(null);
        cmbFiltroMoeda.setValue(null);
        handleFiltrar();
    }
}
