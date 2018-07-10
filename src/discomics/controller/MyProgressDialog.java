package discomics.controller;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by admin on 11/03/2017.
 */
class MyProgressDialog extends MyDialog<Void> {


    MyProgressDialog(Task task, String message) {
        final ProgressBar progressBar = new ProgressBar(0);

        this.setTitle("Progress");
        this.setHeaderText(message);
        this.getDialogPane().setPrefWidth(400d);
        this.initModality(Modality.APPLICATION_MODAL);
        this.setGraphic(new ImageView(this.getClass().getResource("/discomics/icon/selected_pack/clock.png").toString()));
        this.getDialogPane().setContent(progressBar);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().add(cancelButtonType);
        Node cancelButton = this.getDialogPane().lookupButton(cancelButtonType);

        ((Button) cancelButton).setOnAction(event1 -> {
            if (task.isRunning())
                task.cancel(true);
        });

        progressBar.progressProperty().bind(task.progressProperty());
    }

    MyProgressDialog(Task task, String message, Stage parent) {
        this(task, message);

        this.setOnShown(event -> {
            double positionX = parent.getX() + (parent.getWidth() - getWidth()) / 2;
            double positionY = parent.getY() + (parent.getHeight() - getHeight()) / 2;

            setX(positionX);
            setY(positionY);
        });
    }
}
