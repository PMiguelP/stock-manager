package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class BatchesController {

    @FXML private TableView<BatchRow> tblBatches;
    @FXML private TableColumn<BatchRow, String> colBatch;
    @FXML private TableColumn<BatchRow, String> colFormula;
    @FXML private TableColumn<BatchRow, String> colStartedAt;
    @FXML private TableColumn<BatchRow, String> colQuantity;
    @FXML private TableColumn<BatchRow, String> colStatus;

    private final ObservableList<BatchRow> data = FXCollections.observableArrayList(
            new BatchRow("BAT-2026-017", "FRM-001", "2026-04-20 07:00", "22.1 t", "Closed"),
            new BatchRow("BAT-2026-018", "FRM-007", "2026-04-20 15:00", "16.8 t", "Running"),
            new BatchRow("BAT-2026-019", "FRM-001", "2026-04-21 00:00", "8.2 t", "Paused")
    );

    @FXML
    public void initialize() {
        colBatch.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().batch()));
        colFormula.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().formula()));
        colStartedAt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().startedAt()));
        colQuantity.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().quantity()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        tblBatches.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblBatches.refresh();
    }

    private record BatchRow(String batch, String formula, String startedAt, String quantity, String status) {}
}

