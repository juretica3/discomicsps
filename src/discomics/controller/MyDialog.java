package discomics.controller;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Created by admin on 13/03/2017.
 */
class MyDialog<T> extends Dialog<T> {

    MyDialog() {
        super();
        DialogPane dialogPane = this.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/discomics/css/main.css").toExternalForm());
        this.initModality(Modality.APPLICATION_MODAL);
    }

    public void centre() {
        Window owner = getOwner();

        double x = owner.getX() + owner.getWidth() / 2 - getWidth() / 2;
        double y = owner.getY() + owner.getHeight() / 2 - getHeight() / 2;

        setX(x);
        setY(y);
    }
}
