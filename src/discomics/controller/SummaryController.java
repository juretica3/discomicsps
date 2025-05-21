package discomics.controller;

import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Created by Jure on 04/12/2016.
 */
abstract class SummaryController {
    String Y_AXIS_TITLE = "Articles Retrieved";

    void initialiseChart(BarChart<String, Number> chart, ScrollPane container, String yAxisTitle) {
        chart.getYAxis().setLabel(yAxisTitle);
        chart.getYAxis().setTickLabelGap(1);

        chart.setAnimated(false); // remove when axis animation bug is fixed

        chart.setLegendVisible(false);

        final double SCALE_DELTA = 1.1;
        chart.setOnScroll(event -> {
            if (event.isMetaDown() || event.isControlDown()) {
                event.consume();

                if (event.getDeltaY() == 0) {
                    return;
                }

                double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
                double newSize = chart.getWidth() * scaleFactor;
                double containerSize = container.getWidth();
                if (newSize > containerSize) {
                    container.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                    chart.setPrefWidth(newSize);
                    chart.setMaxWidth(newSize);
                    chart.setMinWidth(newSize);
                } else {
                    container.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    chart.setPrefWidth(containerSize);
                    chart.setMaxWidth(containerSize);
                    chart.setMinWidth(containerSize);
                }
            }
        });
        
        container.widthProperty().addListener((observable, oldValue, newValue) -> {
            if(chart.getPrefWidth() < newValue.doubleValue()) {
                chart.setPrefWidth(container.getPrefWidth());
                chart.setMaxWidth(container.getMaxWidth());
                chart.setMinWidth(container.getMinWidth());
            }
        });
        
        container.heightProperty().addListener((observable, oldValue, newValue) -> {
            if(chart.getPrefHeight() < newValue.doubleValue()) {
                chart.setPrefHeight(container.getPrefHeight());
                chart.setMaxHeight(container.getMaxHeight());
                chart.setMinHeight(container.getMinHeight());
            }
        });

    }

    void copyChartToClipboard(BarChart<String, Number> chart) {
        ObservableList<XYChart.Data<String, Number>> data = chart.getData().get(0).getData();

        StringBuilder sb = new StringBuilder();
        sb.append("Name\t").append("Nr Art\n");
        for (XYChart.Data<String, Number> dataPoint: data) {
            sb.append(dataPoint.getXValue()).append("\t").append(dataPoint.getYValue()).append("\n");
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }

    abstract void showStage(boolean show);
}
