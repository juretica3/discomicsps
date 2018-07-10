package discomics.controller;

import discomics.model.TextMinedObject;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

/**
 * Created by Jure on 21/11/2016.
 */
public class SummaryCustomController extends SummaryController {

    private MainController mainController;
    private Stage summaryCustomStage;

    @FXML
    private BarChart<String, Number> summaryChart;
    @FXML
    private ScrollPane summaryChartContainer;

    private FileChooser fileChooser = new FileChooser();

    public void init(MainController mainController, Stage summaryCustomStage) {
        this.mainController = mainController;
        this.summaryCustomStage = summaryCustomStage;

        initialiseChart(summaryChart, summaryChartContainer, Y_AXIS_TITLE);

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Portable Network Graphics (*.png)", "*.png"));
    }

    private void setChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
            series.getData().add(new XYChart.Data<>(
                    tmObject.getTextMinableInput().getMainName(), tmObject.getNrRetrievedCustom()
            ));
        }

        summaryChart.getData().clear();
        series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
        summaryChart.getData().add(series);

        for (XYChart.Data<String, Number> item : summaryChart.getData().get(0).getData()) {
            item.getNode().setOnMouseClicked(event -> {
                mainController.focusMainStage();
                mainController.getMainTablesController().selectCustomArticlesTab();

                for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
                    if (tmObject.getTextMinableInput().getMainName().equalsIgnoreCase(item.getXValue())) {
                        mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().selectRows(tmObject);
                        break;
                    }
                }
            });
            item.getNode().setOnMouseEntered(event1 -> summaryChart.getScene().setCursor(Cursor.HAND));
            item.getNode().setOnMouseExited(event2 -> summaryChart.getScene().setCursor(Cursor.DEFAULT));
        }
    }

    void initialiseSummaryView() {
        setChart();
    }

    @FXML
    private void saveChartImageAs() {
        WritableImage image = summaryChart.snapshot(new SnapshotParameters(), null);

        File selectedFile = fileChooser.showSaveDialog(this.summaryCustomStage);

        if (selectedFile == null || image == null)
            return;

        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        try {
            ImageIO.write(bImage, "png", selectedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void copyChartToClipboardTabulated() {
        copyChartToClipboard(summaryChart);
    }

    @Override
    public void showStage(boolean show) {
        if (show) {
            double positionX = mainController.getMainStage().getX() + (mainController.getMainStage().getWidth() - summaryCustomStage.getWidth()) / 2;
            double positionY = mainController.getMainStage().getY() + (mainController.getMainStage().getHeight() - summaryCustomStage.getHeight()) / 2;

            summaryCustomStage.setX(positionX); // centered over MainController
            summaryCustomStage.setY(positionY);
            
            summaryCustomStage.show();
            summaryCustomStage.requestFocus();
        } else
            summaryCustomStage.hide();
    }
}
