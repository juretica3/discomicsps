package discomics.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

/**
 * Created by Jure on 02/12/2016.
 */
class ExceptionDialog {

    private Alert alert;

    ExceptionDialog(Throwable e, Window owner) {
        alert = new MyAlert(Alert.AlertType.ERROR, owner);

        String title = "";
        if(e instanceof IOException)
            title = "An error has occurred during I/O.";
        else if (e instanceof InterruptedException)
            title = "Task has been interrupted.";
        else if(e instanceof ExecutionException)
            title = "Error during task execution.";
        else if (e instanceof ClassNotFoundException)
            title = "Opening of the file has failed. " +
                    "This might have occurred due to discomics version incompatibilities. To resolve the problem open " +
                    "the file with the discomics version with which it was generated.";

        alert.setTitle("Exception Dialog");
        alert.setHeaderText("Oh noooo, an Exception has occurred!");
        alert.setContentText("The developers would appreciate seeing this one. " +
                "Share the Exception stacktrace with us by visiting www.solbioscience.com.\n\n" + title);

        StringWriter sw = new StringWriter();
        sw.write(e.getLocalizedMessage());
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw); // prints to printWriter, which will print to GUI
        e.printStackTrace(); // prints to console
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);
    }

    void showAndWait() {
        alert.showAndWait();
    }


}
