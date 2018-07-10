package discomics.controller;

import discomics.model.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jure on 29/01/2017.
 */
public class DetailsArticleController {

    private String redStyle = "-fx-fill: red;";
    private String blueStyle = "-fx-fill: blue;";
    private String boldStyle = "-fx-font-weight: bold;";

    @FXML
    private AnchorPane containerArticleDetails;

    public void init() {
        showEmptyArticleDetails();
    }


    private void setArticleDetails(ArticleDetailsPane detailsGridPane) {
        Platform.runLater(() -> {
            ArrayList<Node> childList = new ArrayList<>(containerArticleDetails.getChildren());
            for (Node child : childList) {
                containerArticleDetails.getChildren().remove(child);
            }

            containerArticleDetails.getChildren().add((detailsGridPane));
            AnchorPane.setBottomAnchor(detailsGridPane, 5d);
            AnchorPane.setTopAnchor(detailsGridPane, 5d);
            AnchorPane.setLeftAnchor(detailsGridPane, 0d);
            AnchorPane.setRightAnchor(detailsGridPane, 5d);
        });
    }

    void showArticleDetailsProteolysis(Article article, List<TextMinedObject> tmProts) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ArticleDetailsPane detailsGridPane;

                if (article == null)
                    showEmptyArticleDetails();
                else {

                    detailsGridPane = new ArticleDetailsPane() {
                        @Override
                        void styleAbstractText() {

                            TextMinedObject textMinedProtein = null;
                            for (TextMinedObject tmProt : tmProts) {
                                if (tmProt.getArticleCollectablePlys().getArticleCollection().contains(article)) {
                                    textMinedProtein = tmProt;
                                }
                            }
                            List<String> textMinedObjectNames;
                            if (textMinedProtein != null) { // if TextMinedProtein refers to protein
                                textMinedObjectNames = textMinedProtein.getTextMinableInput().getTextMiningNames(null);
                                styleAbstractTextProteolysis(this.textArea, textMinedObjectNames);
                            }
                        }
                    };
                    detailsGridPane.construct(article);
                    setArticleDetails(detailsGridPane);
                }
                return null;
            }
        };

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
    }

    void showArticleDetailsBiomarker(Article article, List<TextMinedObject> tmProts) {

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ArticleDetailsPane detailsGridPane;

                if (article == null)
                    showEmptyArticleDetails();
                else {
                    detailsGridPane = new ArticleDetailsPane() {
                        @Override
                        void styleAbstractText() {
                            TextMinedObject textMinedProtein = null;
                            for (TextMinedObject tmObject : tmProts) {
                                if (tmObject.getArticleCollectableBiom().getArticleCollection().contains(article)) {
                                    textMinedProtein = tmObject;
                                }
                            }
                            List<String> textMinedObjectNames;
                            if (textMinedProtein != null) { // if TextMinedProtein refers to protein
                                textMinedObjectNames = textMinedProtein.getTextMinableInput().getTextMiningNames(null);
                                styleAbstractTextBiomarker(this.textArea, textMinedObjectNames);
                            }
                        }
                    };
                    detailsGridPane.construct(article);
                    setArticleDetails(detailsGridPane);
                }
                return null;
            }
        };

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
    }

    void showArticleDetailsCustom(Article article, List<TextMinedObject> tmProts, List<CustomInputTermBlock> customTerms) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                ArticleDetailsPane detailsGridPane;

                if (article == null)
                    showEmptyArticleDetails();
                else {
                    detailsGridPane = new ArticleDetailsPane() {
                        @Override
                        void styleAbstractText() {
                            TextMinedObject textMinedProtein = null;
                            for (TextMinedObject tmProt : tmProts) {
                                if (tmProt.getArticleCollectableCust().getArticleCollection().contains(article)) {
                                    textMinedProtein = tmProt;
                                }
                            }

                            List<String> textMinedObjectNames;
                            if (textMinedProtein != null) { // if TextMinedProtein refers to protein
                                textMinedObjectNames = textMinedProtein.getTextMinableInput().getTextMiningNames(null);
                                styleAbstractTextCustom(this.textArea, textMinedObjectNames, customTerms);
                            }
                        }
                    };
                    detailsGridPane.construct(article);
                    setArticleDetails(detailsGridPane);
                }
                return null;
            }
        };
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
    }


    void showEmptyArticleDetails() {
        ArticleDetailsPane details = new ArticleDetailsPane() {
            @Override
            void styleAbstractText() {

            }
        };
        details.constructNothingCanBeShown();
        setArticleDetails(details);
    }


    private void styleAbstractTextProteolysis(InlineCssTextArea textArea, List<String> textMinedObjectNames) {
        String abstractText = textArea.getText().toLowerCase().replaceAll("-", " ");
        highlightProteinNames(textArea, abstractText, textMinedObjectNames);

        // highlight ADAM and ADAMTS proteases
        if (IoModel.getProteaseFamilySelected().equals(IoModel.getADAM())) {
            for (String name : IoModel.getADAM().getHighlightTerms()) { // red and bold if this is protease selected
                styleTextWordBreakBefore(textArea, abstractText, name, redStyle + boldStyle);
            }
            for (String name : IoModel.getADAMTS().getHighlightTerms()) {
                styleTextWordBreakBefore(textArea, abstractText, name, redStyle + boldStyle);
            }
        } else {
            for (String name : IoModel.getADAM().getHighlightTerms()) { // bold only if this is not protease selected
                styleTextWordBreakBefore(textArea, abstractText, name, boldStyle);
            }
            for (String name : IoModel.getADAMTS().getHighlightTerms()) {
                styleTextWordBreakBefore(textArea, abstractText, name, boldStyle);
            }
        }

        // highlight MMP proteases
        if (IoModel.getProteaseFamilySelected().equals(IoModel.getMMP())) {
            for (String name : IoModel.getMMP().getHighlightTerms()) {
                styleTextWordBreakBefore(textArea, abstractText, name, boldStyle + redStyle);
            }
        } else {
            for (String name : IoModel.getMMP().getHighlightTerms()) {
                styleTextWordBreakBefore(textArea, abstractText, name, boldStyle);
            }
        }

        // highlight CTS proteases

        if (IoModel.getProteaseFamilySelected().equals(IoModel.getCTS())) {
            for (String name : IoModel.getCTS().getHighlightTerms()) {
                styleTextWordBreakBefore(textArea, abstractText, name, boldStyle + redStyle);
            }
        } else {
            for (String name : IoModel.getCTS().getHighlightTerms()) {
                styleTextWordBreakBefore(textArea, abstractText, name, boldStyle);
            }
        }
        // highlight search verb roots (hydroly, degradat, etc)
        for (String queryTerm : IoModel.getSearchVerbRoots()) {
            styleText(textArea, abstractText, queryTerm, blueStyle + boldStyle);
        }
    }

    private void highlightProteinNames(InlineCssTextArea textArea, String abstractText, List<String> textMinedObjectNames) {
        for (String text : textMinedObjectNames) {
            styleTextWordBreakBefore(textArea, abstractText, text, redStyle + boldStyle); // as is highlighting
            text = text.replaceAll("[\\p{Punct}]+", " ");
            text = text.replaceAll("  ", " ");
            styleTextWordBreakBefore(textArea, abstractText, text, redStyle + boldStyle); // without punctuation highlighting
        }
    }

    private void styleAbstractTextBiomarker(InlineCssTextArea textArea, List<String> textMinedObjectNames) {
        String abstractText = textArea.getText().toLowerCase().replaceAll("-", " ");
        highlightProteinNames(textArea, abstractText, textMinedObjectNames);

        for (String name : IoModel.getBLOOD().getSearchTerms()) {
            styleTextWordBreakBefore(textArea, abstractText, name, blueStyle + boldStyle);
        }

        for (String name : IoModel.getSALIVA().getSearchTerms()) {
            styleTextWordBreakBefore(textArea, abstractText, name, blueStyle + boldStyle);
        }

        for (String name : IoModel.getURINE().getSearchTerms()) {
            styleTextWordBreakBefore(textArea, abstractText, name, blueStyle + boldStyle);
        }

        for (String name : IoModel.getCUSTOM().getSearchTerms()) {
            styleTextWordBreakBefore(textArea, abstractText, name, blueStyle + boldStyle);
        }

        styleTextWordBreakBefore(textArea, abstractText, "biomarker", blueStyle + boldStyle);
        styleTextWordBreakBefore(textArea, abstractText, "marker", blueStyle + boldStyle);
    }

    private void styleAbstractTextCustom(InlineCssTextArea textArea, List<String> textMinedObjectNames, List<CustomInputTermBlock> customTerms) {
        String abstractText = textArea.getText().toLowerCase().replaceAll("-", " ");

        // highlight all protein names in red
        highlightProteinNames(textArea, abstractText, textMinedObjectNames);

        // highlight all user defined search terms in blue
        for (CustomInputTermBlock termBlock : customTerms) {
            for (CustomInputTermBlock.CustomInputTerm term : termBlock.getCustomInputTerms()) {
                if (term.isPhrase()) // if term searched as a whole phrase highlight whole term as phrase
                    styleTextWordBreakBefore(textArea, abstractText, term.getSearchTerm(), blueStyle + boldStyle);
                else { // if term not searched as phrase, split the words in the term and highlight each separately
                    String[] array = term.getSearchTerm().split("\\s+");
                    for (String text1 : array) {
                        styleTextWordBreakBefore(textArea, abstractText, text1, blueStyle + boldStyle);
                    }
                }
            }
        }
    }

    private void styleTextWordBreakBefore(InlineCssTextArea textArea, String abstractText, String query, String style) {
        String queryBreakBefore = "\\b" + query.toLowerCase();
        styleText(textArea, abstractText, queryBreakBefore, style);
    }

    private void styleText(InlineCssTextArea textArea, String abstractText, String query, String style) {
        Pattern p = Pattern.compile(query.toLowerCase());
        Matcher m = p.matcher(abstractText);

        while (m.find()) {
            textArea.setStyle(m.start(), m.end(), style);
        }
    }

    abstract private class ArticleDetailsPane extends VBox {

        InlineCssTextArea textArea;
        VirtualizedScrollPane<InlineCssTextArea> vsPane;

        ArticleDetailsPane() {
            super();
            setSpacing(5d);

            textArea = new InlineCssTextArea();
            vsPane = new VirtualizedScrollPane<>(textArea);

            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setBackground(Background.EMPTY);

            textArea.prefHeightProperty().bind(heightProperty()); // make text area same height as VBox container

        }

        void construct(Article article) {

            String title1 = "PMID";
            String title2 = "Year";
            String title3 = "Authors";
            String title4 = "Journal";
            String title5 = "Title";
            String title6 = "Abstract";

            textArea.appendText(title1 + "\n");
            textArea.appendText(article.getPmid() + "\n\n");

            int startIndex = 0;
            int endIndex = title1.length();
            textArea.setStyle(startIndex, endIndex, "-fx-font-weight:bold;");

            textArea.appendText(title2 + "\n");
            textArea.appendText(String.valueOf(article.getPubDateYear()) + "\n\n");

            startIndex = endIndex + article.getPmid().length() + 3;
            endIndex = startIndex + title2.length();
            textArea.setStyle(startIndex, endIndex, "-fx-font-weight:bold;");

            textArea.appendText(title3 + "\n");
            textArea.appendText(article.getAuthorString() + "\n\n");

            startIndex = endIndex + String.valueOf(article.getPubDateYear()).length() + 3;
            endIndex = startIndex + title3.length();
            textArea.setStyle(startIndex, endIndex, "-fx-font-weight:bold;");

            textArea.appendText(title4 + "\n");
            textArea.appendText(article.getJournalTitle() + "\n\n");

            startIndex = endIndex + article.getAuthorString().length() + 3;
            endIndex = startIndex + title4.length();
            textArea.setStyle(startIndex, endIndex, "-fx-font-weight:bold;");

            textArea.appendText(title5 + "\n");
            textArea.appendText(article.getTitle() + "\n\n");

            startIndex = endIndex + article.getJournalTitle().length() + 3;
            endIndex = startIndex + title5.length();
            textArea.setStyle(startIndex, endIndex, "-fx-font-weight:bold;");

            textArea.appendText(title6 + "\n");
            textArea.appendText(article.getArtAbstract());

            startIndex = endIndex + article.getTitle().length() + 3;
            endIndex = startIndex + title6.length();
            textArea.setStyle(startIndex, endIndex, "-fx-font-weight:bold;");

            styleAbstractText(); // call to abstract method that will highlight text separately defined for proteolysis/biomarker/custom modules
            getChildren().add(vsPane); // add scroll pane to container to show article details
        }

        void constructNothingCanBeShown() {
            textArea.appendText("No information can be shown: no selection in corresponding table.");
        }

        abstract void styleAbstractText();
    }
}
