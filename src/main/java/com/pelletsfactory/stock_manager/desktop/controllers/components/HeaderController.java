package com.pelletsfactory.stock_manager.desktop.controllers.components;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.Breadcrumbs.BreadCrumbItem;
import atlantafx.base.theme.Styles;
import com.pelletsfactory.stock_manager.common.entities.Funcionario;
import com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario;
import com.pelletsfactory.stock_manager.common.dto.response.NotificacaoResponseDTO;
import com.pelletsfactory.stock_manager.common.enums.TipoEventoNotificacao;
import com.pelletsfactory.stock_manager.common.services.NotificacaoService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.NavigationEvent;
import com.pelletsfactory.stock_manager.desktop.services.NavigationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class HeaderController {

    private static final Logger log = LoggerFactory.getLogger(HeaderController.class);

    @FXML private Breadcrumbs<String> breadcrumbs;
    @FXML private Label lblUserInitials;
    @FXML private Label lblUserName;
    @FXML private Label lblUserNumber;
    @FXML private Label lblNotificationCount;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private I18nService i18nService;

    @FXML
    public void initialize() {
        configurarBreadcrumbs(List.of("Home"));
        carregarFuncionarioLogado();
        atualizarContadorNotificacoes();
    }

    @FXML
    private void handleOpenSettings() {
        navigationService.navigateTo("/settings");
    }

    @FXML
    private void handleOpenNotifications() {
        try {
            atualizarContadorNotificacoes();

            VBox notificationsDrawer = new VBox(0);
            notificationsDrawer.setMinWidth(550);
            notificationsDrawer.setPrefWidth(550);
            notificationsDrawer.setMaxWidth(550);
            notificationsDrawer.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(notificationsDrawer, Priority.ALWAYS);
            notificationsDrawer.setStyle("-fx-background-color: -color-bg-default; -fx-border-width: 0 0 0 1; -fx-border-color: -color-border-muted;");

            // 1. HEADER
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(30, 30, 10, 30));

            Label title = new Label("Notifications");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnClose = new Button();
            // CORREÇÃO CRÍTICA: mdi2c-close em vez de mdi2x-close
            btnClose.setGraphic(new FontIcon("mdi2c-close"));
            btnClose.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 18px;");
            btnClose.setOnAction(e -> navigationService.hideModal());

            header.getChildren().addAll(title, spacer, btnClose);
            notificationsDrawer.getChildren().add(header);

            // 2. SUB-HEADER
            HBox subHeader = new HBox();
            subHeader.setAlignment(Pos.CENTER_LEFT);
            subHeader.setPadding(new Insets(0, 30, 20, 30));

            long unreadCount = notificacaoService.contarNotLidas();
            Label subTitle = new Label(unreadCount + " unread notifications");
            subTitle.setStyle("-fx-text-fill: -color-fg-muted;");

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            Hyperlink markRead = new Hyperlink("Mark all as read");
            markRead.setStyle("-fx-text-fill: -color-accent-fg; -fx-underline: false; -fx-font-weight: bold;");
            markRead.setDisable(unreadCount == 0);

            subHeader.getChildren().addAll(subTitle, spacer2, markRead);
            notificationsDrawer.getChildren().add(subHeader);

            // 3. LISTA DE CARDS (SCROLLABLE)
            VBox listContainer = new VBox(15);
            listContainer.setPadding(new Insets(0, 30, 30, 30));

            ScrollPane scrollPane = new ScrollPane(listContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            Page<NotificacaoResponseDTO> page = notificacaoService.listarParaUtilizadorAtual(
                    1, 10, null, null, null, "createdAt", "DESC"
            );
            List<NotificacaoResponseDTO> items = page.getContent();

            markRead.setOnAction(e -> {
                for (NotificacaoResponseDTO item : items) {
                    if (!Boolean.TRUE.equals(item.lida())) {
                        notificacaoService.marcarComoLida(item.id());
                    }
                }
                navigationService.hideModal();
                atualizarContadorNotificacoes();
                handleOpenNotifications();
            });

            if (items.isEmpty()) {
                VBox emptyState = new VBox(8);
                emptyState.setAlignment(Pos.CENTER);
                emptyState.setPadding(new Insets(40));
                emptyState.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;");
                Label emptyTitle = new Label("Sem notificações");
                emptyTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
                Label emptyDesc = new Label("Quando houver novidades, aparecem aqui.");
                emptyDesc.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");
                emptyState.getChildren().addAll(emptyTitle, emptyDesc);
                listContainer.getChildren().add(emptyState);
            }

            for (NotificacaoResponseDTO item : items) {
                TipoEventoNotificacao tipo = item.tipoEvento();
                boolean unread = !Boolean.TRUE.equals(item.lida());
                HBox card = new HBox(15);
                card.setPadding(new Insets(18));
                card.setAlignment(Pos.TOP_LEFT);
                card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;");

                // Ícone com fundo circular suave
                StackPane iconBox = new StackPane();
                iconBox.setMinWidth(48); iconBox.setMinHeight(48);
                String color = colorFor(tipo);
                iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 10;");

                FontIcon icon = criarIcone(iconFor(tipo), color, 22);
                iconBox.getChildren().add(icon);

                // Textos
                VBox texts = new VBox(4);
                HBox.setHgrow(texts, Priority.ALWAYS);

                HBox titleLine = new HBox();
                titleLine.setAlignment(Pos.CENTER_LEFT);
                Label lblTitle = new Label(item.titulo());
                lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                titleLine.getChildren().add(lblTitle);

                if (unread) {
                    Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
                    Circle dot = new Circle(4, Color.web(color));
                    titleLine.getChildren().addAll(s, dot);
                }

                Label lblDesc = new Label(item.mensagem());
                lblDesc.setWrapText(true);
                lblDesc.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 13px;");

                Label lblTime = new Label(formatTimeAgo(item.createdAt()));
                lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");

                texts.getChildren().addAll(titleLine, lblDesc, lblTime);
                card.getChildren().addAll(iconBox, texts);

                // Hover
                card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: -color-base-3; -fx-background-radius: 12; -fx-border-color: -color-accent-emphasis; -fx-border-radius: 12; -fx-cursor: hand;"));
                card.setOnMouseExited(e -> card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;"));
                card.setOnMouseClicked(e -> {
                    if (unread) {
                        notificacaoService.marcarComoLida(item.id());
                        atualizarContadorNotificacoes();
                    }
                });

                listContainer.getChildren().add(card);
            }

            notificationsDrawer.getChildren().add(scrollPane);

            // 4. FOOTER
            HBox footer = new HBox();
            footer.setAlignment(Pos.CENTER);
            footer.setPadding(new Insets(20));
            Hyperlink viewAll = new Hyperlink("View All Notifications");
            viewAll.setStyle("-fx-text-fill: -color-accent-fg; -fx-font-weight: bold;");
            viewAll.setOnAction(e -> {
                navigationService.hideModal();
                navigationService.navigateTo("/notifications");
            });
            footer.getChildren().add(viewAll);
            notificationsDrawer.getChildren().add(footer);

            navigationService.showModal(notificationsDrawer);

        } catch (Exception e) {
            log.error("Erro ao abrir notificações", e);
        }
    }

    @EventListener
    public void onNavigationEvent(NavigationEvent event) {
        Platform.runLater(() -> {
            if (breadcrumbs != null && event.breadcrumbs() != null && !event.breadcrumbs().isEmpty()) {
                configurarBreadcrumbs(event.breadcrumbs());
            }
        });
    }

    public void updateBreadcrumbPath(String... breadcrumbPath) {
        if (breadcrumbPath == null || breadcrumbPath.length == 0) {
            return;
        }
        configurarBreadcrumbs(List.of(breadcrumbPath));
    }

    private void configurarBreadcrumbs(List<String> items) {
        BreadCrumbItem<String> root = Breadcrumbs.buildTreeModel(items.toArray(String[]::new));
        breadcrumbs.setCrumbFactory(crumb -> {
            var btn = new Button(crumb.getValue());
            btn.getStyleClass().add(Styles.FLAT);
            btn.setFocusTraversable(false);
            return btn;
        });
        breadcrumbs.setDividerFactory(item -> (item == null) ? new FontIcon(MaterialDesignH.HOME) : (!item.isLast() ? new FontIcon(MaterialDesignC.CHEVRON_RIGHT) : null));

        BreadCrumbItem<String> lastItem = root;
        while (lastItem.getChildren() != null && !lastItem.getChildren().isEmpty()) {
            lastItem = (BreadCrumbItem<String>) lastItem.getChildren().get(0);
        }
        breadcrumbs.setSelectedCrumb(lastItem);
        atualizarContadorNotificacoes();
    }

    private void carregarFuncionarioLogado() {
        Funcionario funcionario = SessaoFuncionario.getFuncionarioLogado();
        if (funcionario == null) {
            lblUserInitials.setText(i18nService.translate("common.initialsPlaceholder"));
            lblUserName.setText(i18nService.translate("header.employee"));
            lblUserNumber.setText(i18nService.translate("header.employeeNumber"));
            return;
        }

        String nome = funcionario.getNome() != null && !funcionario.getNome().isBlank()
                ? funcionario.getNome().trim()
                : "Funcionário";

        lblUserInitials.setText(criarIniciais(nome));
        lblUserName.setText(nome);
        lblUserNumber.setText(funcionario.getNumeroFuncionario() != null
                ? "Nº " + funcionario.getNumeroFuncionario()
                : i18nService.translate("header.employeeNumber"));
    }

    private String criarIniciais(String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    private void atualizarContadorNotificacoes() {
        if (lblNotificationCount == null || notificacaoService == null) {
            return;
        }

        long count = notificacaoService.contarNotLidas();
        lblNotificationCount.setText(count > 99 ? "99+" : String.valueOf(count));
        lblNotificationCount.setVisible(count > 0);
        lblNotificationCount.setManaged(count > 0);
    }

    private String formatTimeAgo(Instant instant) {
        if (instant == null) {
            return "";
        }
        Duration duration = Duration.between(instant, Instant.now());
        if (duration.toMinutes() < 1) return "agora";
        if (duration.toMinutes() < 60) return duration.toMinutes() + " min";
        if (duration.toHours() < 24) return duration.toHours() + " h";
        return duration.toDays() + " d";
    }

    private String iconFor(TipoEventoNotificacao tipo) {
        if (tipo == null) return "mdi2b-bell-outline";
        return switch (tipo) {
            case STOCK_BAIXO -> "mdi2a-alert-circle-outline";
            case NOVA_ENCOMENDA -> "mdi2c-cart-outline";
            case NOVA_ORDEM_PRODUCAO -> "mdi2f-factory";
            case ORDEM_CONCLUIDA -> "mdi2c-check-circle-outline";
            case EXPEDICAO_REALIZADA -> "mdi2t-truck-delivery";
            case ERRO_PRODUCAO -> "mdi2a-alert-circle-outline";
        };
    }

    private String colorFor(TipoEventoNotificacao tipo) {
        if (tipo == null) return "#64748b";
        return switch (tipo) {
            case STOCK_BAIXO, ERRO_PRODUCAO -> "#f97316";
            case NOVA_ENCOMENDA -> "#3b82f6";
            case NOVA_ORDEM_PRODUCAO -> "#8b5cf6";
            case ORDEM_CONCLUIDA, EXPEDICAO_REALIZADA -> "#22c55e";
        };
    }

    private FontIcon criarIcone(String iconLiteral, String color, int size) {
        FontIcon icon = new FontIcon();
        icon.setIconLiteral(iconLiteral);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(color));
        return icon;
    }
}
