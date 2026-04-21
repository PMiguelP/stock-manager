package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class SuppliersController {

    @FXML private TableView<SupplierRow> tblSuppliers;
    @FXML private TableColumn<SupplierRow, String> colCode;
    @FXML private TableColumn<SupplierRow, String> colName;
    @FXML private TableColumn<SupplierRow, String> colContact;
    @FXML private TableColumn<SupplierRow, String> colLeadTime;
    @FXML private TableColumn<SupplierRow, String> colStatus;

    private final ObservableList<SupplierRow> data = FXCollections.observableArrayList(
            new SupplierRow("SUP-001", "Foresta Biomass", "+351 910 000 111", "4 days", "Active"),
            new SupplierRow("SUP-014", "Agro Timber", "+351 910 000 245", "6 days", "Active"),
            new SupplierRow("SUP-027", "Eco Residues", "+351 910 000 390", "8 days", "On Hold")
    );

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().code()));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colContact.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().contact()));
        colLeadTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().leadTime()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        tblSuppliers.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblSuppliers.refresh();
    }

    private record SupplierRow(String code, String name, String contact, String leadTime, String status) {}
}

