package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrdersController {

    @FXML private TableView<PurchaseOrderRow> tblPurchaseOrders;
    @FXML private TableColumn<PurchaseOrderRow, String> colPoNumber;
    @FXML private TableColumn<PurchaseOrderRow, String> colSupplier;
    @FXML private TableColumn<PurchaseOrderRow, String> colEta;
    @FXML private TableColumn<PurchaseOrderRow, String> colStatus;
    @FXML private TableColumn<PurchaseOrderRow, String> colTotal;

    private final ObservableList<PurchaseOrderRow> data = FXCollections.observableArrayList(
            new PurchaseOrderRow("PO-2026-071", "Foresta Biomass", "2026-04-24", "In Transit", "EUR 9,250"),
            new PurchaseOrderRow("PO-2026-072", "Agro Timber", "2026-04-26", "Approved", "EUR 6,480"),
            new PurchaseOrderRow("PO-2026-073", "Eco Residues", "2026-04-29", "Draft", "EUR 4,990")
    );

    @FXML
    public void initialize() {
        colPoNumber.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().number()));
        colSupplier.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().supplier()));
        colEta.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().eta()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        colTotal.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().total()));
        tblPurchaseOrders.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblPurchaseOrders.refresh();
    }

    private record PurchaseOrderRow(String number, String supplier, String eta, String status, String total) {}
}

