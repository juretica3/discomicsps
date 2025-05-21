package discomics.model;

import java.io.Serializable;
import java.net.SocketException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TextMinedObject<T extends TextMinableInput> implements Serializable {
    private static long serialVersionUID = 5654343465115L;

    T textMinableInput;
    private ArticleCollectablePlys articleCollectablePlys = new ArticleCollectablePlys();
    private ArticleCollectableBiom articleCollectableBiom = new ArticleCollectableBiom();
    ArticleCollectableCust articleCollectableCust = new ArticleCollectableCust();

    QuerySettings querySettings;


    TextMinedObject(T textMinableInput, QuerySettings querySettings) {
        this.textMinableInput = textMinableInput;
        this.querySettings = querySettings;
    }

    TextMinedObject(T textMinableInput, List<CustomInputTermBlock> customSearchTerms, QuerySettings querySettings) {
        this(textMinableInput, querySettings);
        this.articleCollectableCust = new ArticleCollectableCust(customSearchTerms);
    }

    public void queryEPmc() throws SocketException {
        if (querySettings.isProteaseSearch()) {
            this.articleCollectablePlys.queryEPmc(this.textMinableInput, querySettings);
        }
        if (querySettings.isBiomarkerSearch()) {
            this.articleCollectableBiom.queryEPmc(this.textMinableInput, querySettings);
        }
        if (querySettings.isCustomSearch()) {
            this.articleCollectableCust.queryEPmc(this.textMinableInput, querySettings);
        }
    }

    public void queryPubmed() throws SocketException {
        if (querySettings.isProteaseSearch()) {
            this.articleCollectablePlys.queryPubmed(this.textMinableInput, querySettings);
        }
        if (querySettings.isBiomarkerSearch()) {
            this.articleCollectableBiom.queryPubmed(this.textMinableInput, querySettings);
        }
        if (querySettings.isCustomSearch()) {
            this.articleCollectableCust.queryPubmed(this.textMinableInput, querySettings);
        }
    }

    public void finalisePostQuery(int maxArticlesRetrieved) {
        if (querySettings.isProteaseSearch()) {
            this.articleCollectablePlys.filterArticlesTmObjectNames(textMinableInput, querySettings);
            this.articleCollectablePlys.finalisePostQuery(maxArticlesRetrieved);
        }
        if (querySettings.isBiomarkerSearch()) {
            this.articleCollectableBiom.filterArticlesTmObjectNames(textMinableInput, querySettings);
            this.articleCollectableBiom.finalisePostQuery(maxArticlesRetrieved);
        }
        if (querySettings.isCustomSearch()) {
            this.articleCollectableCust.filterArticlesTmObjectNames(textMinableInput, querySettings);
            this.articleCollectableCust.finalisePostQuery(maxArticlesRetrieved);
        }
    }

    public ArticleCollectablePlys getArticleCollectablePlys() {
        return articleCollectablePlys;
    }

    public ArticleCollectableBiom getArticleCollectableBiom() {
        return articleCollectableBiom;
    }

    public ArticleCollectableCust getArticleCollectableCust() {
        return articleCollectableCust;
    }

    public TextMinableInput getTextMinableInput() { return textMinableInput; }

    public String getMainName() {
        return textMinableInput.getMainName();
    }

    public int getNrDrugInteractions() {
        return 0;
    }


    /**
     * ------------------- TABLE GETTERS ---------------------------
     */

    public int getNrRetrievedProteolysis() {
        return articleCollectablePlys.getNrRetrieved();
    }

    public int getNrRetrievedBiomarker() {
        return articleCollectableBiom.getNrRetrieved();
    }

    public int getNrRetrievedBiomarker(Biomarker biomarker) {
        List<Article> artColl = this.articleCollectableBiom.getUncompressedArticleCollection();
        Set<Article> articles = artColl.stream()
                .filter(art -> art.getBiomarkersMentioned().contains(biomarker))
                .collect(Collectors.toSet());
        return articles.size();
    }

    public int getNrRetrievedCustom() {
        return articleCollectableCust.getNrRetrieved();
    }

    public int getTotalHitsProteolysis() {
        return articleCollectablePlys.getTotalHitCount();
    }

    public int getTotalHitsBiomarker() {
        return articleCollectableBiom.getTotalHitCount();
    }

    public int getTotalHitsBiomarker(Biomarker biomarker) {
        List<Article> artColl = this.articleCollectableBiom.getArticleCollection();
        Set<Article> articles = artColl.stream()
                .filter(art -> art.getBiomarkersMentioned().contains(biomarker))
                .collect(Collectors.toSet());
        return articles.size();
    }

    public int getTotalHitsCustom() {
        return articleCollectableCust.getTotalHitCount();
    }


    /**
     * ------------------- TEXT MINING SUCCESS CHECKERS ---------------------------
     */
    public Boolean[] isTextMiningSuccessfulProteolysis() {
        Boolean[] output = new Boolean[2]; // first value refers to ePMC mining, second to Pubmed mining

        output[0] = this.articleCollectablePlys.isTextMiningEPmcSuccessful();

        if (!this.querySettings.isQueryPubmed())
            output[1] = true;
        else
            output[1] = this.articleCollectablePlys.isTextMiningPubmedSuccessful();

        return output;
    }

    public Boolean[] isTextMiningSuccessfulBiomarker() {
        Boolean[] output = new Boolean[2]; // first value refers to ePMC mining, second to Pubmed mining

        output[0] = this.articleCollectableBiom.isTextMiningEPmcSuccessful();

        if (!this.querySettings.isQueryPubmed())
            output[1] = true;
        else
            output[1] = this.articleCollectableBiom.isTextMiningPubmedSuccessful();

        return output;
    }

    public Boolean[] isTextMiningSuccessfulCustom() {
        Boolean[] output = new Boolean[2]; // first value refers to ePMC mining, second to Pubmed mining

        output[0] = this.articleCollectableCust.isTextMiningEPmcSuccessful();

        if (!this.querySettings.isQueryPubmed())
            output[1] = true;
        else
            output[1] = this.articleCollectableCust.isTextMiningPubmedSuccessful();

        return output;
    }

}
