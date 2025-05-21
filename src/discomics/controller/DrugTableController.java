//package discomics.controller;
//
//import discomics.application.AlphanumComparator;
//import discomics.model.Drug;
//import javafx.fxml.FXML;
//import javafx.scene.control.SelectionMode;
//import javafx.scene.control.TableColumn;
//import javafx.scene.control.TableView;
//import javafx.scene.control.cell.PropertyValueFactory;
//
///**
// * Created by Jure on 29.12.2016.
// */
//public class DrugTableController implements TableControllable {
//    private MainController mainController;
//
//    private MyTableModel<Drug> drugTableModel;
//
//    @FXML
//    private TableView<Drug> drugTable;
//    @FXML
//    private TableColumn<Drug, String> drugNameColumn;
//    @FXML
//    private TableColumn<Drug, String> interactionPartnerColumn;
//
//    public void init(MainController mainController) {
//        this.mainController = mainController;
//        this.drugTableModel = new MyTableModel<>(drugTable);
//
//        drugNameColumn.setCellValueFactory(new PropertyValueFactory<>("drugName"));
//        interactionPartnerColumn.setCellValueFactory(new PropertyValueFactory<>("interactionPartnerName"));
//        drugNameColumn.setComparator(new AlphanumComparator());
//        drugTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//    }
//
//    void initialiseTableAllEntries() {
//        drugTable.getSelectionModel().clearSelection();
//        drugTableModel.clearAndUpdateTable(getAllDrugInteractions(mainController.getModel()));
//    }
//
//    public TableView<Drug> getTable() {
//        return drugTable;
//    }
//
//    // CONTEXT MENU
//    // TODO
//    @FXML
//    void copyAllRows() {}
//    @FXML
//    private void copySelectedRows() {}
//    @FXML
//    private void clearSelection() {}
//
////    public MyTableModel<Drug> getDrugTableModel() {
////        return drugTableModel;
////    }
//}
