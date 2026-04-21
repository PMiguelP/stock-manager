package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class FormulasController {

    @FXML private TableView<FormulaRow> tblFormulas;
    @FXML private TableColumn<FormulaRow, String> colCode;
    @FXML private TableColumn<FormulaRow, String> colPelletType;
    @FXML private TableColumn<FormulaRow, String> colVersion;
    @FXML private TableColumn<FormulaRow, String> colYield;
    @FXML private TableColumn<FormulaRow, String> colStatus;

    private final ObservableList<FormulaRow> data = FXCollections.observableArrayList(
            new FormulaRow("FRM-001", "Domestic Premium", "v2.1", "93.4%", "Approved"),
            new FormulaRow("FRM-007", "Industrial Blend", "v1.4", "91.8%", "Testing"),
            new FormulaRow("FRM-011", "Standard Heat", "v3.0", "89.6%", "Archived")
    );

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().code()));
        colPelletType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().pelletType()));
        colVersion.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().version()));
        colYield.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().yield()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        tblFormulas.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblFormulas.refresh();
    }

    private record FormulaRow(String code, String pelletType, String version, String yield, String status) {}
}

