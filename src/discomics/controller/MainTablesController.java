package discomics.controller;

import com.sun.istack.internal.Nullable;
import discomics.model.*;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.List;

/**
 * Created by Jure on 13/04/2017.
 */
public class MainTablesController implements TableControllable<Article> {
    private MainController mainController;

    // ENCAPSULATED FXMLs
    @FXML
    private GeneCountsTablesController geneCountsTablesController;
    @FXML
    private GeneCountsTablesController geneFamilyCountsTablesController;

    @FXML
    private ArticleTableController articleTableProteaseController;
    @FXML
    private ArticleTableController articleTableBiomarkerController;
    @FXML
    private ArticleTableController articleTableCustomController;

    @FXML
    private DetailsArticleController detailsArticleController;
    @FXML
    private DetailsProteinController detailsProteinController;
    @FXML
    private DetailsGeneFamilyController detailsGeneFamilyController;

    @FXML
    private TabPane geneTablesTabPane;
    @FXML
    private Tab geneTablesTab;
    @FXML
    private Tab geneFamilyTablesTab;

    @FXML
    private TabPane articlesTabPane;
    @FXML
    private Tab customArticlesTab;
    @FXML
    private Tab proteolysisArticlesTab;
    @FXML
    private Tab biomarkerArticlesTab;

    @FXML
    private TabPane detailsTabPane;
    @FXML
    private Tab proteinDetailsTab;
    @FXML
    private Tab familyDetailsTab;
    @FXML
    private Tab articleDetailsTab;

    private BooleanBinding proteolysisModelBinding;
    private BooleanBinding biomarkerModelBinding;
    private BooleanBinding customModelBinding;
    private BooleanBinding proteolysisCustomModelBinding;
    private BooleanBinding biomarkerCustomModelBinding;
    private BooleanBinding anyModelBinding;

    public void init(MainController mainController) {
        this.mainController = mainController;

        // refresh protein tables when unclassified proteolysis menu item is selected
        mainController.getUnclassifiedProteolysisMenuItem().selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (geneFamilyTablesTab.isSelected()) {
                geneCountsTablesController.getProteinTableListener().fire(); // order matters
                geneFamilyCountsTablesController.getProteinTableListener().fire(); // refresh selected tab second to update article table accordingly
            } else {
                geneFamilyCountsTablesController.getProteinTableListener().fire();
                geneCountsTablesController.getProteinTableListener().fire();
            }
        });

        mainController.getBiomarkerMenuGroup().selectedToggleProperty().addListener(event -> {
            if (geneFamilyTablesTab.isSelected()) {
                geneCountsTablesController.getProteinTableListener().fire(); // order matters
                geneFamilyCountsTablesController.getProteinTableListener().fire(); // refresh selected tab second to update article table accordingly
            } else {
                geneFamilyCountsTablesController.getProteinTableListener().fire();
                geneCountsTablesController.getProteinTableListener().fire();
            }

            if (articlesTabPane.getSelectionModel().getSelectedItem().equals(biomarkerArticlesTab)) {
                geneCountsTablesController.getProteinTableController().showBiomarkerColumn();
                geneFamilyCountsTablesController.getProteinTableController().showBiomarkerColumn();
            } else {
                articlesTabPane.getSelectionModel().select(biomarkerArticlesTab); // selection listener activates
            }
        });

        geneCountsTablesController.init(mainController, this);
        geneFamilyCountsTablesController.init(mainController, this);

        articleTableProteaseController.init(this.mainController);
        articleTableBiomarkerController.init(this.mainController);
        articleTableCustomController.init(this.mainController);

        detailsArticleController.init();
        detailsProteinController.init(this);
        detailsGeneFamilyController.init(this);

        // other properties
        biomarkerArticlesTab.setDisable(true);
        proteolysisArticlesTab.setDisable(true);
        customArticlesTab.setDisable(true);

        setListeners();

        // bindings to models being defined (proteolysis, biomarker, custom, any); to alter binding SET DISABLE TO FALSE for article tabs, and FILL gene counts table
        proteolysisModelBinding = new BooleanBinding() {
            {
                super.bind(proteolysisArticlesTab.disableProperty(), geneCountsTablesController.getProteinTableController().getTable().getItems());
            }

            @Override
            protected boolean computeValue() {
                return proteolysisArticlesTab.isDisabled() || geneCountsTablesController.getProteinTableController().getTable().getItems().isEmpty();
            }
        };

        biomarkerModelBinding = new BooleanBinding() {
            {
                super.bind(biomarkerArticlesTab.disableProperty(), geneCountsTablesController.getProteinTableController().getTable().getItems());
            }

            @Override
            protected boolean computeValue() {
                return biomarkerArticlesTab.isDisabled() || geneCountsTablesController.getProteinTableController().getTable().getItems().isEmpty();
            }
        };

        customModelBinding = new BooleanBinding() {
            {
                super.bind(customArticlesTab.disableProperty(), geneCountsTablesController.getProteinTableController().getTable().getItems());
            }

            @Override
            protected boolean computeValue() {
                return customArticlesTab.isDisabled() || geneCountsTablesController.getProteinTableController().getTable().getItems().isEmpty();
            }
        };

        proteolysisCustomModelBinding = new BooleanBinding() {
            {
                super.bind(customArticlesTab.disableProperty(), proteolysisArticlesTab.disableProperty());
            }

            @Override
            protected boolean computeValue() {
                return !(proteolysisArticlesTab.isDisabled() || customArticlesTab.isDisabled());
            }
        };

        biomarkerCustomModelBinding = new BooleanBinding() {
            {
                super.bind(customArticlesTab.disableProperty(), biomarkerArticlesTab.disableProperty());
            }

            @Override
            protected boolean computeValue() {
                return !(biomarkerArticlesTab.isDisabled() || customArticlesTab.isDisabled());
            }
        };

        anyModelBinding = new BooleanBinding() {
            {
                super.bind(customArticlesTab.disableProperty(), proteolysisArticlesTab.disableProperty(), biomarkerArticlesTab.disableProperty());
            }

            @Override
            protected boolean computeValue() {
                return customArticlesTab.isDisabled() && biomarkerArticlesTab.isDisabled() && proteolysisArticlesTab.isDisabled();
            }
        };

        // disable properties for all tabs of main tables controller
        this.geneTablesTab.disableProperty().bind(anyModelBinding);
        this.geneFamilyTablesTab.disableProperty().bind(anyModelBinding);
        this.proteinDetailsTab.disableProperty().bind(anyModelBinding);
        this.articleDetailsTab.disableProperty().bind(anyModelBinding);
        this.familyDetailsTab.disableProperty().bind(anyModelBinding);

        // retrieve tables to set table listeners
        final TableView<Article> articleTableCustom = articleTableCustomController.getTable();
        final TableView<Article> articleTableBiomarker = articleTableBiomarkerController.getTable();
        final TableView<Article> articleTableProteolysis = articleTableProteaseController.getTable();

        // ARTICLE TABLE LISTENERS; display article details
        articleTableProteolysis.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Article>) c -> {
            Article selectedArticle = articleTableProteolysis.getSelectionModel().getSelectedItem();

            // get selected protein based on GENE or GENEFAMILY tab selected
            List<TextMinedObject> selectedProteins;
            if (geneFamilyTablesTab.isSelected()) {
                selectedProteins = geneFamilyCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            } else {
                selectedProteins = geneCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            }

            detailsArticleController.showArticleDetailsProteolysis(selectedArticle, selectedProteins);

            SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel();
            if (detailsTabPane.getTabs().contains(articleDetailsTab))
                selectionModel.select(articleDetailsTab);
        });

        articleTableBiomarker.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Article>) c -> {
            Article selectedArticle = articleTableBiomarker.getSelectionModel().getSelectedItem();

            // get selected protein based on GENE or GENEFAMILY tab selected
            List<TextMinedObject> selectedProteins;
            if (geneFamilyTablesTab.isSelected()) {
                selectedProteins = geneFamilyCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            } else {
                selectedProteins = geneCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            }

            detailsArticleController.showArticleDetailsBiomarker(selectedArticle, selectedProteins);

            SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel();
            if (detailsTabPane.getTabs().contains(articleDetailsTab))
                selectionModel.select(articleDetailsTab);
        });

        articleTableCustom.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Article>) c -> {
            Article selectedArticle = articleTableCustom.getSelectionModel().getSelectedItem();

            // get selected protein based on GENE or GENEFAMILY tab selected
            List<TextMinedObject> selectedProteins;
            if (geneFamilyTablesTab.isSelected()) {
                selectedProteins = geneFamilyCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            } else {
                selectedProteins = geneCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            }

            detailsArticleController.showArticleDetailsCustom(selectedArticle, selectedProteins,
                    mainController.getModel().getCustomSearchTerms());

            SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel();
            if (detailsTabPane.getTabs().contains(articleDetailsTab))
                selectionModel.select(articleDetailsTab);
        });

        // SET PROTEIN TABLE LISTENERS: display protein details
        geneCountsTablesController.getProteinTableController().getTable()
                .getSelectionModel().getSelectedItems().addListener((ListChangeListener<TextMinedObject>) c -> {

            // identify selected gene families in table
            List<TextMinedObject> selectedProteins = geneCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();

            detailsProteinController.showProteinDetails(selectedProteins); // showStage protein family details
            detailsArticleController.showEmptyArticleDetails(); // showStage empty article details since article table refreshes, and no articles are selected

            SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel(); // switch to protein family details tab
            if (detailsTabPane.getTabs().contains(proteinDetailsTab))
                selectionModel.select(proteinDetailsTab);
        });

        geneFamilyCountsTablesController.getProteinTableController().getTable()
                .getSelectionModel().getSelectedItems().addListener((ListChangeListener<TextMinedObject>) c -> {

            // identify selected gene families in table
            List<TextMinedObject> selectedProteins = geneFamilyCountsTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();

            detailsGeneFamilyController.showProteinDetails(selectedProteins); // showStage protein family details
            detailsArticleController.showEmptyArticleDetails(); // showStage empty article details since article table refreshes, and no articles are selected

            SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel(); // switch to protein family details tab
            if (detailsTabPane.getTabs().contains(familyDetailsTab))
                selectionModel.select(familyDetailsTab);
        });


        // REFRESH ARTICLE TABLES ON GENE COUNTS TABLES TAB SELECTION
        geneTablesTab.setOnSelectionChanged(event -> {
            if (geneTablesTab.isSelected()) {
                geneCountsTablesController.getProteinTableListener().fire(); // refresh article table

                // select protein details tab in details tab pane
                SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel();
                if (detailsTabPane.getTabs().contains(proteinDetailsTab))
                    selectionModel.select(proteinDetailsTab);
            }
        });

        geneFamilyTablesTab.setOnSelectionChanged(event -> {
            if (geneFamilyTablesTab.isSelected()) {
                geneFamilyCountsTablesController.getProteinTableListener().fire(); // refresh article table

                // select family details tab in details tab pane
                SingleSelectionModel<Tab> selectionModel = detailsTabPane.getSelectionModel();
                if (detailsTabPane.getTabs().contains(familyDetailsTab))
                    selectionModel.select(familyDetailsTab);
            }
        });
    }


    private void setListeners() {
        // KEYBOARD SHORTCUT LISTENERS
        final KeyCombination findKeyCombination = new KeyCodeCombination(KeyCode.F,
                KeyCombination.CONTROL_DOWN);

        mainController.getMainStage().getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (findKeyCombination.match(event)) {
                if (proteolysisArticlesTab.isSelected())
                    articleTableProteaseController.getFilterTextField().requestFocus();
                if (biomarkerArticlesTab.isSelected())
                    articleTableBiomarkerController.getFilterTextField().requestFocus();
                if (customArticlesTab.isSelected())
                    articleTableCustomController.getFilterTextField().requestFocus();
            }
        });

        // ARTICLES TAB SELECTION LISTENERS
        proteolysisArticlesTab.setOnSelectionChanged(event -> {
            if (proteolysisArticlesTab.isSelected()) {
                geneCountsTablesController.getProteinTableController().showProteolysisColumn(); // change protein table column
                geneFamilyCountsTablesController.getProteinTableController().showProteolysisColumn();

                geneCountsTablesController.toggleProteaseTable(true);
                geneFamilyCountsTablesController.toggleProteaseTable(true);
            }
        });

        biomarkerArticlesTab.setOnSelectionChanged(event -> {
            if (biomarkerArticlesTab.isSelected()) {
                geneCountsTablesController.getProteinTableController().showBiomarkerColumn(); // change protein table column
                geneFamilyCountsTablesController.getProteinTableController().showBiomarkerColumn();

                geneCountsTablesController.toggleProteaseTable(false);
                geneFamilyCountsTablesController.toggleProteaseTable(false);
            }
        });

        customArticlesTab.setOnSelectionChanged(event -> {
            if (customArticlesTab.isSelected()) {
                geneCountsTablesController.getProteinTableController().showCustomColumn(); // change protein table column
                geneFamilyCountsTablesController.getProteinTableController().showCustomColumn();

                geneCountsTablesController.toggleProteaseTable(false);
                geneFamilyCountsTablesController.toggleProteaseTable(false);
            }
        });
    }

    private void initialiseApplicationViews() {

        if (mainController.getModel().getQuerySettings().isCustomSearch()) {
            customArticlesTab.setDisable(false);
            articleTableCustomController.initialiseTable();

            geneCountsTablesController.getProteinTableController().showCustomColumn();
            geneFamilyCountsTablesController.getProteinTableController().showCustomColumn();
            articlesTabPane.getSelectionModel().select(customArticlesTab);
        } else {
            customArticlesTab.setDisable(true);
        }
        if (mainController.getModel().getQuerySettings().isBiomarkerSearch()) {
            biomarkerArticlesTab.setDisable(false);
            articleTableBiomarkerController.initialiseTable();

            geneCountsTablesController.getProteinTableController().showBiomarkerColumn();
            geneFamilyCountsTablesController.getProteinTableController().showBiomarkerColumn();
            articlesTabPane.getSelectionModel().select(biomarkerArticlesTab);
        } else {
            biomarkerArticlesTab.setDisable(true);
        }
        if (mainController.getModel().getQuerySettings().isProteaseSearch()) {
            proteolysisArticlesTab.setDisable(false);
            articleTableProteaseController.initialiseTable();

            geneCountsTablesController.getProteaseTableController().initialiseTableAllEntries();
            geneFamilyCountsTablesController.getProteaseTableController().initialiseTableAllEntries();
            geneCountsTablesController.getProteinTableController().showProteolysisColumn();
            geneFamilyCountsTablesController.getProteinTableController().showProteolysisColumn();

            articlesTabPane.getSelectionModel().select(proteolysisArticlesTab);
        } else {
            proteolysisArticlesTab.setDisable(true);
        }
    }

    void initialiseViews(List<TextMinedObject> textMinedProteins, @Nullable List<TextMinedObject> textMinedGeneFamilies) {
        geneCountsTablesController.getProteinTableController().initialiseTableWithEntries(textMinedProteins);
        geneFamilyCountsTablesController.getProteinTableController().initialiseTableWithEntries(textMinedGeneFamilies);
        initialiseApplicationViews();
    }

    // TODO fix showStage empty details to showStage always
    @FXML
    private void biomarkerArticleTabSelection() {
        if (this.detailsArticleController != null)
            detailsArticleController.showEmptyArticleDetails();
    }

    @FXML
    private void proteaseArticleTabSelection() {
        if (this.detailsArticleController != null)
            detailsArticleController.showEmptyArticleDetails();
    }

    BooleanBinding proteolysisModelBindingProperty() {
        return proteolysisModelBinding;
    }

    BooleanBinding biomarkerModelBindingProperty() {
        return biomarkerModelBinding;
    }

    BooleanBinding proteolysisCustomModelBindingProperty() {
        return proteolysisCustomModelBinding;
    }

    BooleanBinding biomarkerCustomModelBindingProperty() {
        return biomarkerCustomModelBinding;
    }

    BooleanBinding customModelBindingProperty() {
        return customModelBinding;
    }

    BooleanBinding anyModelBindingProperty() {
        return anyModelBinding;
    }

    void selectCustomArticlesTab() {
        articlesTabPane.getSelectionModel().select(this.customArticlesTab);
    }

    void selectProteolysisArticlesTab() {
        articlesTabPane.getSelectionModel().select(this.proteolysisArticlesTab);
    }

    void selectBiomarkerArticlesTab() {
        articlesTabPane.getSelectionModel().select(this.biomarkerArticlesTab);
    }

    ArticleTableController getArticleTableProteaseController() {
        return articleTableProteaseController;
    }

    ArticleTableController getArticleTableBiomarkerController() {
        return articleTableBiomarkerController;
    }

    ArticleTableController getArticleTableCustomController() {
        return articleTableCustomController;
    }

    boolean isCustomArticlesTabSelected() {
        return customArticlesTab.isSelected();
    }

    boolean isProteolysisArticlesTabSelected() {
        return proteolysisArticlesTab.isSelected();
    }

    boolean isBiomarkerArticlesTabSelected() {
        return biomarkerArticlesTab.isSelected();
    }

    IoModel getModel() {
        return this.mainController.getModel();
    }

    public boolean isGeneFamilyTabSelected() {
        return geneFamilyTablesTab.isSelected();
    }

    public GeneCountsTablesController getGeneCountsTablesController() {
        return geneCountsTablesController;
    }

    public GeneCountsTablesController getGeneFamilyCountsTablesController() {
        return geneFamilyCountsTablesController;
    }

    public MainController getMainController() {
        return mainController;
    }

    public void disableGeneFamilyView() {
        this.geneTablesTabPane.getTabs().remove(geneFamilyTablesTab);
        this.detailsTabPane.getTabs().remove(familyDetailsTab);
    }
}
