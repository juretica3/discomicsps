package discomics.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

public class TasksProgressPane {

    @FXML
    private ImageView stepOneImg;
    @FXML
    private ImageView stepTwoImg;
    @FXML
    private ImageView stepThreeImg;
    @FXML
    private ImageView stepFourImg;


    public TasksProgressPane() {

    }

    void init() {
        setCurrentStep(-1); // initialise; can be any other integer besides 0, 1, 2 or 3
    }


    void setCurrentStep(int step) {
        File currentStepImgFile = new File("resources/discomics/icon/right_arrow_icon.png");
        Image currentStepImg = new Image(currentStepImgFile.toURI().toString());

        File completedStepImgFile = new File("resources/discomics/icon/tick_icon.png");
        Image completedStepImg = new Image(completedStepImgFile.toURI().toString());


        if (step == 0) {
            stepOneImg.setImage(currentStepImg);
        } else if (step == 1) {
            stepOneImg.setImage(completedStepImg);
            stepTwoImg.setImage(currentStepImg);
        } else if (step == 2) {
            stepOneImg.setImage(completedStepImg);
            stepTwoImg.setImage(completedStepImg);
            stepThreeImg.setImage(currentStepImg);
        } else if (step == 3) {
            stepOneImg.setImage(completedStepImg);
            stepTwoImg.setImage(completedStepImg);
            stepThreeImg.setImage(completedStepImg);
            stepFourImg.setImage(currentStepImg);
        } else if (step == 4) {
            stepOneImg.setImage(completedStepImg);
            stepTwoImg.setImage(completedStepImg);
            stepThreeImg.setImage(completedStepImg);
            stepFourImg.setImage(completedStepImg);
        } else {
            stepOneImg.setImage(null);
            stepTwoImg.setImage(null);
            stepThreeImg.setImage(null);
        }
    }
}
