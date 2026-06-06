package com.pelletsfactory.stock_manager.desktop.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.ResponderTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketMensagemDTO;
import com.pelletsfactory.stock_manager.common.dto.response.TicketSimpleDTO;
import com.pelletsfactory.stock_manager.common.enums.AutorMensagemTicket;
import com.pelletsfactory.stock_manager.common.enums.EstadoTicket;
import com.pelletsfactory.stock_manager.common.services.TicketService;
import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import com.pelletsfactory.stock_manager.desktop.services.SupportTicketSelectionService;
import com.pelletsfactory.stock_manager.desktop.services.ToastService;
import com.pelletsfactory.stock_manager.desktop.services.ViewReloadable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SupportController implements ViewReloadable {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final String CARD_STYLE =
            "-fx-padding: 13; -fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; "
                    + "-fx-border-radius: 6; -fx-background-radius: 6;";
    private static final String SELECTED_CARD_STYLE =
            "-fx-padding: 13; -fx-background-color: rgba(59, 130, 246, 0.10); -fx-border-color: -color-accent-emphasis; "
                    + "-fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;";

    private final TicketService ticketService;
    private final SupportTicketSelectionService selectionService;
    private final ToastService toastService;
    private final I18nService i18n;

    @FXML private VBox ticketsContainer;
    @FXML private VBox messagesContainer;
    @FXML private Label lblTicketTitle;
    @FXML private Label lblTicketState;
    @FXML private Label lblEmptyConversation;
    @FXML private Label lblClient;
    @FXML private Label lblTracking;
    @FXML private Label lblOrderDate;
    @FXML private Label lblOrderTotal;
    @FXML private Label lblResponsible;
    @FXML private Label lblOrderState;
    @FXML private Label lblOrderQuantity;
    @FXML private VBox orderItemsContainer;
    @FXML private ComboBox<TicketFilter> cmbFilter;
    @FXML private TextArea txtReply;
    @FXML private Button btnReply;
    @FXML private Button btnAssign;
    @FXML private Button btnResolve;

    private final Timeline refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> refresh()));
    private List<TicketSimpleDTO> tickets = List.of();
    private UUID selectedTicketId;

    public SupportController(TicketService ticketService,
                             SupportTicketSelectionService selectionService,
                             ToastService toastService,
                             I18nService i18n) {
        this.ticketService = ticketService;
        this.selectionService = selectionService;
        this.toastService = toastService;
        this.i18n = i18n;
    }

    @FXML
    public void initialize() {
        cmbFilter.getItems().setAll(TicketFilter.values());
        cmbFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(TicketFilter filter) {
                return filter == null ? "" : i18n.translate(filter.translationKey);
            }

            @Override
            public TicketFilter fromString(String value) {
                return null;
            }
        });
        cmbFilter.setValue(TicketFilter.PENDING);
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        refresh();
    }

    @FXML
    private void handleFilterChanged() {
        renderTickets();
    }

    @Override
    public void onViewShown() {
        UUID requestedTicket = selectionService.consume();
        if (requestedTicket != null) {
            selectedTicketId = requestedTicket;
        }
        refresh();
    }

    @FXML
    private void handleReply() {
        try {
            requireSelection();
            ticketService.responderComoFuncionario(selectedTicketId, new ResponderTicketRequestDTO(txtReply.getText()));
            txtReply.clear();
            toastService.showSuccess(i18n.translate("common.success"), i18n.translate("support.messageSent"));
            refresh();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void handleAssign() {
        try {
            requireSelection();
            ticketService.atribuirAoFuncionarioAtual(selectedTicketId);
            toastService.showSuccess(i18n.translate("common.success"), i18n.translate("support.assigned"));
            refresh();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void handleResolve() {
        try {
            requireSelection();
            ticketService.resolverTicket(selectedTicketId);
            toastService.showSuccess(i18n.translate("common.success"), i18n.translate("support.resolved"));
            refresh();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void refresh() {
        try {
            tickets = ticketService.listarInboxComercial();
            if (selectedTicketId != null && tickets.stream().noneMatch(ticket -> ticket.id().equals(selectedTicketId))) {
                selectedTicketId = null;
            }
            renderTickets();
            renderSelectedTicket();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void renderTickets() {
        ticketsContainer.getChildren().clear();
        List<TicketSimpleDTO> visibleTickets = filteredTickets();
        if (visibleTickets.isEmpty()) {
            ticketsContainer.getChildren().add(muted(i18n.translate("support.noTickets")));
            return;
        }
        visibleTickets.forEach(ticket -> ticketsContainer.getChildren().add(ticketCard(ticket)));
    }

    private VBox ticketCard(TicketSimpleDTO ticket) {
        VBox card = new VBox(5);
        boolean selected = ticket.id().equals(selectedTicketId);
        card.setStyle(selected ? SELECTED_CARD_STYLE : CARD_STYLE);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            selectedTicketId = ticket.id();
            renderTickets();
            renderSelectedTicket();
        });

        HBox title = new HBox(8);
        Label subject = bold(ticket.assunto());
        Label state = ticketStateBadge(ticket.estado());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        title.getChildren().addAll(subject, spacer, state);
        HBox meta = new HBox(8);
        Label tracking = muted(value(ticket.codigoTracking()));
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        Label time = muted(ticket.ultimaMensagemEm() != null ? DATE_TIME.format(ticket.ultimaMensagemEm()) : "");
        meta.getChildren().addAll(tracking, metaSpacer, time);
        card.getChildren().addAll(title, muted(ticket.clienteNome()), meta);
        if (ticket.estado() == EstadoTicket.AGUARDA_EQUIPE) {
            card.getChildren().add(pendingIndicator());
        }
        return card;
    }

    private void renderSelectedTicket() {
        boolean hasSelection = selectedTicketId != null;
        lblEmptyConversation.setVisible(!hasSelection);
        lblEmptyConversation.setManaged(!hasSelection);
        txtReply.setDisable(!hasSelection);
        btnReply.setDisable(!hasSelection);
        btnAssign.setDisable(!hasSelection);
        btnResolve.setDisable(!hasSelection);
        messagesContainer.getChildren().clear();
        if (!hasSelection) {
            clearSummary();
            return;
        }

        TicketDetailsDTO ticket = ticketService.obterConversaFuncionario(selectedTicketId);
        lblTicketTitle.setText(ticket.assunto());
        lblTicketState.setText(ticket.estado().getDisplayName());
        lblClient.setText(ticket.clienteNome());
        lblTracking.setText(value(ticket.codigoTracking()));
        lblOrderDate.setText(ticket.dataEncomenda() != null ? ticket.dataEncomenda().toString() : "-");
        lblOrderState.setText(ticket.estadoEncomenda() != null ? ticket.estadoEncomenda().getDisplayName() : "-");
        lblOrderQuantity.setText(kg(ticket.quantidadeTotalKg()));
        lblOrderTotal.setText((ticket.moedaSimbolo() == null ? "" : ticket.moedaSimbolo())
                + String.format("%.2f", ticket.totalFinal() == null ? 0.0 : ticket.totalFinal()));
        lblResponsible.setText(ticket.responsavelNome() != null ? ticket.responsavelNome() : i18n.translate("support.unassigned"));
        orderItemsContainer.getChildren().clear();
        ticket.itens().forEach(item -> orderItemsContainer.getChildren().add(
                muted(item.tipoPelletNome() + " · " + kg(item.quantidadeKg()))
        ));
        if (ticket.itens().isEmpty()) {
            orderItemsContainer.getChildren().add(muted("-"));
        }
        btnResolve.setDisable(ticket.estado() == EstadoTicket.RESOLVIDO);
        btnReply.setDisable(ticket.estado() == EstadoTicket.RESOLVIDO);
        txtReply.setDisable(ticket.estado() == EstadoTicket.RESOLVIDO);
        ticket.mensagens().forEach(message -> messagesContainer.getChildren().add(messageBubble(message)));
    }

    private VBox messageBubble(TicketMensagemDTO message) {
        boolean staff = message.autorTipo() == AutorMensagemTicket.FUNCIONARIO;
        VBox bubble = new VBox(5);
        bubble.setMaxWidth(560);
        bubble.setPadding(new Insets(10, 12, 10, 12));
        bubble.setStyle(staff
                ? "-fx-background-color: rgba(59, 130, 246, 0.16); -fx-background-radius: 7; -fx-border-color: rgba(59, 130, 246, 0.55); -fx-border-radius: 7;"
                : "-fx-background-color: -color-bg-default; -fx-background-radius: 7; -fx-border-color: -color-border-default; -fx-border-radius: 7;");
        bubble.setAlignment(staff ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        Label meta = muted(message.autorNome() + " · " + (message.createdAt() != null ? DATE_TIME.format(message.createdAt()) : ""));
        Label body = new Label(message.mensagem());
        body.setWrapText(true);
        bubble.getChildren().addAll(meta, body);
        HBox row = new HBox(bubble);
        row.setAlignment(staff ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        HBox.setHgrow(bubble, Priority.ALWAYS);
        VBox wrapper = new VBox(row);
        return wrapper;
    }

    private void clearSummary() {
        lblTicketTitle.setText(i18n.translate("support.selectConversation"));
        lblTicketState.setText("-");
        lblClient.setText("-");
        lblTracking.setText("-");
        lblOrderDate.setText("-");
        lblOrderState.setText("-");
        lblOrderQuantity.setText("-");
        lblOrderTotal.setText("-");
        lblResponsible.setText("-");
        orderItemsContainer.getChildren().clear();
    }

    private void requireSelection() {
        if (selectedTicketId == null) {
            throw new IllegalStateException(i18n.translate("support.selectConversation"));
        }
    }

    private void showError(RuntimeException exception) {
        toastService.showError(i18n.translate("common.error"), exception.getMessage());
    }

    private Label bold(String text) {
        Label label = new Label(value(text));
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private Label muted(String text) {
        Label label = new Label(value(text));
        label.setStyle("-fx-text-fill: -color-fg-muted;");
        return label;
    }

    private Label ticketStateBadge(EstadoTicket estado) {
        String color = switch (estado) {
            case AGUARDA_EQUIPE -> "#f59e0b";
            case AGUARDA_CLIENTE -> "#3b82f6";
            case RESOLVIDO -> "#22c55e";
        };
        Label label = new Label(estado.getDisplayName());
        label.setStyle("-fx-padding: 3 7 3 7; -fx-background-color: " + color + "22; "
                + "-fx-border-color: " + color + "88; -fx-border-radius: 10; -fx-background-radius: 10; "
                + "-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        return label;
    }

    private Label pendingIndicator() {
        Label label = new Label(i18n.translate("support.pendingReply"));
        label.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-font-weight: bold;");
        return label;
    }

    private List<TicketSimpleDTO> filteredTickets() {
        TicketFilter filter = cmbFilter.getValue() != null ? cmbFilter.getValue() : TicketFilter.PENDING;
        return tickets.stream()
                .filter(filter::matches)
                .collect(Collectors.toList());
    }

    private String kg(Double quantity) {
        return String.format("%.2f kg", quantity == null ? 0.0 : quantity);
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private enum TicketFilter {
        PENDING("support.filterPending") {
            @Override boolean matches(TicketSimpleDTO ticket) {
                return ticket.estado() == EstadoTicket.AGUARDA_EQUIPE;
            }
        },
        MINE("support.filterMine") {
            @Override boolean matches(TicketSimpleDTO ticket) {
                var logged = com.pelletsfactory.stock_manager.common.entities.SessaoFuncionario.getFuncionarioLogado();
                return logged != null && logged.getId().equals(ticket.responsavelId());
            }
        },
        RESOLVED("support.filterResolved") {
            @Override boolean matches(TicketSimpleDTO ticket) {
                return ticket.estado() == EstadoTicket.RESOLVIDO;
            }
        },
        ALL("support.filterAll") {
            @Override boolean matches(TicketSimpleDTO ticket) {
                return true;
            }
        };

        private final String translationKey;

        TicketFilter(String translationKey) {
            this.translationKey = translationKey;
        }

        abstract boolean matches(TicketSimpleDTO ticket);
    }
}
