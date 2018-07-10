package discomics.controller;

import discomics.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.ArrayList;
import java.util.List;

public class DetailsGeneFamilyController {
    private MainTablesController mainTablesController;

    @FXML
    private ScrollPane containerGeneFamilyDetails;


    public void init(MainTablesController mainTablesController) {
        this.mainTablesController = mainTablesController;
        showEmptyDetails();
    }

    void showProteinDetails(List<TextMinedObject> selectedGeneFamilies) {
        GeneFamilyDetailsPane proteinDetailsGridPane = new GeneFamilyDetailsPane();

        if (selectedGeneFamilies.size() == 1) {
            proteinDetailsGridPane.construct(selectedGeneFamilies.get(0));
            containerGeneFamilyDetails.setContent(proteinDetailsGridPane);
        } else if (selectedGeneFamilies.size() == 0) {
            proteinDetailsGridPane.constructNoSelection();
            containerGeneFamilyDetails.setContent(proteinDetailsGridPane);
        } else {
            proteinDetailsGridPane.constructMultipleSelection(selectedGeneFamilies);
            containerGeneFamilyDetails.setContent(proteinDetailsGridPane);
        }
    }

    private void showEmptyDetails() {
        GeneFamilyDetailsPane details = new GeneFamilyDetailsPane();
        details.constructNoSelection();
        containerGeneFamilyDetails.setContent(details);
    }

    private class GeneFamilyDetailsPane extends TextMinedObjectDetailsPane {
        @Override
        void construct(TextMinedObject tmGeneFamily) {
            final GeneFamily family = (GeneFamily) tmGeneFamily.getTextMinableInput();

            String title1 = "Gene Family Name";
            String title2 = "Belonging Input Genes";

            addTextFieldToGrid(title1, true);
            addTextFieldToGrid(tmGeneFamily.getTextMinableInput().getMainName(), false);

            addTextFieldToGrid(title2, true);

            List<Protein> proteinsBelongingToFamily = new ArrayList<>();
            for (Protein protein : mainTablesController.getModel().getProteinCollection().getOutputProteinList()) {
                if (protein.getGeneFamily() != null) {
                    if (protein.getGeneFamily().equals(family)) {
                        proteinsBelongingToFamily.add(protein);
                    }
                }
            }

            for (Protein protein : proteinsBelongingToFamily) {
                addTextFieldToGrid(protein.getMainName(), false);
            }

            // construct copyToClipboard button
            Button copyToClipboardButton = new Button("Copy to Clipboard");
            getChildren().add(copyToClipboardButton);
            copyToClipboardButton.setOnAction(event -> {
                StringBuilder sb = new StringBuilder();
                sb.append(title1).append("\t").append(family.getMainName()).append("\n");
                sb.append(title2).append("\t");
                for (Protein protein : proteinsBelongingToFamily) {
                    sb.append(protein.getMainName()).append("\t");
                }

                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(sb.toString());
                clipboard.setContent(content);
            });
        }

        @Override
        void constructMultipleSelection(List<TextMinedObject> geneFamilies) {
            addTextFieldToGrid("No information can be shown: multiple selection in corresponding table.", false);
        }
    }
}
