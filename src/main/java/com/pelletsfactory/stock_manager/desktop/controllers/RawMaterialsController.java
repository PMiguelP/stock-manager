package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.MateriaPrimaRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.MateriaPrimaSimpleDTO;
import com.pelletsfactory.stock_manager.common.services.StockService;
import com.pelletsfactory.stock_manager.desktop.services.FormValidationService;
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
    private final FormValidationService formValidationService;

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
    private Label lblErroNomeAdicionar;
    private Label lblErroUnidadeAdicionar;
    private Label lblErroStockAdicionar;

    private PaginationControls pagination;

    private final ObservableList<MateriaPrimaRow> materiais = FXCollections.observableArrayList();

    public RawMaterialsController(StockService stockService,
                                  NavigationService navigationService,
                                  ToastService toastService,
                                  I18nService i18nService,
                                  FormValidationService formValidationService) {
        this.stockService = stockService;
        this.navigationService = navigationService;
        this.toastService = toastService;
        this.i18nService = i18nService;
        this.formValidationService = formValidationService;
    }

    @FXML
    public void initialize() {
        pagination = new PaginationControls(10, this::carregarMaterias, i18nService);
        cmbFiltroStatus.setItems(FXCollections.observableArrayList("Normal", "Low", "Critical"));
        cmbFiltroStatus.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String key) { return key == null ? "" : i18nService.translate(key); }
            @Override public String fromString(String s) { return s; }
        });
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
            String status = cmbFiltroStatus.getValue();
            Page<MateriaPrimaSimpleDTO> page = stockService.listarMateriasPrimasComFiltros(
                    pagination.pageNumberForService(), pagination.pageSize(), nome, null, status, "nome", "ASC"
            );

            List<MateriaPrimaRow> rows = new ArrayList<>();
            for (int i = 0; i < page.getContent().size(); i++) {
                MateriaPrimaSimpleDTO item = page.getContent().get(i);
                String itemStatus = calcularStatus(item.stockAtual(), item.stockMinimo());
                rows.add(MateriaPrimaRow.from(item, pagination.currentPage(), pagination.pageSize(), i, itemStatus));
            }

            materiais.setAll(rows);
            pagination.attachTo(vboxContainer);
            pagination.update(page);
        } catch (Exception e) {
            mostrarErro(i18nService.translate("rawMaterials.loadError") + ": " + e.getMessage());
        }
        try {
            atualizarAlerta();
        } catch (Exception ignored) {}
    }

    private void atualizarAlerta() {
        long count = stockService.contarMateriasAbaixoMinimo();
        if (count > 0) {
            alertBox.setVisible(true);
            alertBox.setManaged(true);
            lblAlertCount.setText(count + " " + i18nService.translate("rawMaterials.lowStockAlertCount"));
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
        HBox header = UiFactory.drawerHeader("Nova Matéria-Prima", navigationService::hideModal);

        txtNomeAdicionar = new TextField();
        txtNomeAdicionar.setPromptText("ex: Aparas de Madeira");
        lblErroNomeAdicionar = formValidationService.createErrorLabel();

        cmbUnidadeAdicionar = new ComboBox<>(FXCollections.observableArrayList("kg", "ton", "m3", "l"));
        cmbUnidadeAdicionar.setEditable(true);
        cmbUnidadeAdicionar.setPromptText("Selecionar unidade");
        cmbUnidadeAdicionar.setMaxWidth(Double.MAX_VALUE);
        lblErroUnidadeAdicionar = formValidationService.createErrorLabel();

        txtStockAdicionar = new TextField();
        txtStockAdicionar.setPromptText("0");
        lblErroStockAdicionar = formValidationService.createErrorLabel();

        txtMinimoAdicionar = new TextField();
        txtMinimoAdicionar.setPromptText("0");

        formValidationService.attachTextAutoClear(txtNomeAdicionar, lblErroNomeAdicionar);
        formValidationService.attachComboAutoClear(cmbUnidadeAdicionar, lblErroUnidadeAdicionar);
        formValidationService.attachTextAutoClear(txtStockAdicionar, lblErroStockAdicionar);

        VBox form = new VBox(20,
                criarCampoComErro("Nome", txtNomeAdicionar, lblErroNomeAdicionar),
                criarCampoComErro("Unidade de Medida", cmbUnidadeAdicionar, lblErroUnidadeAdicionar),
                criarCampoComErro("Stock Atual", txtStockAdicionar, lblErroStockAdicionar),
                criarCampo("Stock Mínimo", txtMinimoAdicionar)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);
        HBox footer = UiFactory.drawerFooter();
        Button btnCreate = new Button("Guardar Matéria-Prima");
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

    private VBox criarCampoComErro(String label, Control input, Label erroLabel) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("text-muted");
        return new VBox(6, lbl, input, erroLabel);
    }

    private void limparFormulario() {
        txtNomeAdicionar.clear();
        cmbUnidadeAdicionar.setValue(null);
        txtStockAdicionar.clear();
        txtMinimoAdicionar.clear();
        formValidationService.clearError(txtNomeAdicionar, lblErroNomeAdicionar);
        formValidationService.clearError(cmbUnidadeAdicionar, lblErroUnidadeAdicionar);
        formValidationService.clearError(txtStockAdicionar, lblErroStockAdicionar);
    }

    private void handleAdicionar() {
        boolean valido = true;
        valido = formValidationService.validateRequiredText(txtNomeAdicionar, lblErroNomeAdicionar, "Nome é obrigatório") && valido;
        valido = formValidationService.validateRequiredCombo(cmbUnidadeAdicionar, lblErroUnidadeAdicionar, "Unidade é obrigatória") && valido;

        Double stockAtual = null;
        if (!txtStockAdicionar.getText().isBlank()) {
            valido = formValidationService.validateRegex(
                    txtStockAdicionar, lblErroStockAdicionar,
                    "[0-9]+(\\.[0-9]+)?([,][0-9]+)?",
                    "Stock deve ser um número não negativo"
            ) && valido;
            if (valido) {
                stockAtual = Double.parseDouble(txtStockAdicionar.getText().replace(",", "."));
            }
        }

        if (!valido) return;

        try {
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
            mostrarSucesso(i18nService.translate("rawMaterials.created"));
        } catch (Exception e) {
            mostrarErro(e.getMessage());
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
            mostrarErro(e.getMessage());
        }
    }

    private VBox criarDrawerDetalhes(MateriaPrimaDetailsDTO d) {
        VBox root = UiFactory.drawerRoot(550);
        HBox header = UiFactory.drawerHeader("Editar Matéria-Prima", navigationService::hideModal);

        TextField txtNome = new TextField(d.nome() != null ? d.nome() : "");
        Label lblErroNome = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtNome, lblErroNome);

        ComboBox<String> cmbUnidade = new ComboBox<>(FXCollections.observableArrayList("kg", "ton", "m3", "l"));
        cmbUnidade.setEditable(true);
        cmbUnidade.setValue(d.unidade());
        cmbUnidade.setMaxWidth(Double.MAX_VALUE);
        Label lblErroUnidade = formValidationService.createErrorLabel();
        formValidationService.attachComboAutoClear(cmbUnidade, lblErroUnidade);

        TextField txtStock = new TextField(d.stockAtual() != null ? String.format("%.2f", d.stockAtual()) : "");
        Label lblErroStock = formValidationService.createErrorLabel();
        formValidationService.attachTextAutoClear(txtStock, lblErroStock);

        TextField txtMinimo = new TextField(d.stockMinimo() != null ? String.format("%.2f", d.stockMinimo()) : "");

        VBox form = new VBox(20,
                criarCampoComErro("Nome", txtNome, lblErroNome),
                criarCampoComErro("Unidade de Medida", cmbUnidade, lblErroUnidade),
                criarCampoComErro("Stock Atual", txtStock, lblErroStock),
                criarCampo("Stock Mínimo", txtMinimo)
        );
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = UiFactory.transparentScroll(form);

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(25));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        Button btnGuardar = new Button("Guardar Alterações");
        btnGuardar.getStyleClass().add("accent");
        btnGuardar.setPrefHeight(44);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnGuardar, Priority.ALWAYS);

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.setPrefHeight(44);
        btnEliminar.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;");

        footer.getChildren().addAll(btnGuardar, btnEliminar);

        btnGuardar.setOnAction(e -> {
            boolean valido = true;
            valido = formValidationService.validateRequiredText(txtNome, lblErroNome, "Nome é obrigatório") && valido;
            valido = formValidationService.validateRequiredCombo(cmbUnidade, lblErroUnidade, "Unidade é obrigatória") && valido;

            Double stockAtual = null;
            if (!txtStock.getText().isBlank()) {
                valido = formValidationService.validateRegex(txtStock, lblErroStock,
                        "[0-9]+(\\.[0-9]+)?([,][0-9]+)?", "Stock deve ser um número não negativo") && valido;
                if (valido) stockAtual = Double.parseDouble(txtStock.getText().replace(",", "."));
            }
            if (!valido) return;

            try {
                Double stockMinimo = parseNumeroNaoNegativoOuNull(txtMinimo.getText(), "Stock mínimo");
                stockService.atualizarMateriaPrima(d.id(), new MateriaPrimaRequestDTO(
                        txtNome.getText().trim(),
                        cmbUnidade.getValue().trim(),
                        stockAtual,
                        stockMinimo
                ));
                pagination.resetPage();
                carregarMaterias();
                navigationService.hideModal();
                mostrarSucesso(i18nService.translate("rawMaterials.updated"));
            } catch (Exception ex) {
                mostrarErro(ex.getMessage());
            }
        });

        btnEliminar.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Eliminar a matéria-prima \"" + d.nome() + "\"?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirmar eliminação");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        stockService.apagarMateriaPrima(d.id());
                        pagination.resetPage();
                        carregarMaterias();
                        navigationService.hideModal();
                        mostrarSucesso(i18nService.translate("rawMaterials.deleted"));
                    } catch (Exception ex) {
                        mostrarErro(ex.getMessage());
                    }
                }
            });
        });

        root.getChildren().addAll(header, scrollPane, footer);
        return root;
    }

    private void mostrarErro(String m) {
        toastService.showError(i18nService.translate("common.error"), m);
    }

    private void mostrarSucesso(String m) {
        toastService.showSuccess(i18nService.translate("common.success"), m);
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
