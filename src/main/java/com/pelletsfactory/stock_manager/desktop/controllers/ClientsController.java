package com.pelletsfactory.stock_manager.desktop.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;

@Component
public class ClientsController {

    @FXML private TableView<ClientRow> tblClients;
    @FXML private TableColumn<ClientRow, String> colCode;
    @FXML private TableColumn<ClientRow, String> colName;
    @FXML private TableColumn<ClientRow, String> colCountry;
    @FXML private TableColumn<ClientRow, String> colSegment;
    @FXML private TableColumn<ClientRow, String> colStatus;

    private final ObservableList<ClientRow> data = FXCollections.observableArrayList(
            new ClientRow("CLI-001", "Iberia Power", "PT", "Industrial", "Active"),
            new ClientRow("CLI-015", "Green Heat SA", "ES", "Distributor", "Active"),
            new ClientRow("CLI-022", "Nordic Home", "SE", "Retail", "Prospect")
    );

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().code()));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colCountry.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().country()));
        colSegment.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().segment()));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        tblClients.setItems(data);
    }

    @FXML
    private void handleRefresh() {
        tblClients.refresh();
    }

    private record ClientRow(String code, String name, String country, String segment, String status) {}
}

