package discomics.controller;

import discomics.model.Article;
import discomics.model.TextMinedObject;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class MainTablesDrugsController implements TableControllable<TextMinedObject> {

    private MainController mainController;

    @FXML
    private ArticleTableController articleTableCustomController;
    @FXML
    private DetailsArticleController detailsArticleController;


    private MyTableModel<TextMinedObject> drugsTableModel;

    @FXML
    private TableView<TextMinedObject> drugsTable;
    @FXML
    private TableColumn<TextMinedObject, String> drugNameColumn;
    @FXML
    private TableColumn<TextMinedObject, Integer> totalHitsColumn;
    @FXML
    private TableColumn<TextMinedObject, Integer> nrRetrievedColumn;


    public void init(MainController mainController) {
        this.mainController = mainController; // set main controller field
        this.articleTableCustomController.init(mainController); // initialise articles table linked to the drugs table
        this.drugsTableModel = new MyTableModel<>(drugsTable); // initialise drugs table model
        this.articleTableCustomController.linkTextMinedObjectTable(drugsTable); // link drugs table to articles table

        this.detailsArticleController.init(); // initialise article details tab

        // set listener for article details tab to display article details upon selection from the articles table
        this.articleTableCustomController.getTable().getSelectionModel().getSelectedItems().addListener((ListChangeListener<Article>) observable -> {
            List<TextMinedObject> selectedDrugTmObjects = drugsTable.getSelectionModel().getSelectedItems(); // retrieve selected drugs
            Article selectedArticle = this.articleTableCustomController.getTable().getSelectionModel().getSelectedItem(); // retrieve selected article

            if(selectedArticle != null) {
                detailsArticleController.showArticleDetailsCustom(selectedArticle, selectedDrugTmObjects, mainController.getModel().getCustomSearchTerms()); // display selected article
            } else {
                detailsArticleController.showEmptyArticleDetails(); // no article selected
            }
        });

        this.drugNameColumn.setCellValueFactory(new PropertyValueFactory<>("mainName"));
        this.totalHitsColumn.setCellValueFactory(new PropertyValueFactory<>("totalHitsCustom"));
        this.nrRetrievedColumn.setCellValueFactory(new PropertyValueFactory<>("nrRetrievedCustom"));

        TableControllable.super.setKeyNavigationListeners(drugsTable, 0); // set key navigation listeners
    }

    void initialiseViews(List<TextMinedObject> drugs) {
        drugsTableModel.clearAndUpdateTable(drugs); // adds drugs to drugs table
        articleTableCustomController.initialiseTable(); // clears article table
    }

    // CONTEXT MENU LISTENERS
    @FXML
    private void copyAllRows() {
        TableControllable.super.copyAllRows(this.drugsTable);
    }

    @FXML
    private void copySelectedRows() {
        TableControllable.super.copySelectedRows(this.drugsTable);
    }

    @FXML
    private void clearSelection() {
        this.drugsTable.getSelectionModel().clearSelection();
    }
}
