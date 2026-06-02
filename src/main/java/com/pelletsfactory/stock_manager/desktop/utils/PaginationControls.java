package com.pelletsfactory.stock_manager.desktop.utils;

import com.pelletsfactory.stock_manager.desktop.services.I18nService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;

import java.text.MessageFormat;

public final class PaginationControls {

    private final Runnable reload;
    private final I18nService i18nService;
    private final Label statusLabel = new Label();
    private final HBox pageButtons = new HBox(5);
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>(
            FXCollections.observableArrayList(10, 25, 50, 100)
    );

    private int currentPage;
    private int pageSize;
    private int totalPages;
    private boolean attached;

    public PaginationControls(int pageSize, Runnable reload, I18nService i18nService) {
        this.pageSize = pageSize;
        this.reload = reload;
        this.i18nService = i18nService;
        statusLabel.getStyleClass().add("text-muted");
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.setOnAction(e -> {
            this.pageSize = pageSizeSelect.getValue();
            this.currentPage = 0;
            reload.run();
        });
    }

    public int pageNumberForService() {
        return currentPage + 1;
    }

    public int pageSize() {
        return pageSize;
    }

    public int currentPage() {
        return currentPage;
    }

    public void resetPage() {
        currentPage = 0;
    }

    public void resetView() {
        attached = false;
        currentPage = 0;
        totalPages = 0;
    }

    public void attachTo(VBox container) {
        if (attached) {
            return;
        }

        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(20, 0, 20, 0));
        nav.setStyle("-fx-border-color: -color-border-muted; -fx-border-width: 1 0 0 0;");

        HBox left = new HBox(statusLabel);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox center = new HBox(10, new Label(i18nService.translate("common.perPage")), pageSizeSelect);
        center.setAlignment(Pos.CENTER);
        HBox.setHgrow(center, Priority.ALWAYS);

        HBox right = new HBox(pageButtons);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(right, Priority.ALWAYS);

        nav.getChildren().addAll(left, center, right);
        container.getChildren().add(nav);
        attached = true;
    }

    public void update(Page<?> page) {
        totalPages = page.getTotalPages();
        updateStatus(page);
        updateButtons();
    }

    private void updateStatus(Page<?> page) {
        if (page.getTotalElements() == 0) {
            statusLabel.setText(i18nService.translate("common.noResults"));
            return;
        }

        long start = (long) page.getNumber() * page.getSize() + 1;
        long end = Math.min(start + page.getNumberOfElements() - 1, page.getTotalElements());
        statusLabel.setText(MessageFormat.format(
                i18nService.translate("common.showingRange"),
                start,
                end,
                page.getTotalElements()
        ));
    }

    private void updateButtons() {
        pageButtons.getChildren().clear();

        Button prev = new Button();
        prev.setGraphic(new FontIcon("mdi2c-chevron-left"));
        prev.setDisable(currentPage == 0);
        prev.setOnAction(e -> {
            currentPage--;
            reload.run();
        });
        pageButtons.getChildren().add(prev);

        for (int i = 0; i < totalPages; i++) {
            if (i < 3 || i > totalPages - 2 || (i >= currentPage - 1 && i <= currentPage + 1)) {
                Button page = new Button(String.valueOf(i + 1));
                page.getStyleClass().add(i == currentPage ? "accent" : "flat");
                int pageIndex = i;
                page.setOnAction(e -> {
                    currentPage = pageIndex;
                    reload.run();
                });
                pageButtons.getChildren().add(page);
            }
        }

        Button next = new Button();
        next.setGraphic(new FontIcon("mdi2c-chevron-right"));
        next.setDisable(currentPage >= totalPages - 1);
        next.setOnAction(e -> {
            currentPage++;
            reload.run();
        });
        pageButtons.getChildren().add(next);
    }
}
