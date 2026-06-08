package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroResponseDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MovimentoFinanceiroSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MoedaSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.TipoMovimento;
import com.pelletsfactory.stock_manager.common.services.FinanceiroService;
import com.pelletsfactory.stock_manager.common.services.MoedaService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
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

@Component
public class FinancialController {

    private final FinanceiroService financeiroService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final MoedaService moedaService;
    private final I18nService i18nService;

    @FXML private ComboBox<TipoMovimento> cmbFiltroTipo;
    @FXML private ComboBox<MoedaSimpleDTO> cmbFiltroMoeda;
    @FXML private TableView<MovimentoFinanceiroSimpleDTO> tblMovimentos;
    @FXML private VBox vboxContainer;
    @FXML private Button btnFilter, btnClear;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, TipoMovimento> colTipo;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Double> colValor;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, String> colMoeda;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Instant> colData;
    @FXML private TableColumn<MovimentoFinanceiroSimpleDTO, Void> colAcoes;

    private PaginationControls pagination;
    private final ObservableList<MovimentoFinanceiroSimpleDTO> movimentos = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FinancialController(FinanceiroService financeiroService,
                               NavigationService navigationService,
                               ToastService toastService,
                               MoedaService moedaService,
                               I18nService i18nService) {
        this.financeiroService = financeiroService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.moedaService = moedaService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarMovimentos, i18nService);
        configurarIcones();
        configurarTabela();
        configurarComboBoxes();
        carregarMovimentos();
    }

    private void configurarIcones() {
        setButtonIcon(btnFilter, "mdi2f-filter-outline");
        setButtonIcon(btnClear, "mdi2c-close-circle-outline");
    }

    private void setButtonIcon(Button button, String literal) {
        if (button == null) return;
        FontIcon icon = new FontIcon();
        icon.setIconLiteral(literal);
        icon.setIconSize(16);
        button.setGraphic(icon);
    }

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
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private HBox criarBadgeTipo(TipoMovimento tipo) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        String color = (tipo == TipoMovimento.ENTRADA) ? "#22c55e" : "#ef4444";
        b.setStyle(String.format(
                "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5; -fx-background-color: %s20; -fx-border-color: %s;",
                color, color));
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
            public String toString(MoedaSimpleDTO moeda) { return moeda == null ? "" : moeda.codigo(); }
            @Override
            public MoedaSimpleDTO fromString(String codigo) {
                if (codigo == null || codigo.isBlank()) return null;
                return cmbFiltroMoeda.getItems().stream()
                        .filter(m -> codigo.equalsIgnoreCase(m.codigo()))
                        .findFirst().orElse(null);
            }
        });
    }

    private void handleAbrirDetalhes(MovimentoFinanceiroSimpleDTO mov) {
        try {
            MovimentoFinanceiroResponseDTO detalhes = financeiroService.obterMovimentoFinanceiro(mov.id());
            navigationService.showModal(criarDrawerDetalhes(detalhes));
        } catch (Exception e) {
            toastService.showError(i18nService.translate("common.error"), e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(MovimentoFinanceiroResponseDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader(i18nService.translate("stock.movementDetails"), navigationService::hideModal);

        VBox content = new VBox(16);
        content.setPadding(new Insets(30));

        HBox tipoBox = new HBox(10);
        tipoBox.setAlignment(Pos.CENTER_LEFT);
        Label lblTipo = new Label(i18nService.translate("common.type"));
        lblTipo.getStyleClass().add("text-muted");
        lblTipo.setPrefWidth(140);
        tipoBox.getChildren().addAll(lblTipo, d.tipoMovimento() != null ? criarBadgeTipo(d.tipoMovimento()) : new Label("-"));

        String dataFormatada = d.createdAt() != null
                ? d.createdAt().atZone(ZoneId.systemDefault()).format(DATETIME_FORMATTER) : "-";
        String encomendaRelacionada = d.idEncomendaCliente() != null
                ? d.idEncomendaCliente().toString()
                : (d.idEncomendaFornecedor() != null ? d.idEncomendaFornecedor().toString() : "-");

        content.getChildren().addAll(
                tipoBox,
                criarCampoLeitura(i18nService.translate("common.value"),
                        d.valorTotal() != null ? String.format("%.2f", d.valorTotal()) : "-"),
                criarCampoLeitura(i18nService.translate("common.currency"), valorOuVazio(d.moedaCodigo())),
                criarCampoLeitura(i18nService.translate("common.date"), dataFormatada),
                criarCampoLeitura(i18nService.translate("stock.relatedOrder"), encomendaRelacionada)
        );

        root.getChildren().addAll(header, UiFactory.transparentScroll(content));
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

    @FXML private void handleFiltrar() { pagination.resetPage(); carregarMovimentos(); }
    @FXML private void handleMostrarTodos() {
        cmbFiltroTipo.setValue(null);
        cmbFiltroMoeda.setValue(null);
        handleFiltrar();
    }
}
