package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class StockController {

    @FXML private TableView<StockRow> tblStock;
    @FXML private TableColumn<StockRow, String> colItem;
    @FXML private TableColumn<StockRow, String> colLocation;
    @FXML private TableColumn<StockRow, String> colOnHand;
    @FXML private TableColumn<StockRow, String> colReserved;
    @FXML private TableColumn<StockRow, String> colReorder;

    private final ObservableList<StockRow> data = FXCollections.observableArrayList(
            new StockRow("Domestic Premium", "Warehouse A", "74.2 t", "10.0 t", "20.0 t"),
            new StockRow("Industrial Blend", "Warehouse B", "31.8 t", "5.5 t", "15.0 t"),
            new StockRow("Pine Sawdust", "Silo 2", "42,000 kg", "6,500 kg", "25,000 kg")
    );

    @FXML
    public void initialize() {
        colItem.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().item()));
        colLocation.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().location()));
        colOnHand.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().onHand()));
        colReserved.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reserved()));
        colReorder.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reorder()));
        tblStock.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblStock.refresh();
    }

    private record StockRow(String item, String location, String onHand, String reserved, String reorder) {}
}

