package discomics.controller;

import discomics.model.*;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.TilePane;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 29/01/2017.
 */
public class DetailsProteinController {
    private MainTablesController mainTablesController;

    @FXML
    private ScrollPane containerProteinDetails;


    public void init(MainTablesController mainTablesController) {
        this.mainTablesController = mainTablesController;
        showEmptyDetails();
    }

    void showProteinDetails(List<TextMinedObject> selectedProteins) {
        ProteinDetailsPane proteinDetailsGridPane = new ProteinDetailsPane(selectedProteins);
        containerProteinDetails.setContent(proteinDetailsGridPane);
    }

    private void showEmptyDetails() {
        ProteinDetailsPane details = new ProteinDetailsPane(new ArrayList<>());
        details.constructNoSelection();
        containerProteinDetails.setContent(details);
    }

    private class ProteinDetailsPane extends TextMinedObjectDetailsPane {
        String title1 = "Gene Name";
        String title2 = "Uniprot ID";
        String title3;
        String title4 = "Ensembl ID";
        String title5 = "Physical Interaction Protease Partners";
        String title6 = "Drug Interactions";
        String title7 = "GO Annotations (Molecular Function)";
        String title8 = "GO Annotations (Biological Process)";
        String title9 = "GO Annotations (Cellular Component)";

        ProteinDetailsPane(List<TextMinedObject> textMinedObjects) {

            if (textMinedObjects.size() == 1) {
                final Protein protein = (Protein) textMinedObjects.get(0).getTextMinableInput();

                if (protein.getStringId() != null) {
                    if (protein.getStringId().contains(Protein.getHomoSapiensTaxonId()))
                        title3 = "String ID (Human)";
                    else if (protein.getStringId().contains(Protein.getRattusNorvegicusTaxonId()))
                        title3 = "String ID (Rat)";
                    else if (protein.getStringId().contains(Protein.getMusMusculusTaxonId()))
                        title3 = "String ID (Mouse)";
                    else
                        title3 = "String ID";
                }

                construct(textMinedObjects.get(0));

            } else if (textMinedObjects.size() == 0)
                constructNoSelection();
            else
                constructMultipleSelection(textMinedObjects);
        }

        TilePane constructTilePane(TextMinedObject tmProt) {
            final Protein protein = (Protein) tmProt.getTextMinableInput();

            TilePane tilePane = new TilePane(5d, 5d);
            int buttonWidth = 100;

            if (protein.getUniprotId() != null) {
                Button linkButton = new Button("UniProt");
                linkButton.setPrefWidth(buttonWidth);
                linkButton.setStyle("-fx-text-fill: indigo;");

                // button listener; open link
                linkButton.setOnAction(event -> {
                    String url = "https://www.uniprot.org/uniprot/" + protein.getUniprotId();
                    mainTablesController.getMainController().getHostServices().showDocument(url);
                });

                // disable button if ID not defined
                BooleanProperty booleanProperty = new SimpleBooleanProperty(((Protein) tmProt.getTextMinableInput()).getUniprotId().isEmpty());
                linkButton.disableProperty().bind(booleanProperty);

                tilePane.getChildren().add(linkButton);
            }

            if (protein.getMainName() != null) {
                Button linkButton = new Button("GeneCards");
                linkButton.setPrefWidth(buttonWidth);
                linkButton.setStyle("-fx-text-fill: indigo;");

                // button listener; open link
                linkButton.setOnAction(event -> {
                    String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene=" + protein.getMainName();
                    mainTablesController.getMainController().getHostServices().showDocument(url);
                });

                // disable button if ID not defined
                BooleanProperty booleanProperty = new SimpleBooleanProperty((tmProt.getTextMinableInput()).getMainName().isEmpty());
                linkButton.disableProperty().bind(booleanProperty);

                tilePane.getChildren().add(linkButton);
            }

            if (protein.getHgncId() != null) {
                Button linkButton = new Button("HGNC");
                linkButton.setPrefWidth(buttonWidth);
                linkButton.setStyle("-fx-text-fill: indigo;");

                // button listener; open link
                linkButton.setOnAction(event -> {
                    String url = "https://www.genenames.org/cgi-bin/gene_symbol_report?hgnc_id=" + protein.getHgncId();
                    mainTablesController.getMainController().getHostServices().showDocument(url);
                });

                // disable button if ID not defined
                boolean isHgncIdLegal = !((Protein) tmProt.getTextMinableInput()).getHgncId().contains("HGNC:"); // all hgnc ids contain this root
                BooleanProperty booleanProperty = new SimpleBooleanProperty(isHgncIdLegal);
                linkButton.disableProperty().bind(booleanProperty);

                tilePane.getChildren().add(linkButton);
            }

            if (protein.getStringId() != null) {
                Button linkButton = new Button("STRING");
                linkButton.setPrefWidth(buttonWidth);
                linkButton.setStyle("-fx-text-fill: indigo;");

                linkButton.setOnAction(event -> {
                    String url = "https://string-db.org/network/" + protein.getStringId();
                    mainTablesController.getMainController().getHostServices().showDocument(url);
                });

                // disable button if ID not defined
                BooleanProperty booleanProperty = new SimpleBooleanProperty(((Protein) tmProt.getTextMinableInput()).getStringId().isEmpty());
                linkButton.disableProperty().bind(booleanProperty);


                tilePane.getChildren().add(linkButton);
            }

            // add drug text mining button
            Button drugMiningButton = new Button("Mine Drugs");
            drugMiningButton.setPrefWidth(buttonWidth);
            drugMiningButton.setStyle("-fx-text-fill: firebrick;");

            drugMiningButton.setOnAction(event -> {
                if (mainTablesController.getModel().getQuerySettings().isCustomSearch()) { // if custom search was enabled
                    mainTablesController.getMainController().drugTextMiningSearch(); // performs drug text mining according to protein selected in table
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setTitle("Information");
                    alert.setContentText("Drug text mining unavailable because CUSTOM search is not defined.");
                    alert.show();
                }
            });

            // disable button if no drugs available
            BooleanProperty booleanProperty = new SimpleBooleanProperty(((Protein) tmProt.getTextMinableInput()).getDrugs().isEmpty());
            drugMiningButton.disableProperty().bind(booleanProperty);

            tilePane.getChildren().add(drugMiningButton);

            // define copy to clipboard button
            Button copyToClipboardButton = new Button("Copy All");
            copyToClipboardButton.setPrefWidth(buttonWidth);

            copyToClipboardButton.setOnAction(event -> {
                StringBuilder sb = new StringBuilder();
                sb.append(title1).append("\t").append(protein.getMainName()).append("\n");
                sb.append(title2).append("\t").append(protein.getUniprotId()).append("\n");
                sb.append(title3).append("\t").append(protein.getStringId()).append("\n\n");

                // Only if proteolysis model is defined!
                if (mainTablesController.getModel().getQuerySettings().isProteaseSearch()) {
                    sb.append(title6).append("\n");

                    final List<NetworkEdge> physInt = tmProt.getArticleCollectablePlys().getPhysicalInteractions();

                    if (physInt.size() == 0) {
                        sb.append("None found\n\n");
                    } else {
                        sb.append("protein 1\tprotein 2\tscore\te-score\n");

                        for (NetworkEdge interaction : physInt) {
                            sb.append(interaction.getNode1()).append("\t")
                                    .append(interaction.getNode2()).append("\t")
                                    .append(interaction.getScore()).append("\t")
                                    .append(interaction.getEscore()).append("\n");
                        }
                    }
                    sb.append("\n");
                }

                sb.append(title6).append("\n");

                final List<Drug> drugInt = protein.getDrugs();
                if (drugInt.size() == 0) {
                    sb.append("None found\n");
                } else {
                    sb.append("Drug name\tType\n");
                    for (Drug interaction : drugInt) {
                        sb.append(interaction.getMainName()).append("\t")
                                .append(interaction.getInteractionType()).append("\n");
                    }
                }
                sb.append("\n");

                if (protein.isSuccessBuildingUniProt()) {

                    List<GoAnnotation> goAnnotationsFunc = new ArrayList<>();
                    List<GoAnnotation> goAnnotationsProc = new ArrayList<>();
                    List<GoAnnotation> goAnnotationsComp = new ArrayList<>();

                    for (GoAnnotation goAnnotation : protein.getGoAnnotations()) {
                        if (goAnnotation.getType() == 1) { // proc
                            goAnnotationsProc.add(goAnnotation);
                        } else if (goAnnotation.getType() == 2) { // func
                            goAnnotationsFunc.add(goAnnotation);
                        } else if (goAnnotation.getType() == 3) { // comp
                            goAnnotationsComp.add(goAnnotation);
                        }
                    }

                    sb.append(title7).append("\n");
                    if (goAnnotationsFunc.size() == 0) {
                        sb.append("None found\n");
                    } else {
                        sb.append("GO id\tGO term\n");
                        for (GoAnnotation goAnnotation : goAnnotationsFunc) {
                            sb.append(goAnnotation.getGoId()).append("\t")
                                    .append(goAnnotation.getGoTerm()).append("\n");
                        }
                    }
                    sb.append("\n");

                    sb.append(title8).append("\n");
                    if (goAnnotationsProc.size() == 0) {
                        sb.append("None found\n");
                    } else {
                        sb.append("GO id\tGO term\n");
                        for (GoAnnotation goAnnotation : goAnnotationsProc) {
                            sb.append(goAnnotation.getGoId()).append("\t")
                                    .append(goAnnotation.getGoTerm()).append("\n");
                        }
                    }
                    sb.append("\n");

                    sb.append(title9).append("\n");
                    if (goAnnotationsComp.size() == 0) {
                        sb.append("None found\n");
                    } else {
                        sb.append("GO id\tGO term\n");
                        for (GoAnnotation goAnnotation : goAnnotationsComp) {
                            sb.append(goAnnotation.getGoId()).append("\t")
                                    .append(goAnnotation.getGoTerm()).append("\n");
                        }
                    }
                }
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(sb.toString());
                clipboard.setContent(content);
            });

            // add copy button
            tilePane.getChildren().add(copyToClipboardButton);

            return tilePane;
        }

        @Override
        void construct(TextMinedObject tmProt) {
            final Protein protein = (Protein) tmProt.getTextMinableInput();

            TilePane tilePaneLinks = constructTilePane(tmProt);
            getChildren().add(tilePaneLinks);

            if (protein.getMainName() != null) {
                addTextFieldToGrid(title1, true);
                addTextFieldToGrid(protein.getMainName(), false);
            }
            if (protein.getProteinName() != null) {
                addTextFieldToGrid(protein.getProteinName(), false);
            }
            if (protein.getUniprotId() != null) {
                addTextFieldToGrid(title2, true);
                addTextFieldToGrid(protein.getUniprotId(), false);
            }
            if (protein.getStringId() != null) {
                addTextFieldToGrid(title3, true);
                addTextFieldToGrid(protein.getStringId(), false);
            }
            if (protein.getEnsemblId() != null) {
                addTextFieldToGrid(title4, true);
                addTextFieldToGrid(protein.getEnsemblId(), false);
            }

            // Physical interactions with proteases
            if (mainTablesController.getModel().getQuerySettings().isProteaseSearch() && protein.isSuccessBuildingSTRING()) {
                addTextFieldToGrid(title5, true);

                final List<NetworkEdge> physInt = tmProt.getArticleCollectablePlys().getPhysicalInteractions();

                if (physInt.size() == 0) {
                    addTextFieldToGrid("None found", false);
                } else {

                    for (NetworkEdge interaction : physInt) {
                        if (interaction.getNode1().toLowerCase().contains(IoModel.getProteaseFamilySelected().getStandardAbbreviation().toLowerCase())) {
                            addTextFieldToGrid(interaction.getNode1(), false);
                        } else {
                            addTextFieldToGrid(interaction.getNode2(), false);
                        }
                    }
                }
            }

            // Drugs that can target protein
            final List<Drug> drugInt = protein.getDrugs();
            if (protein.isSuccessBuildingDGI()) {
                addTextFieldToGrid(title6, true); // add title

                if (drugInt.isEmpty()) {
                    addTextFieldToGrid("None found", false); // add none found label if no drugs for selected protein
                } else {

                    for (int i = 0; i < drugInt.size(); i++) {
                        Drug interaction = drugInt.get(i);
                        StringBuilder interactionString = new StringBuilder();
                        interactionString.append(interaction.getMainName());
                        if (interaction.getInteractionType().size() > 0) {
                            interactionString.append(" (");
                            for (int j = 0; j < interaction.getInteractionType().size(); j++) {
                                String interactionType = interaction.getInteractionType().get(j);

                                if (j != 0)
                                    interactionString.append(", ");
                                interactionString.append(interactionType);
                            }
                            interactionString.append(")");
                        }
                        addTextFieldToGrid(i + 1 + "| " + interactionString.toString(), false);
                    }
                }
            }

            List<GoAnnotation> goAnnotationsFunc = new ArrayList<>();
            List<GoAnnotation> goAnnotationsProc = new ArrayList<>();
            List<GoAnnotation> goAnnotationsComp = new ArrayList<>();

            if (protein.isSuccessBuildingUniProt()) {

                for (GoAnnotation goAnnotation : protein.getGoAnnotations()) {
                    if (goAnnotation.getType() == 1) { // proc
                        goAnnotationsProc.add(goAnnotation);
                    } else if (goAnnotation.getType() == 2) { // func
                        goAnnotationsFunc.add(goAnnotation);
                    } else if (goAnnotation.getType() == 3) { // comp
                        goAnnotationsComp.add(goAnnotation);
                    }
                }

                addTextFieldToGrid(title7, true);
                if (goAnnotationsFunc.isEmpty()) {
                    addTextFieldToGrid("None found", false);
                } else {
                    for (int i = 0; i < goAnnotationsFunc.size(); i++) {
                        addTextFieldToGrid(i + 1 + "| " + goAnnotationsFunc.get(i).getGoTerm(), false);
                    }
                }
                addTextFieldToGrid(title8, true);
                if (goAnnotationsProc.isEmpty()) {
                    addTextFieldToGrid("None found", false);
                } else {
                    for (int i = 0; i < goAnnotationsProc.size(); i++) {
                        addTextFieldToGrid(i + 1 + "| " + goAnnotationsProc.get(i).getGoTerm(), false);
                    }
                }
                addTextFieldToGrid(title9, true);
                if (goAnnotationsComp.isEmpty()) {
                    addTextFieldToGrid("None found", false);
                } else {
                    for (int i = 0; i < goAnnotationsComp.size(); i++) {
                        addTextFieldToGrid(i + 1 + "| " + goAnnotationsComp.get(i).getGoTerm(), false);
                    }
                }
            }
        }

        @Override
        void constructMultipleSelection(List<TextMinedObject> listProteins) {
            addTextFieldToGrid("No information can be shown: multiple selection in corresponding table.", false);

            // only if protease search is enabled
            if (mainTablesController.getModel().getQuerySettings().isProteaseSearch()) {
                // button to copy all protease-protein physical interactions
                Button copyInteractions = new Button("Copy Protease Physical Interactions");
                copyInteractions.setPrefWidth(220d);

                copyInteractions.setOnAction(event -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("protein 1\tprotein 2\tscore\te-score\n");

                    for (TextMinedObject tmProt : listProteins) {

                        List<NetworkEdge> interactions = tmProt.getArticleCollectablePlys().getPhysicalInteractions();

                        for (NetworkEdge inter : interactions) {
                            sb.append(inter.getNode1()).append("\t")
                                    .append(inter.getNode2()).append("\t")
                                    .append(inter.getScore()).append("\t")
                                    .append(inter.getEscore()).append("\n");
                        }
                    }
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    content.putString(sb.toString());
                    clipboard.setContent(content);
                });

                getChildren().add(copyInteractions);
            }

            // button to copy drug list of all selected proteins
            Button copyDrugs = new Button("Copy Drug Interactions");
            copyDrugs.setPrefWidth(220d);

            copyDrugs.setOnAction(event -> {
                StringBuilder sb = new StringBuilder();
                sb.append("protein\tdrug name\ttype\n");
                for (TextMinedObject tmProt : listProteins) {
                    for (Drug inter : ((TextMinedProtein) tmProt).getTextMinableInput().getDrugs()) {
                        sb.append(tmProt.getTextMinableInput().getMainName()).append("\t");
                        sb.append(inter.getMainName()).append("\t");
                        sb.append(inter.getInteractionType()).append("\n");
                    }
                }
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(sb.toString());
                clipboard.setContent(content);
            });
            getChildren().add(copyDrugs);

            // button to start text mining of drugs of all selected proteins
            Button textMineDrugs = new Button("Text-Mine All Drugs");
            textMineDrugs.setPrefWidth(220d);

            textMineDrugs.setOnAction(event -> {
                mainTablesController.getMainController().drugTextMiningSearch();
            });

            getChildren().add(textMineDrugs);
        }
    }

}
