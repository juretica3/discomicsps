package discomics.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class MyMainTaskProgressDialog extends MyProgressDialog {

    private TasksProgressPane tasksProgressPaneController;

    MyMainTaskProgressDialog(Stage parent, Task task, String message) {
        super(parent, task, message);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/discomics/view/TasksProgressPane.fxml"));
            Parent tasksProgressPane = loader.load();

            this.tasksProgressPaneController = loader.getController();
            this.tasksProgressPaneController.init();

            getDialogPane().setContent(tasksProgressPane);

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    void setProgress(int step) {
        tasksProgressPaneController.setCurrentStep(step);
    }

}
