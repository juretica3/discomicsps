package discomics.controller;

import discomics.application.AlphanumComparator;
import discomics.model.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Jure on 4.9.2016.
 */
public class NetworkController {

    private MainController mainController;
    private Stage networkStage;

    private ProteinInteractionNetwork fullNetwork;
    private ProteinInteractionNetwork currentlyDisplayedNetwork;

    @FXML
    private ProteinTableController proteinTableController;
    @FXML
    private ProteaseTableController proteaseTableController;

    @FXML
    private Button saveImageButton;
    @FXML
    private Button saveTsvButton;
    @FXML
    private Button homeButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button showPpiListButton;

    @FXML
    private SplitPane mainSplitPane;

    private Node ppiListPane;

    private final ObservableList<NetworkEdge> ppiTableData = FXCollections.observableArrayList();
    @FXML
    private TableView<NetworkEdge> ppiTable;
    @FXML
    private TableColumn<NetworkEdge, String> protein1;
    @FXML
    private TableColumn<NetworkEdge, String> protein2;
    @FXML
    private TableColumn<NetworkEdge, String> score;


    private final ObservableList<NetworkNodeProtein> nodesTableData = FXCollections.observableArrayList();
    @FXML
    private TableView<NetworkNodeProtein> nodesTable;
    @FXML
    private TableColumn<NetworkNodeProtein, String> nodeCol;
    @FXML
    private TableColumn<NetworkNodeProtein, String> scoreCol;

    @FXML
    private ImageView networkImage;
    @FXML
    private ScrollPane imageContainer;
    @FXML
    private SplitPane proteinProteaseTableSplitPane;

    private Node proteaseTableNode;

    private FileChooser saveImageFileChooser = new FileChooser();
    private FileChooser saveTsvFileChooser = new FileChooser();

    private int networkMode;
    // 0: proteolysis
    // 1: biomarker
    // 2: custom
    // 3: drug interaction net

    public void init(MainController mainController, Stage networkStage) {
        this.mainController = mainController;
        this.networkStage = networkStage;
        this.ppiListPane = mainSplitPane.getItems().get(2);

        proteaseTableController.init(this.mainController);
        proteinTableController.init(this.mainController);

        // initialise Nodes Table
        nodeCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        scoreCol.setCellValueFactory(param -> {
            String formattedValue = new DecimalFormat("#.##").format(param.getValue().getScore());
            return new ReadOnlyStringWrapper(formattedValue);
        });

        nodeCol.setComparator(new AlphanumComparator());
        scoreCol.setComparator(new AlphanumComparator());
        nodesTable.setItems(nodesTableData);

        // initialise Interaction Table
        protein1.setCellValueFactory(new PropertyValueFactory<>("node1"));
        protein2.setCellValueFactory(new PropertyValueFactory<>("node2"));
        score.setCellValueFactory(new PropertyValueFactory<>("score"));

        protein1.setComparator(new AlphanumComparator());
        protein2.setComparator(new AlphanumComparator());

        ppiTable.setItems(ppiTableData);

        proteaseTableNode = proteinProteaseTableSplitPane.getItems().get(1);
        proteinProteaseTableSplitPane.getItems().removeAll(proteaseTableNode);

        saveImageFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image file (*.png)", "*.png"));
        saveImageFileChooser.setTitle("Save Network Image As");
        saveTsvFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file (*.txt)", "*.txt"));
        saveTsvFileChooser.setTitle("Save Network Data As");

        refreshButton.disableProperty().bind(
                Bindings.isEmpty(proteinTableController.getTable().getSelectionModel().getSelectedItems())
        );

        BooleanBinding bb1 = new BooleanBinding() {
            {
                super.bind(networkImage.imageProperty());
            }

            @Override
            protected boolean computeValue() {
                return currentlyDisplayedNetwork == null || currentlyDisplayedNetwork.getNetworkImage() == null;
            }
        };
        saveImageButton.disableProperty().bind(bb1);

        double SCALE_DELTA = 1.1;
        networkImage.setOnScroll(event -> {

            if (event.isControlDown() || event.isMetaDown()) {
                event.consume();

                if (event.getDeltaY() == 0) {
                    return;
                }

                double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
                double newWidth = networkImage.getFitWidth() * scaleFactor;
                double newHeight = networkImage.getFitHeight() * scaleFactor;
                double containerWidth = imageContainer.getWidth();
                double containerHeight = imageContainer.getHeight();

                if ((newWidth > containerWidth) || (newHeight > containerHeight)) {
                    imageContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                    imageContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
                    networkImage.setFitHeight(newHeight);
                    networkImage.setFitWidth(newWidth);
                } else {
                    imageContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    imageContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    networkImage.setFitHeight(containerHeight);
                    networkImage.setFitWidth(containerWidth);
                }
            }
        });

        TableControllable<NetworkNodeProtein> tableControllableNode = new TableControllable<NetworkNodeProtein>() {};
        tableControllableNode.setKeyNavigationListeners(nodesTable, 0);

        TableControllable<NetworkEdge> tableControllableEdge = new TableControllable<NetworkEdge>() {};
        tableControllableEdge.setKeyNavigationListeners(ppiTable, 0);
    }

    @FXML
    private void homeButtonActionListener() {
        updateNetworkView(this.fullNetwork);
        // when flag == -1 the protein table is left unchanged
    }

    @FXML
    private void showPpiListButtonActionListener() {
        if (mainSplitPane.getItems().contains(ppiListPane)) {
            mainSplitPane.getItems().remove(ppiListPane);
        } else {
            mainSplitPane.getItems().add(2, ppiListPane);
            mainSplitPane.setDividerPositions(0.15, 0.8);
        }
    }

    void initialiseNetworkView(ProteinInteractionNetwork network, List<TextMinedObject> proteinList, int flag) {
        this.fullNetwork = network;
        this.networkMode = flag;

        this.proteinTableController.getTableModel().clearAndUpdateTable(proteinList);

        if (flag == 0) { // proteolysis network
            proteinTableController.showProteolysisColumn();
            proteaseTableController.initialiseTableAllEntries();
            hideProteaseTable(false);
        } else if (flag == 1) { // biomarker network
            proteinTableController.showBiomarkerColumn();
            hideProteaseTable(true);
        } else if (flag == 2) { // custom network
            proteinTableController.showCustomColumn();
            hideProteaseTable(true);
        } else if (flag == 3) { // drug interaction network
            proteinTableController.showDrugColumn();
            hideProteaseTable(true);
        }
        proteinTableController.getTable().getSelectionModel().clearSelection();
        proteaseTableController.getTable().getSelectionModel().clearSelection();

        updateNetworkView(network);
    }

    private void updateNetworkView(ProteinInteractionNetwork network) {
        ProteinInteractionNetwork pastNetwork = this.currentlyDisplayedNetwork;
        try {
            this.currentlyDisplayedNetwork = network;
            Image image = network.getNetworkImage();
            setNetworkImage(image);
            setPpiTableRows(network.getProteinProteinInteractionList());
            setNodesTableRows(network.getNetworkNodes());
        } catch (NullPointerException e) { // error disaplying new network
            this.currentlyDisplayedNetwork = pastNetwork;
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setTitle("Error");
            alert.setContentText("Network could not be generated.");
            alert.show();
            e.printStackTrace();
        }
    }

    private void hideProteaseTable(boolean hide) {
        if (proteinProteaseTableSplitPane.getItems().contains(proteaseTableNode)) {
            if (hide)
                proteinProteaseTableSplitPane.getItems().remove(proteaseTableNode);
        } else {
            if (!hide) {
                proteinProteaseTableSplitPane.getItems().add(proteaseTableNode);
                proteinProteaseTableSplitPane.setDividerPositions(0.5);
            }
        }
    }

    @FXML
    private void refreshButtonActionListener() {
        final Alert alert = new MyAlert(Alert.AlertType.INFORMATION, this.networkStage);

        Task<ProteinInteractionNetwork> task = new Task<ProteinInteractionNetwork>() {

            @Override
            protected ProteinInteractionNetwork call() throws Exception {
                List<TextMinedObject> selectedRowsProtein = proteinTableController.getTable().getSelectionModel().getSelectedItems();
                List<ProteaseCount> selectedRowsProtease = proteaseTableController.getTable().getSelectionModel().getSelectedItems();

                List<Protein> selectedProteinObjects = new ArrayList<>();
                selectedProteinObjects.addAll(selectedRowsProtein.stream().map(textMinedObject -> (Protein) textMinedObject.getTextMinableInput()).collect(Collectors.toList())); // TODO null pointer thrown unpredictably

                List<Protein> selectedProteaseObjects = new ArrayList<>();

                // if no protease selected, include all proteases comentioned with selected proteins
                if (networkMode == 0) { // only include proteases if network mode is proteolysis
                    if (selectedRowsProtease.isEmpty()) {
                        List<ProteaseCount> proteaseCounts = mainController.getModel().getProteaseCountTableEntries(selectedRowsProtein);
                        selectedProteaseObjects.addAll(proteaseCounts.stream().map(ProteaseCount::getProtease).collect(Collectors.toList()));
                    } else {
                        selectedProteaseObjects.addAll(selectedRowsProtease.stream().map(ProteaseCount::getProtease).collect(Collectors.toList()));
                    }
                }
                ProteinInteractionNetwork output;

                if (networkMode != 3) { // if not drug interaction network
                    List<Protein> allSelectedObjects = new ArrayList<>();
                    allSelectedObjects.addAll(selectedProteinObjects);
                    allSelectedObjects.addAll(selectedProteaseObjects);
                    output = new ProteinInteractionNetwork(allSelectedObjects);
                    output.build();
                } else { // if drug interaction network
                    List<Drug> drugs = new ArrayList<>();
                    selectedProteinObjects.forEach(protein -> drugs.addAll(protein.getDrugs()));
                    if (drugs.size() == 0) {
                        output = null;
                    } else {
                        output = new ProteinDrugInteractionNetwork(selectedProteinObjects);
                        output.build();
                    }
                }
                return output;
            }
        };

        task.setOnRunning(event -> {
            alert.setHeaderText("Downloading ...");
            alert.setContentText("Please wait.");
            alert.getButtonTypes().clear();
            alert.show();
        });

        task.setOnSucceeded(event -> {
            try {
                alert.setResult(ButtonType.CANCEL);
                alert.close();
                ProteinInteractionNetwork network = task.get();
                if (network != null) {
                    currentlyDisplayedNetwork = network;
                    updateNetworkView(currentlyDisplayedNetwork);
                } else {
                    Alert alert1 = new MyAlert(Alert.AlertType.INFORMATION, this.networkStage);
                    alert1.setContentText("No drug interactions found for input proteins.");
                    alert1.setHeaderText("");
                    alert1.show();
                }
            } catch (InterruptedException | ExecutionException e) {
                ExceptionDialog exceptionDialog = new ExceptionDialog(e, this.networkStage);
                exceptionDialog.showAndWait();
            }
        });

        task.setOnCancelled(event -> {
            alert.setResult(ButtonType.CANCEL);
            alert.close();
        });

        task.setOnFailed(event -> {
            alert.setResult(ButtonType.CANCEL);
            alert.close();
            if (task.getException() instanceof SocketException) {
                Alert alert1 = new MyAlert(Alert.AlertType.ERROR, this.networkStage);
                alert1.setHeaderText("Download Unsuccessful");
                alert1.setContentText("Download has not been successful. Check internet connectivity.");
                alert1.showAndWait();
            } else {
                ExceptionDialog exceptionDialog = new ExceptionDialog(task.getException(), this.networkStage);
                exceptionDialog.showAndWait();
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
    }

    @FXML
    private void stringButtonListener() {
        final StringBuilder sb = new StringBuilder();
        sb.append("http://string-db.org/newstring_cgi/show_network_section.pl?");
        sb.append("limit=0");
        sb.append("&targetmode=proteins");
        sb.append("&species=9606");
        sb.append("&caller_identity=discomics");
        sb.append("&identifiers=");

        for (int i = 0; i < currentlyDisplayedNetwork.getStringIdList().size(); i++) {
            sb.append(currentlyDisplayedNetwork.getStringIdList().get(i));
            if (i != currentlyDisplayedNetwork.getStringIdList().size() - 1)
                sb.append("%0d%0a");
        }

        sb.append("&required_score=");

        if (sb.length() + 4 > 2000) {
            StringBuilder sb1 = new StringBuilder();
            for (String s : currentlyDisplayedNetwork.getStringIdList()) {
                sb1.append(s).append("\n");
            }

            Alert alert = new MyAlert(Alert.AlertType.INFORMATION, this.networkStage);
            alert.setTitle("Info Dialog");
            alert.setHeaderText("Paste clipboard contents into STRING manually.");
            alert.setContentText("Too many proteins were selected, making the URL to STRING too long. The STRING" +
                    "webpage will open after you click OK. The selected genes were copied to your clipboard; simply paste" +
                    "them into STRING.");
            alert.showAndWait();

            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(sb1.toString());
            clipboard.setContent(content);

            mainController.getHostServices().showDocument("http://string-db.org/cgi/input.pl?UserId=OaqIPeuMcSm5&sessionId=5yWy3UquRVBu&input_page_active_form=multiple_identifiers");
        } else {
            NetworkDbLinkPopup popup = new NetworkDbLinkPopup() {
                @Override
                void okButtonClick() {
                    sb.append(getSpinnerThreshold() * 1000);
                    mainController.getHostServices().showDocument(sb.toString());
                    close();
                }
            };
            popup.show();
        }
    }

//    @FXML
//    private void geneManiaButtonListener() {
//
//        StringBuilder sb1 = new StringBuilder();
//        for (String s : currentlyDisplayedNetwork.getStringIdList()) {
//            sb1.append(s.getMainName()).append("\n");
//        }
//        if (sb1.toString().length() > 1900) {
//            Alert alert = new MyAlert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Info Dialog");
//            alert.setHeaderText("Paste clipboard contents into GeneMANIA manually.");
//            alert.setContentText("Too many proteins were selected, making the URL to GeneMANIA too long. The GeneMANIA" +
//                    "webpage will open after you click OK. The selected genes were copied to your clipboard; simply paste" +
//                    "them into GeneMANIA.");
//            alert.showAndWait();
//
//            Clipboard clipboard = Clipboard.getSystemClipboard();
//            ClipboardContent content = new ClipboardContent();
//            content.putString(sb1.toString());
//            clipboard.setContent(content);
//
//            mainController.getHostServices().showDocument("http://genemania.org/");
//        } else {
//            NetworkDbLinkPopup popup = new NetworkDbLinkPopup(false) {
//                @Override
//                void okButtonClick() {
//                    StringBuilder sb = new StringBuilder();
//                    sb.append("http://genemania.org/link?o=9606");
//                    sb.append("&r=").append(getSlider().getValue());
//                    sb.append("&g=");
//                    for (Protein prot : currentlyDisplayedNetwork.getProteinList()) {
//                        sb.append(prot.getMainName()).append("|");
//                    }
//
//                    mainController.getHostServices().showDocument(sb.toString());
//                    close();
//                }
//            };
//            popup.showStage();
//        }
//    }

    private abstract class NetworkDbLinkPopup extends Stage {

        private VBox vbox = new VBox(10d);
        private Spinner<Double> spinnerThreshold = new Spinner<>(0.150d, 1.000d, 0.150d);

        private NetworkDbLinkPopup() {
            super();

            vbox.setPadding(new Insets(15));
            vbox.setAlignment(Pos.CENTER);

            Label label = new Label("Select the minimum required interaction score.");
            label.setStyle("-fx-font-size: 14;");
            vbox.getChildren().add(label);

            spinnerThreshold.setEditable(true);
            SpinnerValueFactory<Double> factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.150d, 1.000d, 0.150d, 0.010d);
            spinnerThreshold.setValueFactory(factory);
            TextFormatter<Double> formatter = new TextFormatter<>(factory.getConverter(), factory.getValue());
            spinnerThreshold.getEditor().setTextFormatter(formatter);
            vbox.getChildren().add(spinnerThreshold);

            Button okButton = new Button("OK");
            okButton.setPrefWidth(80d);
            Button cancelButton = new Button("Cancel");
            cancelButton.setPrefWidth(80d);
            vbox.getChildren().add(okButton);
            vbox.getChildren().add(cancelButton);

            initModality(Modality.WINDOW_MODAL);
            Scene dialogScene = new MyScene(vbox);
            setScene(dialogScene);

            okButton.setOnMouseClicked(event -> okButtonClick());
            cancelButton.setOnMouseClicked(event -> close());

        }

        abstract void okButtonClick();

        double getSpinnerThreshold() {
            return Double.parseDouble(spinnerThreshold.getValue().toString());
        }
    }

    @FXML
    private void copyPpiTableAction() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(constructPpiTableData());
        clipboard.setContent(content);
    }

    private String constructPpiTableData() {
        StringBuilder sb = new StringBuilder();
        sb.append("#node1\tnode2\tnode1_string_id\tnode2_string_id\tweight\tcoexpression\tdatabase_annotated" +
                "\texperimentally_determined_interaction\tgene_fusion\thomology\tneighbourhood_on_chromosome\t" +
                "phylogenetic_score\tautomated_textmining\n");

        for (NetworkEdge protInt : ppiTableData) {
            sb.append(protInt.getNode1()).append("\t")
                    .append(protInt.getNode2()).append("\t")
                    .append(protInt.getStringId1()).append("\t")
                    .append(protInt.getStringId2()).append("\t")
                    .append(protInt.getScore()).append("\t");

            if (protInt.getAscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getAscore()).append("\t");

            if (protInt.getDscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getDscore()).append("\t");

            if (protInt.getEscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getEscore()).append("\t");

            if (protInt.getFscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getFscore()).append("\t");

            if (protInt.getHscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getHscore()).append("\t");

            if (protInt.getNscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getNscore()).append("\t");

            if (protInt.getPscore() == null)
                sb.append(0).append("\t");
            else
                sb.append(protInt.getPscore()).append("\t");

            if (protInt.getTscore() == null)
                sb.append(0).append("\n");
            else
                sb.append(protInt.getTscore()).append("\n");
        }
        return sb.toString();
    }

    @FXML
    private void copyNodesTableAction() {
        StringBuilder sb = new StringBuilder();
        sb.append("node\tscore\n");

        for (NetworkNodeProtein networkNode : nodesTableData) {
            sb.append(networkNode.getName()).append("\t").append(networkNode.getScore()).append("\n");
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }

    @FXML
    private void saveImageButtonActionListener() {
        File selectedFile = saveImageFileChooser.showSaveDialog(saveImageButton.getScene().getWindow());

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(currentlyDisplayedNetwork.getNetworkImage(), null), "png", selectedFile);
        } catch (IOException e2) {
            ExceptionDialog exceptionDialog = new ExceptionDialog(e2, this.networkStage);
            exceptionDialog.showAndWait();
        }
    }

    @FXML
    private void saveTsvButtonActionListener() {
        File selectedFile = saveTsvFileChooser.showSaveDialog(saveTsvButton.getScene().getWindow());

        try {
            FileUtils.writeStringToFile(selectedFile, constructPpiTableData(), "UTF-8");
        } catch (IOException e) {
            ExceptionDialog exceptionDialog = new ExceptionDialog(e, this.networkStage);
            exceptionDialog.showAndWait();
        }
    }

    private void setPpiTableRows(List<NetworkEdge> ppiList) {
        ppiTableData.clear();
        ppiTableData.setAll(ppiList);
    }

    private void setNodesTableRows(List<NetworkNodeProtein> nodesList) {
        nodesTableData.clear();
        nodesTableData.setAll(nodesList);
    }

    private void setNetworkImage(Image image) {
        networkImage.setImage(image);
    }

    void showStage(boolean show) {
        if (show) {
            networkStage.show();
            networkStage.setX(this.mainController.getMainStage().getX() + 20); // cascading MainController stage
            networkStage.setY(this.mainController.getMainStage().getY() + 20);
            networkStage.requestFocus();
        } else
            networkStage.hide();
    }
}
