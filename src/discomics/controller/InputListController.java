package discomics.controller;

import discomics.application.MyLogger;
import discomics.model.*;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Created by Jure on 4.9.2016.
 */
public class InputListController {
    private static final String CLASS_NAME = InputListController.class.getName();

    private static List<String> superCommonGenes = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/super_common_geneID.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static List<String> connectiveTissueContaminantGenes = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/connective_tissue_contaminant_geneID.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static List<String[]> hgncDatabaseFull = new ArrayList<String[]>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/db/hgnc_20171219_processed.txt");
            List<String> databaseLines = IOUtils.readLines(is);
            for (String databaseLine : databaseLines) {
                add(databaseLine.split("\\t"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private int NR_THREADS_EPMC = 2;
    private final int NR_THREADS_PUBMED = 1;

    private MainController mainController;
    private Stage inputListStage;

    @FXML
    private TextArea inputTextArea;

    private ToggleGroup proteaseRadioButtonGroup;
    @FXML
    private RadioButton mmpRadioButton;
    @FXML
    private RadioButton adamRadioButton;
    @FXML
    private RadioButton ctsRadioButton;

    @FXML
    private Button okButton;

    @FXML
    private CheckBox proteolysisCheckBox;
    @FXML
    private CheckBox biomarkerCheckBox;
    @FXML
    private CheckBox customCheckBox;
    @FXML
    private CheckBox pubmedCheckBox;
    @FXML
    private CheckBox filteringCheckBox;
    @FXML
    private CheckBox geneNameOnlyCheckBox;
    @FXML
    private CheckBox supplementPseudogenesCheckBox;

    private CustomTextMiningInput customTextMiningInputMenu;

    public void init(MainController mainController, Stage inputListStage) {
        this.mainController = mainController;
        this.inputListStage = inputListStage;

        this.inputListStage.initModality(Modality.APPLICATION_MODAL);
        this.customTextMiningInputMenu = new CustomTextMiningInput(inputListStage);

        proteaseRadioButtonGroup = new ToggleGroup();
        mmpRadioButton.setToggleGroup(proteaseRadioButtonGroup);
        adamRadioButton.setToggleGroup(proteaseRadioButtonGroup);
        ctsRadioButton.setToggleGroup(proteaseRadioButtonGroup);

        BooleanBinding continueButtonEnabledBinding = new BooleanBinding() {
            {
                super.bind(inputTextArea.textProperty(),
                        proteaseRadioButtonGroup.selectedToggleProperty(),
                        proteolysisCheckBox.selectedProperty(),
                        biomarkerCheckBox.selectedProperty(),
                        customCheckBox.selectedProperty());
            }

            @Override
            protected boolean computeValue() {
                if (inputTextArea.getText().isEmpty())
                    return true;
                else {
                    boolean proteolysisBlockPartlyComplete = proteolysisCheckBox.isSelected()
                            && !(adamRadioButton.isSelected() || ctsRadioButton.isSelected() || mmpRadioButton.isSelected());

                    if (proteolysisBlockPartlyComplete)
                        return true;

                    boolean proteolysisBlockComplete = proteolysisCheckBox.isSelected() &&
                            (adamRadioButton.isSelected() || ctsRadioButton.isSelected() || mmpRadioButton.isSelected());

                    // if any blocks are complete, the following is TRUE
                    boolean protBioCustBlockCompl = proteolysisBlockComplete || biomarkerCheckBox.isSelected() || customCheckBox.isSelected();
                    // FALSE retrurned only when database is selected AND blocks complete, so OK button disable property OFF
                    return !protBioCustBlockCompl;
                }
            }
        };

        okButton.disableProperty().bind(continueButtonEnabledBinding);

        BooleanBinding bb1 = new BooleanBinding() {
            {
                super.bind(proteolysisCheckBox.selectedProperty());
            }

            @Override
            protected boolean computeValue() {
                return !proteolysisCheckBox.isSelected();
            }
        };

        pubmedCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Alert alert = new MyAlert(Alert.AlertType.WARNING, this.inputListStage);
                alert.setTitle("Warning");
                alert.setHeaderText(null);
                alert.setContentText("Enabling this option will result in a serious decrease in performance, " +
                        "and might lead to reduced stability during peak usage times of Pubmed servers.");
                alert.showAndWait();
            }
        });

        adamRadioButton.disableProperty().bind(bb1);
        mmpRadioButton.disableProperty().bind(bb1);
        ctsRadioButton.disableProperty().bind(bb1);
    }


    @FXML
    public void okButtonAction() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME + " InputListOKButtonListener", methodName);

        ProteinCollection proteinCollection = new ProteinCollection();
        proteinCollection.processUserInput(inputTextArea.getText()); // process user input gene list

        final TextMiningTask<IoModel> textMiningTask = new TextMiningTask<IoModel>(mainController.getMainStage()) {

            @Override
            protected IoModel call() throws Exception {
                stopWatch.start();

                IoModel model = new IoModel();
                IoModel currentModel = mainController.getModel();

                // set some variables in the IoModel
                if (proteolysisCheckBox.isSelected()) {

                    if (mmpRadioButton.isSelected()) {
                        model.setProteaseFamilySelected(IoModel.getMMP());
                    }
                    if (adamRadioButton.isSelected())
                        model.setProteaseFamilySelected(IoModel.getADAM());

                    if (ctsRadioButton.isSelected())
                        model.setProteaseFamilySelected(IoModel.getCTS());
                }

                model.setProteinCollection(proteinCollection);

                QuerySettings querySettings = new QuerySettings(proteolysisCheckBox.isSelected(), biomarkerCheckBox.isSelected(),
                        customCheckBox.isSelected(), filteringCheckBox.isSelected(), false, geneNameOnlyCheckBox.isSelected(),
                        supplementPseudogenesCheckBox.isSelected(), pubmedCheckBox.isSelected());

                model.setQuerySettings(querySettings);

                if (customCheckBox.isSelected()) {
                    model.setCustomSearchTerms(customTextMiningInputMenu.retrieveSearchTerms());
                }

                // search protein objects in local database
//                for (String inputGene: model.getProteinCollection().getInputProteinSet()) {
//                    Protein protein = retrieveProteinHgncDatabaseLocal(inputGene);
//                    if (protein != null) {
//                        proteinCollection.addProtein(protein);
//                        proteinCollection.removeInputProtein(inputGene);
//                    }
//                }

                setProgressStep(0); // set progress to first step (protein info downloading)

                ExecutorService executorService = Executors.newFixedThreadPool(NR_THREADS_EPMC);
                final ArrayList<Future<Protein>> outputProteinFutures = new ArrayList<>();

                // if some search has already been conducted in the current session, carry over Protein information for
                // common proteins
                final ArrayList<Protein> carryOverFromCurrentModel = new ArrayList<>();
                for (String inputGeneName : model.getProteinCollection().getInputProteinSet()) {
                    for (Protein protein : currentModel.getProteinCollection().getOutputProteinList()) {
                        if (protein.getMainName().equalsIgnoreCase(inputGeneName)) {
                            if (model.getProteinCollection().removeInputProtein(inputGeneName)) // if removed successfully
                                carryOverFromCurrentModel.add(protein);
                        }
                    }
                }

                // assign tasks to thread pool and run tasks: protein information retrieval
                // query gene information for input genes that have not been removed due to protein being present in previous search set
                for (String protein : model.getProteinCollection().getInputProteinSet()) {
                    Thread.sleep(200);
                    Future<Protein> outputProtein = executorService.submit(() -> new Protein(protein));
                    outputProteinFutures.add(outputProtein);
                }

                // after tasks finish, collect the Protein objects, and add them to ProteinCollection object
                for (Future<Protein> proteinFuture : outputProteinFutures) {
                    if (isCancelled()) {
                        return null;
                    }

                    Protein protein = proteinFuture.get();
                    model.getProteinCollection().addProtein(protein);
                }

                executorService.shutdown();
                executorService.awaitTermination(30, TimeUnit.MINUTES);

                model.getProteinCollection().addProteins(carryOverFromCurrentModel); // add all proteins that have been carried over from previous search
                model.getProteinCollection().postprocessProteinList(querySettings); // supplement pseudogenes with parental if user selects respective check box
                model.getProteinCollection().buildInteractionNetwork(); // build interaction network of input proteins only (for centrality scores in SummaryScoreController


                setProgressStep(1); // completed protein mining, continue to text-mining step

                // set up executor services to start textmining on ePmc and Pubmed
                ExecutorService executorService1 = Executors.newFixedThreadPool(NR_THREADS_EPMC);
                final List<Future<TextMinedProtein>> tmProtFutures = new ArrayList<>();

                for (Protein protein : model.getProteinCollection().getOutputProteinList()) { // loop through Protein list previously queried and construct TextMinedProtein objects
                    Thread.sleep(400);

                    if (!customCheckBox.isSelected()) {
                        Future<TextMinedProtein> artCollFuture = executorService1.submit(() -> {
                            TextMinedProtein tmProt = new TextMinedProtein(protein, querySettings);

                            tmProt.queryEPmc();
                            tmProt.retrievePhysicalProteaseInteractions();
                            return tmProt;
                        });
                        tmProtFutures.add(artCollFuture);
                    } else {
                        Future<TextMinedProtein> artCollFuture = executorService1.submit(() -> {
                            TextMinedProtein tmProt = new TextMinedProtein(protein, model.getCustomSearchTerms(), querySettings);

                            tmProt.queryEPmc();
                            tmProt.retrievePhysicalProteaseInteractions();
                            return tmProt;
                        });
                        tmProtFutures.add(artCollFuture);
                    }
                }

                for (Future<TextMinedProtein> tmProtFut : tmProtFutures) {
                    if (isCancelled()) // check if task is cancelled, return from method and pass NULL, fail the search
                        return null;

                    try {
                        model.addTextMinedProtein(tmProtFut.get());
                    } catch (Exception e) {
                        System.err.print("Exception occurred with: " + tmProtFut.get().getTextMinableInput().getQueryInputGene());
                        throw e;
                    }
                }

                executorService1.shutdown();
                executorService1.awaitTermination(30, TimeUnit.MINUTES);

                if (pubmedCheckBox.isSelected()) {
                    ExecutorService executorService3 = Executors.newFixedThreadPool(NR_THREADS_PUBMED);

                    for (TextMinedObject tmProt : model.getTextMinedProteins()) {
                        executorService3.submit((Callable<Void>) () -> {
                            tmProt.queryPubmed();
                            return null;
                        });
                    }

                    executorService3.shutdown();
                    executorService3.awaitTermination(30, TimeUnit.MINUTES);
                }

                // finalise post-query (surplus article compression, definition of proteases and biomarkers mentioned in abstracts
                for (TextMinedObject tmProt : model.getTextMinedProteins()) {
                    tmProt.finalisePostQuery(mainController.getMaxArticlesRetrieved());
                }


                setProgressStep(2); // proceed to gene family text-mining step

                // START GENE FAMILY TEXT MINING
                ExecutorService executorService4 = Executors.newFixedThreadPool(NR_THREADS_EPMC);
                Set<GeneFamily> allGeneFamilies = new HashSet<>();

                for (Protein protein : model.getProteinCollection().getOutputProteinList()) {
                    GeneFamily geneFamily = protein.getGeneFamily();
                    if (geneFamily != null)
                        allGeneFamilies.add(geneFamily);
                }

                final List<Future<TextMinedGeneFamily>> tmProtFuturesGeneFamily = new ArrayList<>();

                for (GeneFamily geneFamily : allGeneFamilies) { // loop through Protein list previously queried and construct TextMinedProtein objects
                    Thread.sleep(400);

                    if (!customCheckBox.isSelected()) {
                        Future<TextMinedGeneFamily> artCollFuture = executorService4.submit(() -> {
                            TextMinedGeneFamily tmProt = new TextMinedGeneFamily(geneFamily, querySettings);
                            tmProt.queryEPmc();
                            return tmProt;
                        });
                        tmProtFuturesGeneFamily.add(artCollFuture);
                    } else {
                        Future<TextMinedGeneFamily> artCollFuture = executorService4.submit(() -> {
                            TextMinedGeneFamily tmProt = new TextMinedGeneFamily(geneFamily, model.getCustomSearchTerms(), querySettings);
                            tmProt.queryEPmc();
                            return tmProt;
                        });
                        tmProtFuturesGeneFamily.add(artCollFuture);
                    }
                }

                for (Future<TextMinedGeneFamily> tmGeneFamFut : tmProtFuturesGeneFamily) {
                    if (isCancelled()) // check if task is cancelled, return from method and pass NULL, fail the search
                        return null;

                    try {
                        model.addTextMinedGeneFamily(tmGeneFamFut.get());
                    } catch (Exception e) {
                        System.err.print("Exception occurred with: " + tmGeneFamFut.get().getTextMinableInput().getMainName());
                        throw e;
                    }
                }

                executorService4.shutdown();
                executorService4.awaitTermination(30, TimeUnit.MINUTES);

                if (pubmedCheckBox.isSelected()) {
                    ExecutorService executorService5 = Executors.newFixedThreadPool(NR_THREADS_PUBMED);

                    for (TextMinedObject tmGeneFamily : model.getTextMinedGeneFamilies()) {
                        executorService5.submit((Callable<Void>) () -> {
                            tmGeneFamily.queryPubmed();
                            return null;
                        });
                    }

                    executorService5.shutdown();
                    executorService5.awaitTermination(30, TimeUnit.MINUTES);
                }

                // finalise post-query (surplus article compression, definition of proteases and biomarkers mentioned in abstracts
                for (TextMinedObject tmGeneFamily : model.getTextMinedGeneFamilies())
                    tmGeneFamily.finalisePostQuery(mainController.getMaxArticlesRetrieved());


                setProgressStep(3); // finished text-mining, start protein-protein network retrieval step

                // START NETWORK QUERY
                model.constructAllPpis(); // construct all Protein Protein interaction networks

                setProgressStep(4); // finished

                stopWatch.stop();
                return model;
            }
        };

        textMiningTask.setOnSucceeded(event -> {
            try {
                IoModel model = textMiningTask.get();
                if (model != null) {
                    mainController.setModel(null); // clear previous model
                    textMiningTask.onSucceeded(model.getProteinCollection().getProteinsFailedIdentify()); // defined in TextMiningTask class, closes progress dialog, shows result information, errors during task
                    mainController.setModel(model); // set the model with what was built in input task and overwrite previous data

                    // Initialise all application views (main, network, summary)
                    mainController.initialiseApplicationViews();
                }
            } catch (Exception e) {
                ExceptionDialog exceptionDialog = new ExceptionDialog(e, mainController.getMainStage());
                exceptionDialog.showAndWait();
            }
        });

        // initialise progress dialog
        MyProgressDialog myProgressDialog = new MyMainTaskProgressDialog(mainController.getMainStage(), textMiningTask, "Downloading data ...");
        textMiningTask.setProgressDialog(myProgressDialog);

        // input check 1: genes contianing non-alphanumeric characters
        List<String> wrongInputStructureGenes = checkInputGeneStructure(proteinCollection.getInputProteinSet());

        if (wrongInputStructureGenes.size() > 0) {
            Alert alert = new MyAlert(Alert.AlertType.WARNING, this.inputListStage);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            String msgRoot = "The following genes contain illegal non-alphanumeric characters.\n\n";
            StringBuilder sb = new StringBuilder();
            sb.append(msgRoot);
            for (int i = 0; i < wrongInputStructureGenes.size(); i++) {
                String wrongInput = wrongInputStructureGenes.get(i);
                sb.append(wrongInput);
                if (i != (wrongInputStructureGenes.size() - 1))
                    sb.append("\n");
            }
            alert.setContentText(sb.toString());

            ButtonType buttonTypeOne = new ButtonType("Remove Genes");
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == buttonTypeOne) { // if button type two (not present) then do nothing, so proceed to end of method where search is initiated
                    for (String wrongGene : wrongInputStructureGenes) {
                        proteinCollection.removeInputProtein(wrongGene);
                    }
                } else if (result.get() == buttonTypeCancel) {
                    return; // return from method without running the search
                }
            } else {
                return; // return without running search
            }
        }

        // input check 2; contamination, super common plasma genes


        List<String> superCommonGenes = checkPlasmaGenes(proteinCollection.getInputProteinSet());


        if (superCommonGenes.size() > 0) {
            String alertMessage = "The following entered genes are among the most abundant constituents of human plasma. " +
                    "Their presence in the query set might skew the results.\n\n";
            MyAlertContamination alert = new MyAlertContamination(Alert.AlertType.INFORMATION, this.inputListStage, superCommonGenes, alertMessage);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == alert.getRemoveGeneButton()) { // if 'KEEP' button pressed then do nothing, so proceed to end of method where search is initiated
                    for (String superCommonGene : superCommonGenes) {
                        proteinCollection.removeInputProtein(superCommonGene);
                    }
                } else if (result.get() == alert.getCancelButton()) {
                    return; // return from method without running the search
                }
            } else {
                return; // return without running search
            }
        }

        // input check 3; contamination, connective tissue keratins
        List<String> connectiveContaminantGenes = checkConnectiveContaminants(proteinCollection.getInputProteinSet());

        if (connectiveContaminantGenes.size() > 0) {
            String alertMessage2 = "The following entered genes may indicate sample contamination with connective tissue.\n\n";
            MyAlertContamination alert2 = new MyAlertContamination(Alert.AlertType.INFORMATION, this.inputListStage, connectiveContaminantGenes, alertMessage2);
            Optional<ButtonType> result = alert2.showAndWait();

            if (result.isPresent()) {
                if (result.get() == alert2.getRemoveGeneButton()) { // if button type two (not present) then do nothing, so proceed to end of method where search is initiated
                    for (String wrongGene : wrongInputStructureGenes) {
                        proteinCollection.removeInputProtein(wrongGene);
                    }
                } else if (result.get() == alert2.getCancelButton()) {
                    return; // return from method without running the search
                }
            } else {
                return; // return without running search
            }
        }

        // submit job
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(textMiningTask);

        this.inputListStage.hide();

        MyLogger.exiting(CLASS_NAME, methodName);
    }

    private Protein retrieveProteinHgncDatabaseLocal(String inputGene) throws SocketException {
        for (String[] databaseEntry : hgncDatabaseFull) {
            if (databaseEntry[4].equalsIgnoreCase(inputGene)) //  databaseEntry[4] is the approved gene name
                return new Protein(databaseEntry);
        }
        return null;
    }

    @FXML
    private void resetButtonAction() {
        inputTextArea.clear();
        proteolysisCheckBox.setSelected(false);
        biomarkerCheckBox.setSelected(false);
        customCheckBox.setSelected(false);

        customTextMiningInputMenu = new CustomTextMiningInput(this.inputListStage);

        adamRadioButton.setSelected(false);
        ctsRadioButton.setSelected(false);
        mmpRadioButton.setSelected(false);
        pubmedCheckBox.setSelected(false);

        filteringCheckBox.setSelected(false);
        pubmedCheckBox.setSelected(false);
        geneNameOnlyCheckBox.setSelected(false);
        supplementPseudogenesCheckBox.setSelected(false);
    }

    @FXML
    private void customCheckBoxAction() {
        if (customCheckBox.isSelected()) {
            // showStage and set dialog position at centre of input dialog
            customTextMiningInputMenu.showCustomInputDialog();

            double positionX = inputListStage.getX() + (inputListStage.getWidth() - customTextMiningInputMenu.dialogScene.getWidth()) / 2;
            double positionY = inputListStage.getY() + (inputListStage.getHeight() - customTextMiningInputMenu.dialogScene.getHeight()) / 2;

            customTextMiningInputMenu.dialog.setX(positionX);
            customTextMiningInputMenu.dialog.setY(positionY);
        }
    }

    // returns all genes that do not have correct input structure (only alphanumeric characters are allowed)
    private List<String> checkInputGeneStructure(Set<String> inputGenes) {
        List<String> output = new ArrayList<>();
        String regexPunctuation = "^[a-zA-Z0-9\\-]*$";
        Pattern regexPattern = Pattern.compile(regexPunctuation);

        for (String inputGene : inputGenes) {
            if (!regexPattern.matcher(inputGene).find()) {
                output.add(inputGene);
            }
        }

        return output;
    }

    private List<String> checkPlasmaGenes(Set<String> inputGenes) {
        return findInGeneList(inputGenes, superCommonGenes);
    }

    private List<String> checkConnectiveContaminants(Set<String> inputGenes) {
        return findInGeneList(inputGenes, connectiveTissueContaminantGenes);
    }

    private List<String> findInGeneList(Set<String> parentList, List<String> toBeSearched) {
        List<String> output = new ArrayList<>(); // all genes in input list that are super common
        for (String inputGene : parentList) {
            if (toBeSearched.contains(inputGene)) {
                output.add(inputGene);
            }
        }
        return output;
    }

    @FXML
    private void proteolysisCheckBoxAction() {
        if (!proteolysisCheckBox.isSelected()) {
            adamRadioButton.setSelected(false);
            mmpRadioButton.setSelected(false);
            ctsRadioButton.setSelected(false);
        }
    }

    void setNumberSearchThreads(int threadNr) {
        this.NR_THREADS_EPMC = threadNr;
    }

    int getNumberSearchThreads() {
        return this.NR_THREADS_EPMC;
    }

    void showStage(boolean show) {
        if (show) {
            inputListStage.show();

            double positionX = mainController.getMainStage().getX() + (mainController.getMainStage().getWidth() - inputListStage.getWidth()) / 2;
            double positionY = mainController.getMainStage().getY() + (mainController.getMainStage().getHeight() - inputListStage.getHeight()) / 2;
            inputListStage.setX(positionX);
            inputListStage.setY(positionY);

            inputListStage.requestFocus();
        } else
            inputListStage.hide();
    }


    private class CustomTextMiningInput {
        private final Stage dialog = new Stage();
        private final Scene dialogScene;

        private VBox container = new VBox();
        private ArrayList<InputBlock> inputBlocks;

        private Button addBlock;
        private Button okButton;
        private Button resetButton;

        private CustomTextMiningInput(Stage owner) {
            constructNewDialog();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(owner);
            dialogScene = new MyScene(container);
            dialog.setScene(dialogScene);
            dialog.setResizable(false);

            final KeyCombination okKey = new KeyCodeCombination(KeyCode.ENTER);
            dialogScene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                if (okKey.match(event))
                    okButton.fire();
            });
        }

        private void constructNewDialog() {
            container = new VBox();
            inputBlocks = new ArrayList<>();

            addBlock = new Button("Add Block");
            okButton = new Button("OK");
            resetButton = new Button("Reset Form");

            container.setSpacing(5d);
            container.setAlignment(Pos.CENTER);
            container.setPadding(new Insets(15));

            Label title = new Label("Enter search terms for your custom text mining query");
            title.setStyle("-fx-font-size: 14;");
            container.getChildren().add(title);

            InputBlock inputBlock1 = new InputBlock(true);
            inputBlocks.add(inputBlock1);
            container.getChildren().add(inputBlock1);

            final Separator separatorFinal = new Separator(Orientation.HORIZONTAL);
            container.getChildren().add(separatorFinal);

            addBlock.setPrefWidth(100d);
            resetButton.setPrefWidth(100d);
            okButton.setPrefWidth(100d);
            container.getChildren().add(addBlock);
            container.getChildren().add(resetButton);
            container.getChildren().add(okButton);

            addBlock.setOnAction(event -> {
                InputBlock inputBlock = new InputBlock(false);
                inputBlocks.add(inputBlock);
                container.getChildren().add(container.getChildren().indexOf(separatorFinal), inputBlock);
                dialog.sizeToScene();
                inputBlock.inputBoxes.get(0).inputTextField.requestFocus();
            });

            resetButton.setOnAction(event -> {
                constructNewDialog();
                Scene dialogScene = new MyScene(container);
                dialog.setScene(dialogScene);
                dialog.sizeToScene();
            });

            okButton.setOnAction(event -> dialog.hide());

            dialog.setOnHiding(event -> {
                // some input control; don't allow for very short search terms
                List<CustomInputTermBlock> userInputTermBlocks = retrieveSearchTerms();
                if (userInputTermBlocks.isEmpty()) {
                    customCheckBox.setSelected(false);
                    return;
                }
                for (CustomInputTermBlock customInputTermBlock : userInputTermBlocks) {
                    for (CustomInputTermBlock.CustomInputTerm term : customInputTermBlock.getCustomInputTerms()) {
                        if (term.getSearchTerm().length() < 3) {
                            customCheckBox.setSelected(false);
                            Alert alert = new MyAlert(Alert.AlertType.INFORMATION, this.dialog);
                            alert.getButtonTypes().remove(ButtonType.CANCEL);
                            alert.setContentText("Do not enter search terms 3 letters or shorter.");
                            alert.setHeaderText("");
                            alert.showAndWait();
                            return;
                        }
                    }
                }
            });

        }

        void showCustomInputDialog() {
            dialog.show();
            //dialog.sizeToScene();

            double positionX = inputListStage.getX() + (inputListStage.getWidth() - dialog.getWidth()) / 2;
            double positionY = inputListStage.getY() + (inputListStage.getHeight() - dialog.getHeight()) / 2;
            dialog.setX(positionX);
            dialog.setY(positionY);

        }

        List<CustomInputTermBlock> retrieveSearchTerms() {
            List<CustomInputTermBlock> output = new ArrayList<>();

            for (InputBlock inputBlock : inputBlocks) { // loop through all inputBlocks
                CustomInputTermBlock customInputTermBlock = new CustomInputTermBlock();
                for (InputBlock.InputBox inputBox : inputBlock.retrieveSearchTerms()) { // loop through inputBoxes
                    String term = inputBox.inputTextField.getText().trim();
                    if (!term.isEmpty())
                        customInputTermBlock.addInputTerm(term, inputBox.isPhraseCheckBox.isSelected());
                }
                if (!customInputTermBlock.getCustomInputTerms().isEmpty())
                    output.add(customInputTermBlock);
            }
            return output;
        }

        private class InputBlock extends VBox {
            ObservableList<InputBox> inputBoxes = FXCollections.observableArrayList();

            Button addTermButton = new Button("Add term");
            Button removeBlockButton = new Button("Remove block");

            private InputBlock(boolean isFirst) {
                super();
                setSpacing(5d);
                setAlignment(Pos.CENTER);
                getChildren().add(new Separator(Orientation.HORIZONTAL)); // add separator below previous block

                InputBox inputBox1 = new InputBox(this); // input box holds textfield and checkbox
                inputBoxes.add(inputBox1); // add to arraylist

                TilePane tileButtons = new TilePane(Orientation.HORIZONTAL); // button TilePane
                tileButtons.setHgap(10d);
                tileButtons.setPrefRows(1);
                tileButtons.setPrefColumns(2);
                addTermButton.setMaxWidth(Double.MAX_VALUE); // allow buttons to grow to size of largest within tilePane
                removeBlockButton.setMaxWidth(Double.MAX_VALUE);
                tileButtons.getChildren().add(addTermButton); // add buttons

                getChildren().addAll(inputBox1, tileButtons); // add to scene

                addTermButton.setOnAction(event -> { // add term button listener
                    InputBox inputBox = new InputBox(this);
                    inputBoxes.add(inputBox); // add input box to ArrayList
                    getChildren().add(getChildren().size() - 1, inputBox);
                    dialog.sizeToScene();
                    inputBox.inputTextField.requestFocus();
                });

                if (!isFirst) {
                    tileButtons.getChildren().add(removeBlockButton);

                    removeBlockButton.setOnAction(event -> {
                        inputBlocks.remove(this); // remove from arraylist
                        container.getChildren().remove(this); // remove from stage view
                        dialog.sizeToScene();
                    });
                }

                tileButtons.setPrefWidth(this.getPrefWidth());
                tileButtons.setMaxWidth(this.getPrefWidth());
                tileButtons.setAlignment(Pos.CENTER);
            }

            List<InputBox> retrieveSearchTerms() {
                return new ArrayList<>(inputBoxes);
            }

            private class InputBox extends HBox {
                private TextField inputTextField = new TextField();
                private CheckBox isPhraseCheckBox = new CheckBox("Phrase");
                private Button deleteFieldButton = new Button("X");

                private InputBox(final InputBlock parent) {
                    super(10d);
                    this.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(inputTextField, Priority.ALWAYS);
                    this.getChildren().addAll(inputTextField, isPhraseCheckBox, deleteFieldButton);

                    deleteFieldButton.setOnMouseClicked(event -> {
                        parent.getChildren().remove(this);
                        dialog.sizeToScene();
                    });

                    inputTextField.setOnKeyReleased(event -> {
                        String currentText = inputTextField.getText().trim();

                        if (currentText.contains("*") && currentText.contains(" ")) { // wildcard goes only with single word
                            inputTextField.setStyle("-fx-text-box-border: red; " +
                                    "-fx-focus-color: red;");
                        } else if (currentText.contains("*")) {
                            isPhraseCheckBox.setDisable(true);
                            isPhraseCheckBox.setSelected(false);
                            inputTextField.setStyle(null); // reset style
                        } else {
                            isPhraseCheckBox.setDisable(false);
                            inputTextField.setStyle(null); // reset style
                        }
                    });
                }
            }
        }
    }
}
