package discomics.controller;

import discomics.application.AlphanumComparator;
import discomics.model.ProteaseCount;
import discomics.model.TextMinedObject;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Created by Jure on 9.9.2016.
 */
public class ProteaseTableController implements TableControllable<ProteaseCount> {
    private MainController mainController;
    private MyTableListener tableSelectionChangeListener;

    private MyTableModel<ProteaseCount> proteaseTableModel;

    @FXML
    private TableView<ProteaseCount> proteaseTable;
    @FXML
    private TableColumn<ProteaseCount, String> geneNameProteaseTableColumn;
    @FXML
    private TableColumn<ProteaseCount, String> totalHitsProteaseTableColumn;
    @FXML
    private TableColumn<ProteaseCount, String> nrRetrievedProteaseTableColumn;

    public void init(MainController proteomixMainController) {

        this.mainController = proteomixMainController;
        this.proteaseTableModel = new MyTableModel<>(proteaseTable);

        geneNameProteaseTableColumn.setCellValueFactory(
                new PropertyValueFactory<>("geneName"));
        totalHitsProteaseTableColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalHits"));
        nrRetrievedProteaseTableColumn.setCellValueFactory(
                new PropertyValueFactory<>("retrieved"));

        geneNameProteaseTableColumn.setComparator(new AlphanumComparator());
        proteaseTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setListeners();
        setKeyNavigationListeners(this.proteaseTable, 0); // sets keyboard navigation listeners on first column of table
    }

    private void setListeners() {
        // table selection change tableSelectionChangeListener
        proteaseTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ProteaseCount>) c -> {
            if (tableSelectionChangeListener != null)
                tableSelectionChangeListener.fire();
        });
    }

    void selectRow(ProteaseCount proteaseCount) {
        proteaseTable.getSelectionModel().clearSelection();
        proteaseTable.getSelectionModel().select(proteaseCount);
    }

    // CONTEXT MENU LISTENERS

    @FXML
    private void copyAllRows() {
        copyAllRows(proteaseTable);
    }

    @FXML
    private void copySelectedRows() {
        copySelectedRows(proteaseTable);
    }

    @FXML
    private void clearSelectionContextMenu() {
        proteaseTable.getSelectionModel().clearSelection();
    }

    // OTHER METHODS

    void initialiseTableAllEntries() {
        proteaseTable.getSelectionModel().clearSelection();

        List<TextMinedObject> allProteins = mainController.getModel().getTextMinedProteins();
        proteaseTableModel.clearAndUpdateTable(mainController.getModel().getProteaseCountTableEntries(allProteins));
        proteaseTable.getSortOrder().add(proteaseTable.getColumns().get(1));
        proteaseTable.getSortOrder().get(0).setSortType(TableColumn.SortType.DESCENDING);
    }

    TableView<ProteaseCount> getTable() {
        return proteaseTable;
    }

    MyTableModel<ProteaseCount> getTableModel() {
        return proteaseTableModel;
    }

    void setSelectionChangeListener(MyTableListener listener) {
        this.tableSelectionChangeListener = listener;
    }
}
