package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class OrdersController {

    @FXML private TableView<OrderRow> tblOrders;
    @FXML private TableColumn<OrderRow, String> colOrderNo;
    @FXML private TableColumn<OrderRow, String> colClient;
    @FXML private TableColumn<OrderRow, String> colStatus;
    @FXML private TableColumn<OrderRow, String> colDelivery;
    @FXML private TableColumn<OrderRow, String> colQuantity;

    private final ObservableList<OrderRow> data = FXCollections.observableArrayList(
            new OrderRow("ORD-2026-104", "Iberia Power", "Confirmed", "2026-04-23", "42.0 t"),
            new OrderRow("ORD-2026-105", "Green Heat SA", "Picking", "2026-04-24", "31.5 t"),
            new OrderRow("ORD-2026-106", "BioFuel Norte", "Pending", "2026-04-26", "25.0 t")
    );

    @FXML
    public void initialize() {
        colOrderNo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().orderNo()));
        colClient.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().client()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        colDelivery.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().deliveryDate()));
        colQuantity.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().quantity()));
        tblOrders.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblOrders.refresh();
    }

    private record OrderRow(String orderNo, String client, String status, String deliveryDate, String quantity) {}
}

