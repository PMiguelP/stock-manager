package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.MateriaPrimaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.utils.PaginationControls;
import com.pelletsfactory.stock_manager.desktop.utils.UiFactory;
import javafx.beans.property.SimpleObjectProperty;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RawMaterialsController {

    private final StockService stockService;
    private final NavigationService navigationService;
    private final ToastService toastService;
    private final I18nService i18nService;

    @FXML private VBox vboxContainer;
    @FXML private HBox alertBox;
    @FXML private Label lblAlertCount;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private TableView<MateriaPrimaRow> tblRawMaterials;
    @FXML private TableColumn<MateriaPrimaRow, String> colMaterialId;
    @FXML private TableColumn<MateriaPrimaRow, String> colName;
    @FXML private TableColumn<MateriaPrimaRow, String> colUnit;
    @FXML private TableColumn<MateriaPrimaRow, String> colStock;
    @FXML private TableColumn<MateriaPrimaRow, String> colMinimum;
    @FXML private TableColumn<MateriaPrimaRow, String> colStatus;
    @FXML private TableColumn<MateriaPrimaRow, Void> colActions;

    private VBox drawerAdicionar;
    private TextField txtNomeAdicionar;
    private ComboBox<String> cmbUnidadeAdicionar;
    private TextField txtStockAdicionar;
    private TextField txtMinimoAdicionar;

    private PaginationControls pagination;

    private final ObservableList<MateriaPrimaRow> materiais = FXCollections.observableArrayList();

    public RawMaterialsController(StockService stockService,
                                  NavigationService navigationService,
                                  ToastService toastService,
                                  I18nService i18nService) {
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarMaterias, i18nService);
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(
                i18nService.translate("Normal"),
                i18nService.translate("Low"),
                i18nService.translate("Critical")
        ));
        configurarTabela();
        configurarDrawerAdicionar();
        carregarMaterias();
    }

    private void configurarTabela() {
        colMaterialId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().codigo()));
        configurarColunaTexto(colMaterialId);

        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().nome()));
        configurarColunaTexto(colName);

        colUnit.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().unidade()));
        configurarColunaTexto(colUnit);

        colStock.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stockAtual()));
        configurarColunaTexto(colStock);

        colMinimum.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stockMinimo()));
        configurarColunaTexto(colMinimum);

        colStatus.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().status()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setGraphic(empty || status == null ? null : criarBadgeStatus(status));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().addAll("button-icon", "flat");
                btn.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btn.setOnAction(e -> handleAbrirDetalhes(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });

        tblRawMaterials.setFixedCellSize(48);
        tblRawMaterials.setItems(materiais);
    }

    private <T> void configurarColunaTexto(TableColumn<MateriaPrimaRow, T> coluna) {
        coluna.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
    }

    private void carregarMaterias() {
        try {
            String nome = (txtSearch != null && !txtSearch.getText().isBlank()) ? txtSearch.getText() : null;
            Page<MateriaPrimaSimpleDTO> page = stockService.listarMateriasPrimasComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, "nome", "ASC"
            );

            List<MateriaPrimaRow> rows = new ArrayList<>();
            for (int i = 0; i < page.getContent().size(); i++) {
                MateriaPrimaSimpleDTO item = page.getContent().get(i);
                String status = calcularStatus(item.stockAtual(), item.stockMinimo());
                String statusLabel = i18nService.translate(status);
                if (cmbFiltroStatus.getValue() == null || cmbFiltroStatus.getValue().equalsIgnoreCase(statusLabel)) {
                    rows.add(MateriaPrimaRow.from(item, pagination.currentPage(), pagination.pageSize(), i, status));
                }
            }

            materiais.setAll(rows);
            pagination.attachTo(vboxContainer);
            pagination.update(page);
            atualizarAlerta();
        } catch (Exception e) {
            mostrarErro("Erro ao carregar materias-primas: " + e.getMessage());
        }
    }

    private void atualizarAlerta() {
        long count = stockService.contarMateriasAbaixoMinimo();
        if (count > 0) {
            alertBox.setVisible(true);
            alertBox.setManaged(true);
            lblAlertCount.setText(count + " " + i18nService.translate("raw materials are below minimum stock threshold"));
        } else {
            alertBox.setVisible(false);
            alertBox.setManaged(false);
        }
    }

    private String calcularStatus(Double stockAtual, Double stockMinimo) {
        if (stockAtual == null || stockMinimo == null || stockMinimo <= 0) {
            return "Normal";
        }
        if (stockAtual <= 0) {
            return "Critical";
        }
        if (stockAtual < stockMinimo * 0.5) {
            return "Critical";
        }
        if (stockAtual < stockMinimo) {
            return "Low";
        }
        return "Normal";
    }

    private HBox criarBadgeStatus(String status) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = switch (status) {
            case "Critical" -> "#ef4444";
            case "Low" -> "#f59e0b";
            default -> "#22c55e";
        };
        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s20; -fx-border-color: %s;",
                color.replace("#", ""), color));
        Label l = new Label(i18nService.translate(status));
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
        b.getChildren().add(l);
        return b;
    }

    @FXML
    private void handleFiltrar() {
        pagination.resetPage();
        carregarMaterias();
    }

    @FXML
    private void handleLimpar() {
        txtSearch.clear();
        cmbFiltroStatus.setValue(null);
        pagination.resetPage();
        carregarMaterias();
    }

    @FXML
    private void handleAbrirModal() {
        limparFormulario();
        navigationService.showModal(drawerAdicionar);
    }

    private void configurarDrawerAdicionar() {
        drawerAdicionar = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("New Raw Material", navigationService::hideModal);

        txtNomeAdicionar = new TextField();
        txtNomeAdicionar.setPromptText("e.g., Wood Chips");

        cmbUnidadeAdicionar = new ComboBox<>(FXCollections.observableArrayList("kg", "ton", "m3", "l"));
        cmbUnidadeAdicionar.setEditable(true);
        cmbUnidadeAdicionar.setPromptText("Select unit");
        cmbUnidadeAdicionar.setMaxWidth(Double.MAX_VALUE);

        txtStockAdicionar = new TextField();
        txtStockAdicionar.setPromptText("0");

        txtMinimoAdicionar = new TextField();
        txtMinimoAdicionar.setPromptText("0");

        VBox form = new VBox(20,
                criarCampo("Material Name", txtNomeAdicionar),
                criarCampo("Unit of Measure", cmbUnidadeAdicionar),
                criarCampo("Current Stock", txtStockAdicionar),
                criarCampo("Minimum Stock Threshold", txtMinimoAdicionar)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();
        Button btnCreate = new Button("Create Material");
        btnCreate.getStyleClass().add("accent");
        btnCreate.setPrefHeight(44);
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        btnCreate.setOnAction(e -> handleAdicionar());
        footer.getChildren().add(btnCreate);

        drawerAdicionar.getChildren().addAll(header, scrollPane, footer);
    }

    private VBox criarCampo(String label, Control input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(8, lbl, input);
    }

    private void limparFormulario() {
        txtNomeAdicionar.clear();
        cmbUnidadeAdicionar.setValue(null);
        txtStockAdicionar.clear();
        txtMinimoAdicionar.clear();
    }

    private void handleAdicionar() {
        if (txtNomeAdicionar.getText().isBlank()) {
            mostrarErro(i18nService.translate("Nome da matéria-prima é obrigatório"));
            return;
        }
        if (cmbUnidadeAdicionar.getValue() == null || cmbUnidadeAdicionar.getValue().isBlank()) {
            mostrarErro(i18nService.translate("Unidade da matéria-prima é obrigatória"));
            return;
        }

        try {
            Double stockAtual = parseNumeroNaoNegativoOuNull(txtStockAdicionar.getText(), "Stock atual");
            Double stockMinimo = parseNumeroNaoNegativoOuNull(txtMinimoAdicionar.getText(), "Stock mínimo");
            MateriaPrimaRequestDTO dto = new MateriaPrimaRequestDTO(
                    txtNomeAdicionar.getText().trim(),
                    cmbUnidadeAdicionar.getValue().trim(),
                    stockAtual,
                    stockMinimo
            );
            stockService.criarMateriaPrima(dto);
            pagination.resetPage();
            carregarMaterias();
            navigationService.hideModal();
            mostrarSucesso(i18nService.translate("Matéria-prima criada!"));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("Erro ao criar matéria-prima: ") + e.getMessage());
        }
    }

    private Double parseNumeroNaoNegativoOuNull(String raw, String campo) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double valor = Double.parseDouble(raw.replace(",", "."));
            if (!Double.isFinite(valor) || valor < 0) {
                throw new NumberFormatException();
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " deve ser um número igual ou superior a zero");
        }
    }

    private void handleAbrirDetalhes(MateriaPrimaRow row) {
        try {
            MateriaPrimaDetailsDTO d = stockService.obterDetalhesMateriaPrima(row.id());
            navigationService.showModal(criarDrawerDetalhes(d));
        } catch (Exception e) {
            mostrarErro(i18nService.translate("Erro ao obter detalhes: ") + e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(MateriaPrimaDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Raw Material Details", navigationService::hideModal);

        TextField txtNome = new TextField(d.nome());
        TextField txtUnidade = new TextField(d.unidade());
        TextField txtStock = new TextField(d.stockAtual() != null ? d.stockAtual().toString() : "");
        TextField txtMinimo = new TextField(d.stockMinimo() != null ? d.stockMinimo().toString() : "");
        txtNome.setEditable(false);
        txtUnidade.setEditable(false);
        txtStock.setEditable(false);
        txtMinimo.setEditable(false);

        VBox form = new VBox(20,
                criarCampo("Material Name", txtNome),
                criarCampo("Unit of Measure", txtUnidade),
                criarCampo("Current Stock", txtStock),
                criarCampo("Minimum Stock Threshold", txtMinimo)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private void mostrarErro(String m) {
        toastService.showError(i18nService.translate("Erro"), m);
    }

    private void mostrarSucesso(String m) {
        toastService.showSuccess(i18nService.translate("Sucesso"), m);
    }

    private record MateriaPrimaRow(
            UUID id,
            String codigo,
            String nome,
            String unidade,
            String stockAtual,
            String stockMinimo,
            String status
    ) {
        static MateriaPrimaRow from(MateriaPrimaSimpleDTO dto, int paginaAtual, int itemsPerPage, int index, String status) {
            int seq = paginaAtual * itemsPerPage + index + 1;
            String codigo = String.format("RM-%03d", seq);
            String stockAtual = dto.stockAtual() != null ? String.format("%.2f", dto.stockAtual()) : "-";
            String stockMinimo = dto.stockMinimo() != null ? String.format("%.2f", dto.stockMinimo()) : "-";
            return new MateriaPrimaRow(
                    dto.id(),
                    codigo,
                    dto.nome(),
                    dto.unidade(),
                    stockAtual,
                    stockMinimo,
                    status
            );
        }
    }
}
