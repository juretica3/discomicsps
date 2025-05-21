package discomics.controller;

import javafx.beans.NamedArg;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Created by admin on 13/03/2017.
 */
class MyAlert extends Alert {


    MyAlert(@NamedArg("alertType") AlertType alertType, Window owner) {
        super(alertType);
        DialogPane dialogPane = this.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/discomics/css/main.css").toExternalForm());
        this.initModality(Modality.APPLICATION_MODAL);
        this.initOwner(owner);
    }

//    MyAlert(@NamedArg("alertType") AlertType alertType, @NamedArg("contentText") String contentText, ButtonType... buttons) {
//        super(alertType, contentText, buttons);
//        DialogPane dialogPane = this.getDialogPane();
//        dialogPane.getStylesheets().add(getClass().getResource("/discomics/css/main.css").toExternalForm());
//        this.initModality(Modality.APPLICATION_MODAL);
//    }

}

