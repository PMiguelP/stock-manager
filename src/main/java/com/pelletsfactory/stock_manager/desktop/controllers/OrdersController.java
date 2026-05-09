package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaClienteSimpleDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaClienteResponseDTO;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.services.VendaService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OrdersController{
    private final VendaService vendaService;
    private final NavigationService navigationService;
    private final ToastService toastService;

    @FXML private ComboBox<EstadoEncomendaCliente> cmbFiltroEstado;
    @FXML private TextField txtFiltroCliente;
    @FXML private TableView<EncomendaClienteSimpleDTO> tblEncomendas;
    @FXML private VBox vboxContainer;

    @FXML private TableColumn<EncomendaClienteSimpleDTO, String> colCliente;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, LocalDate> colData;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, EstadoEncomendaCliente> colEstado;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, Double> colTotal;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, String> colMoeda;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, String> colTracking;
    @FXML private TableColumn<EncomendaClienteSimpleDTO, Void> colAcoes;

    private Label lblPaginaStatus;
    private ComboBox<Integer> cmbItemsPerPage;
    private HBox paginationButtons;
    private int itemsPerPage = 10;
    private int paginaAtual = 0;
    private int totalPaginas = 0;

    private final ObservableList<EncomendaClienteSimpleDTO> encomendas = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public OrdersController(VendaService vendaService,
                                      NavigationService navigationService,
                                      ToastService toastService) {
        this.vendaService = vendaService;
        this.navigationService = navigationService;
        this.toastService = toastService;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarComboBoxes();
        carregarEncomendas();
    }

    private void configurarTabela() {
        colCliente.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().clienteNome()));
        configurarColunaTexto(colCliente);

        colData.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().data()));
        colData.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.format(DATE_FORMATTER));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colEstado.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().estado()));
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoEncomendaCliente estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                } else {
                    setGraphic(criarBadgeEstado(estado));
                }
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colTotal.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().totalFinal()));
        colTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f", item));
                setPadding(new Insets(8, 10, 8, 10));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        colMoeda.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().moedaCodigo()));
        configurarColunaTexto(colMoeda);

        colTracking.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().codigoTracking()));
        configurarColunaTexto(colTracking);

        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("button-icon", "flat");
                btnDetails.setGraphic(new FontIcon("mdi2e-eye-outline:20"));
                btnDetails.setTooltip(new Tooltip("Ver Detalhes"));
                btnDetails.setOnAction(event -> {
                    EncomendaClienteSimpleDTO enc = getTableView().getItems().get(getIndex());
                    handleAbrirDetalhes(enc);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
                setAlignment(Pos.CENTER);
            }
        });

        tblEncomendas.setFixedCellSize(48);
        tblEncomendas.setItems(encomendas);
    }

    private <T> void configurarColunaTexto(TableColumn<EncomendaClienteSimpleDTO, T> coluna) {
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

    private void carregarEncomendas() {
        try {
            String clienteNome = (txtFiltroCliente != null && !txtFiltroCliente.getText().isEmpty())
                    ? txtFiltroCliente.getText() : null;
            EstadoEncomendaCliente estado = (cmbFiltroEstado != null) ? cmbFiltroEstado.getValue() : null;

            Page<EncomendaClienteSimpleDTO> page = vendaService.listarEncomendasComFiltrosSimples(
                    null, estado, paginaAtual + 1, itemsPerPage, "data", "DESC"
            );

            encomendas.setAll(page.getContent());
            totalPaginas = page.getTotalPages();
            if (lblPaginaStatus == null) configurarPaginacao(vboxContainer);
            atualizarLabelStatus(page);
            atualizarBotoesPaginacao();
        } catch (Exception e) {
            mostrarErro("Erro ao carregar: " + e.getMessage());
        }
    }

    @FXML
    private void handleAbrirModal() {
        mostrarErro("Funcionalidade de criação ainda não implementada");
    }

    private void handleAbrirDetalhes(EncomendaClienteSimpleDTO enc) {
        try {
            EncomendaClienteDetailsDTO d = vendaService.obterDetalhesEncomendaCliente(enc.id());
            VBox detalhesDrawer = criarDrawerVisualizacao(d);
            navigationService.showModal(detalhesDrawer);
        } catch (Exception e) {
            mostrarErro("Erro ao obter detalhes: " + e.getMessage());
        }
    }

    private VBox criarDrawerVisualizacao(EncomendaClienteDetailsDTO d) {
        VBox root = new VBox(0);
        root.setMinWidth(550);
        root.setPrefWidth(550);
        root.setMaxWidth(550);
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-subtle;");
        Label titulo = new Label("Detalhes da Encomenda");
        titulo.getStyleClass().add("title-3");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("mdi2c-close:22"));
        btnClose.getStyleClass().addAll("button-icon", "flat");
        btnClose.setOnAction(e -> navigationService.hideModal());
        header.getChildren().addAll(titulo, sp, btnClose);

        // Informação do Cliente
        VBox secaoCliente = criarSecao("Informação do Cliente");
        VBox camposCliente = new VBox(15);
        camposCliente.setPadding(new Insets(15));
        camposCliente.getChildren().addAll(
                criarCampoLeitura("Cliente", d.clienteNome()),
                criarCampoLeitura("Data da Encomenda", d.data() != null ? d.data().format(DATE_FORMATTER) : "")
        );
        secaoCliente.getChildren().add(camposCliente);

        // Detalhes da Encomenda
        VBox secaoDetalhes = criarSecao("Detalhes da Encomenda");
        VBox camposDetalhes = new VBox(15);
        camposDetalhes.setPadding(new Insets(15));

        HBox estadoBox = new HBox(10);
        estadoBox.setAlignment(Pos.CENTER_LEFT);
        Label lblEstadoLabel = new Label("Estado");
        lblEstadoLabel.getStyleClass().add("text-muted");
        lblEstadoLabel.setPrefWidth(120);
        estadoBox.getChildren().addAll(lblEstadoLabel, criarBadgeEstado(d.estado()));

        camposDetalhes.getChildren().addAll(
                estadoBox,
                criarCampoLeitura("Total Líquido", String.format("%.2f %s", d.totalNet(), d.moedaCodigo())),
                criarCampoLeitura("IVA", String.format("%.2f %s", d.totalIva(), d.moedaCodigo())),
                criarCampoLeitura("Total Final", String.format("%.2f %s", d.totalFinal(), d.moedaCodigo())),
                criarCampoLeitura("Código de Tracking", valorOuVazio(d.codigoTracking()))
        );
        secaoDetalhes.getChildren().add(camposDetalhes);

        // Itens da Encomenda
        VBox secaoItens = criarSecao("Itens da Encomenda (" + (d.itens() != null ? d.itens().size() : 0) + ")");
        if (d.itens() != null && !d.itens().isEmpty()) {
            VBox listaItens = new VBox(10);
            listaItens.setPadding(new Insets(15));
            for (ItemEncomendaClienteResponseDTO item : d.itens()) {
                VBox itemBox = new VBox(5);
                itemBox.setStyle("-fx-background-color: -color-bg-subtle; -fx-padding: 12; -fx-background-radius: 6;");
                Label lblTipo = new Label(item.tipoPelletNome());
                lblTipo.setStyle("-fx-font-weight: 500;");
                Label lblQuantidade = new Label(String.format("Quantidade: %.2f kg", item.quantidadeKg()));
                Label lblPreco = new Label(String.format("Preço Unitário: %.2f", item.precoUnitarioNet()));
                Label lblIva = new Label(String.format("IVA (%.1f%%): %.2f", item.taxaIva(), item.valorIvaCalculado()));
                itemBox.getChildren().addAll(lblTipo, lblQuantidade, lblPreco, lblIva);
                listaItens.getChildren().add(itemBox);
            }
            secaoItens.getChildren().add(listaItens);
        }

        VBox form = new VBox(20, secaoCliente, secaoDetalhes, secaoItens);
        form.setPadding(new Insets(30));

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);
        return root;
    }

    private VBox criarSecao(String titulo) {
        VBox secao = new VBox(0);
        secao.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("title-4");
        lblTitulo.setPadding(new Insets(15));
        lblTitulo.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0;");
        lblTitulo.setMaxWidth(Double.MAX_VALUE);

        secao.getChildren().add(lblTitulo);
        return secao;
    }

    private HBox criarCampoLeitura(String label, String valor) {
        HBox campo = new HBox(10);
        campo.setAlignment(Pos.CENTER_LEFT);
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("text-muted");
        lblLabel.setPrefWidth(120);
        Label lblValor = new Label(valorOuVazio(valor));
        lblValor.setStyle("-fx-font-weight: 500;");
        campo.getChildren().addAll(lblLabel, lblValor);
        return campo;
    }

    private String valorOuVazio(String value) {
        return value != null && !value.isEmpty() ? value : "-";
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
        cmbItemsPerPage.setOnAction(e -> {
            itemsPerPage = cmbItemsPerPage.getValue();
            paginaAtual = 0;
            carregarEncomendas();
        });
        HBox center = new HBox(10, new Label("Por página"), cmbItemsPerPage);
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
        prev.setOnAction(e -> { paginaAtual--; carregarEncomendas(); });
        paginationButtons.getChildren().add(prev);

        for (int i = 0; i < totalPaginas; i++) {
            if (i < 3 || i > totalPaginas - 2 || (i >= paginaAtual - 1 && i <= paginaAtual + 1)) {
                Button p = new Button(String.valueOf(i + 1));
                p.getStyleClass().add(i == paginaAtual ? "accent" : "flat");
                int finalI = i;
                p.setOnAction(e -> { paginaAtual = finalI; carregarEncomendas(); });
                paginationButtons.getChildren().add(p);
            }
        }

        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(paginaAtual >= totalPaginas - 1);
        next.setOnAction(e -> { paginaAtual++; carregarEncomendas(); });
        paginationButtons.getChildren().add(next);
    }

    private void atualizarLabelStatus(Page<EncomendaClienteSimpleDTO> page) {
        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        lblPaginaStatus.setText("Mostrando " + start + " a " + end + " de " + page.getTotalElements());
    }

    private HBox criarBadgeEstado(EstadoEncomendaCliente estado) {
        HBox b = new HBox(8);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(4, 10, 4, 10));
        b.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1.5;");

        String color = switch (estado) {
            case PENDENTE -> "#eab308";
            case CONFIRMADA -> "#3b82f6";
            case EM_PRODUCAO -> "#8b5cf6";
            case PRONTA -> "#06b6d4";
            case EXPEDIDA -> "#f97316";
            case CANCELADA -> "#ef4444";
        };

        b.setStyle(b.getStyle() + String.format("-fx-background-color: %s20; -fx-border-color: %s;",
                color.replace("#", ""), color));

        String icon = switch (estado) {
            case PENDENTE -> "mdi2c-clock-outline";
            case CONFIRMADA -> "mdi2c-check-circle-outline";
            case EM_PRODUCAO -> "mdi2c-cog-outline";
            case PRONTA -> "mdi2p-package-variant";
            case EXPEDIDA -> "mdi2t-truck-delivery-outline";
            case CANCELADA -> "mdi2c-close-circle-outline";
        };

        FontIcon ic = new FontIcon(icon);
        ic.setIconColor(javafx.scene.paint.Color.web(color));

        Label l = new Label(estado.name());
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");

        b.getChildren().addAll(ic, l);
        return b;
    }

    private void configurarComboBoxes() {
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(EstadoEncomendaCliente.values()));
    }

    private void mostrarSucesso(String m) { toastService.showSuccess("Sucesso", m); }
    private void mostrarErro(String m) { toastService.showError("Erro", m); }

    @FXML
    private void handleFiltrar() { paginaAtual = 0; carregarEncomendas(); }

    @FXML
    private void handleMostrarTodos() {
        txtFiltroCliente.clear();
        cmbFiltroEstado.setValue(null);
        handleFiltrar();
    }
}