package discomics.controller;

import discomics.application.AlphanumComparator;
import discomics.model.IoModel;
import discomics.model.TextMinedObject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by Jure on 9.9.2016.
 */
public class ProteinTableController implements TableControllable<TextMinedObject> {
    private MainController mainController;

    private MyTableListener tableSelectionChangeListener;
    // if null is passed, protein and the other table are not linked
    private MyTableModel<TextMinedObject> proteinTableModel;

    @FXML
    private TableView<TextMinedObject> proteinTable;
    @FXML
    private TableColumn<TextMinedObject, String> geneNameProteinTableColumn;
    @FXML
    private MenuItem nonStringentSearchMenuButton;

    private TableColumn<TextMinedObject, Integer> totalHitsProteolysis = new TableColumn<>("Total Hits");
    private TableColumn<TextMinedObject, Integer> totalHitsBiomarker = new TableColumn<>("Total Hits");
    private TableColumn<TextMinedObject, Integer> totalHitsCustom = new TableColumn<>("Total Hits");
    private TableColumn<TextMinedObject, Integer> nrRetrievedBiomarker = new TableColumn<>("Retrieved");
    private TableColumn<TextMinedObject, Integer> nrRetrievedProteolysis = new TableColumn<>("Retrieved");
    private TableColumn<TextMinedObject, Integer> nrRetrievedCustom = new TableColumn<>("Retrieved");
    private TableColumn<TextMinedObject, Integer> drugInteractionsColumn = new TableColumn<>("Nr Drugs");

    public void init(MainController mainController) {

        this.mainController = mainController;
        this.proteinTableModel = new MyTableModel<>(proteinTable);

        totalHitsProteolysis.setCellValueFactory(new PropertyValueFactory<>("totalHitsProteolysis"));

        totalHitsBiomarker.setCellValueFactory(param -> {
            if (mainController.isBiomarkerAllMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getTotalHitsBiomarker());
            } else if (mainController.isBiomarkerBloodMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getTotalHitsBiomarker(IoModel.getBLOOD()));
            } else if (mainController.isBiomarkerSalivaMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getTotalHitsBiomarker(IoModel.getSALIVA()));
            } else if (mainController.isBiomarkerUrineMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getTotalHitsBiomarker(IoModel.getURINE()));
            } else if (mainController.isBiomarkerCustomMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getTotalHitsBiomarker(IoModel.getCUSTOM()));
            } else {
                return new SimpleObjectProperty<>(0);
            }
        });

        totalHitsCustom.setCellValueFactory(new PropertyValueFactory<>("totalHitsCustom"));

        geneNameProteinTableColumn.setCellValueFactory(
                new PropertyValueFactory<>("mainName"));

        nrRetrievedBiomarker.setCellValueFactory(param -> {
            if (mainController.isBiomarkerAllMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getNrRetrievedBiomarker());
            } else if (mainController.isBiomarkerBloodMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getNrRetrievedBiomarker(IoModel.getBLOOD()));
            } else if (mainController.isBiomarkerSalivaMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getNrRetrievedBiomarker(IoModel.getSALIVA()));
            } else if (mainController.isBiomarkerUrineMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getNrRetrievedBiomarker(IoModel.getURINE()));
            } else if (mainController.isBiomarkerCustomMenuItemSelected()) {
                return new SimpleObjectProperty<>(param.getValue().getNrRetrievedBiomarker(IoModel.getCUSTOM()));
            } else {
                return new SimpleObjectProperty<>(0);
            }
        });

        nrRetrievedProteolysis.setCellValueFactory(
                new PropertyValueFactory<>("nrRetrievedProteolysis"));
        nrRetrievedCustom.setCellValueFactory(
                new PropertyValueFactory<>("nrRetrievedCustom"));
        drugInteractionsColumn.setCellValueFactory(
                new PropertyValueFactory<>("nrDrugInteractions"));

        geneNameProteinTableColumn.setComparator(new AlphanumComparator());
        proteinTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // resize table columns to be equal in size when number of columns changes
        proteinTable.getColumns().addListener((ListChangeListener<TableColumn<TextMinedObject, ?>>) change -> {
            double width = proteinTable.getWidth() / proteinTable.getColumns().size();
            proteinTable.getColumns().forEach(col -> col.setPrefWidth(width));
        });

        // sets keyboard navigation listeners on first column of table
        setListeners();
        setKeyNavigationListeners(this.proteinTable, 0);
    }

    private void setListeners() {
        // table selection change tableSelectionChangeListener
        proteinTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TextMinedObject>) c -> {
            if (tableSelectionChangeListener != null)
                tableSelectionChangeListener.fire();
        });

        // enable stringent search button when at least one protein selected
        proteinTable.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (proteinTable.getSelectionModel().getSelectedCells().isEmpty()) {
                nonStringentSearchMenuButton.setDisable(true);
            } else {
                nonStringentSearchMenuButton.setDisable(false);
            }
        });
    }

    void showProteolysisColumn() {
        removeAllCountColumns();
        addCountColumn(this.totalHitsProteolysis);
        addCountColumn(this.nrRetrievedProteolysis);
    }

    void showBiomarkerColumn() {
        removeAllCountColumns();
        addCountColumn(this.totalHitsBiomarker);
        addCountColumn(this.nrRetrievedBiomarker);
    }

    void showCustomColumn() {
        removeAllCountColumns();
        addCountColumn(this.totalHitsCustom);
        addCountColumn(this.nrRetrievedCustom);
    }

    void showDrugColumn() {
        removeAllCountColumns();
        addCountColumn(this.drugInteractionsColumn);
    }

    private void addCountColumn(TableColumn<TextMinedObject, Integer> column) {
        proteinTable.getColumns().add(column);
        proteinTable.getSortOrder().clear();
        proteinTable.getSortOrder().add(column);
        proteinTable.getSortOrder().getFirst().setSortType(TableColumn.SortType.DESCENDING);
    }

    private void removeAllCountColumns() {
        proteinTable.getColumns().remove(this.totalHitsProteolysis);
        proteinTable.getColumns().remove(this.totalHitsBiomarker);
        proteinTable.getColumns().remove(this.totalHitsCustom);

        proteinTable.getColumns().remove(this.nrRetrievedCustom);
        proteinTable.getColumns().remove(this.nrRetrievedProteolysis);
        proteinTable.getColumns().remove(this.nrRetrievedBiomarker);
        proteinTable.getColumns().remove(this.drugInteractionsColumn);
    }

    void selectRows(TextMinedObject tmObjects) {
        proteinTable.getSelectionModel().clearSelection();
        proteinTable.getSelectionModel().select(tmObjects);
    }

    void selectRows(List<TextMinedObject> tmObjects) {
        proteinTable.getSelectionModel().clearSelection();
        for (TextMinedObject tmObject : tmObjects) {
            proteinTable.getSelectionModel().select(tmObject);
        }
    }

    // CONTEXT MENU LISTENERS

    @FXML
    void copyAllRows() {
        TableControllable.super.copyAllRows(this.proteinTable);
    }

    @FXML
    private void copySelectedRows() {
        TableControllable.super.copySelectedRows(this.proteinTable);
    }

    @FXML
    private void clearSelection() {
        proteinTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void nonStringentSearchMenuAction() {
        mainController.nonStringentTextMiningSearch();
    }

    // OTHER METHODS
    void disableNonStringentSearch(boolean val) {
        nonStringentSearchMenuButton.setDisable(val);
    }

    void initialiseTableWithEntries(List<TextMinedObject> tmObjects) {
        if (tmObjects != null) {
            proteinTableModel.clearAndUpdateTable(tmObjects);
        }
    }

    public TableView<TextMinedObject> getTable() {
        return proteinTable;
    }

    MyTableModel<TextMinedObject> getTableModel() {
        return proteinTableModel;
    }

    void setSelectionChangeListener(MyTableListener listener) {
        this.tableSelectionChangeListener = listener;
    }

}
