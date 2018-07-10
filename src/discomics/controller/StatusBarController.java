package discomics.controller;

import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 13.9.2016.
 */
public class StatusBarController {

    private final List<MyTask> activeTasks = new ArrayList<>();

    @FXML
    private ProgressBar progressBar;
    @FXML
    private Rectangle statusSquare;

    public void init() {

        BooleanBinding bb = new BooleanBinding() {
            @Override
            protected boolean computeValue() {
                return activeTasks.isEmpty();
            }
        };
        progressBar.disableProperty().bind(bb);
    }

    public void updateCumulativeProgress(Task task, double progress) throws Exception {
        if (!progressBar.isDisabled()) {

            for(MyTask myTask: activeTasks) {
                if(myTask.getTask().equals(task)) {
                    myTask.updateCumProgress(progress);
                    break;
                }
            }

            progressBar.setProgress(computeProgress());
        } else
            throw new Exception("Progress bar disabled, no tasks ongoing");
    }

    private double computeProgress() {
        if(activeTasks.size() == 0)
            return 0;

        double progress = 0d;
        for (MyTask progressTask : activeTasks) {
            progress += progressTask.getProgress();
        }
        progress /= activeTasks.size();
        return progress;
    }

    public void startTask(Task task, boolean canBeCancelled) {
        activeTasks.add(new MyTask(task, 0, canBeCancelled));

        progressBar.setProgress(computeProgress());
        statusSquare.setStyle("-fx-fill: red;");
    }

    public synchronized void finishTask(Task task) {
        for(MyTask myTask: activeTasks) {
            if(myTask.getTask() == task) {
                activeTasks.remove(myTask);
                System.out.println("FINISHED!");
            }
        }

        progressBar.setProgress(computeProgress());

        if (activeTasks.size() == 0)
            statusSquare.setStyle("-fx-fill: green;");
    }

    public boolean cancelTask(Task task) {
        for(MyTask myTask: activeTasks) {
            if(myTask.getTask().equals(task)) {
                return myTask.getTask().cancel(true);
            }
        }
        return false;
    }

    private class MyTask {
        private Task task;
        private double progress;
        private boolean canBeCancelled;

        public MyTask(Task task, double progress, boolean canBeCancelled) {
            this.task = task;
            this.progress = progress;
            this.canBeCancelled = canBeCancelled;
        }

        public double getProgress() {
            return progress;
        }

        public Task getTask() {
            return task;
        }

        public void updateCumProgress(double progress) {
            this.progress += progress;
        }
    }
}
