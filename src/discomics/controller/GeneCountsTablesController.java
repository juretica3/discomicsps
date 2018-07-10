package discomics.controller;

import discomics.model.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

import java.util.List;

public class GeneCountsTablesController implements TableControllable<TextMinedObject> {
    private MainController mainController;
    private MyTableListener proteinTableListener;

    @FXML
    private ProteinTableController proteinTableController;
    @FXML
    private ProteaseTableController proteaseTableController;

    @FXML
    private SplitPane mainSplitPane;
    private Node proteaseTablePane;

    public void init(MainController mainController, MainTablesController mainTablesController) {
        this.mainController = mainController;

        // Protein table selection listener
        final MyTableListener proteinListener = () -> {
            // update protease article table
            List<TextMinedObject> selectedCollectablesProt = proteinTableController.getTable().getSelectionModel().getSelectedItems();

            if (mainController.getModel().getQuerySettings().isProteaseSearch()) {
                ArticleTableController articleTableProteaseController = mainTablesController.getArticleTableProteaseController();
                articleTableProteaseController.clearFilterFieldText();

                if (mainController.getUnclassifiedProteolysisMenuItem().isSelected()) {
                    articleTableProteaseController.clearAndUpdateTable(retrieveArticlesProteolysis(selectedCollectablesProt));
                    List<ProteaseCount> proteases = retrieveProteases(selectedCollectablesProt, mainController.getModel());
                    proteaseTableController.getTableModel().clearAndUpdateTable(proteases);

                } else { // unclassified articles not to be displayed
                    // update article table
                    List<Article> articles = retrieveArticlesProteolysisClassified(selectedCollectablesProt);
                    articleTableProteaseController.clearAndUpdateTable(articles);

                    // update protease table
                    List<ProteaseCount> proteases = retrieveProteases(selectedCollectablesProt, mainController.getModel());
                    proteaseTableController.getTableModel().clearAndUpdateTable(proteases);
                }
            }

            if (mainController.getModel().getQuerySettings().isBiomarkerSearch()) {
                ArticleTableController articleTableBiomarkerController = mainTablesController.getArticleTableBiomarkerController();
                // update biomarker article table
                articleTableBiomarkerController.clearFilterFieldText();

                if (mainController.isBiomarkerAllMenuItemSelected()) {
                    List<Article> articles = retrieveArticlesBiomarker(selectedCollectablesProt);
                    articleTableBiomarkerController.clearAndUpdateTable(articles);
                } else if (mainController.isBiomarkerBloodMenuItemSelected()) {
                    List<Article> arts = retrieveArticlesBiomarker(selectedCollectablesProt, IoModel.getBLOOD());
                    articleTableBiomarkerController.clearAndUpdateTable(arts);
                } else if (mainController.isBiomarkerSalivaMenuItemSelected()) {
                    articleTableBiomarkerController.clearAndUpdateTable(retrieveArticlesBiomarker(selectedCollectablesProt, IoModel.getSALIVA()));
                } else if (mainController.isBiomarkerUrineMenuItemSelected()) {
                    articleTableBiomarkerController.clearAndUpdateTable(retrieveArticlesBiomarker(selectedCollectablesProt, IoModel.getURINE()));
                } else if (mainController.isBiomarkerCustomMenuItemSelected()) {
                    articleTableBiomarkerController.clearAndUpdateTable(retrieveArticlesBiomarker(selectedCollectablesProt, IoModel.getCUSTOM()));
                }
            }

            if (mainController.getModel().getQuerySettings().isCustomSearch()) {
                ArticleTableController articleTableCustomController = mainTablesController.getArticleTableCustomController();

                // update custom article table
                articleTableCustomController.clearFilterFieldText();
                articleTableCustomController.clearAndUpdateTable(retrieveArticlesCustom(selectedCollectablesProt));
            }
        };

        this.proteinTableListener = proteinListener;

        // Protease table selection listener
        final MyTableListener proteaseListener = () -> {
            ArticleTableController articleTableProteaseController = mainController.getMainTablesController().getArticleTableProteaseController();
            articleTableProteaseController.clearFilterFieldText();

            List<TextMinedObject> selectedProteins = proteinTableController.getTable().getSelectionModel().getSelectedItems();
            List<ProteaseCount> selectedProteases = proteaseTableController.getTable().getSelectionModel().getSelectedItems();

            List<Article> articles = retrieveArticlesProteolysis(selectedProteins, selectedProteases, mainController.getModel());
            articleTableProteaseController.clearAndUpdateTable(articles);
        };

        proteinTableController.init(this.mainController);
        proteinTableController.setSelectionChangeListener(proteinListener);

        proteaseTableController.init(this.mainController);
        proteaseTableController.setSelectionChangeListener(proteaseListener);

        proteaseTablePane = mainSplitPane.getItems().get(1);
    }

    void toggleProteaseTable(boolean show) {
        if (show) {
            if (!mainSplitPane.getItems().contains(proteaseTablePane))  // add protease table
                mainSplitPane.getItems().add(1, proteaseTablePane);
        } else {
            if (mainSplitPane.getItems().contains(proteaseTablePane))  // remove protease table
                mainSplitPane.getItems().remove(proteaseTablePane);
        }
    }

    MyTableListener getProteinTableListener() {
        return proteinTableListener;
    }

    ProteinTableController getProteinTableController() {
        return proteinTableController;
    }

    ProteaseTableController getProteaseTableController() {
        return proteaseTableController;
    }
}
