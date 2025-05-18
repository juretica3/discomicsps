package discomics;

import discomics.application.EnglishDictionary;
import discomics.application.GreekAlphabet;
import discomics.controller.MainController;
import discomics.controller.MyScene;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;



public class Main extends Application {

    public static final String NAME = "DiscOmicsPS";
    public static final String VERSION = "1.0";

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/discomics/view/MainController.fxml"));
        Parent root1 = loader1.load();
        primaryStage.setTitle(Main.NAME + " v" + Main.VERSION);

        Scene primaryScene = new MyScene(root1);
        primaryStage.setScene(primaryScene);

        MainController controller1 = loader1.getController();
        controller1.setMainStage(primaryStage);
        controller1.setHostServices(getHostServices());
        controller1.init();

        new GreekAlphabet();
        new EnglishDictionary();
        primaryStage.show();

    }

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "app name");

        launch(args);
    }

}
