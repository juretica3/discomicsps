package discomics.controller;


import discomics.model.*;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MainControllerDrugMining {

    private MainController mainController;
    private Stage mainDrugMiningControllerStage;

    @FXML
    private MainTablesDrugsController mainTablesDrugsController; // encapsulated controller


    public void init(MainController mainController, Stage thisStage) throws Exception {
        this.mainController = mainController;
        this.mainDrugMiningControllerStage = thisStage;

        this.mainTablesDrugsController.init(mainController);
    }

    void showStage(boolean show) {
        if (show) {
            if (!mainDrugMiningControllerStage.isShowing()) {
                this.mainDrugMiningControllerStage.setX(this.mainController.getMainStage().getX() + 20); // cascading MainController stage
                this.mainDrugMiningControllerStage.setY(this.mainController.getMainStage().getY() + 20);
                this.mainDrugMiningControllerStage.show();
                this.mainDrugMiningControllerStage.requestFocus();
            }
        } else {
            this.mainDrugMiningControllerStage.hide();
        }
    }

    List<TextMinedObject> textMineDrugs(Protein protein, List<CustomInputTermBlock> customTerms) {

        List<Drug> drugs = new ArrayList<>();
        drugs.addAll(protein.getDrugs()); // collect drugs

        List<TextMinedObject> textMinedDrugs = new ArrayList<>();
        QuerySettings querySettings = new QuerySettings(false, false, true,
                true, false, false, false,
                false);

        ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (Drug drug : drugs) { // loop through drugs
            executorService.submit((Callable<Void>) () -> {
                TextMinedObject textMinedDrug = new TextMinedDrug(drug, customTerms, querySettings); // create text mined drug object
                textMinedDrug.queryEPmc(); // perform text mining
                textMinedDrug.finalisePostQuery(); // perform filtering, compression
                textMinedDrugs.add(textMinedDrug); // add to output list
                return null;
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return textMinedDrugs;
    }

    void initialiseViews() {
        mainTablesDrugsController.initialiseViews(mainController.getModel().getTextMinedDrugs());
    }

    public Stage getStage() {
        return mainDrugMiningControllerStage;
    }
}
