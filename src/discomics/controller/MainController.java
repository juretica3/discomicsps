package discomics.controller;

import discomics.Main;
import discomics.model.*;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Jure on 4.9.2016.
 */
public class MainController implements TableControllable {
    public static final String CLASS_NAME = MainController.class.getName();

    private IoModel model = new IoModel();

    private InputListController inputListController;
    private NetworkController networkController;
    private SummaryProteolysisController summaryProteolysisController;
    private SummaryBiomarkerController summaryBiomarkerController;
    private SummaryCustomController summaryCustomController;
    private SummaryScoreController summaryScoreController;

    private MainControllerNonStringent mainControllerNonStringent;
    private MainControllerDrugMining mainControllerDrugMining;

    private MainControllerSettings mainControllerSettings;

    private int MAX_ARTICLES_RETRIEVED = 50;

    // ENCAPSULATED CONTROLLER
    @FXML
    private MainTablesController mainTablesController;

    private Stage mainStage;
    private HostServices hostServices;

    @FXML
    private CheckMenuItem unclassifiedProteolysisMenuItem;
    @FXML
    private MenuButton summaryMenuButton;

    private ToggleGroup biomarkerMenuGroup;
    @FXML
    private RadioMenuItem allMenuItem;
    @FXML
    private RadioMenuItem bloodMenuItem;
    @FXML
    private RadioMenuItem salivaMenuItem;
    @FXML
    private RadioMenuItem urineMenuItem;
    @FXML
    private RadioMenuItem customMenuItem;

    // file menu
    @FXML
    private MenuItem openFileButton;
    @FXML
    private MenuItem saveAsFileButton;

    // summary menu
    @FXML
    private MenuItem summaryProteolysisButton;
    @FXML
    private MenuItem summaryBiomarkerButton;
    @FXML
    private MenuItem summaryCustomButton;
    @FXML
    private MenuItem summaryScoreButton;
    @FXML
    private MenuItem logMenuButton;

    // network menu
    @FXML
    private Menu proteolysisPpiNetworkMenu;
    @FXML
    private Menu biomarkerPpiNetworkMenu;
    @FXML
    private MenuItem customPpiNetworkMenuButton;
    @FXML
    private MenuItem drugPpiNetworkMenuButton;
    @FXML
    private MenuItem drugCustomNetworkMenuButton;
    @FXML
    private MenuItem drugStringentProteolysisNetMenuButton;

    private LogWindow logWindow;

    private FileChooser openFileChooser;
    private FileChooser saveFileChooser;

    @FXML
    private MenuBar mainMenuBar;

    public void init() throws Exception {
//        if (System.getProperty("os.name").startsWith("Mac")) {
//            MenuToolkit tk = MenuToolkit.toolkit();
//            mainMenuBar.getMenus().add(tk.createDefaultApplicationMenu("DiscOmicsPS"));
//            tk.setGlobalMenuBar(mainMenuBar);
//        }

        // define file choosers
        openFileChooser = new FileChooser();
        openFileChooser.setTitle("Open File");

        saveFileChooser = new FileChooser();
        saveFileChooser.setTitle("Save File As");

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("DiscOmicsPS Workspace", "*.diw");
        openFileChooser.getExtensionFilters().add(extensionFilter);
        saveFileChooser.getExtensionFilters().add(extensionFilter);

        // biomarker menu group
        biomarkerMenuGroup = new ToggleGroup();
        allMenuItem.setToggleGroup(biomarkerMenuGroup);
        bloodMenuItem.setToggleGroup(biomarkerMenuGroup);
        salivaMenuItem.setToggleGroup(biomarkerMenuGroup);
        urineMenuItem.setToggleGroup(biomarkerMenuGroup);
        customMenuItem.setToggleGroup(biomarkerMenuGroup);
        allMenuItem.setSelected(true);

        this.mainTablesController.init(this);
        // disable save as button if no model defined
        saveAsFileButton.disableProperty().bind(mainTablesController.anyModelBindingProperty());

        // close app listener
        mainStage.setOnCloseRequest(this::quitApplicationButtonAction);

        // bind proteolysis model defined
        unclassifiedProteolysisMenuItem.disableProperty().bind(mainTablesController.proteolysisModelBindingProperty());
        summaryProteolysisButton.disableProperty().bind(mainTablesController.proteolysisModelBindingProperty());
        proteolysisPpiNetworkMenu.disableProperty().bind(mainTablesController.proteolysisModelBindingProperty());
        drugStringentProteolysisNetMenuButton.disableProperty().bind(mainTablesController.proteolysisModelBindingProperty());

        // bind biomarker model defined
        summaryBiomarkerButton.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());
        allMenuItem.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());
        bloodMenuItem.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());
        salivaMenuItem.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());
        urineMenuItem.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());
        customMenuItem.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());
        biomarkerPpiNetworkMenu.disableProperty().bind(mainTablesController.biomarkerModelBindingProperty());

        // bind custom model defined
        summaryCustomButton.disableProperty().bind(mainTablesController.customModelBindingProperty());
        customPpiNetworkMenuButton.disableProperty().bind(mainTablesController.customModelBindingProperty());
        drugCustomNetworkMenuButton.disableProperty().bind(mainTablesController.customModelBindingProperty());

        // bind at least one model defined
        logMenuButton.disableProperty().bind(mainTablesController.anyModelBindingProperty());
        drugPpiNetworkMenuButton.disableProperty().bind(mainTablesController.anyModelBindingProperty());
        summaryScoreButton.disableProperty().bind(mainTablesController.anyModelBindingProperty());

        // keyboard listeners
        final KeyCombination inputListKeyCombination = new KeyCodeCombination(KeyCode.N,
                KeyCombination.CONTROL_DOWN);
        final KeyCombination saveKeyCombination = new KeyCodeCombination(KeyCode.S,
                KeyCombination.CONTROL_DOWN);
        final KeyCombination openKeyCombination = new KeyCodeCombination(KeyCode.O,
                KeyCombination.CONTROL_DOWN);

        mainStage.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (saveKeyCombination.match(event)) {
                if (!saveAsFileButton.isDisable())
                    saveAsFileAction();
            } else if (openKeyCombination.match(event)) {
                openFileAction();
            } else if (inputListKeyCombination.match(event)) {
                inputListController.showStage(true);
            }
        });

        // initialise other views
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/discomics/view/InputListController.fxml"));
        Parent inputListView = loader.load();
        Stage inputListStage = new Stage();
        Scene inputListScene = new MyScene(inputListView);
        inputListStage.setScene(inputListScene);
        inputListStage.setTitle(Main.NAME + " v." + Main.VERSION);
        inputListStage.setResizable(false);
        this.inputListController = loader.getController();
        this.inputListController.init(this, inputListStage);

        FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/discomics/view/NetworkController.fxml"));
        Parent networkView = loader1.load();
        Stage networkStage = new Stage();
        Scene networkScene = new MyScene(networkView);
        networkStage.setScene(networkScene);
        networkStage.setTitle(Main.NAME + " v." + Main.VERSION);
        this.networkController = loader1.getController();
        this.networkController.init(this, networkStage);

        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/discomics/view/summaryControllers/SummaryProteolysisController.fxml"));
        Parent summaryView = loader2.load();
        Stage summaryStageProteolysis = new Stage();
        Scene summarySceneProteolysis = new MyScene(summaryView);
        summaryStageProteolysis.setScene(summarySceneProteolysis);
        summaryStageProteolysis.setTitle(Main.NAME + " v." + Main.VERSION);
        this.summaryProteolysisController = loader2.getController();
        this.summaryProteolysisController.init(this, summaryStageProteolysis);

        FXMLLoader loader3 = new FXMLLoader(getClass().getResource("/discomics/view/summaryControllers/SummaryBiomarkerController.fxml"));
        Parent summaryView2 = loader3.load();
        Stage summaryStageBiomarker = new Stage();
        Scene summarySceneBiomarker = new MyScene(summaryView2);
        summaryStageBiomarker.setScene(summarySceneBiomarker);
        summaryStageBiomarker.setTitle(Main.NAME + " v." + Main.VERSION);
        this.summaryBiomarkerController = loader3.getController();
        this.summaryBiomarkerController.init(this, summaryStageBiomarker);

        FXMLLoader loader4 = new FXMLLoader(getClass().getResource("/discomics/view/summaryControllers/SummaryCustomController.fxml"));
        Parent summaryView3 = loader4.load();
        Stage summaryStageCustom = new Stage();
        Scene summarySceneCustom = new MyScene(summaryView3);
        summaryStageCustom.setScene(summarySceneCustom);
        summaryStageCustom.setTitle(Main.NAME + " v." + Main.VERSION);
        this.summaryCustomController = loader4.getController();
        this.summaryCustomController.init(this, summaryStageCustom);

        FXMLLoader loader5 = new FXMLLoader(getClass().getResource("/discomics/view/summaryControllers/SummaryScoreController.fxml"));
        Parent summaryScoreView = loader5.load();
        Stage summaryScoreStage = new Stage();
        Scene summaryScoreScene = new MyScene(summaryScoreView);
        summaryScoreStage.setScene(summaryScoreScene);
        summaryScoreStage.setTitle(Main.NAME + " v." + Main.VERSION);
        this.summaryScoreController = loader5.getController();
        this.summaryScoreController.init(this, summaryScoreStage);

        FXMLLoader loader6 = new FXMLLoader(getClass().getResource("/discomics/view/MainControllerNonStringent.fxml"));
        Parent mainControllerNonStringentView = loader6.load();
        Stage mainControllerNonStringentStage = new Stage();
        Scene mainControllerNonStringentScene = new MyScene(mainControllerNonStringentView);
        mainControllerNonStringentStage.setScene(mainControllerNonStringentScene);
        mainControllerNonStringentStage.setTitle(Main.NAME + " v." + Main.VERSION);
        this.mainControllerNonStringent = loader6.getController();
        this.mainControllerNonStringent.init(this, mainControllerNonStringentStage);

        FXMLLoader loader7 = new FXMLLoader(getClass().getResource("/discomics/view/MainControllerDrugMining.fxml"));
        Parent drugMiningControllerView = loader7.load();
        Stage drugMiningStage = new Stage();
        Scene drugMiningScene = new MyScene(drugMiningControllerView);
        drugMiningStage.setScene(drugMiningScene);
        drugMiningStage.setTitle(Main.NAME + " v." + Main.VERSION);
        drugMiningStage.setResizable(false);
        this.mainControllerDrugMining = loader7.getController();
        this.mainControllerDrugMining.init(this, drugMiningStage);

        FXMLLoader loader8 = new FXMLLoader(getClass().getResource("/discomics/view/MainControllerSettings.fxml"));
        Parent settingsControllerView = loader8.load();
        Stage settingsStage = new Stage();
        Scene settingsScene = new MyScene(settingsControllerView);
        settingsStage.setScene(settingsScene);
        settingsStage.setTitle(Main.NAME + " v." + Main.VERSION);
        settingsStage.setResizable(false);
        this.mainControllerSettings = loader8.getController();
        this.mainControllerSettings.init(this, settingsStage);
    }

    void initialiseApplicationViews() {
        mainTablesController.initialiseViews(this.model.getTextMinedProteins(), this.model.getTextMinedGeneFamilies());

        if (model.getQuerySettings().isCustomSearch()) {
            summaryCustomController.initialiseSummaryView();
        }
        if (model.getQuerySettings().isBiomarkerSearch()) {
            summaryBiomarkerController.initialiseSummaryView();
        }
        if (model.getQuerySettings().isProteaseSearch()) {
            summaryProteolysisController.initialiseSummaryView();
        }
        summaryScoreController.setScoreTableData();
        logWindow = new LogWindow(this.model);

        mainControllerDrugMining.initialiseViews();
    }

    /**
     * TOOLBAR LISTENERS
     */
    @FXML
    private void inputListButtonAction() {
        inputListController.showStage(true);
        summaryProteolysisController.showStage(false);
        summaryBiomarkerController.showStage(false);
        summaryCustomController.showStage(false);
        networkController.showStage(false);
    }

    /**
     * TOOLBAR LISTENERS: NETWORK MENU
     */
    @FXML
    private void showFullProteolysisNetwork() {
        // collect all proteins with more than 0 hits in the proteolysis search
        List<TextMinedObject> nonZeroHitTmProt = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsProteolysis() > 0)
                .collect(Collectors.toList());

        showNetwork(model.getProteolysisFullPpi(), nonZeroHitTmProt, 0);
    }

    @FXML
    private void showStringentProteolysisNetwork() {
        // collect textminedproteins with non-empty physical interaction lists
        List<TextMinedObject> tmProts = model.getTextMinedProteins().stream()
                .filter(tmProt -> !tmProt.getArticleCollectablePlys().getPhysicalInteractions().isEmpty())
                .collect(Collectors.toList());

        showNetwork(model.getProteolysisStringentPpi(), tmProts, 0);
    }

    @FXML
    private void showFullBiomarkerNetwork() {
        // collect textminedproteins with more than 0 biomarker hits
        List<TextMinedObject> tmProts = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsBiomarker() > 0)
                .collect(Collectors.toList());

        showNetwork(model.getBiomarkerFullPpi(), tmProts, 1);
    }

    @FXML
    private void showBloodBiomarkerNetwork() {
        // collect all TextMinedProteins that contain one or more hits mentioning desired biomarker (to be displayed in protein table of network view)
        List<TextMinedObject> tmProts = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsBiomarker(IoModel.getBLOOD()) > 0)
                .collect(Collectors.toList());

        showNetwork(model.getBiomarkerBloodPpi(), tmProts, 1);
    }

    @FXML
    private void showUrineBiomarkerNetwork() {
        // collect all TextMinedProteins that contain one or more hits mentioning desired biomarker (to be displayed in protein table of network view)
        List<TextMinedObject> tmProts = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsBiomarker(IoModel.getURINE()) > 0)
                .collect(Collectors.toList());

        showNetwork(model.getBiomarkerUrinePpi(), tmProts, 1);
    }

    @FXML
    private void showSalivaBiomarkerNetwork() {
        // collect all TextMinedProteins that contain one or more hits mentioning desired biomarker (to be displayed in protein table of network view)
        List<TextMinedObject> tmProts = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsBiomarker(IoModel.getSALIVA()) > 0)
                .collect(Collectors.toList());

        showNetwork(model.getBiomarkerSalivaPpi(), tmProts, 1);
    }

    @FXML
    private void showCustomNetwork() {
        // collect all TextMinedProteins that contain one or more custom hits (to be displayed in protein table of network view)
        List<TextMinedObject> tmProts = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsCustom() > 0)
                .collect(Collectors.toList());

        showNetwork(model.getCustomFullPpi(), tmProts, 2);
    }

    @FXML
    private void showDrugInteractionNetwork() {
        // collect all ProteinDrugInteraction objects from all TextMinedProtein objects. return error if no drugs present
        final List<Drug> drugs = new ArrayList<>();
        model.getTextMinedProteins().forEach(textMinedProtein -> drugs.addAll(((TextMinedProtein) textMinedProtein).getTextMinableInput().getDrugs()));
        if (drugs.size() == 0) {
            Alert alert = new MyAlert(Alert.AlertType.INFORMATION, this.mainStage);
            alert.setContentText("No drug interactions found for input proteins.");
            alert.setHeaderText("");
            alert.show();
        } else
            showNetwork(model.getProteinDrugInteractionNetwork(), model.getTextMinedProteins(), 3);
    }

    @FXML
    private void showDrugStringentProteolysisInteractionNetwork() {
        // collect all TextMinedProteins that contain one or more physical protease interactions (to be displayed in protein table of network view)
        List<TextMinedObject> tmObjects = model.getTextMinedProteins().stream()
                .filter(tmProt -> !tmProt.getArticleCollectablePlys().getPhysicalInteractions().isEmpty())
                .collect(Collectors.toList());
        //model.getProteinDrugProteolysisStringentNetwork().get

        showNetwork(model.getProteinDrugProteolysisStringentNetwork(), tmObjects, 3); // initialise and showStage network view
    }

    @FXML
    private void showDrugCustomInteractionNetwork() {
        // collect all TextMinedProteins that contain one or more hits in the custom search (to be displayed in protein table of network view)
        final List<TextMinedObject> tmObjects = model.getTextMinedProteins().stream()
                .filter(tmProt -> tmProt.getTotalHitsCustom() > 0).collect(Collectors.toList());

        showNetwork(model.getProteinDrugCustomNetwork(), tmObjects, 2); // initialise and showStage network view
    }

    private void showNetwork(ProteinInteractionNetwork network, List<TextMinedObject> tmObjects, int mode) {
        if (!tmObjects.isEmpty()) {
            networkController.initialiseNetworkView(network, tmObjects, mode);
            networkController.showStage(true);
        } else {
            Alert alert = new MyAlert(Alert.AlertType.WARNING, this.mainStage);
            alert.setHeaderText("");
            alert.setContentText("Network is empty.");
            alert.show();
        }
    }

    /**
     * TOOLBAR LISTENERS: SUMMARY MENU
     */
    @FXML
    private void summaryProteolysisButtonAction() {
        summaryProteolysisController.showStage(true);
    }

    @FXML
    private void summaryBiomarkerButtonAction() {
        summaryBiomarkerController.showStage(true);
    }

    @FXML
    private void summaryCustomButtonAction() {
        summaryCustomController.showStage(true);
    }

    @FXML
    private void summaryScoreButtonAction() {
        summaryScoreController.showStage(true);
    }

    @FXML
    private void helpButtonActionListener() {
    }

    @FXML
    private void showNonStringentMiningWindow() {
        mainControllerNonStringent.showStage(true);
    }

    @FXML
    private void showDrugMiningWindow() {
        mainControllerDrugMining.showStage(true);
    }

    @FXML
    private void openSettingsButtonAction() {
        mainControllerSettings.showStage();
    }

    // GUI METHODS
    void focusMainStage() {
        mainStage.requestFocus();
    }

    void selectBiomarkerAllMenuItem() {
        allMenuItem.setSelected(true);
    }

    // used in SummaryBiomarkerController to programmatically switch to other biomarker in response to bar in bar chart selection
    void selectBiomarkerMenuItem(Biomarker biomarker) {
        if (biomarker.equals(IoModel.getBLOOD()))
            biomarkerMenuGroup.selectToggle(bloodMenuItem);
        else if (biomarker.equals(IoModel.getSALIVA()))
            biomarkerMenuGroup.selectToggle(salivaMenuItem);
        else if (biomarker.equals(IoModel.getURINE()))
            biomarkerMenuGroup.selectToggle(urineMenuItem);
        else if (biomarker.equals(IoModel.getCUSTOM()))
            biomarkerMenuGroup.selectToggle(customMenuItem);
        else
            biomarkerMenuGroup.selectToggle(allMenuItem);
    }

    // make new non-stringent text mining search
    void nonStringentTextMiningSearch() {
        List<TextMinedObject> tmProteins = mainTablesController.getGeneCountsTablesController()
                .getProteinTableController().getTable().getSelectionModel().getSelectedItems();
        mainControllerNonStringent.searchAndShowStage(tmProteins);
    }

    // make new drug text mining search
    void drugTextMiningSearch() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<TextMinedObject> selectedTmObjects = mainTablesController.getGeneCountsTablesController()
                .getProteinTableController().getTable().getSelectionModel().getSelectedItems();

        TextMiningTask<List<TextMinedObject>> textMiningTask = new TextMiningTask<List<TextMinedObject>>(this.mainControllerDrugMining.getStage()) {
            @Override
            protected List<TextMinedObject> call() throws Exception {
                List<TextMinedObject> textMinedDrugs = new ArrayList<>();

                for (TextMinedObject tmObject : selectedTmObjects) {
                    Protein selectedProtein = ((TextMinedProtein) tmObject).getTextMinableInput();
                    List<TextMinedObject> outputTextMinedDrugs = mainControllerDrugMining.textMineDrugs(selectedProtein, model.getCustomSearchTerms());
                    for (TextMinedObject tmDrug : textMinedDrugs) {
                        if (tmDrug.getTextMinableInput().getMainName().toLowerCase().contains("pamidronate")) {
                            System.out.println("Nr articles: " + tmDrug.getArticleCollectableCust().getArticleCollection().size());
                        }
                    }
                    textMinedDrugs.addAll(outputTextMinedDrugs);
                }

                return textMinedDrugs;
            }
        };

        textMiningTask.setOnSucceeded(event -> { // set of succeeded
            try {
                List<TextMinedObject> tmDrugsResult = textMiningTask.get();
                textMiningTask.onSucceeded(new ArrayList<>()); // no failed cases, ever (arbitrary)

                if (tmDrugsResult != null) { // update MODEL
                    for (TextMinedObject tmDrug : tmDrugsResult) {
                        this.getModel().addTextMinedDrug((TextMinedDrug) tmDrug);
                    }
                }
                mainControllerDrugMining.initialiseViews(); // initialise tables
                mainControllerDrugMining.showStage(true); // show drug text mining window

            } catch (InterruptedException | ExecutionException e) {
                ExceptionDialog exceptionDialog = new ExceptionDialog(e, this.getMainStage());
                exceptionDialog.showAndWait();
            }
        });

        executorService.submit(textMiningTask); // submit task

    }

    /**
     * CUSTOM BIOMARKER SELECTION
     */
    @FXML
    private void customBiomarkerSelectedActionListener() {
        CustomBiomarkerDefinitionDialog dialog = new CustomBiomarkerDefinitionDialog();
        dialog.createNewForm();
        dialog.showDialog();

//        GridPane gridPane = new GridPane();
//        gridPane.setVgap(10);
//        gridPane.setPadding(new Insets(15));
//
//        Label title = new Label("Enter search terms for your custom biomarker");
//        title.setStyle("-fx-font-size: 14;");
//        gridPane.add(title, 0, 1);
//
//        VBox textFieldBox = new VBox(5);
//        TextField searchTermField = new TextField();
//        searchTermField.setPromptText("Search Term");
//        textFieldBox.getChildren().add(searchTermField);
//        gridPane.add(textFieldBox, 0, 2);
//
//        Button addField = new Button("Add");
//        addField.setTooltip(new Tooltip("Ctrl+Tab"));
//        addField.setPrefWidth(80);
//
//        gridPane.add(addField, 0, 3);
//
//        Button ok = new Button("OK");
//        ok.setPrefWidth(80);
//        gridPane.add(ok, 0, 4);
//
//        ColumnConstraints columnConstraints = new ColumnConstraints();
//        columnConstraints.setHalignment(HPos.CENTER);
//        gridPane.getColumnConstraints().add(columnConstraints);
//
//        final Stage dialog = new Stage();
//        dialog.initModality(Modality.APPLICATION_MODAL);
//        dialog.initOwner(mainStage);
//        Scene dialogScene = new MyScene(gridPane);
//        dialog.setScene(dialogScene);
//        dialog.setResizable(false);
//
//        final KeyCombination combination = new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN);
//        dialogScene.setOnKeyReleased(event -> {
//            if (combination.match(event)) {
//                addField.fire();
//            }
//        });
//
//        BooleanBinding bb = new BooleanBinding() {
//            {
//                super.bind(searchTermField.textProperty());
//            }
//
//            @Override
//            protected boolean computeValue() {
//                return searchTermField.getText().trim().isEmpty();
//            }
//        };
//        ok.disableProperty().bind(bb);
//        addField.disableProperty().bind(bb);
//
//        addField.setOnAction(event -> {
//            TextField field = new TextField();
//            field.setPromptText("Search Term");
//            textFieldBox.getChildren().add(field);
//            field.requestFocus();
//            dialog.sizeToScene();
//        });
//
//        ok.setOnAction(event -> {
//            List<Node> parameters = textFieldBox.getChildren();
//            List<String> parametersString = new ArrayList<>();
//            for (Node parameter : parameters) {
//                if (parameter instanceof TextField) {
//                    if (!((TextField) parameter).getText().trim().isEmpty()) {
//                        parametersString.add(((TextField) parameter).getText().trim());
//                    }
//                }
//            }
//
//            for (TextMinedProtein tmProt : model.getTextMinedProteins())
//                tmProt.getArticleCollectableBiom().removeCustomBiomarkerFromArticles();
//
//            IoModel.getCUSTOM().setSearchTerms(parametersString);
//
//            for (TextMinedProtein tmProt : model.getTextMinedProteins())
//                tmProt.getArticleCollectableBiom().defineCustomBiomarkerForArticles();
//
//            summaryBiomarkerController.initialiseCustomSummaryView();
//
//            List<TextMinedProtein> selectedCollectablesProt = mainTablesController.getProteinTableController().getTable().getSelectionModel().getSelectedItems();
//            mainTablesController.getArticleTableBiomarkerController().clearAndUpdateTable(retrieveArticlesBiomarker(selectedCollectablesProt, IoModel.getCUSTOM()));
//
//            dialog.close();
//        });
//
//        dialog.setOnCloseRequest(event -> {
//            allMenuItem.setSelected(true);
//            dialog.close();
//        });
//
//        dialog.showStage();
//        ok.requestFocus();
    }

    private class CustomBiomarkerDefinitionDialog extends GridPane {
        private Stage dialog;
        private Scene dialogScene;
        private VBox userInputContainer;

        CustomBiomarkerDefinitionDialog() {
            dialog = new Stage();
            dialogScene = new MyScene(this);
            // define Stage and Scene to visualise, insert GridPane formed earlier into Scene
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(mainStage);
            dialog.setScene(dialogScene);
            dialog.setResizable(false);

            setVgap(10);
            setPadding(new Insets(15));
            createNewForm();
        }

        private void showDialog() {
            dialog.show();

            double positionX = mainStage.getX() + (mainStage.getWidth() - dialog.getWidth()) / 2;
            double positionY = mainStage.getY() + (mainStage.getHeight() - dialog.getHeight()) / 2;
            dialog.setX(positionX);
            dialog.setY(positionY);
        }

        private void createNewForm() {
            //dialog = new Stage(); // reset dialog stage

            // create and add dialog title to parent GridPane
            Label title = new Label("Enter search terms for your custom biomarker");
            title.setStyle("-fx-font-size: 14;");
            add(title, 0, 1);

            // create and add first input box to parent GridPane
            userInputContainer = new VBox(5);
            TextField searchTermField = new TextField();
            searchTermField.setPromptText("Search Term");
            userInputContainer.getChildren().add(searchTermField);
            add(userInputContainer, 0, 2);
            searchTermField.requestFocus();

            // create and add button to add another input box to parent GridPane
            Button addField = new Button("Add");
            addField.setTooltip(new Tooltip("Ctrl+Enter"));
            addField.setPrefWidth(80);
            add(addField, 0, 3);

            // create and add OK button to parent GridPane
            Button okButton = new Button("OK");
            addField.setTooltip(new Tooltip("Enter"));
            okButton.setPrefWidth(80);
            add(okButton, 0, 5);

            // align gridpane content to centre
            ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setHalignment(HPos.CENTER);
            getColumnConstraints().add(columnConstraints);

            // key listeners
            final KeyCombination addCombination = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);
            final KeyCombination okCombination = new KeyCodeCombination(KeyCode.ENTER);
            dialogScene.setOnKeyReleased(event -> {
                if (addCombination.match(event)) {
                    addField.fire();
                } else if (okCombination.match(event)) {
                    okButton.fire();
                }
            });

            // disable property binding for OK button
            BooleanBinding bb = new BooleanBinding() {
                {
                    super.bind(searchTermField.textProperty());
                }

                @Override
                protected boolean computeValue() {
                    return searchTermField.getText().trim().isEmpty();
                }
            };
            okButton.disableProperty().bind(bb);

            // add field button action listener
            addField.setOnAction(event -> {
                TextField field = new TextField();
                field.setPromptText("Search Term");
                userInputContainer.getChildren().add(field);
                field.requestFocus();
                dialog.sizeToScene();
            });

            // ok button action listener
            okButton.setOnAction(event -> {
                // collect all entered inputs into ArrayList parametersStrings
                List<Node> parameters = userInputContainer.getChildren();
                List<String> parametersStrings = new ArrayList<>();
                for (Node parameter : parameters) {
                    if (parameter instanceof TextField) {
                        if (!((TextField) parameter).getText().trim().isEmpty()) {
                            parametersStrings.add(((TextField) parameter).getText().trim());
                        }
                    }
                }

                // remove previous Custom biomarker from all articles
                for (TextMinedObject tmObject : model.getTextMinedProteins())
                    tmObject.getArticleCollectableBiom().removeCustomBiomarkerFromArticles();

                // define new custom Biomarker
                IoModel.getCUSTOM().setSearchTerms(parametersStrings);

                // search through all articles and define new Biomarker
                for (TextMinedObject tmObject : model.getTextMinedProteins())
                    tmObject.getArticleCollectableBiom().defineCustomBiomarkerForArticles();

                // initialise Summary view table to be updated with the new article numbers
                summaryBiomarkerController.initialiseCustomSummaryView();

                allMenuItem.setSelected(true); // TODO rearrange listeners for Biomarker toggle menu group (extremely ugly code)
                customMenuItem.setSelected(true);

                dialog.close();
            });

            dialog.setOnCloseRequest(event -> {
                allMenuItem.setSelected(true);
                dialog.close();
            });
        }
    }


    /**
     * SAVE AND OPEN FILE FUNCTIONALITY
     */

    @FXML
    private void openFileAction() {
        final File selectedFile = openFileChooser.showOpenDialog(mainStage);

        if (selectedFile == null)
            return;

        Task<IoModel> openFileTask = new Task<IoModel>() {
            @Override
            protected IoModel call() throws Exception {

                IoModel model = new IoModel();
                ObjectInputStream ois = null;
                FileInputStream fis = null;
                try {

                    fis = new FileInputStream(selectedFile);
                    ois = new ObjectInputStream(fis);

                    model = (IoModel) ois.readObject();
                    return model;

                } finally {
                    if (ois != null)
                        ois.close();

                    if (fis != null)
                        fis.close();
                }
            }
        };

        final MyProgressDialog myProgressDialog = new MyProgressDialog(mainStage, openFileTask, "Opening file");

        openFileTask.setOnSucceeded(event -> {
            try {
                IoModel outputModel = openFileTask.get();
                if (outputModel != null) {
                    myProgressDialog.close();
                    setModel(outputModel);
                    initialiseApplicationViews();
                    Alert alert = new MyAlert(Alert.AlertType.INFORMATION, getMainStage());
                    alert.setContentText("File opened successfully!");
                    alert.setHeaderText("");
                    alert.show();
                }
            } catch (InterruptedException | ExecutionException | NullPointerException e1) {
                ExceptionDialog exceptionDialog = new ExceptionDialog(e1, getMainStage());
                exceptionDialog.showAndWait();
            }
        });

        openFileTask.setOnFailed(event -> {
            myProgressDialog.close();
            ExceptionDialog exceptionDialog = new ExceptionDialog(openFileTask.getException(), getMainStage());
            exceptionDialog.showAndWait();
        });

        openFileTask.setOnScheduled(event -> myProgressDialog.show());

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(openFileTask);
        executorService.shutdown();
    }

    @FXML
    private void saveAsFileAction() {
        final File selectedFile = saveFileChooser.showSaveDialog(mainStage);

        if (selectedFile == null)
            return;

        Task<Boolean> saveFileTask = new Task<Boolean>() {

            @Override
            protected Boolean call() throws Exception {
                IoModel model = getModel();
                if (model.getProteinCollection().getOutputProteinList().size() == 0)
                    return false;

                FileOutputStream fos = null;
                ObjectOutputStream oos = null;
                try {
                    fos = new FileOutputStream(selectedFile);
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(model);
                    oos.flush();

                } finally {
                    if (fos != null)
                        fos.close();

                    if (oos != null)
                        oos.close();
                }
                return true;
            }
        };

        final MyProgressDialog myProgressDialog = new MyProgressDialog(mainStage, saveFileTask, "Saving to file");

        saveFileTask.setOnFailed(event -> {
            myProgressDialog.close();
            ExceptionDialog exceptionDialog = new ExceptionDialog(saveFileTask.getException(), getMainStage());
            exceptionDialog.showAndWait();
        });

        saveFileTask.setOnSucceeded(event -> {
            myProgressDialog.close();
            Alert alert = new MyAlert(Alert.AlertType.INFORMATION, getMainStage());
            alert.setContentText("Saved successfully!");
            alert.setHeaderText("");
            alert.show();
        });

        saveFileTask.setOnScheduled(event -> myProgressDialog.show());

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(saveFileTask);
        executorService.shutdown();
    }

    @FXML
    private void quitApplicationButtonAction(Event event) {
        MyAlert alert = new MyAlert(Alert.AlertType.WARNING, this.mainStage);
        alert.initModality(Modality.APPLICATION_MODAL);

        alert.setTitle("Warning");
        alert.setContentText("Remember to save your queries if you would like to access them offline.");
        alert.setHeaderText("Are you sure?");

        ButtonType buttonTypeOne = new ButtonType("Exit", ButtonBar.ButtonData.YES);
        ButtonType buttonTypeTwo = new ButtonType("Don't Exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == buttonTypeOne) {
                Platform.exit();
                System.exit(0);
            } else if (result.get() == buttonTypeTwo) {
                alert.hide();
                event.consume();
            }
        }
    }

    /**
     * LOG WINDOW
     */
    @FXML
    private void showLogWindow() {
        logWindow.show();
    }

    private class LogWindow extends Stage {
        private VBox parentVBox = new VBox(5d);
        private ScrollPane scrollPane = new ScrollPane();
        private VBox vBoxInScrollPane = new VBox(5d);

        private StringBuilder allContent = new StringBuilder();

        private LogWindow(IoModel model) {
            super();

            initModality(Modality.NONE);
            initOwner(mainStage);
            Scene dialogScene = new MyScene(parentVBox);
            setScene(dialogScene);
            parentVBox.setPrefHeight(500);
            parentVBox.setPrefWidth(350);
            parentVBox.setPadding(new Insets(10d));
            parentVBox.setAlignment(Pos.CENTER);

            Label titleLabel = new Label("Log");
            titleLabel.setStyle("-fx-font-size: 18;");
            parentVBox.getChildren().add(titleLabel);

            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color:transparent;");

            allContent = new StringBuilder();

            List<String> proteinReplaced = new ArrayList<>();
            List<String> proteinAmbiguous = new ArrayList<>();
            List<String> proteinStringIdNotFound = new ArrayList<>();
            List<String> proteinFailedDgi = new ArrayList<>();
            List<String> proteinFailedUniProt = new ArrayList<>();
            for (Protein protein : model.getProteinCollection().getOutputProteinList()) {

                if (!protein.getQueryInputGene().equalsIgnoreCase(protein.getMainName())) {
                    proteinReplaced.add(protein.getQueryInputGene() + " -> " + protein.getMainName());
                }
                if (protein.isAmbiguousHGNC()) {
                    proteinAmbiguous.add(protein.getQueryInputGene());
                }
                if (!protein.isSuccessBuildingSTRING()) {
                    proteinStringIdNotFound.add(protein.getQueryInputGene());
                }
                if (!protein.isSuccessBuildingDGI()) {
                    proteinFailedDgi.add(protein.getQueryInputGene());
                }
                if (!protein.isSuccessBuildingUniProt()) {
                    proteinFailedUniProt.add(protein.getQueryInputGene());
                }
            }

            addHeading("Failed to retrieve gene nomenclature for:");
            if (!model.getProteinCollection().getProteinsFailedIdentify().isEmpty())
                model.getProteinCollection().getProteinsFailedIdentify().forEach(this::addText);
            else
                addText("None");

            addHeading("Replaced gene name and successfully retrieved nomenclature for:");
            if (!proteinReplaced.isEmpty())
                proteinReplaced.forEach(this::addText);
            else
                addText("None");

            addHeading("Gene nomenclature ambiguous for:");
            if (!proteinAmbiguous.isEmpty())
                proteinAmbiguous.forEach(this::addText);
            else
                addText("None");

            // add information on genes that were supplemented (parental genes of pseudogenes)
//            addHeading("Supplemented input gene list with the following genes:");
//            List<String> inputGeneList = new ArrayList<>();
//            model.getProteinCollection().getInputProteinSet().forEach(s -> inputGeneList.add(s.toLowerCase())); // get input set of gene identifiers
//
//            Set<String> supplementedGenes = new HashSet<>();
//            for (Protein protein : model.getProteinCollection().getOutputProteinList()) { // loop through queried protein list
//                if (!inputGeneList.contains(protein.getMainName().toLowerCase())) { // if input set of gene identifiers does not contain a protein then it must be a supplemented protein
//                    supplementedGenes.add(protein.getMainName());
//                }
//            }
//            if (!supplementedGenes.isEmpty()) // add text to GUI
//                supplementedGenes.forEach(this::addText);
//            else
//                addText("None");

            addHeading("STRING ID search failed for:");
            if (!proteinStringIdNotFound.isEmpty())
                proteinStringIdNotFound.forEach(this::addText);
            else
                addText("None");

            addHeading("Drug interaction search failed for:");
            if (!proteinFailedDgi.isEmpty())
                proteinFailedDgi.forEach(this::addText);
            else
                addText("None");

            addHeading("GO Annotation search failed for:");
            if (!proteinFailedUniProt.isEmpty())
                proteinFailedUniProt.forEach(this::addText);
            else
                addText("None");

            if (model.getQuerySettings().isProteaseSearch()) {
                List<String> failedList = new ArrayList<>();
                for (TextMinedObject tmObject : model.getTextMinedProteins()) {
                    Boolean[] isTextMiningSuccessful = tmObject.isTextMiningSuccessfulProteolysis();

                    if (!isTextMiningSuccessful[0]) {
                        failedList.add(tmObject.getTextMinableInput().getMainName() + " (EPMC)");
                    }
                    if (!isTextMiningSuccessful[1]) {
                        failedList.add(tmObject.getTextMinableInput().getMainName() + " (Pubmed)");
                    }
                }
                addHeading("Proteolysis text mining failed for:");
                if (!failedList.isEmpty())
                    failedList.forEach(this::addText);
                else
                    addText("None");
            }
            if (model.getQuerySettings().isBiomarkerSearch()) {
                List<String> failedList = new ArrayList<>();
                for (TextMinedObject tmObject : model.getTextMinedProteins()) {
                    Boolean[] isTextMiningSuccessful = tmObject.isTextMiningSuccessfulBiomarker();

                    if (!isTextMiningSuccessful[0]) {
                        failedList.add(tmObject.getTextMinableInput().getMainName() + " (EPMC)");
                    }
                    if (!isTextMiningSuccessful[1]) {
                        failedList.add(tmObject.getTextMinableInput().getMainName() + " (Pubmed)");
                    }
                }
                addHeading("Biomarker text mining failed for:");
                if (!failedList.isEmpty())
                    failedList.forEach(this::addText);
                else
                    addText("None");
            }
            if (model.getQuerySettings().isCustomSearch()) {
                List<String> failedList = new ArrayList<>();
                for (TextMinedObject tmObject : model.getTextMinedProteins()) {
                    Boolean[] isTextMiningSuccessful = tmObject.isTextMiningSuccessfulCustom();

                    if (!isTextMiningSuccessful[0]) {
                        failedList.add(tmObject.getTextMinableInput().getMainName() + " (EPMC)");
                    }
                    if (!isTextMiningSuccessful[1]) {
                        failedList.add(tmObject.getTextMinableInput().getMainName() + " (Pubmed)");
                    }
                }
                addHeading("Custom text mining failed for:");
                if (!failedList.isEmpty())
                    failedList.forEach(this::addText);
                else
                    addText("None");
            }

            scrollPane.setContent(vBoxInScrollPane);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            parentVBox.getChildren().add(scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        private void addHeading(String text) {
            vBoxInScrollPane.getChildren().add(new MyLabel(text, true));
            allContent.append("\n").append(text).append("\n");
        }

        private void addText(String text) {
            vBoxInScrollPane.getChildren().add(new MyLabel(text, false));
            allContent.append(text).append(";");
        }

        private class MyLabel extends Label {

            MyLabel(String text, boolean isHeader) {
                super(text);
                maxHeight(500d);
                setWrapText(true);
                setStyle("-fx-text-fill:black;");

                if (isHeader) {
                    setStyle("-fx-font-weight:bold;");
                    //setFont(Font.font("System", FontWeight.BOLD, 12));
                }
            }
        }
    }

    /**
     * GETTERS AND SETTERS
     */
    Stage getMainStage() {
        return mainStage;
    }

    public void setMainStage(Stage mainProteomixStage) {
        this.mainStage = mainProteomixStage;
    }


    HostServices getHostServices() {
        return hostServices;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }


    public IoModel getModel() {
        return model;
    }

    public void setModel(IoModel model) {
        this.model = model;
    }

    public MainTablesController getMainTablesController() {
        return mainTablesController;
    }

    MainControllerNonStringent getMainControllerNonStringent() {
        return mainControllerNonStringent;
    }

    ToggleGroup getBiomarkerMenuGroup() {
        return biomarkerMenuGroup;
    }

    CheckMenuItem getUnclassifiedProteolysisMenuItem() {
        return unclassifiedProteolysisMenuItem;
    }

    boolean isBiomarkerAllMenuItemSelected() {
        return allMenuItem.isSelected();
    }

    boolean isBiomarkerBloodMenuItemSelected() {
        return bloodMenuItem.isSelected();
    }

    boolean isBiomarkerSalivaMenuItemSelected() {
        return salivaMenuItem.isSelected();
    }

    boolean isBiomarkerUrineMenuItemSelected() {
        return urineMenuItem.isSelected();
    }

    boolean isBiomarkerCustomMenuItemSelected() {
        return customMenuItem.isSelected();
    }

    public void addTmProt(TextMinedProtein tmProt) {
        this.model.addTextMinedProtein(tmProt);
    }

    String getLogContent() {
        return logWindow.allContent.toString();
    }

    void setNumberSearchThreads(int threadNr) {
        this.inputListController.setNumberSearchThreads(threadNr);
    }

    int getNumberSearchThreads() {
        return this.inputListController.getNumberSearchThreads();
    }

    void setMaxArticlesRetrieved(int value) {
        this.MAX_ARTICLES_RETRIEVED = value;
    }

    int getMaxArticlesRetrieved() {
        return this.MAX_ARTICLES_RETRIEVED;
    }
}
