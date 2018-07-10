package discomics.controller;

import discomics.model.Protein;
import discomics.model.QuerySettings;
import discomics.model.TextMinedObject;
import discomics.model.TextMinedProtein;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jure on 13/04/2017.
 */
public class MainControllerNonStringent {
    private static final int NR_THREADS = 4;

    private MainController mainController;
    @FXML
    private MainTablesController mainTablesNonStringentController;

    private Stage nonStringentMainStage;


    public void init(MainController mainController, Stage nonStringentMainStage) {
        this.mainController = mainController;
        this.nonStringentMainStage = nonStringentMainStage;

        this.mainTablesNonStringentController.init(mainController);
        this.mainTablesNonStringentController.getGeneCountsTablesController().getProteinTableController().disableNonStringentSearch(true);

        this.mainTablesNonStringentController.disableGeneFamilyView();
    }

    void searchAndShowStage(List<TextMinedObject> tmProts) {
        search(tmProts);
        showStage(true);
    }

    void showStage(boolean show) {
        if (show) {
            if(!nonStringentMainStage.isShowing()) {
                this.nonStringentMainStage.setX(this.mainController.getMainStage().getX() + 20); // cascading MainController stage
                this.nonStringentMainStage.setY(this.mainController.getMainStage().getY() + 20);
                this.nonStringentMainStage.show();
                this.nonStringentMainStage.requestFocus();
            }
        } else
            this.nonStringentMainStage.hide();
    }

    private void search(List<TextMinedObject> tmProts) {

        Task<Void> searchTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                ExecutorService executorService1 = Executors.newFixedThreadPool(NR_THREADS);
                // START ARTICLE QUERY
                final List<Future<TextMinedProtein>> tmProtFutures = new ArrayList<>();

                QuerySettings querySettings = new QuerySettings(mainController.getModel().getQuerySettings().isProteaseSearch(),
                        mainController.getModel().getQuerySettings().isBiomarkerSearch(),
                        mainController.getModel().getQuerySettings().isCustomSearch(),
                        false, true, false, false,true);

                if (!mainController.getModel().getQuerySettings().isCustomSearch()) {
                    for (TextMinedObject textMinedProtein : tmProts) {
                        Future<TextMinedProtein> artCollFuture = executorService1.submit(() -> {

                            TextMinedProtein tmProt = new TextMinedProtein((Protein) textMinedProtein.getTextMinableInput(), querySettings);

                            tmProt.queryEPmc();
                            tmProt.queryPubmed();
                            return tmProt;
                        });

                        tmProtFutures.add(artCollFuture);
                    }
                } else {
                    for (TextMinedObject textMinedProtein : tmProts) {
                        Future<TextMinedProtein> artCollFuture = executorService1.submit(() -> {
                            TextMinedProtein tmProt = new TextMinedProtein((Protein) textMinedProtein.getTextMinableInput(),
                                    mainController.getModel().getCustomSearchTerms(), querySettings);

                            tmProt.queryEPmc();
                            tmProt.queryPubmed();
                            return tmProt;
                        });

                        tmProtFutures.add(artCollFuture);
                    }
                }

                for (Future<TextMinedProtein> tmProtFut : tmProtFutures) {
                    if (isCancelled())
                        return null;

                    mainController.getModel().addTextMinedProteinDeepSearch(tmProtFut.get());
                }

                executorService1.shutdown();
                executorService1.awaitTermination(30, TimeUnit.MINUTES);

                return null;
            }
        };

        final MyProgressDialog myProgressDialog = new MyProgressDialog(searchTask, "Downloading data");
        searchTask.setOnScheduled(event -> {
            myProgressDialog.show();
        });

        searchTask.setOnSucceeded(event -> {
            mainTablesNonStringentController.initialiseViews(mainController.getModel().getTextMinedProteinsDeepSearch(), null);
            myProgressDialog.close();
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(searchTask);
        executorService.shutdown();
    }

    public MainTablesController getMainTablesController() {
        return mainTablesNonStringentController;
    }
}
