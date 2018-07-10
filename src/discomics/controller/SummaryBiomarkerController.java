package discomics.controller;

import discomics.model.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jure on 13.9.2016.
 */
public class SummaryBiomarkerController extends SummaryController {

    private MainController mainController;
    private Stage summaryBiomarkerStage;

    @FXML
    private BarChart<String, Number> allBarChart;
    @FXML
    private BarChart<String, Number> bloodBarChart;
    @FXML
    private BarChart<String, Number> urineBarChart;
    @FXML
    private BarChart<String, Number> salivaBarChart;
    @FXML
    private BarChart<String, Number> customBarChart;

    @FXML
    private Tab allTab;
    @FXML
    private Tab bloodTab;
    @FXML
    private Tab salivaTab;
    @FXML
    private Tab urineTab;
    @FXML
    private Tab customTab;

    @FXML
    private ScrollPane allChartContainer;
    @FXML
    private ScrollPane bloodChartContainer;
    @FXML
    private ScrollPane urineChartContainer;
    @FXML
    private ScrollPane salivaChartContainer;
    @FXML
    private ScrollPane customChartContainer;

    private FileChooser fileChooser = new FileChooser();

    public void init(MainController mainController, Stage summaryBiomakerStage) {
        this.mainController = mainController;
        this.summaryBiomarkerStage = summaryBiomakerStage;

        initialiseChart(allBarChart, allChartContainer, Y_AXIS_TITLE);
        initialiseChart(bloodBarChart, bloodChartContainer, Y_AXIS_TITLE);
        initialiseChart(urineBarChart, urineChartContainer, Y_AXIS_TITLE);
        initialiseChart(salivaBarChart, salivaChartContainer, Y_AXIS_TITLE);
        initialiseChart(customBarChart, customChartContainer, Y_AXIS_TITLE);

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Portable Network Graphics (*.png)", "*.png"));
    }


    private void setAllBarChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
            int nrRetrieved = tmObject.getNrRetrievedBiomarker();
            if (nrRetrieved != 0) {
                series.getData().add(new XYChart.Data<>(
                        tmObject.getTextMinableInput().getMainName(), nrRetrieved));
            }
        }
        allBarChart.getData().clear();
        allBarChart.layout();
        series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
        allBarChart.getData().add(series);

        for (XYChart.Data<String, Number> item : allBarChart.getData().get(0).getData()) {
            item.getNode().setOnMouseClicked(event -> {
                mainController.focusMainStage();
                mainController.getMainTablesController().selectBiomarkerArticlesTab();
                mainController.selectBiomarkerAllMenuItem();

                for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
                    if (tmObject.getTextMinableInput().getMainName().equalsIgnoreCase(item.getXValue())) {
                        mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().selectRows(tmObject);
                        break;
                    }
                }
            });
            item.getNode().setOnMouseEntered(event1 -> summaryBiomarkerStage.getScene().setCursor(Cursor.HAND));
            item.getNode().setOnMouseExited(event2 -> summaryBiomarkerStage.getScene().setCursor(Cursor.DEFAULT));
        }
    }


    private void setBloodBarChart() {
        bloodBarChart.getData().clear();
        bloodBarChart.layout();
        bloodBarChart.getData().add(getSeriesForBiomarker(IoModel.getBLOOD()));
        addListenersToChartBars(bloodBarChart, IoModel.getBLOOD());
    }

    private void setUrineBarChart() {
        urineBarChart.getData().clear();
        urineBarChart.layout();
        urineBarChart.getData().add(getSeriesForBiomarker(IoModel.getURINE()));
        addListenersToChartBars(urineBarChart, IoModel.getURINE());
    }

    private void setSalivaBarChart() {
        salivaBarChart.getData().clear();
        salivaBarChart.layout();
        salivaBarChart.getData().add(getSeriesForBiomarker(IoModel.getSALIVA()));
        addListenersToChartBars(salivaBarChart, IoModel.getSALIVA());
    }

    private void setCustomBarChart() {
        customBarChart.getData().clear();
        customBarChart.layout();
        customBarChart.getData().add(getSeriesForBiomarker(IoModel.getCUSTOM()));
        addListenersToChartBars(customBarChart, IoModel.getCUSTOM());
    }

    private XYChart.Series<String, Number> getSeriesForBiomarker(Biomarker biomarker) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
            Set<Article> articles = new HashSet<>();

            for (Article article : tmObject.getArticleCollectableBiom().getArticleCollection()) {
                if (article.getBiomarkersMentioned().contains(biomarker))
                    articles.add(article);
            }
            if (articles.size() != 0) {
                series.getData().add(new XYChart.Data<>(
                        tmObject.getTextMinableInput().getMainName(), articles.size()));
            }
        }
        series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));

        return series;
    }

    private void addListenersToChartBars(BarChart<String, Number> chart, Biomarker biomarker) {
        for (XYChart.Data<String, Number> item : chart.getData().get(0).getData()) {
            item.getNode().setOnMouseClicked(event -> {
                mainController.focusMainStage();
                mainController.getMainTablesController().selectBiomarkerArticlesTab();
                mainController.selectBiomarkerMenuItem(biomarker);

                for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
                    if (tmObject.getTextMinableInput().getMainName().equalsIgnoreCase(item.getXValue())) {
                        mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().selectRows(tmObject);
                        break;
                    }
                }
            });
            item.getNode().setOnMouseEntered(event1 -> summaryBiomarkerStage.getScene().setCursor(Cursor.HAND));
            item.getNode().setOnMouseExited(event2 -> summaryBiomarkerStage.getScene().setCursor(Cursor.DEFAULT));
        }
    }

    void initialiseSummaryView() {
        setAllBarChart();
        setBloodBarChart();
        setSalivaBarChart();
        setUrineBarChart();
    }

    void initialiseCustomSummaryView() {
        setCustomBarChart();
    }

    @FXML
    private void saveChartImageAs() {
        WritableImage image = null;

        if (allTab.isSelected()) {
            image = allBarChart.snapshot(new SnapshotParameters(), null);
        } else if (bloodTab.isSelected()) {
            image = bloodBarChart.snapshot(new SnapshotParameters(), null);
        } else if (salivaTab.isSelected()) {
            image = salivaBarChart.snapshot(new SnapshotParameters(), null);
        } else if (urineTab.isSelected()) {
            image = urineBarChart.snapshot(new SnapshotParameters(), null);
        } else if (customTab.isSelected()) {
            image = customBarChart.snapshot(new SnapshotParameters(), null);
        }

        File selectedFile = fileChooser.showSaveDialog(this.summaryBiomarkerStage);

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
        if (allTab.isSelected()) {
            copyChartToClipboard(allBarChart);
        } else if (salivaTab.isSelected()) {
            copyChartToClipboard(salivaBarChart);
        } else if (urineTab.isSelected()) {
            copyChartToClipboard(urineBarChart);
        } else if (bloodTab.isSelected()) {
            copyChartToClipboard(bloodBarChart);
        } else if (customTab.isSelected()) {
            copyChartToClipboard(customBarChart);
        }
    }

    @Override
    public void showStage(boolean show) {
        if (show) {
            double positionX = mainController.getMainStage().getX() + (mainController.getMainStage().getWidth() - summaryBiomarkerStage.getWidth()) / 2;
            double positionY = mainController.getMainStage().getY() + (mainController.getMainStage().getHeight() - summaryBiomarkerStage.getHeight()) / 2;

            summaryBiomarkerStage.setX(positionX); // centered over MainController
            summaryBiomarkerStage.setY(positionY);

            summaryBiomarkerStage.show();
            summaryBiomarkerStage.requestFocus();
        } else
            summaryBiomarkerStage.hide();
    }
}
