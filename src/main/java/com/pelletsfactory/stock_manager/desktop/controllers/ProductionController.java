package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class ProductionController {

    @FXML private TableView<ProductionRow> tblProduction;
    @FXML private TableColumn<ProductionRow, String> colBatch;
    @FXML private TableColumn<ProductionRow, String> colLine;
    @FXML private TableColumn<ProductionRow, String> colShift;
    @FXML private TableColumn<ProductionRow, String> colStatus;
    @FXML private TableColumn<ProductionRow, String> colOutput;

    private final ObservableList<ProductionRow> data = FXCollections.observableArrayList(
            new ProductionRow("BAT-2026-017", "Line A", "Morning", "Running", "18.2 t"),
            new ProductionRow("BAT-2026-018", "Line B", "Evening", "Setup", "0.0 t"),
            new ProductionRow("BAT-2026-019", "Line A", "Night", "Completed", "21.7 t")
    );

    @FXML
    public void initialize() {
        colBatch.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().batch()));
        colLine.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().line()));
        colShift.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shift()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        colOutput.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().output()));
        tblProduction.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblProduction.refresh();
    }

    private record ProductionRow(String batch, String line, String shift, String status, String output) {}
}

