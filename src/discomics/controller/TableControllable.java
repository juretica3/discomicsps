package discomics.controller;

import discomics.application.AlphanumComparator;
import discomics.model.*;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jure on 9.9.2016.
 */
interface TableControllable<T> {

    /**
     * Retrieves Protease Count table entries for the input proteins
     */
    default List<ProteaseCount> retrieveProteases(List<TextMinedObject> selectedObjects, IoModel model) { // remove from unclassified checkbox & replace with initial state method
        return model.getProteaseCountTableEntries(selectedObjects);
    }

    /**
     * Returns all proteolysis articles for the input proteins
     */
    default List<Article> retrieveArticlesProteolysis(List<TextMinedObject> selectedObjects) {
        HashSet<Article> articlesToDisplay = new HashSet<>();
        for (TextMinedObject tmObject : selectedObjects) {
            articlesToDisplay.addAll(tmObject.getArticleCollectablePlys().getAllArticlesUncompressed());
        }

        return new ArrayList<>(articlesToDisplay);
    }

    /**
     * Returns all classified proteolysis articles for the input proteins
     */
    default List<Article> retrieveArticlesProteolysisClassified(List<TextMinedObject> selectedObjects) {
        HashSet<Article> articlesToDisplay = new HashSet<>();
        for (TextMinedObject tmObject : selectedObjects) {
            articlesToDisplay.addAll((tmObject.getArticleCollectablePlys()).getClassifiedArticlesUncompressed());
        }

        return new ArrayList<>(articlesToDisplay);
    }

    /** Returns all unclassified proteolysis articles for the input proteins */
//    default List<Article> retrieveArticlesProteolysisUnclassified(List<TextMinedProtein> selectedProteins) {
//        if (selectedProteins.isEmpty())
//            return new ArrayList<>();
//
//        HashSet<Article> articlesToDisplay = new HashSet<>();
//        for (TextMinedProtein tmProt : selectedProteins) {
//            articlesToDisplay.addAll((tmProt.getArticleCollectablePlys()).getUnclassifiedArticles());
//        }
//
//        return new ArrayList<>(articlesToDisplay);
//    }

    /**
     * Retrieves all biomarker articles for the input proteins
     */
    default List<Article> retrieveArticlesBiomarker(List<TextMinedObject> selectedObjects) { // remove from unclassified checkbox
        HashSet<Article> articlesToDisplay = new HashSet<>();
        for (TextMinedObject tmObject : selectedObjects) {
            articlesToDisplay.addAll(tmObject.getArticleCollectableBiom().getUncompressedArticleCollection());
        }

        return new ArrayList<>(articlesToDisplay);
    }

    /**
     * Retrieves all biomarker articles for the input proteins and a given input biomarker
     */
    default List<Article> retrieveArticlesBiomarker(List<TextMinedObject> selectedObjects, Biomarker biomarker) {
        List<Article> articles = retrieveArticlesBiomarker(selectedObjects);
        return articles.stream()
                .filter(article -> article.getBiomarkersMentioned().contains(biomarker))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all custom articles for the input proteins
     */
    default List<Article> retrieveArticlesCustom(List<TextMinedObject> selectedObjects) {
        if (selectedObjects.isEmpty())
            return new ArrayList<>();

        HashSet<Article> articlesToDisplay = new HashSet<>();
        for (TextMinedObject tmObject : selectedObjects) {
            articlesToDisplay.addAll(tmObject.getArticleCollectableCust().getUncompressedArticleCollection());
        }

        return new ArrayList<>(articlesToDisplay);
    }

    /**
     * Retrieves all proteolysis articles for the selected proteins and proteases
     */
    default List<Article> retrieveArticlesProteolysis(List<TextMinedObject> selectedProteins, List<ProteaseCount> selectedProteases, IoModel model) {
        HashSet<Article> articles = new HashSet<>();

        // empty protein table selection
        if (selectedProteins.isEmpty()) { // no selection in protein table
            List<Protein> selectedObjectsProtease = selectedProteases.stream().map(ProteaseCount::getProtease).collect(Collectors.toList()); // get Protease objects from ProteaseCount

            for (TextMinedObject tmObject : model.getTextMinedProteins()) {
                for (Article article : tmObject.getArticleCollectablePlys().getArticleCollection()) {
                    articles.addAll(
                            selectedObjectsProtease.stream()
                                    .filter(protease -> article.getProteasesMentioned().contains(protease) && !article.isCompressed())
                                    .map(protease -> article).collect(Collectors.toList())
                    );
                }
            }

        } else { // protein table selection.

            if (!selectedProteases.isEmpty()) { // selection in protease table as well
                for (TextMinedObject tmObject : selectedProteins) {
                    for (Article article : tmObject.getArticleCollectablePlys().getArticleCollection()) {
                        articles.addAll(
                                selectedProteases.stream()
                                        .filter(proteaseCount -> article.getProteasesMentioned().contains(proteaseCount.getProtease()) && !article.isCompressed())
                                        .map(proteaseCount -> article).collect(Collectors.toList())
                        );
                    }
                }
            } else { // only selection in protein table
                return retrieveArticlesProteolysis(selectedProteins);
            }
        }
        return new ArrayList<>(articles);
    }

    // ------------------- DRUGS -----------------
//
//    default List<Drug> getAllDrugInteractions(IoModel model) {
//        HashSet<Drug> allDrugs = new HashSet<>();
//        model.getProteinCollection().getOutputProteinList().forEach(protein -> allDrugs.addAll(protein.getDrugs()));
//        allDrugs.forEach(drugInteraction -> System.out.println(drugInteraction.getMainName()));
//        return new ArrayList<>(allDrugs);
//    }


    // ----------------- METHODS FOR COPYING TABLE DATA TO CSV ----------------------
    default void copyAllRows(TableView<T> table) {
        ObservableList<TableColumn<T, ?>> columns = table.getColumns();

        if (columns.get(0) == null) {
            Alert alert = new MyAlert(Alert.AlertType.ERROR, table.getScene().getWindow());
            alert.setTitle("Error");
            alert.setContentText("Error copying data from table.");
            alert.showAndWait();
        }

        StringBuilder sb = new StringBuilder();
        // append column headers
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<T, ?> column = columns.get(i);
            sb.append(column.getText());

            if (i == (columns.size() - 1))
                sb.append("\n");
            else
                sb.append("\t");
        }

        // append data
        int iniIndex = 0;
        while (true) {
            for (int i = 0; i < columns.size(); i++) {
                TableColumn<T, ?> column = columns.get(i);
                sb.append(column.getCellData(iniIndex));

                if (i == (columns.size() - 1))
                    sb.append("\n");
                else
                    sb.append("\t");
            }
            iniIndex++;

            if (columns.get(0).getCellData(iniIndex) == null)
                break;
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }

    default void copySelectedRows(TableView<T> table) {
        ObservableList<TableColumn<T, ?>> columns = table.getColumns();
        ObservableList<Integer> selectedIndices = table.getSelectionModel().getSelectedIndices();

        if (columns.get(0) == null) {
            Alert alert = new MyAlert(Alert.AlertType.ERROR, table.getScene().getWindow());
            alert.setTitle("Error");
            alert.setContentText("Error copying data from table.");
            alert.showAndWait();
        }

        StringBuilder sb = new StringBuilder();

        // append column headers
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<T, ?> column = columns.get(i);
            sb.append(column.getText());

            if (i == (columns.size() - 1))
                sb.append("\n");
            else
                sb.append("\t");
        }

        // append data
        for (int selectedIndex : selectedIndices) {
            for (int i = 0; i < columns.size(); i++) {
                TableColumn<T, ?> column = columns.get(i);
                sb.append(column.getCellData(selectedIndex));

                if (i == (columns.size() - 1))
                    sb.append("\n");
                else
                    sb.append("\t");
            }
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);
    }

    HashMap<String, KeyCombination> keyAlphabet = new HashMap<String, KeyCombination>() {{
        put("A", new KeyCodeCombination(KeyCode.A));
        put("B", new KeyCodeCombination(KeyCode.B));
        put("C", new KeyCodeCombination(KeyCode.C));
        put("D", new KeyCodeCombination(KeyCode.D));
        put("E", new KeyCodeCombination(KeyCode.E));
        put("F", new KeyCodeCombination(KeyCode.F));
        put("G", new KeyCodeCombination(KeyCode.G));
        put("H", new KeyCodeCombination(KeyCode.H));
        put("I", new KeyCodeCombination(KeyCode.I));
        put("J", new KeyCodeCombination(KeyCode.J));
        put("K", new KeyCodeCombination(KeyCode.K));
        put("L", new KeyCodeCombination(KeyCode.L));
        put("M", new KeyCodeCombination(KeyCode.M));
        put("N", new KeyCodeCombination(KeyCode.N));
        put("O", new KeyCodeCombination(KeyCode.O));
        put("P", new KeyCodeCombination(KeyCode.P));
        put("Q", new KeyCodeCombination(KeyCode.Q));
        put("R", new KeyCodeCombination(KeyCode.R));
        put("S", new KeyCodeCombination(KeyCode.S));
        put("T", new KeyCodeCombination(KeyCode.T));
        put("U", new KeyCodeCombination(KeyCode.U));
        put("V", new KeyCodeCombination(KeyCode.V));
        put("W", new KeyCodeCombination(KeyCode.W));
        put("X", new KeyCodeCombination(KeyCode.X));
        put("Y", new KeyCodeCombination(KeyCode.Y));
        put("Z", new KeyCodeCombination(KeyCode.Z));
    }};


    default void setKeyNavigationListeners(TableView<T> tableView, int column) {
        // keyboard selection listener; when letter is typed select row whose gene name begins with that letter, cycle through gene names
        tableView.setOnKeyReleased(event -> {

            Map<Integer, String> geneNames = new HashMap<>(); // map containing geneNames as values, row indices as keys
            for (int i = 0; i < tableView.getItems().size(); i++)
                geneNames.put(i, (String) tableView.getColumns().get(column).getCellData(i));

            List<Map.Entry<Integer, String>> list = new LinkedList<>(geneNames.entrySet()); // convert map to linkedlist

            list.sort((o1, o2) -> { // sort the linked list in alphanumerical order
                AlphanumComparator comparator = new AlphanumComparator();
                return comparator.compare(o1.getValue(), o2.getValue());
            });

            ObservableList<Integer> selectedRows = tableView.getSelectionModel().getSelectedIndices(); // currently selected rows
            boolean multipleSelection = selectedRows.size() > 1; // is there multiple selection?

            String currentSelection = (String) tableView.getColumns().get(column).getCellData(selectedRows.get(0)); // get current selection (ignored if multiple selection is present)

            Set<String> keySet = keyAlphabet.keySet();

            for (String key : keySet) { // loops through keyboard key combinations

                if (keyAlphabet.get(key).match(event)) { // identifies the pressed key
                    tableView.getSelectionModel().clearSelection(); // clears selection

                    boolean isSameLetter = currentSelection != null && currentSelection.charAt(0) == key.charAt(0); // checks if the same letter was pressed compared to current selection

                    if (isSameLetter && !multipleSelection) { // enters this block if same letter as current selection is pressed AND if there is no multiple selection
                        for (int i = 0; i < list.size(); i++) { // loop through sorted list of table entries
                            Map.Entry<Integer, String> entry = list.get(i);
                            if (entry.getValue().equalsIgnoreCase(currentSelection)) { // if current selection equals the current element of loop
                                if (i != (list.size() - 1) && list.get(i + 1).getValue().charAt(0) == key.charAt(0)) { // if this is not the last element AND if next element also starts with pressed letter
                                    tableView.getSelectionModel().select(list.get(i + 1).getKey()); // select next element
                                    return;
                                } else // else break from loop and go to next section
                                    break;
                            }
                        }
                    }

                    // enters this section if previous section failed to select a new row for any reason (return statement in previous selection)
                    // also enters if there is NO current selection, if a new letter is pressed compared to current selection, or if multiple selection is present

                    // selects first entry of pressed keyboard letter in alphanumerical order
                    for (Map.Entry<Integer, String> entry : list) { // loop through table entries sorted in alphanumerical order
                        if (entry.getValue().charAt(0) == key.charAt(0)) { // the first entry where first letter matches letter of pressed key is selected
                            tableView.getSelectionModel().select(entry.getKey());
                            return;
                        }
                    }
                    break;
                }
            }

        });
    }
}
