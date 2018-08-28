package discomics.controller;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;

abstract class TextMiningTask<T> extends Task<T> {
    private Stage parent;
    private final MyProgressDialog myProgressDialog;
    final StopWatch stopWatch = new StopWatch();

    TextMiningTask(Stage parent) {
        this.parent = parent;
        myProgressDialog = new MyProgressDialog(this, "Downloading data ...", parent);

        this.setOnScheduled(event -> {
            myProgressDialog.show();
        });

        this.setOnFailed(event -> {
            myProgressDialog.close();
            getException().printStackTrace();
            // socket exceptions are thrown by all downloadParseArticles tasks in DownloadFileHTTP and propagated to the top if internet connectivity interrupted
            if (getException().getLocalizedMessage().equals("java.net.SocketException")) {
                Alert alert = new MyAlert(Alert.AlertType.WARNING, parent);
                alert.setTitle("Warning");
                alert.setHeaderText("Lost internet connectivity");
                alert.setContentText("Connect to the internet and try again. " +
                        "If connected to the internet, one of the data servers might be currently offline: try again later.");
                alert.show();
                this.cancel(); // cancel the running task
                return;
            }

            ExceptionDialog exceptionDialog = new ExceptionDialog(getException(), parent);
            exceptionDialog.showAndWait();
        });

        this.setOnCancelled(event -> {
            myProgressDialog.close();

            Alert alert = new MyAlert(Alert.AlertType.WARNING, parent);
            alert.setHeaderText("Interrupted");
            alert.setTitle("Interrupted!");
            alert.show();
        });
    }

    // called by setOnSucceeded listener set after instantiation of the class
    void onSucceeded(List<String> failedCases) {
        myProgressDialog.close();

        long timeSec = stopWatch.getTime() / 1000;
        int timeMin = (int) Math.floor(timeSec / 60);
        int remainingSec = Math.round(timeSec - timeMin * 60);

        if (failedCases.size() > 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("Time taken: ").append(timeMin).append(" minutes and ").append(remainingSec).append(" seconds.");
            msg.append("Task completed, but errors have occurred with these inputs:\n");
            for (String failedCase : failedCases) {
                msg.append(failedCase).append("\n");
            }
            Alert alert = new MyAlert(Alert.AlertType.WARNING, this.parent);
            alert.setTitle("Warning!");
            alert.setHeaderText("");
            alert.setContentText(msg.toString());
            alert.show();
        } else {
            Alert alert = new MyAlert(Alert.AlertType.INFORMATION, this.parent);
            alert.setTitle("Information");
            alert.setHeaderText("");
            alert.setContentText("Completed successfully!\nTime taken: " + timeMin + " minutes and " + remainingSec + " seconds.");
            alert.show();
        }
    }

    void setProgressStep(int step) {
        myProgressDialog.setProgress(step);
    }
}
