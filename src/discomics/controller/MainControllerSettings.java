package discomics.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class MainControllerSettings {
    private MainController mainController;
    private Stage settingsStage;

    @FXML
    private Spinner<Integer> spinnerMaxArticlesRetrieved;
    @FXML
    private Spinner<Integer> spinnerSearchThreadsNr;

    void init(MainController mainController, Stage settingsStage) {
        this.mainController = mainController;
        this.settingsStage = settingsStage;

        initialiseSpinnerSearchThreadsNr();
        spinnerSearchThreadsNr.setEditable(true);
        spinnerMaxArticlesRetrieved.setEditable(true);


        // spinner threads nr listeners
        spinnerSearchThreadsNr.getEditor().setOnAction(event -> {
            int newValue = spinnerSearchThreadsNr.getValue();
            setSpinnerSearchThreadsNr(newValue);
        });

        spinnerSearchThreadsNr.focusedProperty().addListener(event -> {
            int newValue = spinnerSearchThreadsNr.getValue();
            setSpinnerSearchThreadsNr(newValue);
            commitEditorText(spinnerSearchThreadsNr);
        });


        // spinner maximum articles retrieved listeners
        spinnerMaxArticlesRetrieved.focusedProperty().addListener(event -> {
            commitEditorText(spinnerMaxArticlesRetrieved);
        });

    }

    private <T> void commitEditorText(Spinner<T> spinner) {
        if (!spinner.isEditable()) return;
        String text = spinner.getEditor().getText();
        SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
        if (valueFactory != null) {
            StringConverter<T> converter = valueFactory.getConverter();
            if (converter != null) {
                T value = converter.fromString(text);
                valueFactory.setValue(value);
            }
        }
    }

    // initialise spinner maximum to either be default of 4 (defined in FXML) or if number of cores is lower then use number of cores
    private void initialiseSpinnerSearchThreadsNr() {
        int nrcores = Runtime.getRuntime().availableProcessors();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinnerSearchThreadsNr.getValueFactory();
        int spinnerMaxValue = valueFactory.getMax();

        if (nrcores < spinnerMaxValue) {
            valueFactory.setMax(nrcores);
        }

        mainController.setNumberSearchThreads(this.spinnerSearchThreadsNr.getValue());
    }

    // set spinner value; if desired value higher then number of cores, then set value as number of cores
    private void setSpinnerSearchThreadsNr(int value) {
        int nrcores = Runtime.getRuntime().availableProcessors();

        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinnerSearchThreadsNr.getValueFactory();
        int spinnerMaxValue = valueFactory.getMax();

        if (nrcores > spinnerMaxValue) {
            this.spinnerSearchThreadsNr.getValueFactory().setValue(value);
        } else {
            this.spinnerSearchThreadsNr.getValueFactory().setValue(nrcores);
        }
    }

    void showStage() {
        // retrieve and set current values
        this.setSpinnerSearchThreadsNr(mainController.getNumberSearchThreads());
        this.spinnerMaxArticlesRetrieved.getValueFactory().setValue(mainController.getMaxArticlesRetrieved());

        // show settings window
        this.settingsStage.show();
    }

    @FXML
    void saveAndCloseAction() {
        mainController.setNumberSearchThreads(this.spinnerSearchThreadsNr.getValue());
        mainController.setMaxArticlesRetrieved(this.spinnerMaxArticlesRetrieved.getValue());

        this.settingsStage.close();
    }
}
