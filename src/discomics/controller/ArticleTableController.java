package discomics.controller;

import discomics.Main;
import discomics.application.AlphanumComparator;
import discomics.model.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Jure on 9.9.2016.
 */
public class ArticleTableController implements TableControllable<Article> {
    private MainController mainController;

    @FXML
    private TableView<Article> articleTable;
    @FXML
    private TableColumn<Article, Boolean> relevanceTableColumn;
    @FXML
    private TableColumn<Article, String> pmidTableColumn;
    @FXML
    private TableColumn<Article, String> yearTableColumn;
    @FXML
    private TableColumn<Article, String> titleTableColumn;
    @FXML
    private TableColumn<Article, String> citedByTableColumn;
    @FXML
    private TableColumn<Article, String> urlTableColumn;
    @FXML
    private TableColumn<Article, Boolean> reviewTableColumn;

    private ObservableList<Article> articleTableData = FXCollections.observableArrayList();

    @FXML
    private TextField filterTextField;
    @FXML
    private Label articleTableLabel;
    @FXML
    private MenuItem reportErrorsMenuButton;
    @FXML
    private MenuItem removeArticlesMenuButton;
    @FXML
    private MenuItem downloadCitationButton;

    private FileChooser saveCitationFileChooser;


    public void init(MainController mainController) {
        this.mainController = mainController;

        relevanceTableColumn.setCellValueFactory(new PropertyValueFactory<>("filteringPassed"));
        relevanceTableColumn.setCellFactory(f -> {
            final Image filteringPassed = new Image("discomics/icon/check-mark-table.png");
            final Image filteringFailed = new Image("discomics/icon/question-table.png");
            return new TableCell<Article, Boolean>() {
                private ImageView imageView = new ImageView();

                @Override
                protected void updateItem(Boolean value, boolean empty) {
                    super.updateItem(value, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (value) {
                            imageView.setImage(filteringPassed);
                        } else {
                            imageView.setImage(filteringFailed);
                        }
                        setGraphic(imageView);
                    }
                }
            };
        });


        pmidTableColumn.setCellValueFactory(new PropertyValueFactory<>("pmid"));
        yearTableColumn.setCellValueFactory(new PropertyValueFactory<>("pubDateYear"));
        titleTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        citedByTableColumn.setCellValueFactory(param -> {
            int citedBy = param.getValue().getCitedByCount();
            if (citedBy == -1)
                return new SimpleStringProperty("NA");
            else
                return new SimpleStringProperty(String.valueOf(citedBy));
        });

        urlTableColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlTableColumn.setCellFactory(f -> new TableCell<Article, String>() {
            private Hyperlink hyperlink = new Hyperlink();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    hyperlink.setText(item);
                    hyperlink.setOnAction(event -> mainController.getHostServices().showDocument(item));
                    setGraphic(hyperlink);
                }
            }
        });

        reviewTableColumn.setCellValueFactory(f -> new SimpleBooleanProperty(f.getValue().getIsReview()));
        reviewTableColumn.setCellFactory(tc -> new CheckBoxTableCell<>());

        titleTableColumn.setComparator(new AlphanumComparator());
        articleTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        filterTextField.setPromptText("Filter articles ...");

        // disable filter field if table is empty
        BooleanBinding bb1 = Bindings.isEmpty(articleTableData).and(filterTextField.textProperty().isEmpty());
        filterTextField.disableProperty().bind(bb1);

        // set filtered list, predicate = true, since initially all articles are displayed
        FilteredList<Article> filteredArticleList = new FilteredList<>(articleTableData, p -> true);

        // add listener to filter text field
        filterTextField.textProperty().addListener((observable, oldValue, newValue) -> filteredArticleList.setPredicate(article -> {
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }

            String lowerCaseFilterText = newValue.toLowerCase();

            if (article.getPmid().toLowerCase().contains(lowerCaseFilterText)) // filter matches PMID
                return true;
            else if (article.getTitle().toLowerCase().contains(lowerCaseFilterText)) // filter matches title
                return true;
            else if (article.getArtAbstract().toLowerCase().contains(lowerCaseFilterText)) // filter matches abstract
                return true;
            else if (article.getAuthorString().toLowerCase().contains(lowerCaseFilterText)) // filter matches author
                return true;
            else if (article.getDoi().toLowerCase().contains(lowerCaseFilterText)) // filter matches DOI
                return true;
            else if (article.getJournalTitle().toLowerCase().contains(lowerCaseFilterText)) // filter matches journal title
                return true;
            else if (article.getJournalTitleShort().toLowerCase().contains(lowerCaseFilterText)) // filter matches short journal title
                return true;

            return false; // does not match
        }));

        SortedList<Article> sortedArticleList = new SortedList<>(filteredArticleList);
        sortedArticleList.comparatorProperty().bind(articleTable.comparatorProperty());
        articleTable.setItems(sortedArticleList);

        filteredArticleList.addListener((ListChangeListener<Article>) c -> {
            // changes color of filter text field to red if no articles found by filter
            if (!articleTableData.isEmpty() && !filterTextField.getText().isEmpty() && filteredArticleList.isEmpty())
                filterTextField.setStyle("-fx-text-fill: red;");
            else
                filterTextField.setStyle("-fx-text-fill: black;");

            setArticleTableLabel(filteredArticleList.size()); // sets article table label (nr articles displayed)
        });


        // fix column widths
        relevanceTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.06));
        pmidTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.108));
        reviewTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.108));
        titleTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.36));
        citedByTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.108));
        urlTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.138));
        yearTableColumn.prefWidthProperty().bind(articleTable.widthProperty().multiply(0.108));


        // bind context menu items disable property to table selection model
        BooleanBinding bbArticleSelected = new BooleanBinding() {
            {
                super.bind(articleTable.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                return articleTable.getSelectionModel().getSelectedItems().isEmpty();
            }
        };
        downloadCitationButton.disableProperty().bind(bbArticleSelected);
        reportErrorsMenuButton.disableProperty().bind(bbArticleSelected);
        removeArticlesMenuButton.disableProperty().bind(bbArticleSelected);

        // define file chooser to save citation
        saveCitationFileChooser = new FileChooser();
        saveCitationFileChooser.setTitle("Save Referenece As");
        saveCitationFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Research Information Systems Citation File", "*.ris"));
    }

    // --------------- CONTEXT MENU BUTTONS ACTIONS ------------------
    @FXML
    private void copyAllMenuAction() {
        copySelectedRows(articleTable);
    }

    @FXML
    private void copyPmidMenuAction() {
        StringBuilder sb = new StringBuilder();
        for (Article selectedArticle : articleTable.getSelectionModel().getSelectedItems()) {
            sb.append(selectedArticle.getPmid()).append("\n");
        }
        toClipboard(sb.toString());
    }

    @FXML
    private void copyTitleMenuAction() {
        StringBuilder sb = new StringBuilder();
        for (Article selectedArticle : articleTable.getSelectionModel().getSelectedItems()) {
            sb.append(selectedArticle.getTitle()).append("\n");
        }
        toClipboard(sb.toString());
    }

    @FXML
    private void copyAbstractMenuAction() {
        StringBuilder sb = new StringBuilder();
        for (Article selectedArticle : articleTable.getSelectionModel().getSelectedItems()) {
            sb.append(selectedArticle.getArtAbstract()).append("\n");
        }
        toClipboard(sb.toString());
    }

    @FXML
    private void copyUrlMenuAction() {
        StringBuilder sb = new StringBuilder();
        for (Article selectedArticle : articleTable.getSelectionModel().getSelectedItems()) {
            sb.append(selectedArticle.getUrl()).append("\n");
        }
        toClipboard(sb.toString());
    }

    @FXML
    private void copyDoiMenuAction() {
        StringBuilder sb = new StringBuilder();
        for (Article selectedArticle : articleTable.getSelectionModel().getSelectedItems()) {
            sb.append(selectedArticle.getDoi()).append("\n");
        }
        toClipboard(sb.toString());
    }

    private void toClipboard(String s) {
        ClipboardContent content = new ClipboardContent();
        content.putString(s);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void removeArticles(List<TextMinedObject> selectedProteins, List<Article> articlesRemove) {
        List<TextMinedObject> allTextMinedProteins = mainController.getModel().getTextMinedProteins();

        for (TextMinedObject textMinedProtein : allTextMinedProteins) {
            for (TextMinedObject selectedProtein : selectedProteins) {
                if (textMinedProtein.getTextMinableInput().equals(selectedProtein.getTextMinableInput())) {
                    if (mainController.getMainTablesController().isBiomarkerArticlesTabSelected()) {
                        textMinedProtein.getArticleCollectableBiom().removeArticlesFromCollection(articlesRemove);
                    } else if (mainController.getMainTablesController().isCustomArticlesTabSelected()) {
                        textMinedProtein.getArticleCollectableCust().removeArticlesFromCollection(articlesRemove);
                    } else if (mainController.getMainTablesController().isProteolysisArticlesTabSelected()) {
                        textMinedProtein.getArticleCollectablePlys().removeArticlesFromCollection(articlesRemove);
                    }
                }
            }
        }
        // refresh protein table with new article numbers
        mainController.getMainTablesController().getGeneCountsTablesController().getProteinTableController().getTable().refresh();
        mainController.getMainTablesController().getGeneFamilyCountsTablesController().getProteinTableController().getTable().refresh();

        // refresh article table
        this.articleTableData.removeAll(articlesRemove);
        this.articleTable.getSelectionModel().clearSelection();

        // recalculate protease count objects and repopulate protease table with updated values
        List<ProteaseCount> proteaseCounts = mainController.getModel().getProteaseCountTableEntries(selectedProteins);
        mainController.getMainTablesController().getGeneCountsTablesController().getProteaseTableController().getTableModel().clearAndUpdateTable(proteaseCounts);
        mainController.getMainTablesController().getGeneCountsTablesController().getProteaseTableController().getTableModel().clearAndUpdateTable(proteaseCounts);
    }

    @FXML
    private void removeArticlesButtonAction() {
        Alert alert = new MyAlert(Alert.AlertType.INFORMATION, mainController.getMainStage());
        alert.setTitle("Information");
        alert.setContentText("Are you sure?");
        ButtonType buttonType1 = ButtonType.YES;
        ButtonType buttonType2 = ButtonType.NO;
        alert.getButtonTypes().setAll(buttonType1, buttonType2);
        alert.setHeaderText(null);

        Optional<ButtonType> result = alert.showAndWait(); // if OK pressed, remove articles
        if (result.isPresent() && result.get() == buttonType1) {
            List<TextMinedObject> selectedProteins; // retrieve selected text mined objects (either protein or gene family depending on
            if (mainController.getMainTablesController().isGeneFamilyTabSelected()) {
                selectedProteins = this.mainController.getMainTablesController().getGeneFamilyCountsTablesController()
                        .getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            } else {
                selectedProteins = this.mainController.getMainTablesController().getGeneCountsTablesController()
                        .getProteinTableController().getTable().getSelectionModel().getSelectedItems();
            }
            List<Article> selectedArticles = this.articleTable.getSelectionModel().getSelectedItems(); // retrieve selected articles

            removeArticles(selectedProteins, selectedArticles); // remove selected articles
        }
    }

    @FXML
    private void reportErrorsMenuButtonAction() {
        String server = "www.jureteaching.com";
        int port = 21;
        String user = "discomics-error-reporting@jureteaching.com";
        String password = "yWI4z8V[5A2b";

        Alert failedAlert = new MyAlert(Alert.AlertType.WARNING, mainController.getMainStage());
        failedAlert.setTitle("Warning");
        failedAlert.setContentText("Error report was not successfully submitted. Check your internet connection and resubmit" +
                "the request. If submission fails again, try later.\n\nWould you like to remove these articles?");
        ButtonType buttonType1 = ButtonType.YES;
        ButtonType buttonType2 = ButtonType.NO;
        failedAlert.getButtonTypes().setAll(buttonType1, buttonType2);
        failedAlert.setHeaderText(null);

        // proteins and articles selected in respective tables
        List<TextMinedObject> selectedProteins;
        if (mainController.getMainTablesController().isGeneFamilyTabSelected()) {
            selectedProteins = this.mainController.getMainTablesController().getGeneFamilyCountsTablesController()
                    .getProteinTableController().getTable().getSelectionModel().getSelectedItems();
        } else {
            selectedProteins = this.mainController.getMainTablesController().getGeneCountsTablesController()
                    .getProteinTableController().getTable().getSelectionModel().getSelectedItems();
        }
        List<Article> selectedArticles = this.articleTable.getSelectionModel().getSelectedItems();

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, password);
            ftpClient.enterLocalPassiveMode();

            StringBuilder sb = new StringBuilder();

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            Date date = new Date();
            sb.append("Date submitted: ").append(dateFormat.format(date)).append("\n");
            sb.append("Software version: ").append(Main.NAME).append(" v").append(Main.VERSION).append("\n");

            IoModel model = this.mainController.getModel();

            // append query settings
            sb.append(model.getQuerySettings().toString()).append("\n\n");

            // append custom search terms
            if (model.getQuerySettings().isCustomSearch()) {
                sb.append("Custom text mining terms:").append("\n");
                for (int i = 0; i < model.getCustomSearchTerms().size(); i++) {
                    sb.append("Block ").append(i).append("\n");
                    CustomInputTermBlock inputBlock = model.getCustomSearchTerms().get(i);

                    for (CustomInputTermBlock.CustomInputTerm inputTerm : inputBlock.getCustomInputTerms()) {
                        sb.append(inputTerm.getSearchTerm()).append(" (isPhrase: ").append(inputTerm.isPhrase()).append(")\n");
                    }
                }
            }

            // append log content
            sb.append(mainController.getLogContent());
            sb.append("\n\n");

            sb.append("Selected tab: ");
            if (mainController.getMainTablesController().isProteolysisArticlesTabSelected())
                sb.append("proteolysis");
            else if (mainController.getMainTablesController().isBiomarkerArticlesTabSelected())
                sb.append("biomarker");
            else if (mainController.getMainTablesController().isCustomArticlesTabSelected())
                sb.append("custom");
            else
                sb.append("error");

            sb.append("\n\n");

            // append selected proteins
            sb.append("Selected proteins:\n");
            for (TextMinedObject selectedProtein : selectedProteins) {
                sb.append(selectedProtein.getTextMinableInput().getMainName()).append("\n");
            }
            sb.append("\n");

            // append selected erroneous articles
            sb.append("Reported erroneous articles (PMID list):").append("\n");
            for (Article selectedArticle : selectedArticles) {
                sb.append(selectedArticle.getPmid()).append("\n");
            }

            // generate remote file name based on current date and random alphanum sequence
            DateFormat dateFormat2 = new SimpleDateFormat("yyyyMMdd");
            String fileExtension = "txt";
            String randomNamePart = String.format("%s.%s", RandomStringUtils.randomAlphanumeric(10), fileExtension);
            String remoteFileName = dateFormat2.format(date) + "_" + randomNamePart;

            // push file to FTP server
            InputStream stream = IOUtils.toInputStream(sb.toString(), "UTF-8");
            boolean done = ftpClient.storeFile(remoteFileName, stream);
            stream.close();

            if (done) { // showStage success message
                Alert alert = new MyAlert(Alert.AlertType.INFORMATION, mainController.getMainStage());
                alert.setTitle("Information");
                alert.setContentText("Error report for the selected articles was successfully submitted to the " +
                        "developers. Thank you for your feedback!\n\nWould you like to remove the reported articles?");
                alert.getButtonTypes().setAll(buttonType1, buttonType2);
                alert.setHeaderText(null);

                Optional<ButtonType> result = alert.showAndWait(); // if OK pressed, remove articles
                if (result.isPresent() && result.get() == buttonType1) {
                    removeArticles(selectedProteins, selectedArticles);
                }
            } else { // showStage error message
                Optional<ButtonType> result = failedAlert.showAndWait();
                if (result.isPresent() && result.get() == buttonType1) { // if OK pressed, remove articles
                    removeArticles(selectedProteins, selectedArticles);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Optional<ButtonType> result = failedAlert.showAndWait(); // showStage error message
            if (result.isPresent() && result.get() == buttonType1) { // if OK pressed, remove articles
                removeArticles(selectedProteins, selectedArticles);
            }
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                failedAlert.showAndWait();
            }
        }
    }

    @FXML
    private void downloadCitationAction() {
        Article article = articleTable.getSelectionModel().getSelectedItem();

        StringBuilder sb = new StringBuilder();
        sb.append("TY  - JOUR\n");
        sb.append("DB  - PMC\n");

        //loop through authors
        for (Article.ArtAuthor artAuthor : article.getArtAuthors()) {
            sb.append(artAuthor.getAuthorRisLine());
        }

        sb.append("T1  - ").append(article.getTitle()).append("\n"); // title OK

        // year published YYYY/MM/DD
        sb.append("PY  - ").append(article.getPubDateYear());
        if (!article.getPubDateMonthName().isEmpty()) {
            sb.append("/").append(article.getPubDateMonthName());
            if (!article.getPubDateDay().isEmpty()) {
                sb.append("/").append(article.getPubDateDay());
            }
        }
        sb.append("\n");

        if (!article.getArtAbstract().isEmpty())
            sb.append("AB  - ").append(article.getArtAbstract()).append("\n"); // abstract
        if (!article.getStartPage().isEmpty())
            sb.append("SP  - ").append(article.getStartPage()).append("\n"); // start page
        if (!article.getEndPage().isEmpty())
            sb.append("EP  - ").append(article.getEndPage()).append("\n"); // end page
        if (!article.getJournalVol().isEmpty())
            sb.append("VL  - ").append(article.getJournalVol()).append("\n"); // volume
        if (!article.getJournalIssue().isEmpty())
            sb.append("IS  - ").append(article.getJournalIssue()).append("\n"); // issue
        if (!article.getPmid().isEmpty())
            sb.append("U1  - ").append(article.getPmid()).append("[pmid]\n"); // pmid format XXXXXXXX[pmid]
        if (!article.getDoi().isEmpty())
            sb.append("DO  - ").append(article.getDoi()).append("\n"); // doi
        if (!article.getPmcid().isEmpty())
            sb.append("AN  - ").append(article.getPmcid()).append("\n"); // pmcid
        if (!article.getUrl().isEmpty())
            sb.append("UR  - ").append(article.getUrl()).append("\n"); // url OK
        if (!article.getJournalTitleShort().isEmpty())
            sb.append("J1  - ").append(article.getJournalTitleShort()).append("\n"); // short journal name
        if (!article.getJournalTitle().isEmpty())
            sb.append("JF  - ").append(article.getJournalTitle()).append("\n"); // full journal name

        try {
            byte[] bytes = sb.toString().getBytes("UTF-8");
            FileOutputStream fos;
            File fileToBeSaved = saveCitationFileChooser.showSaveDialog(mainController.getMainStage());

            if (fileToBeSaved == null)
                return;

            fos = new FileOutputStream(fileToBeSaved);
            fos.write(bytes);
            fos.close();

        } catch (IOException e) {
            ExceptionDialog exceptionDialog = new ExceptionDialog(e, mainController.getMainStage());
            exceptionDialog.showAndWait();
        }
    }

    void linkTextMinedObjectTable(TableView<TextMinedObject> tableView) {
        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super TextMinedObject>) observable -> {
            List<TextMinedObject> selectedItems = tableView.getSelectionModel().getSelectedItems();
            List<Article> articlesToDisplay = TableControllable.super.retrieveArticlesCustom(selectedItems);
            clearAndUpdateTable(articlesToDisplay);
        });
    }

    void initialiseTable() {
        articleTableData.clear();
    }

    TableView<Article> getTable() {
        return articleTable;
    }

    TextField getFilterTextField() {
        return filterTextField;
    }

    void clearFilterFieldText() {
        filterTextField.clear();
    }

    void clearAndUpdateTable(List<Article> articles) {
        if (articleTable == null)
            return;

        TableColumn sortColumn = null;
        TableColumn.SortType st = null;

        if (articleTable.getSortOrder().size() > 0) {
            sortColumn = articleTable.getSortOrder().get(0);
            st = sortColumn.getSortType();
        }

        articleTableData.clear();
        articleTableData.addAll(articles);

        if (sortColumn != null) {
            articleTable.getSortOrder().add(sortColumn);
            sortColumn.setSortType(st);
            sortColumn.setSortable(true);
        }
    }

    private void setArticleTableLabel(int nrArt) {
        articleTableLabel.setText("Article Table (" + nrArt + ")");
    }
}
