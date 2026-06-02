package com.pelletsfactory.stock_manager.desktop.controllers;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardFxmlTest {

    @Test
    void dashboardBubbleChartUsaEixosNumericos() throws Exception {
        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(getClass().getResourceAsStream("/fxml/views/dashboard-view.fxml"));
        var charts = document.getElementsByTagName("BubbleChart");

        assertThat(charts.getLength()).isEqualTo(1);
        Element chart = (Element) charts.item(0);
        assertThat(chart.getElementsByTagName("CategoryAxis").getLength()).isZero();
        assertThat(chart.getElementsByTagName("NumberAxis").getLength()).isEqualTo(2);
    }
}
