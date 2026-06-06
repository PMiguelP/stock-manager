package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.services.NotificacaoService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import com.pelletsfactory.stock_manager.desktop.services.SupportTicketSelectionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class NotificationsController {

    private static final Logger log = LoggerFactory.getLogger(NotificationsController.class);

    @FXML private Label lblNotificationsSummary;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Button btnMarkAllRead;
    @FXML private VBox notificationsList;
    @FXML private ScrollPane notificationsScroll;

    private final NotificacaoService notificacaoService;
    private final ToastService toastService;
    private final NavigationService navigationService;
    private final SupportTicketSelectionService supportTicketSelectionService;
    private final ObservableList<NotificationItem> allNotifications = FXCollections.observableArrayList();

    public NotificationsController(NotificacaoService notificacaoService,
                                   ToastService toastService,
                                   NavigationService navigationService,
                                   SupportTicketSelectionService supportTicketSelectionService) {
        this.notificacaoService = notificacaoService;
        this.toastService = toastService;
        this.navigationService = navigationService;
        this.supportTicketSelectionService = supportTicketSelectionService;
    }

    @FXML
    public void initialize() {
        cmbType.setItems(FXCollections.observableArrayList(buildTypeOptions()));
        cmbStatus.setItems(FXCollections.observableArrayList("All Status", "Unread", "Read", "Pending", "Done"));
        cmbType.setValue("All Types");
        cmbStatus.setValue("All Status");

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> render());
        cmbType.valueProperty().addListener((obs, oldValue, newValue) -> loadNotifications());
        cmbStatus.valueProperty().addListener((obs, oldValue, newValue) -> loadNotifications());
        btnMarkAllRead.setOnAction(event -> markAllAsRead());

        loadNotifications();
    }

    private List<String> buildTypeOptions() {
        List<String> options = Arrays.stream(TipoEventoNotificacao.values())
                .map(TipoEventoNotificacao::getDisplayName)
                .collect(Collectors.toList());
        options.add(0, "All Types");
        return options;
    }

    private void loadNotifications() {
        try {
            TipoEventoNotificacao tipo = selectedTipoEvento();
            Boolean lida = selectedReadFilter();
            Boolean concluida = selectedDoneFilter();

            Page<NotificacaoSimpleDTO> page = notificacaoService.listarParaUtilizadorAtualSimples(
                    1, 100, tipo, lida, concluida, "createdAt", "DESC"
            );

            allNotifications.setAll(page.getContent().stream()
                    .map(NotificationItem::from)
                    .collect(Collectors.toList()));
            render();
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao carregar notificações: " + e.getMessage());
        }
    }

    private TipoEventoNotificacao selectedTipoEvento() {
        String selected = cmbType.getValue();
        if (selected == null || "All Types".equals(selected)) {
            return null;
        }
        return Arrays.stream(TipoEventoNotificacao.values())
                .filter(tipo -> tipo.getDisplayName().equals(selected))
                .findFirst()
                .orElse(null);
    }

    private Boolean selectedReadFilter() {
        String status = cmbStatus.getValue();
        if ("Unread".equals(status)) return false;
        if ("Read".equals(status)) return true;
        return null;
    }

    private Boolean selectedDoneFilter() {
        String status = cmbStatus.getValue();
        if ("Pending".equals(status)) return false;
        if ("Done".equals(status)) return true;
        return null;
    }

    private void markAllAsRead() {
        try {
            notificacaoService.marcarTodasComoLidas();
            loadNotifications();
        } catch (Exception e) {
            toastService.showError("Erro", "Erro ao marcar notificações como lidas: " + e.getMessage());
        }
    }

    private void render() {
        List<NotificationItem> filtered = filterNotifications();
        notificationsList.getChildren().clear();

        if (filtered.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setPadding(new Insets(40));
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;");
            Label title = new Label("Sem notificações para estes filtros");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");
            Label desc = new Label("Tenta ajustar a pesquisa ou os filtros de tipo e estado.");
            desc.getStyleClass().add("text-muted");
            emptyState.getChildren().addAll(title, desc);
            notificationsList.getChildren().add(emptyState);
        } else {
            filtered.forEach(item -> notificationsList.getChildren().add(createNotificationCard(item)));
        }

        long unread = allNotifications.stream().filter(n -> n.unread).count();
        lblNotificationsSummary.setText(unread + " unread notifications • " + allNotifications.size() + " total");
        btnMarkAllRead.setDisable(unread == 0);
        notificationsScroll.setVvalue(0);
    }

    private List<NotificationItem> filterNotifications() {
        String search = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);

        return allNotifications.stream()
                .filter(item -> search.isEmpty()
                        || searchable(item.title).contains(search)
                        || searchable(item.description).contains(search))
                .collect(Collectors.toList());
    }

    private String searchable(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private HBox createNotificationCard(NotificationItem item) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 10; -fx-border-color: -color-border-muted; -fx-border-radius: 10;");
        if (item.isTicketNotification()) {
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(event -> {
                if (item.unread) {
                    notificacaoService.marcarComoLida(item.id);
                }
                supportTicketSelectionService.select(item.linkReferencia);
                navigationService.navigateTo("/support");
            });
        }

        StackPane iconWrap = new StackPane();
        iconWrap.setMinSize(48, 48);
        iconWrap.setPrefSize(48, 48);
        iconWrap.setMaxSize(48, 48);
        iconWrap.setStyle(String.format(
                "-fx-background-color: %s22; -fx-background-radius: 999; -fx-border-color: %s88; "
                        + "-fx-border-radius: 999; -fx-border-width: 1;",
                item.iconColor,
                item.iconColor
        ));

        FontIcon icon = new FontIcon();
        try {
            icon.setIconLiteral(item.iconLiteral);
        } catch (IllegalArgumentException exception) {
            log.warn("Ícone inválido na notificação {}: {}", item.id, item.iconLiteral, exception);
            icon.setIconLiteral("mdi2b-bell-outline");
        }
        icon.setIconSize(22);
        icon.setIconColor(Color.web(item.iconColor));
        iconWrap.getChildren().add(icon);

        VBox content = new VBox(10);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(item.title);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
        titleLine.getChildren().add(title);

        if (item.unread) {
            StackPane dot = new StackPane();
            dot.setMinSize(7, 7);
            dot.setPrefSize(7, 7);
            dot.setStyle("-fx-background-color: -color-accent-emphasis; -fx-background-radius: 999;");
            titleLine.getChildren().add(dot);
        }

        Label desc = new Label(item.description != null ? item.description : "");
        desc.getStyleClass().add("text-muted");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 15px;");

        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(item.type);
        typeBadge.setStyle(String.format(
                "-fx-padding: 4 12 4 12; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: %s; -fx-border-color: %s;",
                item.iconColor,
                item.iconColor
        ));
        meta.getChildren().add(typeBadge);

        if (item.requerAcao) {
            Label doneBadge = new Label(item.done ? "Done" : "Pending");
            String doneColor = item.done ? "#22c55e" : "#f97316";
            doneBadge.setStyle(String.format(
                    "-fx-padding: 4 12 4 12; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: %s; -fx-border-color: %s;",
                    doneColor,
                    doneColor
            ));
            meta.getChildren().add(doneBadge);
        }

        if (item.done && item.doneBy != null) {
            Label doneBy = new Label("Feita por " + item.doneBy);
            doneBy.getStyleClass().add("text-muted");
            meta.getChildren().add(doneBy);
        }

        if (item.unread) {
            Hyperlink markRead = new Hyperlink("Marcar como lida");
            markRead.setFocusTraversable(false);
            markRead.setStyle("-fx-text-fill: -color-accent-emphasis;");
            markRead.setOnAction(event -> {
                try {
                    notificacaoService.marcarComoLida(item.id);
                    loadNotifications();
                } catch (Exception e) {
                    toastService.showError("Erro", "Erro ao marcar como lida: " + e.getMessage());
                }
            });
            meta.getChildren().add(markRead);
        }

        if (item.requerAcao && !item.done) {
            Hyperlink markDone = new Hyperlink("Marcar como feita");
            markDone.setFocusTraversable(false);
            markDone.setStyle("-fx-text-fill: -color-accent-emphasis;");
            markDone.setOnAction(event -> {
                try {
                    notificacaoService.marcarComoConcluida(item.id);
                    loadNotifications();
                } catch (Exception e) {
                    toastService.showError("Erro", "Erro ao marcar como feita: " + e.getMessage());
                }
            });
            meta.getChildren().add(markDone);
        }

        content.getChildren().addAll(titleLine, desc, meta);

        VBox right = new VBox(2);
        right.setAlignment(Pos.TOP_RIGHT);
        Label timeAgo = new Label(item.timeAgo);
        timeAgo.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-fg-muted;");
        Label date = new Label(item.date);
        date.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");
        right.getChildren().addAll(timeAgo, date);

        card.getChildren().addAll(iconWrap, content, right);
        return card;
    }

    private static final class NotificationItem {
        private static final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

        private final UUID id;
        private final String title;
        private final String description;
        private final String type;
        private final String timeAgo;
        private final String date;
        private final String iconLiteral;
        private final String iconColor;
        private final boolean unread;
        private final boolean requerAcao;
        private final boolean done;
        private final String doneBy;
        private final UUID linkReferencia;

        private NotificationItem(UUID id, String title, String description, String type, String timeAgo, String date,
                                 String iconLiteral, String iconColor, boolean unread, boolean requerAcao,
                                 boolean done, String doneBy, UUID linkReferencia) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.timeAgo = timeAgo;
            this.date = date;
            this.iconLiteral = iconLiteral;
            this.iconColor = iconColor;
            this.unread = unread;
            this.requerAcao = requerAcao;
            this.done = done;
            this.doneBy = doneBy;
            this.linkReferencia = linkReferencia;
        }

        private static NotificationItem from(NotificacaoSimpleDTO dto) {
            TipoEventoNotificacao tipo = dto.tipoEvento();
            return new NotificationItem(
                    dto.id(),
                    dto.titulo(),
                    dto.mensagem(),
                    tipo != null ? tipo.getDisplayName() : "Geral",
                    formatTimeAgo(dto.createdAt()),
                    dto.createdAt() != null ? DATE_FORMATTER.format(dto.createdAt()) : "",
                    iconFor(tipo),
                    colorFor(tipo),
                    !Boolean.TRUE.equals(dto.lida()),
                    Boolean.TRUE.equals(dto.requerAcao()),
                    Boolean.TRUE.equals(dto.concluida()),
                    dto.concluidaPorNome(),
                    dto.linkReferencia()
            );
        }

        private static String formatTimeAgo(Instant instant) {
            if (instant == null) {
                return "";
            }
            Duration duration = Duration.between(instant, Instant.now());
            if (duration.toMinutes() < 1) return "agora";
            if (duration.toMinutes() < 60) return duration.toMinutes() + " min";
            if (duration.toHours() < 24) return duration.toHours() + " h";
            return duration.toDays() + " d";
        }

        private static String iconFor(TipoEventoNotificacao tipo) {
            if (tipo == null) return "mdi2b-bell-outline";
            return switch (tipo) {
                case STOCK_BAIXO -> "mdi2a-alert-circle-outline";
                case NOVA_ENCOMENDA -> "mdi2c-cart-outline";
                case NOVA_ORDEM_PRODUCAO -> "mdi2f-factory";
                case ORDEM_CONCLUIDA -> "mdi2c-check-circle-outline";
                case EXPEDICAO_REALIZADA -> "mdi2t-truck-delivery";
                case ERRO_PRODUCAO -> "mdi2a-alert-circle-outline";
                case NOVO_TICKET, NOVA_MENSAGEM_TICKET -> "mdi2m-message-text-outline";
            };
        }

        private static String colorFor(TipoEventoNotificacao tipo) {
            if (tipo == null) return "#64748b";
            return switch (tipo) {
                case STOCK_BAIXO, ERRO_PRODUCAO -> "#f97316";
                case NOVA_ENCOMENDA -> "#3b82f6";
                case NOVA_ORDEM_PRODUCAO -> "#8b5cf6";
                case ORDEM_CONCLUIDA, EXPEDICAO_REALIZADA -> "#22c55e";
                case NOVO_TICKET, NOVA_MENSAGEM_TICKET -> "#3b82f6";
            };
        }

        private boolean isTicketNotification() {
            return linkReferencia != null
                    && (type.equals(TipoEventoNotificacao.NOVO_TICKET.getDisplayName())
                    || type.equals(TipoEventoNotificacao.NOVA_MENSAGEM_TICKET.getDisplayName()));
        }
    }
}
