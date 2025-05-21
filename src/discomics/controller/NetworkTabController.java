//package discomics.controller;
//
//import discomics.application.AlphanumComparator;
//import discomics.model.NetworkEdge;
//import discomics.model.ProteinInteractionNetwork;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.scene.Node;
//import javafx.scene.control.ScrollPane;
//import javafx.scene.control.SplitPane;
//import javafx.scene.control.TableColumn;
//import javafx.scene.control.TableView;
//import javafx.scene.control.cell.PropertyValueFactory;
//import javafx.scene.image.ImageView;
//import javafx.scene.input.Clipboard;
//import javafx.scene.input.ClipboardContent;
//
///**
// * Created by Jure on 07/12/2016.
// */
//public class NetworkTabController {
//    private ProteinInteractionNetwork currentlyDisplayedNetwork;
//    private MainController mainController;
//
//    @FXML
//    private ProteinTableController proteinTableController;
//    @FXML
//    private ProteaseTableController proteaseTableController;
//
//    @FXML
//    private SplitPane mainSplitPane;
//
//    private Node ppiListPane;
//
//    private final ObservableList<NetworkEdge> ppiTableData = FXCollections.observableArrayList();
//    @FXML
//    private TableView<NetworkEdge> ppiTable;
//    @FXML
//    private TableColumn<NetworkEdge, String> protein1;
//    @FXML
//    private TableColumn<NetworkEdge, String> protein2;
//    @FXML
//    private TableColumn<NetworkEdge, String> score;
//
//    @FXML
//    private ImageView networkImage;
//    @FXML
//    private ScrollPane imageContainer;
//
//    public void init(MainController mainController) {
//        this.mainController = mainController;
//        this.ppiListPane = mainSplitPane.getItems().get(2);
//
//        protein1.setCellValueFactory(
//                new PropertyValueFactory<>("protein1"));
//        protein2.setCellValueFactory(
//                new PropertyValueFactory<>("protein2"));
//        score.setCellValueFactory(
//                new PropertyValueFactory<>("score"));
//
//        protein1.setComparator(new AlphanumComparator());
//        protein2.setComparator(new AlphanumComparator());
//
//        ppiTable.setItems(ppiTableData);
//
//        proteaseTableController.init(this.mainController);
//        proteinTableController.init(this.mainController);
//        proteinTableController.showProteolysisColumn();
//    }
//
//    @FXML
//    private void copyPpiTableAction() {
//        final Clipboard clipboard = Clipboard.getSystemClipboard();
//        final ClipboardContent content = new ClipboardContent();
//        content.putString(constructPpiTableData());
//        clipboard.setContent(content);
//    }
//
//    private String constructPpiTableData() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("#node1\tnode2\tnode1_string_id\tnode2_string_id\tweight\tcoexpression\tdatabase_annotated" +
//                "\texperimentally_determined_interaction\tgene_fusion\thomology\tneighbourhood_on_chromosome\t" +
//                "phylogenetic_score\tautomated_textmining\n");
//
//        for (NetworkEdge protInt : ppiTableData) {
//            sb.append(protInt.getNode1()).append("\t")
//                    .append(protInt.getNode2()).append("\t")
//                    .append(protInt.getStringId1()).append("\t")
//                    .append(protInt.getStringId2()).append("\t")
//                    .append(protInt.getScore()).append("\t");
//
//            if (protInt.getAscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getAscore()).append("\t");
//
//            if (protInt.getDscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getDscore()).append("\t");
//
//            if (protInt.getEscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getEscore()).append("\t");
//
//            if (protInt.getFscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getFscore()).append("\t");
//
//            if (protInt.getHscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getHscore()).append("\t");
//
//            if (protInt.getNscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getNscore()).append("\t");
//
//            if (protInt.getPscore() == null)
//                sb.append(0).append("\t");
//            else
//                sb.append(protInt.getPscore()).append("\t");
//
//            if (protInt.getTscore() == null)
//                sb.append(0).append("\n");
//            else
//                sb.append(protInt.getTscore()).append("\n");
//        }
//        return sb.toString();
//    }
//}
