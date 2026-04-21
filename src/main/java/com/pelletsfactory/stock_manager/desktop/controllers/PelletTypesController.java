package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class PelletTypesController {

    @FXML private TableView<PelletTypeRow> tblPelletTypes;
    @FXML private TableColumn<PelletTypeRow, String> colCode;
    @FXML private TableColumn<PelletTypeRow, String> colName;
    @FXML private TableColumn<PelletTypeRow, String> colDiameter;
    @FXML private TableColumn<PelletTypeRow, String> colMoisture;
    @FXML private TableColumn<PelletTypeRow, String> colStatus;

    private final ObservableList<PelletTypeRow> data = FXCollections.observableArrayList(
            new PelletTypeRow("PLT-6A", "Domestic Premium", "6 mm", "6.5%", "Active"),
            new PelletTypeRow("PLT-8I", "Industrial Blend", "8 mm", "8.2%", "Active"),
            new PelletTypeRow("PLT-6S", "Standard Heat", "6 mm", "9.0%", "Deprecated")
    );

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().code()));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colDiameter.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().diameter()));
        colMoisture.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().moisture()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        tblPelletTypes.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblPelletTypes.refresh();
    }

    private record PelletTypeRow(String code, String name, String diameter, String moisture, String status) {}
}

