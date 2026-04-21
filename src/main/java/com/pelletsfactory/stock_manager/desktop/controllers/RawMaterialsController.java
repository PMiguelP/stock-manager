package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class RawMaterialsController {

    @FXML private TableView<RawMaterialRow> tblRawMaterials;
    @FXML private TableColumn<RawMaterialRow, String> colMaterial;
    @FXML private TableColumn<RawMaterialRow, String> colCategory;
    @FXML private TableColumn<RawMaterialRow, String> colUnit;
    @FXML private TableColumn<RawMaterialRow, String> colStock;
    @FXML private TableColumn<RawMaterialRow, String> colReorder;

    private final ObservableList<RawMaterialRow> data = FXCollections.observableArrayList(
            new RawMaterialRow("Pine Sawdust", "Wood", "kg", "42,000", "25,000"),
            new RawMaterialRow("Olive Husk", "Biomass", "kg", "18,500", "15,000"),
            new RawMaterialRow("Corn Starch", "Binder", "kg", "2,300", "2,000")
    );

    @FXML
    public void initialize() {
        colMaterial.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().material()));
        colCategory.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().category()));
        colUnit.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().unit()));
        colStock.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stock()));
        colReorder.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reorderPoint()));
        tblRawMaterials.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblRawMaterials.refresh();
    }

    private record RawMaterialRow(String material, String category, String unit, String stock, String reorderPoint) {}
}

