package discomics.controller;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.List;

public class MyAlertContamination extends MyAlert {

    private ButtonType removeGeneButton = new ButtonType("Remove Genes");
    private ButtonType keepGenesButton = new ButtonType("Keep Genes");
    private ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

    public MyAlertContamination(AlertType alertType, Window owner, List<String> geneList, String message) {
        super(alertType, owner);

        this.setTitle("Information");
        this.setHeaderText(null);
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        for (int i = 0; i < geneList.size(); i++) {
            String superCommonGene = geneList.get(i);
            sb.append(superCommonGene);
            if (i != (geneList.size() - 1))
                sb.append("\n");
        }
        this.setContentText(sb.toString());

        this.getButtonTypes().setAll(removeGeneButton, keepGenesButton, cancelButton);

    }

    public ButtonType getRemoveGeneButton() {
        return removeGeneButton;
    }

    public ButtonType getKeepGenesButton() {
        return keepGenesButton;
    }

    public ButtonType getCancelButton() {
        return cancelButton;
    }
}
