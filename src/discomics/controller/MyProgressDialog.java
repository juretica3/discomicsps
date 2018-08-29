package discomics.controller;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;


/**
 * Created by admin on 11/03/2017.
 */
class MyProgressDialog extends MyDialog<Void> {


    MyProgressDialog(Stage owner, Task task, String message) {
        this.initOwner(owner);

        this.setTitle("Progress");
        this.setHeaderText(message);
        this.getDialogPane().setPrefWidth(400d);
        this.initModality(Modality.APPLICATION_MODAL);
        this.setGraphic(new ImageView(this.getClass().getResource("/discomics/icon/selected_pack/clock.png").toString()));

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().add(cancelButtonType);
        Node cancelButton = this.getDialogPane().lookupButton(cancelButtonType);

        ((Button) cancelButton).setOnAction(event1 -> {
            if (task.isRunning())
                task.cancel(true);
        });

        this.setOnShown(event -> {
            double positionX = owner.getX() + (owner.getWidth() - getWidth()) / 2;
            double positionY = owner.getY() + (owner.getHeight() - getHeight()) / 2;

            setX(positionX);
            setY(positionY);
        });
    }

}
