package discomics.controller;

import discomics.model.*;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

abstract class TextMinedObjectDetailsPane extends VBox {

    TextMinedObjectDetailsPane() {
        super();
        setSpacing(5d);
    }

    abstract void construct(TextMinedObject textMinedObject);


    void addTextFieldToGrid(String text, boolean isHeader) {
        if (text.trim().isEmpty())
            text = "Not available";

        Label label = new TextMinedObjectDetailsPane.MyLabel(text, isHeader);
        getChildren().add(label);
    }

    void addDrugMiningTitleToGrid(String title, MainController mainController) {
        HBox hBox = new HBox(10d);
        hBox.setAlignment(Pos.CENTER_LEFT);

        MyLabel titleLabel = new MyLabel(title, true);
        hBox.getChildren().add(titleLabel);

        Button button = new Button("Mine");
        button.setPrefHeight(10d);

        hBox.getChildren().add(button);
        getChildren().add(hBox);
    }


    class MyLabel extends Label {

        private boolean isHeader;

        MyLabel(String text, boolean isHeader) {
            super(text);
            this.isHeader = isHeader;

            setWrapText(true);
            maxHeight(500d);

            if (isHeader)
                setStyle("-fx-font-weight: bold;");

            setContextMenuListener();
        }

        void setContextMenuListener() {
            if (isHeader) { // copy contents of header label and all further labels under that header if label IS a header
                setOnMouseClicked(event -> {
                    MouseButton mouseButton = event.getButton();
                    if (mouseButton == MouseButton.SECONDARY) {
                        MenuItem copyButton = new MenuItem("Copy");
                        copyButton.setOnAction(event1 -> {
                            StringBuilder sb = new StringBuilder();

                            boolean flagStartCopy = false;
                            // loop through all children of protein details pane
                            for (Node child : TextMinedObjectDetailsPane.super.getChildren()) {
                                if (child instanceof MyLabel) {
                                    MyLabel currentLabel = (MyLabel) child;

                                    if (flagStartCopy) {
                                        if (currentLabel.isHeader()) { // stop coying when reach next header
                                            break;
                                        } else { // copy
                                            sb.append(currentLabel.getText()).append("\n");
                                        }
                                    }

                                    // start copying details in children after reaching current header (next field will be copied, until reaching next header)
                                    if (currentLabel == this) {
                                        flagStartCopy = true;
                                    }

                                } else if (child instanceof HBox) { // only instance of drug interaction header (where mine button and header part of hbox)
                                    HBox currentHBox = (HBox) child;
                                    ObservableList<Node> children = currentHBox.getChildren();
                                    if (children.get(0) == this) {
                                        flagStartCopy = true;
                                    }
                                }
                            }

                            Clipboard clipboard = Clipboard.getSystemClipboard();
                            ClipboardContent clipboardContent = new ClipboardContent();
                            clipboardContent.putString(sb.toString());
                            clipboard.setContent(clipboardContent);
                        });
                        ContextMenu contextMenu = new ContextMenu(copyButton);
                        contextMenu.show(this, null, 0, 0);
                    }
                });
            } else { // only copy the contents of the current Label if it is NOT a header
                setOnMouseClicked(event -> {
                    MouseButton mouseButton = event.getButton();
                    if (mouseButton == MouseButton.SECONDARY) {
                        MenuItem copyButton = new MenuItem("Copy");
                        copyButton.setOnAction(event1 -> {
                            Clipboard clipboard = Clipboard.getSystemClipboard();
                            ClipboardContent clipboardContent = new ClipboardContent();
                            clipboardContent.putString(this.getText());
                            clipboard.setContent(clipboardContent);
                        });
                        ContextMenu contextMenu = new ContextMenu(copyButton);
                        contextMenu.show(this, null, 0, 0);
                    }
                });
            }
        }

        public boolean isHeader() {
            return isHeader;
        }
    }

    void constructNoSelection() {
        addTextFieldToGrid("No information can be shown: no selection in corresponding table.", false);
    }

    abstract void constructMultipleSelection(final List<TextMinedObject> objects);


}
