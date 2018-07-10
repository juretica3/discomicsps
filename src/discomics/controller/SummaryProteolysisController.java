package discomics.controller;

import discomics.model.Article;
import discomics.model.ProteaseCount;
import discomics.model.TextMinedObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jure on 11.9.2016.
 */
public class SummaryProteolysisController extends SummaryController {

    private MainController mainController;
    private Stage summaryProteolysisStage;

    @FXML
    private ProteinTableController proteinTableController;
    @FXML
    private ProteaseTableController proteaseTableController;

    @FXML
    private BarChart<String, Number> proteasesChart;
    @FXML
    private BarChart<String, Number> proteinsChart;
    @FXML
    private BarChart<String, Number> allProteasesChart;
    @FXML
    private BarChart<String, Number> allProteinsChart;

    @FXML
    private Tab allProteinsTab;
    @FXML
    private Tab allProteasesTab;
    @FXML
    private Tab proteinsTab;
    @FXML
    private Tab proteasesTab;

    @FXML
    private ScrollPane allProteinsChartContainer;
    @FXML
    private ScrollPane allProteasesChartContainer;
    @FXML
    private ScrollPane proteinsChartContainer;
    @FXML
    private ScrollPane proteasesChartContainer;

    private ObservableList<XYChart.Data<String, Number>> proteasesChartData = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<String, Number>> proteinsChartData = FXCollections.observableArrayList();

    private ObservableList<XYChart.Data<String, Number>> allProteasesChartData = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<String, Number>> allProteinsChartData = FXCollections.observableArrayList();


    private FileChooser fileChooser = new FileChooser();


    public void init(MainController mainController, Stage summaryProteolysisStage) {
        this.mainController = mainController;
        this.summaryProteolysisStage = summaryProteolysisStage;

        // link arraylists to charts
        proteinsChart.getData().add(new XYChart.Series<>());
        proteinsChart.getData().get(0).setData(proteinsChartData);

        proteasesChart.getData().add(new XYChart.Series<>());
        proteasesChart.getData().get(0).setData(proteasesChartData);

        allProteinsChart.getData().add(new XYChart.Series<>());
        allProteinsChart.getData().get(0).setData(allProteinsChartData);

        allProteasesChart.getData().add(new XYChart.Series<>());
        allProteasesChart.getData().get(0).setData(allProteasesChartData);

        // listener that updates the bar chart based on Protein selection in the corresponding table: select Proteins in
        // the table, and update bar chart with ProteaseCount entries
        MyTableListener proteinListener = () -> {
            // clear old data
            proteinsChartData.clear();
            proteinsChart.layout();

            // gather selected Proteins from the protein table, and corresponding Proteases
            List<TextMinedObject> selectedProteins = proteinTableController.getTable().getSelectionModel().getSelectedItems();
            List<ProteaseCount> proteaseCounts = mainController.getModel().getProteaseCountTableEntries(selectedProteins);

            // construct bar chart series from gathered proteases
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (ProteaseCount proteaseCount : proteaseCounts) {
                series.getData().add(new XYChart.Data<>(
                        proteaseCount.getProtease().getMainName(), proteaseCount.getTotalHits()));
            }

            // sort and add new data
            series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
            proteinsChartData.addAll(series.getData());

            // bar chart bar listeners; showStage articles in main window upon click
            for (XYChart.Data<String, Number> item : proteinsChart.getData().get(0).getData()) {
                item.getNode().setOnMousePressed((MouseEvent event) -> {
                    mainController.focusMainStage(); // showStage main stage
                    mainController.getMainTablesController().selectProteolysisArticlesTab(); // showStage proteolysis tab

                    // select corresponding protein object from the proteins table in the main controller based on selection in the proteins table of the summary view
                    mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController()
                            .selectRows(proteinTableController.getTable().getSelectionModel().getSelectedItems());

                    // select corresponding protease object from the proteases table based on the click
                    for (ProteaseCount proteaseCount : proteaseCounts) {
                        if (proteaseCount.getGeneName().equalsIgnoreCase(item.getXValue())) {
                            mainController.getMainTablesController().getGeneCountsTablesController().getProteaseTableController().selectRow(proteaseCount);
                            break;
                        }
                    }
                });
                // change cursor type when mouse enters the clickable bars of the bar chart
                item.getNode().setOnMouseEntered(event -> summaryProteolysisStage.getScene().setCursor(Cursor.HAND));
                item.getNode().setOnMouseExited(event -> summaryProteolysisStage.getScene().setCursor(Cursor.DEFAULT));
            }
        };

        // select Protease from table, and update bar chart with Proteins and number of hits of each protein with respect
        // to that protease
        MyTableListener proteaseListener = () -> {
            // clear old data
            proteasesChartData.clear();
            proteasesChart.layout();

            // gather new data
            ProteaseCount selectedProtease = proteaseTableController.getTable().getSelectionModel().getSelectedItem();
            XYChart.Series<String, Number> series = new XYChart.Series<>();

            // loop through TextMinedProteins
            for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
                int count = 0;
                for (Article article : tmObject.getArticleCollectablePlys().getArticleCollection()) { // loop through articles of TextMinedProtein
                    if (article.getProteasesMentioned().contains(selectedProtease.getProtease())) // if selected protease is mentioned, increment count of papers by 1
                        count++;
                }
                if (count != 0) { // add to data series if the count is not 0
                    series.getData().add(new XYChart.Data<>(
                            tmObject.getTextMinableInput().getMainName(), count));
                }
            }

            // sort and add new data
            series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
            proteasesChartData.addAll(series.getData());

            // add graph column listeners; showStage associated articles in main window on click
            for (XYChart.Data<String, Number> item : proteasesChart.getData().get(0).getData()) { // loop through chart data
                item.getNode().setOnMousePressed((MouseEvent event) -> { // add listener to the bar chart node
                    mainController.focusMainStage(); // showStage main stage
                    mainController.getMainTablesController().selectProteolysisArticlesTab(); // showStage proteolysis tab

                    for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) { // map TextMinedProtein object to gene name displayed in bar chart
                        if (tmObject.getTextMinableInput().getMainName().equalsIgnoreCase(item.getXValue())) {
                            mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().selectRows(tmObject); // select row
                            break;
                        }
                    }
                    // select protease table rows in main view based on protease table selection in summary view
                    mainController.getMainTablesController().getGeneCountsTablesController().getProteaseTableController()
                            .selectRow(proteaseTableController.getTable().getSelectionModel().getSelectedItem());
                });
                // change cursor type when mouse is in the bar of the chart
                item.getNode().setOnMouseEntered(event -> summaryProteolysisStage.getScene().setCursor(Cursor.HAND));
                item.getNode().setOnMouseExited(event -> summaryProteolysisStage.getScene().setCursor(Cursor.DEFAULT));
            }
        };

        // initialise tables
        proteinTableController.init(mainController);
        proteinTableController.setSelectionChangeListener(proteinListener);
        proteinTableController.getTable().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        proteaseTableController.init(mainController);
        proteaseTableController.setSelectionChangeListener(proteaseListener);
        proteaseTableController.getTable().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // initialise charts
        initialiseChart(proteasesChart, proteasesChartContainer, Y_AXIS_TITLE);
        initialiseChart(proteinsChart, proteinsChartContainer, Y_AXIS_TITLE);
        initialiseChart(allProteasesChart, allProteasesChartContainer, Y_AXIS_TITLE);
        initialiseChart(allProteinsChart, allProteinsChartContainer, Y_AXIS_TITLE);

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Portable Network Graphics (*.png)", "*.png"));
    }

    /** Method used to update the bar chart with the proteolysis search total hits for all input genes */
    private void setProteinsChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // delete old data
        allProteinsChartData.clear();
        allProteinsChart.layout();

        // gather new data
        for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
            int totalHits = tmObject.getTotalHitsProteolysis();
            if (totalHits != 0)
                series.getData().add(new XYChart.Data<>(tmObject.getTextMinableInput().getMainName(), totalHits));
        }

        // sort new data and add
        series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
        allProteinsChartData.addAll(series.getData());

        // add graph column listeners; showStage associated articles in main window on click
        for (XYChart.Data<String, Number> item : allProteinsChart.getData().get(0).getData()) {
            item.getNode().setOnMousePressed((MouseEvent event) -> {
                mainController.focusMainStage();
                mainController.getMainTablesController().selectProteolysisArticlesTab();

                for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) {
                    if (tmObject.getTextMinableInput().getMainName().equalsIgnoreCase(item.getXValue())) {
                        mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().selectRows(tmObject);
                        break;
                    }
                }
            });
            item.getNode().setOnMouseEntered(event -> summaryProteolysisStage.getScene().setCursor(Cursor.HAND));
            item.getNode().setOnMouseExited(event -> summaryProteolysisStage.getScene().setCursor(Cursor.DEFAULT));
        }
    }

    /** Method used to update the bar chart with the proteolysis search total hits for all proteases */
    private void setProteasesChart() {
        // delete old data
        allProteasesChartData.clear();
        allProteasesChart.layout();

        // gather new data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<TextMinedObject> allProteins = mainController.getModel().getTextMinedProteins();
        List<ProteaseCount> proteaseCounts = mainController.getModel().getProteaseCountTableEntries(allProteins);

        for (ProteaseCount proteaseCount : proteaseCounts) {
            series.getData().add(new XYChart.Data<>(
                    proteaseCount.getProtease().getMainName(), proteaseCount.getTotalHits()));
        }

        // sort new data and add
        series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
        allProteasesChartData.addAll(series.getData());

        // add histogram column mouse listeners; showStage associated articles in main window on click
        for (XYChart.Data<String, Number> item : allProteasesChart.getData().get(0).getData()) {
            item.getNode().setOnMousePressed((MouseEvent event) -> {
                mainController.focusMainStage();
                mainController.getMainTablesController().selectProteolysisArticlesTab();
                mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().getTable().getSelectionModel().selectAll();

                for (ProteaseCount proteaseCount : proteaseCounts) {
                    if (proteaseCount.getGeneName().equalsIgnoreCase(item.getXValue())) {
                        mainController.getMainTablesController().getGeneCountsTablesController().getProteaseTableController().selectRow(proteaseCount);
                        break;
                    }
                }
            });
            item.getNode().setOnMouseEntered(event -> summaryProteolysisStage.getScene().setCursor(Cursor.HAND));
            item.getNode().setOnMouseExited(event -> summaryProteolysisStage.getScene().setCursor(Cursor.DEFAULT));
        }
    }

    void initialiseSummaryView() {
        setProteinsChart();
        setProteasesChart();

        // initialise table with no zero proteolysis articles entries
        List<TextMinedObject> nonZeroHitTmProt = mainController.getModel().getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getNrRetrievedProteolysis() > 0)
                .collect(Collectors.toList());
        proteinTableController.initialiseTableWithEntries(nonZeroHitTmProt);

        proteinTableController.showProteolysisColumn();
        proteaseTableController.initialiseTableAllEntries();
    }

    @FXML
    private void saveChartImageAs() {
        WritableImage image = null;

        // find selected tab
        if (allProteinsTab.isSelected()) {
            image = allProteinsChart.snapshot(new SnapshotParameters(), null);
        } else if (allProteasesTab.isSelected()) {
            image = allProteasesChart.snapshot(new SnapshotParameters(), null);
        } else if (proteasesTab.isSelected()) {
            image = proteasesChart.snapshot(new SnapshotParameters(), null);
        } else if (proteinsTab.isSelected()) {
            image = proteinsChart.snapshot(new SnapshotParameters(), null);
        }

        // showStage save file dialog
        File selectedFile = fileChooser.showSaveDialog(this.summaryProteolysisStage);

        // save the image
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
        if (proteinsTab.isSelected()) {
            copyChartToClipboard(proteinsChart);
        } else if (proteasesTab.isSelected()) {
            copyChartToClipboard(proteasesChart);
        } else if (allProteinsTab.isSelected()) {
            copyChartToClipboard(allProteinsChart);
        } else if (allProteasesTab.isSelected()) {
            copyChartToClipboard(allProteasesChart);
        }
    }

    public void showStage(boolean show) {
        if (show) {
            double positionX = mainController.getMainStage().getX() + (mainController.getMainStage().getWidth() - summaryProteolysisStage.getWidth()) / 2;
            double positionY = mainController.getMainStage().getY() + (mainController.getMainStage().getHeight() - summaryProteolysisStage.getHeight()) / 2;

            summaryProteolysisStage.setX(positionX); // centered over MainController
            summaryProteolysisStage.setY(positionY);

            summaryProteolysisStage.show();
            summaryProteolysisStage.requestFocus();
        } else
            summaryProteolysisStage.hide();
    }
}
