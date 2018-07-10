package discomics.model;

import discomics.application.ArticleScanProtease;
import discomics.application.DownloadFileHTTP;
import org.controlsfx.dialog.ExceptionDialog;

import java.io.Serializable;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jure on 4.9.2016.
 */
public class ArticleCollectablePlys extends ArticleCollectable implements Serializable {
    static final transient long serialVersionUID = 4277735462733455L;

    private static final String queryStemSTRINGTabular = "http://string-db.org/api/psi-mi-tab/interactions?";

    private ProteaseFamily proteaseFamily;
    private List<NetworkEdge> physicalInteractions = null;

    ArticleCollectablePlys() {
        super();
        this.proteaseFamily = IoModel.getProteaseFamilySelected();

        articleQueryableEPmc = new ArticleQueryableEPmc() {

//            @Override
//            List<Article> downloadParseFilterArticles(Protein protein, QuerySettings querySettings) throws SocketException {
//                List<Article> articles = super.downloadParseFilterArticles(protein, querySettings); // downloadParseArticles all articles found, filter according to protein nomenclature presence
//                if (querySettings.isNameFilteringEnable()) { // filter according to proteolysis verb presence
//                    articles = new ArrayList<>(filterArticlesProteolysisVerb(articles)); // filter for unwanted verb forms (DNA fragmentation)
//                }
//                return articles;
//            }

            @Override
            public String constructSearchQueryUrl(List<String> searchTermProtNames, QuerySettings querySettings) {
                return constructSearchQueryUrlProteolysis(searchTermProtNames, this, !querySettings.isSearchFullText());
            }
        };

        articleQueryablePubmed = new ArticleQueryablePubmed() {
//
//            @Override
//            List<Article> downloadParseFilterArticles(Protein protein, QuerySettings querySettings) throws SocketException {
//                List<Article> articles = super.downloadParseFilterArticles(protein, querySettings);
//                articles = filterArticlesProteolysisVerb(articles);
//                return articles;
//            }

            @Override
            public String constructSearchQueryUrl(List<String> searchTermProtNames, QuerySettings querySettings) {
                return constructSearchQueryUrlProteolysis(searchTermProtNames, this, false);
            }
        };
    }


    @Override
    public void queryEPmc(TextMinableInput tmInput, QuerySettings querySettings) throws SocketException {
        String geneNameLowerCase = tmInput.getMainName().toLowerCase(); // do not perform proteolysis text-mining for a protein that is a protease
        if (geneNameLowerCase.contains("mmp") || geneNameLowerCase.contains("adam") || geneNameLowerCase.contains("cts"))
            return;

        super.queryEPmc(tmInput, querySettings);
    }

    @Override
    public void queryPubmed(TextMinableInput tmInput, QuerySettings querySettings) throws SocketException {
        String geneNameLowerCase = tmInput.getMainName().toLowerCase(); // do not perform proteolysis text-mining for a protein that is a protease
        if (geneNameLowerCase.contains("mmp") || geneNameLowerCase.contains("adam") || geneNameLowerCase.contains("cts"))
            return;

        super.queryPubmed(tmInput, querySettings);
    }

    @Override
    void filterArticlesTmObjectNames(TextMinableInput tmInput, QuerySettings querySettings) {
        filterArticlesProteolysisVerb(); // filter according to proteolysis verb terms
        super.filterArticlesTmObjectNames(tmInput, querySettings); // filter according to tmInput names
    }

    void finalisePostQuery() {
        for (Article article : this.articleCollection) {
            article.setProteasesMentioned(ArticleScanProtease.processAbstractForProteases(article.getArtAbstract(), proteaseFamily));
        }

        compressArticleSubset();
    }

    private String constructSearchQueryUrlProteolysis(List<String> searchTermProtNames, ArticleQueryable articleQueryable, boolean abstractTitleSearch) {
        StringBuilder sb = new StringBuilder();
        sb.append(articleQueryable.constructBaseUrl());

        sb.append(articleQueryable.appendToBaseUrl(searchTermProtNames, true, true, abstractTitleSearch));
        sb.append("%20AND%20");
        sb.append(articleQueryable.appendToBaseUrl(proteaseFamily.getNamesSearchQuery(), false, true, abstractTitleSearch));
        sb.append("%20AND%20");
        sb.append(articleQueryable.appendToBaseUrl(IoModel.getSearchVerbs(), false, true, abstractTitleSearch));

        if (!proteaseFamily.getBadSynonyms().isEmpty()) {
            sb.append("%20NOT%20");
            sb.append(articleQueryable.appendToBaseUrl(proteaseFamily.getBadSynonyms(), false, true, abstractTitleSearch));
        }

        System.out.println("TEXTMINING PROTEOLYSIS: " + sb.toString());
        return sb.toString();
    }

    void retrievePhysicalProteaseInteractions(Protein protein) throws SocketException {
        // return if physical protease interaction list is not null (must have already been queried)
        if (this.physicalInteractions == null)
            this.physicalInteractions = new ArrayList<>();
        else
            return;

        // Construct query URL with protein of this ArticleQueryableEPmc and all the proteases
        // from the selected protease family
        if (!protein.isSuccessBuildingSTRING() || !protein.getStringId().contains("9606")) // stringId has to be human
            return;

        StringBuilder sb = new StringBuilder();
        sb.append(queryStemSTRINGTabular).append("limit=0&required_score=150&identifiers=")
                .append(protein.getStringId()).append("%0D");

        List<Protein> proteases = new ArrayList<>();

        if (this.proteaseFamily == IoModel.getADAM()) {
            proteases.addAll(IoModel.getProteasesADAM());
            proteases.addAll(IoModel.getProteasesADAMTS());
        } else if (this.proteaseFamily == IoModel.getMMP()) {
            proteases.addAll(IoModel.getProteasesMMP());
        } else if (this.proteaseFamily == IoModel.getCTS()) {
            proteases.addAll(IoModel.getProteasesCTS());
        }

        for (int i = 0; i < proteases.size(); i++) {
            String stringID = proteases.get(i).getStringId();
            if (stringID != null) {
                sb.append(stringID);
                if (i != (proteases.size() - 1))
                    sb.append("%0D");
            }
        }
        System.out.println("PHYSICAL PROTEASE INTERACTIONS " + protein.getMainName() + ": " + sb.toString());

        // Download and parse data
        String psiMiTabFile = DownloadFileHTTP.downloadOnlineFileNoHeader(sb.toString());
        String[] rowsPsiMiTab = psiMiTabFile.split("\\r?\\n");
        for (String row : rowsPsiMiTab) {
            NetworkEdge interaction = new NetworkEdge(row);

            try {
                boolean condition = interaction.getNode1().contains(protein.getMainName()); // throws NullPointerException
                condition = condition || interaction.getNode2().contains(protein.getMainName());

                if (condition) {
                    if ((interaction.getEscore() != null) && (interaction.getEscore() != 0f)) {
                        physicalInteractions.add(interaction);
                    }
                }
            } catch (NullPointerException e) {
                // temporary NullPointer exception catch TODO
                StringBuilder exceptionMessage = new StringBuilder();
                exceptionMessage.append("interaction object: ").append(interaction).append("\n");
                exceptionMessage.append("protein object: ").append(protein).append("\n");
                exceptionMessage.append("psiMiTabFile: ").append(psiMiTabFile).append("\n");
                exceptionMessage.append("protein gene name: ").append(protein.getMainName()).append("\n");
                Exception myException = new Exception(exceptionMessage.toString());

                ExceptionDialog exceptionDialog = new ExceptionDialog(myException);
                exceptionDialog.showAndWait();
            }
        }
    }

    private void filterArticlesProteolysisVerb() {
        Set<Article> outputFilteredList = new HashSet<>();

        for (Article article : this.articleCollection) {
            String artAbstract = article.getArtAbstract().toLowerCase();

            for (String badVerb : IoModel.getProteolysisBadVerbs())
                artAbstract = artAbstract.replaceAll(badVerb, ""); // remove all bad verbs from abstract

            boolean stillContainsVerb = false;
            for (String searchVerb : IoModel.getSearchVerbRoots()) {
                if (artAbstract.contains(searchVerb)) { // if abstract still contains one of the OK search verbs, pass through filtering
                    stillContainsVerb = true;
                    break; // when first match found pass, no need for further searching
                }
            }
            if (stillContainsVerb)
                outputFilteredList.add(article);
        }
        this.articleCollection = outputFilteredList;
    }

    public List<Article> getClassifiedArticlesUncompressed() {
        ArrayList<Article> uncompressedArticlesOnly = new ArrayList<>();
        this.articleCollection.forEach(article -> {
            if (!article.isCompressed() && !article.getProteasesMentioned().isEmpty())
                uncompressedArticlesOnly.add(article);
        });
        return uncompressedArticlesOnly;
    }

    public List<Article> getAllArticlesUncompressed() {
        ArrayList<Article> uncompressedArticlesOnly = new ArrayList<>();
        this.articleCollection.forEach(article -> {
            if (!article.isCompressed())
                uncompressedArticlesOnly.add(article);
        });
        return uncompressedArticlesOnly;
    }

    List<Article> getAllClassifiedArticles() {
        ArrayList<Article> classifiedArticles = new ArrayList<>();
        this.articleCollection.forEach(article -> {
            if (!article.getProteasesMentioned().isEmpty())
                classifiedArticles.add(article);
        });
        return classifiedArticles;
    }

    ProteaseFamily getProteaseFamily() {
        return proteaseFamily;
    }

    public List<NetworkEdge> getPhysicalInteractions() {
        if (physicalInteractions == null)
            return new ArrayList<>();
        else
            return new ArrayList<>(physicalInteractions);
    }
}
