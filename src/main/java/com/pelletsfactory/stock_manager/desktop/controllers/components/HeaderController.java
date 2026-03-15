package com.pelletsfactory.stock_manager.desktop.controllers.components;

import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.Breadcrumbs.BreadCrumbItem;
import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class HeaderController {

    @FXML private Breadcrumbs<String> breadcrumbs;
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;

    @FXML
    public void initialize() {
        configurarBreadcrumbs();
    }

    private void configurarBreadcrumbs() {
        var items = List.of("Home", "Administração");
        BreadCrumbItem<String> root = Breadcrumbs.buildTreeModel(items.toArray(String[]::new));

        breadcrumbs.setCrumbFactory(crumb -> {
            var btn = new Button(crumb.getValue());
            btn.getStyleClass().add(Styles.FLAT);
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> breadcrumbs.setSelectedCrumb(crumb));
            return btn;
        });

        breadcrumbs.setDividerFactory(item -> {
            if (item == null) {
                return new FontIcon(MaterialDesignH.HOME);
            }
            return !item.isLast()
                    ? new FontIcon(MaterialDesignC.CHEVRON_RIGHT)
                    : null;
        });

        BreadCrumbItem<String> lastItem = root;
        while (lastItem.getChildren() != null && !lastItem.getChildren().isEmpty()) {
            lastItem = (BreadCrumbItem<String>) lastItem.getChildren().get(0);
        }

        breadcrumbs.setSelectedCrumb(lastItem);
    }

    //E suposto atualizar o titulo ainda nao esta completo
    public void setTitulo(String titulo, String subtitulo) {
        if (lblTitulo != null) lblTitulo.setText(titulo);
        if (lblSubtitulo != null) lblSubtitulo.setText(subtitulo);
    }

    //Atualizar os breadcrum automaticamente tambem aidna nao esta compelto
    public void setBreadcrumbs(List<String> items) {
        BreadCrumbItem<String> root = Breadcrumbs.buildTreeModel(items.toArray(String[]::new));

        BreadCrumbItem<String> lastItem = root;
        while (lastItem.getChildren() != null && !lastItem.getChildren().isEmpty()) {
            lastItem = (BreadCrumbItem<String>) lastItem.getChildren().get(0);
        }

        breadcrumbs.setSelectedCrumb(lastItem);
    }


    //Mudar os temas que vem naticos do atlanta fx
    @FXML private void changeToPrimerLight() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    @FXML private void changeToPrimerDark() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
    }

    @FXML private void changeToNordLight() {
        Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
    }

    @FXML private void changeToNordDark() {
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
    }

    @FXML private void changeToCupertinoLight() {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
    }

    @FXML private void changeToCupertinoDark() {
        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
    }

    @FXML private void changeToDracula() {
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
    }
}
