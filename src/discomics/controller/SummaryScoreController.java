package discomics.controller;

import discomics.application.AlphanumComparator;
import discomics.model.TextMinedObject;
import discomics.model.TextMinedProtein;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Jure on 26/03/2017.
 */
public class SummaryScoreController extends SummaryController {
    private MainController mainController;
    private Stage summaryScoreStage;

    private MyTableModel<ScoreTableEntry> scoreTableModel;
    @FXML
    private TableView<ScoreTableEntry> scoreTable;
    @FXML
    private TableColumn<ScoreTableEntry, String> geneNameCol;
    @FXML
    private TableColumn<ScoreTableEntry, String> nrDrugsCol;
    @FXML
    private TableColumn<ScoreTableEntry, String> neighborhoodScoreCol;
    @FXML
    private TableColumn<ScoreTableEntry, String> tmScoreCol;
    @FXML
    private TableColumn<ScoreTableEntry, String> overallScoreCol;

    @FXML
    private ScrollPane nrDrugsChartContainer;
    @FXML
    private ScrollPane neighborhoodScoreChartContainer;
    @FXML
    private ScrollPane tmScoreChartContainer;
    @FXML
    private ScrollPane overallScoreChartContainer;

    @FXML
    private TabPane chartsTabPane;

    @FXML
    private BarChart<String, Number> nrDrugsBarChart;
    @FXML
    private BarChart<String, Number> neighborhoodScoreBarChart;
    @FXML
    private BarChart<String, Number> tmScoreBarChart;
    @FXML
    private BarChart<String, Number> overallScoreBarChart;
    @FXML
    private ScatterChart<Number, Number> scatterChart;

    @FXML
    private RadioButton proteolysisTmScoreRadioButton;
    @FXML
    private RadioButton biomarkerTmScoreRadioButton;
    @FXML
    private RadioButton customTmScoreRadioButton;

    private ArrayList<String> xyAxisChoiceBoxChoices = new ArrayList<>();
    @FXML
    private ChoiceBox<String> xAxisChoiceBox;
    @FXML
    private ChoiceBox<String> yAxisChoiceBox;

    private ObservableList<XYChart.Data<String, Number>> nrDrugsBarChartData = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<String, Number>> neighborhoodScoreBarChartData = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<String, Number>> tmScoreBarChartData = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<String, Number>> overallScoreBarChartData = FXCollections.observableArrayList();
    private ObservableList<XYChart.Data<Number, Number>> scatterChartData = FXCollections.observableArrayList();

    private BooleanBinding proteolysisEnabledBinding;
    private BooleanBinding biomarkerEnabledBinding;
    private BooleanBinding customEnabledBinding;

    void init(MainController mainController, Stage stage) {
        this.mainController = mainController;
        this.summaryScoreStage = stage;

        this.scoreTableModel = new MyTableModel<>(scoreTable);

        // column comparators alphanumeric
        this.geneNameCol.setComparator(new AlphanumComparator());
        this.nrDrugsCol.setComparator(new AlphanumComparator());
        this.neighborhoodScoreCol.setComparator(new AlphanumComparator());
        this.tmScoreCol.setComparator(new AlphanumComparator());
        this.overallScoreCol.setComparator(new AlphanumComparator());

        // assign table cell value factories for each column
        this.geneNameCol.setCellValueFactory(new PropertyValueFactory<>("geneName"));
        this.nrDrugsCol.setCellValueFactory(new PropertyValueFactory<>("nrDrugInteractionsString"));
        this.neighborhoodScoreCol.setCellValueFactory(new PropertyValueFactory<>("neighborhoodScoreString"));

        this.tmScoreCol.setCellValueFactory(param -> {
            if (!customEnabledBinding.get()) { // no tm score can be computed, since all rely on custom text-mining result
                return new SimpleStringProperty("0");
            }

            if (!proteolysisTmScoreRadioButton.isDisabled() && proteolysisTmScoreRadioButton.isSelected()) { // P/C tm score selected
                return new SimpleStringProperty(param.getValue().getTmScoreProteolysisString());
            } else if (!biomarkerTmScoreRadioButton.isDisabled() && biomarkerTmScoreRadioButton.isSelected()) { // B/C tm score selected
                return new SimpleStringProperty(param.getValue().getTmScoreBiomarkerString());
            } else if (!customTmScoreRadioButton.isDisabled() && customTmScoreRadioButton.isSelected()) { // 1/C tm score selected
                return new SimpleStringProperty(param.getValue().getTmScoreCustomString());
            } else { // nothing selected
                return new SimpleStringProperty("0");
            }
        });

        this.overallScoreCol.setCellValueFactory(param -> {
            if (!customEnabledBinding.get()) { // overall score cannot be computed, since it relies on custom text-mining result
                return new SimpleStringProperty("0");
            }

            if (!proteolysisTmScoreRadioButton.isDisabled() && proteolysisTmScoreRadioButton.isSelected()) { // P/C tm score selected
                return new SimpleStringProperty(param.getValue().getOverallScoreProteolysisString());
            } else if (!biomarkerTmScoreRadioButton.isDisabled() && biomarkerTmScoreRadioButton.isSelected()) { // B/C tm score selected
                return new SimpleStringProperty(param.getValue().getOverallScoreBiomarkerString());
            } else if (!customTmScoreRadioButton.isDisabled() && customTmScoreRadioButton.isSelected()) { // 1/C tm score selected
                return new SimpleStringProperty(param.getValue().getOverallScoreCustomString());
            } else // nothing selected
                return new SimpleStringProperty("0");
        });

        // bind data arrays to respective bar charts
        this.nrDrugsBarChart.getData().add(new XYChart.Series<>());
        this.nrDrugsBarChart.getData().get(0).setData(this.nrDrugsBarChartData);

        this.neighborhoodScoreBarChart.getData().add(new XYChart.Series<>());
        this.neighborhoodScoreBarChart.getData().get(0).setData(this.neighborhoodScoreBarChartData);

        this.tmScoreBarChart.getData().add(new XYChart.Series<>());
        this.tmScoreBarChart.getData().get(0).setData(this.tmScoreBarChartData);

        this.overallScoreBarChart.getData().add(new XYChart.Series<>());
        this.overallScoreBarChart.getData().get(0).setData(this.overallScoreBarChartData);

        this.scatterChart.getData().add(new XYChart.Series<>());
        this.scatterChart.getData().get(0).setData(this.scatterChartData);
        this.scatterChart.setLegendVisible(false);

        // initialise xAxis and yAxis choice boxes in XY scatter chart tab
        xyAxisChoiceBoxChoices.add("Number of drugs"); // 0
        xyAxisChoiceBoxChoices.add("Neighborhood Score"); // 1
        xyAxisChoiceBoxChoices.add("Proteolysis Articles (P)"); // 2
        xyAxisChoiceBoxChoices.add("TM Score (P/C)"); // 3
        xyAxisChoiceBoxChoices.add("Overall Score (P/C)"); // 4
        xyAxisChoiceBoxChoices.add("Biomarker Articles (B)"); // 5
        xyAxisChoiceBoxChoices.add("TM Score (B/C)"); // 6
        xyAxisChoiceBoxChoices.add("Overall Score (B/C)"); // 7
        xyAxisChoiceBoxChoices.add("Custom Articles (C)"); //8
        xyAxisChoiceBoxChoices.add("TM Score (1/C)"); // 9
        xyAxisChoiceBoxChoices.add("Overall Score (1/C)"); // 10

        xAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices);
        yAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices);

        this.proteolysisEnabledBinding = mainController.getMainTablesController().proteolysisCustomModelBindingProperty();
        this.biomarkerEnabledBinding = mainController.getMainTablesController().biomarkerCustomModelBindingProperty();
        this.customEnabledBinding = mainController.getMainTablesController().customModelBindingProperty().not();

        // bind tm score radio button disable property to whether proteolysis/biomarker/custom models are defined
        this.proteolysisTmScoreRadioButton.disableProperty().bind(proteolysisEnabledBinding.not());
        this.biomarkerTmScoreRadioButton.disableProperty().bind(biomarkerEnabledBinding.not());
        this.customTmScoreRadioButton.disableProperty().bind(customEnabledBinding.not());

        // initialise TM score radio buttons
        ToggleGroup tmScoreToggleGroup = new ToggleGroup();
        this.proteolysisTmScoreRadioButton.setToggleGroup(tmScoreToggleGroup);
        this.biomarkerTmScoreRadioButton.setToggleGroup(tmScoreToggleGroup);
        this.customTmScoreRadioButton.setToggleGroup(tmScoreToggleGroup);

        // initialise chart: remove animations, add zooming and scrolling feature
        initialiseChart(nrDrugsBarChart, nrDrugsChartContainer, xyAxisChoiceBoxChoices.get(0));
        initialiseChart(neighborhoodScoreBarChart, neighborhoodScoreChartContainer, xyAxisChoiceBoxChoices.get(1));
        initialiseChart(tmScoreBarChart, tmScoreChartContainer, "TM Score");
        initialiseChart(overallScoreBarChart, overallScoreChartContainer, "Overall Score");

        // update XY scatter chart listeners based on
        this.xAxisChoiceBox.setOnAction(event -> updateXyScatterChart());
        this.yAxisChoiceBox.setOnAction(event -> updateXyScatterChart());

        // every time new tab is selected, update underlying chart
        this.chartsTabPane.getSelectionModel().selectedItemProperty().addListener((observable, newValue, oldValue) -> {
            if (newValue.equals(chartsTabPane.getTabs().get(0))) { // nr drugs tab
                updateDataNrDrugsBarChart();
            } else if (newValue.equals(chartsTabPane.getTabs().get(1))) { // centrality tab
                updateDataCentralityScoreBarChart();
            } else if (newValue.equals(chartsTabPane.getTabs().get(2))) { // tm score tab
                updateDataTmScoreBarChart();
            } else if (newValue.equals(chartsTabPane.getTabs().get(3))) { // overall score tab
                updateDataOverallScoreBarChart();
            } else if (newValue.equals(chartsTabPane.getTabs().get(4))) { // xy scatter tab
                updateXyScatterChart();
            } else { // update everything in case something goes not as expected
                updateAllCharts();
            }
        });

        // P/C radio button choice listeners; refresh score table, update charts
        this.proteolysisTmScoreRadioButton.setOnAction(event -> {
            scoreTable.refresh();
            scoreTable.sort();
            updateAllCharts();
        });
        this.biomarkerTmScoreRadioButton.setOnAction(event -> {
            scoreTable.refresh();
            scoreTable.sort();
            updateAllCharts();
        });
        this.customTmScoreRadioButton.setOnAction(event -> {
            scoreTable.refresh();
            scoreTable.sort();
            updateAllCharts();
        });

        this.proteolysisTmScoreRadioButton.disableProperty().addListener((observable, oldValue, newValue) -> reconfigureXyChartChoiceBoxLabels());
        this.biomarkerTmScoreRadioButton.disableProperty().addListener((observable, oldValue, newValue) -> reconfigureXyChartChoiceBoxLabels());
        this.customTmScoreRadioButton.disableProperty().addListener((observable, oldValue, newValue) -> reconfigureXyChartChoiceBoxLabels());
    }

    private void reconfigureXyChartChoiceBoxLabels() {
        xAxisChoiceBox.getItems().clear();
        xAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(0), xyAxisChoiceBoxChoices.get(1));
        yAxisChoiceBox.getItems().clear();
        yAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(0), xyAxisChoiceBoxChoices.get(1));

        if (!mainController.getMainTablesController().proteolysisModelBindingProperty().get()) {
            xAxisChoiceBox.getItems().add(xyAxisChoiceBoxChoices.get(2));
            yAxisChoiceBox.getItems().add(xyAxisChoiceBoxChoices.get(2));
        }

        if (proteolysisEnabledBinding.get()) {
            xAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(3), xyAxisChoiceBoxChoices.get(4));
            yAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(3), xyAxisChoiceBoxChoices.get(4));
        }

        if (!mainController.getMainTablesController().biomarkerModelBindingProperty().get()) {
            xAxisChoiceBox.getItems().add(xyAxisChoiceBoxChoices.get(5));
            yAxisChoiceBox.getItems().add(xyAxisChoiceBoxChoices.get(5));
        }

        if (biomarkerEnabledBinding.get()) {
            xAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(6), xyAxisChoiceBoxChoices.get(7));
            yAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(6), xyAxisChoiceBoxChoices.get(7));
        }

        if (!mainController.getMainTablesController().customModelBindingProperty().get()) {
            xAxisChoiceBox.getItems().add(xyAxisChoiceBoxChoices.get(8));
            yAxisChoiceBox.getItems().add(xyAxisChoiceBoxChoices.get(8));
        }

        if (customEnabledBinding.get()) {
            xAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(9), xyAxisChoiceBoxChoices.get(10));
            yAxisChoiceBox.getItems().addAll(xyAxisChoiceBoxChoices.get(9), xyAxisChoiceBoxChoices.get(10));
        }
    }

    private void updateDataTmScoreBarChart() {
        tmScoreBarChartData.clear(); // clear old data and reset layout
        tmScoreBarChart.layout();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ScoreTableEntry entry : scoreTableModel.getData()) {
            if (!proteolysisTmScoreRadioButton.isDisabled() && proteolysisTmScoreRadioButton.isSelected()) // P/C score selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getTmScoreProteolysis()));
            else if (!biomarkerTmScoreRadioButton.isDisabled() && biomarkerTmScoreRadioButton.isSelected()) // B/C score selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getTmScoreBiomarker()));
            else if (!customTmScoreRadioButton.isDisabled() && customTmScoreRadioButton.isSelected()) // 1/C score selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getTmScoreCustom()));
            else {// nothing selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), 0d));
            }
        }

        series.getData().sort(Comparator.comparingDouble(dou -> -dou.getYValue().doubleValue()));
        tmScoreBarChartData.addAll(series.getData());
    }

    private void updateDataOverallScoreBarChart() {
        overallScoreBarChartData.clear();
        overallScoreBarChart.layout();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ScoreTableEntry entry : scoreTableModel.getData()) {
            if (!proteolysisTmScoreRadioButton.isDisabled() && proteolysisTmScoreRadioButton.isSelected()) // P/C score selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getOverallScoreProteolysis()));
            else if (!biomarkerTmScoreRadioButton.isDisabled() && biomarkerTmScoreRadioButton.isSelected()) // B/C score selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getOverallScoreBiomarker()));
            else if (!customTmScoreRadioButton.isDisabled() && customTmScoreRadioButton.isSelected()) // 1/C score selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getOverallScoreCustom()));
            else {// nothing selected
                series.getData().add(new XYChart.Data<>(entry.getGeneName(), 0d));
            }
        }

        series.getData().sort(Comparator.comparingDouble(dou -> -dou.getYValue().doubleValue()));
        overallScoreBarChartData.addAll(series.getData());
    }

    private void updateDataNrDrugsBarChart() {
        nrDrugsBarChartData.clear();
        nrDrugsBarChart.layout();

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (ScoreTableEntry entry : scoreTableModel.getData()) {
            series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getNrDrugInteractions()));
        }

        series.getData().sort(Comparator.comparingInt(in -> -in.getYValue().intValue()));
        nrDrugsBarChartData.addAll(series.getData());
    }

    private void updateDataCentralityScoreBarChart() {
        neighborhoodScoreBarChartData.clear();
        neighborhoodScoreBarChart.layout();

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (ScoreTableEntry entry : scoreTableModel.getData()) {
            series.getData().add(new XYChart.Data<>(entry.getGeneName(), entry.getNeighborhoodScore()));
        }

        series.getData().sort(Comparator.comparingDouble(dou -> -dou.getYValue().doubleValue()));
        neighborhoodScoreBarChartData.addAll(series.getData());
    }

    private void updateXyScatterChart() {
        String xAxisSelection = xAxisChoiceBox.getSelectionModel().getSelectedItem();
        String yAxisSelection = yAxisChoiceBox.getSelectionModel().getSelectedItem();

        if (xAxisSelection == null || yAxisSelection == null) { // selection in both required for scatter chart
            return;
        }

        //System.out.println(xAxisSelection);
        //System.out.println(yAxisSelection);

        scatterChartData.clear();
        scatterChart.layout();

        scatterChart.getXAxis().setLabel(xAxisSelection);
        scatterChart.getYAxis().setLabel(yAxisSelection);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        // add data points to series depending on selection in X and Y choice boxes
        for (ScoreTableEntry entry : scoreTableModel.getData()) {
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>();

            // set x axis value based on check box selection
            if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(0))) {
                dataPoint.setXValue(entry.getNrDrugInteractions());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(1))) {
                dataPoint.setXValue(entry.getNeighborhoodScore());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(2))) {
                dataPoint.setXValue(entry.getTotalProteolysisArticles());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(3))) {
                dataPoint.setXValue(entry.getTmScoreProteolysis());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(4))) {
                dataPoint.setXValue(entry.getOverallScoreProteolysis());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(5))) {
                dataPoint.setXValue(entry.getTotalBiomarkerArticles());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(6))) {
                dataPoint.setXValue(entry.getTmScoreBiomarker());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(7))) {
                dataPoint.setXValue(entry.getOverallScoreBiomarker());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(8))) {
                dataPoint.setXValue(entry.getTotalCustomArticles());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(9))) {
                dataPoint.setXValue(entry.getTmScoreCustom());
            } else if (xAxisSelection.equals(xyAxisChoiceBoxChoices.get(10))) {
                dataPoint.setXValue(entry.getOverallScoreCustom());
            } else {
                dataPoint.setXValue(0d);
            }

            // set y axis value based on check box selection
            if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(0))) {
                dataPoint.setYValue(entry.getNrDrugInteractions());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(1))) {
                dataPoint.setYValue(entry.getNeighborhoodScore());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(2))) {
                dataPoint.setYValue(entry.getTotalProteolysisArticles());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(3))) {
                dataPoint.setYValue(entry.getTmScoreProteolysis());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(4))) {
                dataPoint.setYValue(entry.getOverallScoreProteolysis());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(5))) {
                dataPoint.setYValue(entry.getTotalBiomarkerArticles());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(6))) {
                dataPoint.setYValue(entry.getTmScoreBiomarker());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(7))) {
                dataPoint.setYValue(entry.getOverallScoreBiomarker());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(8))) {
                dataPoint.setYValue(entry.getTotalCustomArticles());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(9))) {
                dataPoint.setYValue(entry.getTmScoreCustom());
            } else if (yAxisSelection.equals(xyAxisChoiceBoxChoices.get(10))) {
                dataPoint.setYValue(entry.getOverallScoreCustom());
            } else {
                dataPoint.setYValue(0d);
            }

            dataPoint.setExtraValue(entry.getGeneName());
            series.getData().add(dataPoint);
            //System.out.println(dataPoint.getXValue() + ", " + dataPoint.getYValue());
        }

        series.getData().sort(new XyScatterDataComparator()); // sort data; important for next step

        // set the nodes to display tooltip with names on click. If more nodes have same X and Y values,
        // display both names one after the other in the same tooltip
        for (int i = 0; i < series.getData().size(); i++) {
            StringBuilder sb = new StringBuilder();

            int j;
            for (j = i; j < series.getData().size(); j++) { // loop through next entries checking for equality and build label if more nodes are equal in both their x and y values
                if (new XyScatterDataComparator().compare(series.getData().get(i), series.getData().get(j)) == 0) {
                    if (i == j)
                        sb.append(series.getData().get(j).getExtraValue());
                    else
                        sb.append(", ").append(series.getData().get(j).getExtraValue());
                } else
                    break;
            }
            for (int k = i; k < j; k++) { // set the labels to all nodes identified to be equal
                series.getData().get(i).setNode(new TooltipClickNode(sb.toString()));
            }
            i = j - 1; // as it enters next iteration of outer for loop, will be added 1
        }

        scatterChartData.addAll(series.getData());
    }

    /**
     * a node which displays a value on hover, but is otherwise empty, for XY scatter chart
     */
    class TooltipClickNode extends StackPane {
        TooltipClickNode(String value) {
            setPrefSize(15, 15);

            final Tooltip tooltip = createDataThresholdLabel(value);

            setOnMousePressed(mouseEvent -> tooltip.show(TooltipClickNode.this, mouseEvent.getScreenX(), mouseEvent.getScreenY()));
            setOnMouseReleased(mouseEvent -> tooltip.hide());
        }

        private Tooltip createDataThresholdLabel(String value) {
            final Tooltip tooltip = new Tooltip(value);
            tooltip.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

            return tooltip;
        }
    }

    @FXML
    private void copyTableDataAction() {
        StringBuilder sb = new StringBuilder();
        sb.append("Gene name").append("\t")
                .append("Nr Drugs").append("\t")
                .append("Neighborhood Score").append("\t");

        if (proteolysisEnabledBinding.getValue()) {
            sb.append("Text-Mining Score (P/C)").append("\t");
            sb.append("Overall Score (P/C)").append("\t");
        }
        if (biomarkerEnabledBinding.getValue()) {
            sb.append("Text-Mining Score (B/C)").append("\t");
            sb.append("Overall Score (B/C)").append("\t");
        }
        if (customEnabledBinding.getValue()) {
            sb.append("Text-Mining Score (1/C)").append("\t");
            sb.append("Overall Score (1/C)").append("\t");
        }

        sb.append("\n");

        for (ScoreTableEntry tableEntry : this.scoreTableModel.getData()) {
            sb.append(tableEntry.getGeneName()).append("\t")
                    .append(tableEntry.getNrDrugInteractions()).append("\t")
                    .append(tableEntry.getNeighborhoodScore()).append("\t");

            if (proteolysisEnabledBinding.getValue()) {
                sb.append(tableEntry.getTmScoreProteolysis()).append("\t");
                sb.append(tableEntry.getOverallScoreProteolysis()).append("\t");
            }
            if (biomarkerEnabledBinding.getValue()) {
                sb.append(tableEntry.getTmScoreBiomarker()).append("\t");
                sb.append(tableEntry.getOverallScoreBiomarker()).append("\t");
            }
            if (customEnabledBinding.getValue()) {
                sb.append(tableEntry.getTmScoreCustom()).append("\t");
                sb.append(tableEntry.getOverallScoreCustom()).append("\t");
            }

            sb.append("\n");
        }

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }

    public void showStage(boolean show) {
        if (show) {
            // update all bar charts before showing the stage
            updateAllCharts();

            summaryScoreStage.show();
            double positionX = mainController.getMainStage().getX() + (mainController.getMainStage().getWidth() - summaryScoreStage.getWidth()) / 2;
            double positionY = mainController.getMainStage().getY() + (mainController.getMainStage().getHeight() - summaryScoreStage.getHeight()) / 2;

            summaryScoreStage.setX(positionX); // centered over MainController
            summaryScoreStage.setY(positionY);
        } else
            summaryScoreStage.hide();
    }

    void setScoreTableData() {
        // define the ScoreTableEntry list to populate TableView
        List<ScoreTableEntry> scoreTableEntries = new ArrayList<>();

        for (TextMinedObject tmObject : mainController.getModel().getTextMinedProteins()) { // loop through TextMinedProteins in IoModel and construct Table entries
            ScoreTableEntry scoreTableEntry = new ScoreTableEntry((TextMinedProtein) tmObject);
            scoreTableEntries.add(scoreTableEntry);
        }
        this.scoreTableModel.clearAndUpdateTable(scoreTableEntries); // update TableView with entries
        chartsTabPane.getSelectionModel().select(chartsTabPane.getTabs().get(0)); // select drug tab to update first graph (tab listeners set in init() method)
    }

    private void updateAllCharts() {
        updateDataNrDrugsBarChart();
        updateDataCentralityScoreBarChart();
        updateDataTmScoreBarChart();
        updateDataOverallScoreBarChart();
        updateXyScatterChart();
    }

    public class ScoreTableEntry {
        private String geneName;
        private Double nrDrugInteractions;
        private Double neighborhoodScore;

        private Double totalProteolysisArticles;
        private Double totalBiomarkerArticles;
        private Double totalCustomArticles;

        private Double tmScoreProteolysis;
        private Double tmScoreBiomarker;
        private Double tmScoreCustom;

        private Double overallScoreProteolysis;
        private Double overallScoreBiomarker;
        private Double overallScoreCustom;

        private ScoreTableEntry(TextMinedProtein tmProtein) {

            this.geneName = tmProtein.getMainName();
            this.nrDrugInteractions = (double) tmProtein.getNrDrugInteractions();
            this.neighborhoodScore = tmProtein.getNetworkScore();

            this.totalProteolysisArticles = Math.log10(tmProtein.getTotalHitsProteolysis() + 1);
            this.totalBiomarkerArticles = Math.log10(tmProtein.getTotalHitsBiomarker() + 1);
            this.totalCustomArticles = Math.log10(tmProtein.getTotalHitsCustom() + 1);

            this.tmScoreProteolysis = calculateTmScoreProteolysis(tmProtein);
            this.tmScoreBiomarker = calculateTmScoreBiomarker(tmProtein);
            this.tmScoreCustom = calculateTmScoreCustom(tmProtein);

            this.overallScoreProteolysis = calculateOverallScoreProteolysis(tmProtein);
            this.overallScoreBiomarker = calculateOverallScoreBiomarker(tmProtein);
            this.overallScoreCustom = calculateOverallScoreCustom(tmProtein);
        }

        // getters need to be public if displayed in table

        public String getGeneName() {
            return this.geneName;
        }

        // TABLE GETTERS
        public String getNrDrugInteractionsString() {
            DecimalFormat decimalFormat = new DecimalFormat("0");
            decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN); // unneccessary
            return decimalFormat.format(this.nrDrugInteractions); // format number not to include too many decimals
        }

        public String getNeighborhoodScoreString() {
            if (this.neighborhoodScore == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.neighborhoodScore); // format number not to include too many decimals
        }

        String getTmScoreProteolysisString() {
            if (this.tmScoreProteolysis == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.tmScoreProteolysis); //format the number
        }

        String getTmScoreBiomarkerString() {
            if (this.tmScoreBiomarker == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.tmScoreBiomarker); //format the number
        }

        String getTmScoreCustomString() {
            if (this.tmScoreCustom == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.tmScoreCustom); //format the number
        }

        String getOverallScoreProteolysisString() {
            if (this.overallScoreProteolysis == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.overallScoreProteolysis);
        }

        String getOverallScoreBiomarkerString() {
            if (this.overallScoreBiomarker == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.overallScoreBiomarker);
        }

        String getOverallScoreCustomString() {
            if (this.overallScoreCustom == null)
                return "x";
            return new DecimalFormat("##0.##").format(this.overallScoreCustom);
        }

        public String getTotalBiomarkerArticlesString() {
            DecimalFormat decimalFormat = new DecimalFormat("0");
            decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN); // unneccessary
            return decimalFormat.format(this.totalBiomarkerArticles); // format number not to include too many decimals
        }

        public String getTotalProteolysisArticlesString() {
            DecimalFormat decimalFormat = new DecimalFormat("0");
            decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN); // unneccessary
            return decimalFormat.format(this.totalProteolysisArticles); // format number not to include too many decimals
        }

        public String getTotalCustomArticlesString() {
            DecimalFormat decimalFormat = new DecimalFormat("0");
            decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN); // unneccessary
            return decimalFormat.format(this.totalCustomArticles); // format number not to include too many decimals
        }

        // COMPUTE SCORE METHODS
        private Double calculateTmScoreProteolysis(TextMinedProtein textMinedProtein) {
            if (proteolysisEnabledBinding.get())
                return (Math.log10(textMinedProtein.getTotalHitsProteolysis() + 1) / Math.log10(2))
                        / (Math.log10(textMinedProtein.getTotalHitsCustom() + 1.5) / Math.log10(2));
            else
                return 0d;
        }

        private Double calculateTmScoreBiomarker(TextMinedProtein textMinedProtein) {
            if (biomarkerEnabledBinding.get())
                return (Math.log10(textMinedProtein.getTotalHitsBiomarker() + 1) / Math.log10(2))
                        / (Math.log10(textMinedProtein.getTotalHitsCustom() + 1.5) / Math.log10(2));
            else
                return 0d;
        }

        private Double calculateTmScoreCustom(TextMinedProtein textMinedProtein) {
            if (customEnabledBinding.get())
                return 1 / (Math.log10(textMinedProtein.getTotalHitsCustom() + 1.5) / Math.log10(2)); // log2(C+1.5)
            else
                return 0d;
        }

        private Double calculateOverallScoreProteolysis(TextMinedProtein textMinedProtein) {
            if (proteolysisEnabledBinding.get()) {
                double drugInteractionLog = Math.log10(textMinedProtein.getNrDrugInteractions() + 1) / Math.log10(4); // log4
                double tmScoreProteolysis = Math.log10(textMinedProtein.getTotalHitsProteolysis() + 1) / Math.log10(2); // log2
                double tmScoreCustom = Math.log10(textMinedProtein.getTotalHitsCustom() + 1.5) / Math.log10(2); // log2
                double networkScore = textMinedProtein.getNetworkScore();

                return (drugInteractionLog + networkScore + tmScoreProteolysis) / tmScoreCustom;
            } else
                return 0d;
        }

        private Double calculateOverallScoreBiomarker(TextMinedProtein textMinedProtein) {
            if (biomarkerEnabledBinding.get()) {
                double drugInteractionLog = Math.log10(textMinedProtein.getNrDrugInteractions() + 1) / Math.log10(4); // log4
                double tmScoreBiomarker = Math.log10(textMinedProtein.getTotalHitsBiomarker() + 1) / Math.log10(2); // log2
                double tmScoreCustom = Math.log10(textMinedProtein.getTotalHitsCustom() + 1.5) / Math.log10(2); // log2
                double networkScore = textMinedProtein.getNetworkScore();

                return (drugInteractionLog + networkScore + tmScoreBiomarker) / tmScoreCustom;
            } else
                return 0d;
        }

        private Double calculateOverallScoreCustom(TextMinedProtein textMinedProtein) {
            if (customEnabledBinding.get()) {
                double drugInteractionLog = Math.log10(textMinedProtein.getNrDrugInteractions() + 1) / Math.log10(4); // log4
                double tmScoreCustom = Math.log10(textMinedProtein.getTotalHitsCustom() + 1.5) / Math.log10(2); // log2
                double networkScore = textMinedProtein.getNetworkScore();

                return (drugInteractionLog + networkScore) / tmScoreCustom;
            } else  // nothing is selected
                return 0d;
        }

        // CALCULATION GETTERS
        private Double getNrDrugInteractions() {
            return nrDrugInteractions;
        }

        private Double getNeighborhoodScore() {
            return neighborhoodScore;
        }

        private Double getTmScoreProteolysis() {
            return tmScoreProteolysis;
        }

        private Double getTmScoreBiomarker() {
            return tmScoreBiomarker;
        }

        private Double getTmScoreCustom() {
            return tmScoreCustom;
        }

        private Double getOverallScoreProteolysis() {
            return overallScoreProteolysis;
        }

        private Double getOverallScoreBiomarker() {
            return overallScoreBiomarker;
        }

        private Double getOverallScoreCustom() {
            return overallScoreCustom;
        }

        Double getTotalProteolysisArticles() {
            return totalProteolysisArticles;
        }

        Double getTotalBiomarkerArticles() {
            return totalBiomarkerArticles;
        }

        Double getTotalCustomArticles() {
            return totalCustomArticles;
        }
    }

    @FXML
    void copyChartToClipboardTabulated() {
        // not defined for this Score Controller
    }

    private class XyScatterDataComparator implements Comparator<XYChart.Data<Number, Number>> {
        @Override
        public int compare(XYChart.Data<Number, Number> o1, XYChart.Data<Number, Number> o2) {
            if ((double) o1.getXValue() < (double) o2.getXValue()) return -1; // according to x value first
            if ((double) o1.getXValue() > (double) o2.getXValue()) return 1;
            if ((double) o1.getYValue() < (double) o2.getYValue()) return -1; // if x values equal, according to y value
            if ((double) o1.getYValue() > (double) o2.getYValue()) return 1;
            return 0; // if both x and y values equal
        }
    }
}
