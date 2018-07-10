package discomics.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 4.9.2016.
 */
public class ArticleCollectableBiom extends ArticleCollectable implements Serializable {
    static final transient long serialVersionUID = 22635357312173256L;

    ArticleCollectableBiom() {
        super();

        articleQueryableEPmc = new ArticleQueryableEPmc() {
            @Override
            public String constructSearchQueryUrl(List<String> searchTermProtNames, QuerySettings querySettings) {
                return constructSearchQueryUrlBiomarker(searchTermProtNames, this, !querySettings.isSearchFullText());
            }
        };

        this.articleQueryablePubmed = new ArticleQueryablePubmed() {
            @Override
            public String constructSearchQueryUrl(List<String> searchTermProtNames, QuerySettings querySettings) {
                return constructSearchQueryUrlBiomarker(searchTermProtNames, this, false);
            }
        };
    }

    @Override
    void filterArticlesTmObjectNames(TextMinableInput tmInput, QuerySettings querySettings) {
        super.filterArticlesTmObjectNames(tmInput, querySettings);
    }

    /**
     * Constructs PubMed or ePMC query URL with constructBaseUrl() and appendToBaseUrl() methods defined in
     * ArticleQueryablePubmed and ArticleQueryableEPmc
     */
    private String constructSearchQueryUrlBiomarker(List<String> searchTerms, ArticleQueryable articleQueryable, boolean abstractTitleSearch) {
        StringBuilder sb = new StringBuilder();
        sb.append(articleQueryable.constructBaseUrl());

        sb.append(articleQueryable.appendToBaseUrl(searchTerms, true, true, abstractTitleSearch));
        //sb.append("%20NOT%20").append(protein.getMainName()).append("%2D%2F%2D"); // %2D%2F%2D = -/- (KO animals) todo KO animals
        sb.append("%20AND%20");

        List<String> biomarkerSearchTerms = new ArrayList<>();
        biomarkerSearchTerms.add("biomark%2A");
        biomarkerSearchTerms.add("marker of disease"); // TODO
        sb.append(articleQueryable.appendToBaseUrl(biomarkerSearchTerms, false, false, abstractTitleSearch));

        System.out.println("TEXTMINING BIOMARKER: " + sb.toString());
        return sb.toString();
    }

    void finalisePostQuery() {
        defineBiomarkersForArticles();
        compressArticleSubset();
    }

    private void defineBiomarkersForArticles() {
        for (Article article : this.articleCollection) {
            article.processAbstractForBiomarkers(IoModel.getBLOOD());
            article.processAbstractForBiomarkers(IoModel.getSALIVA());
            article.processAbstractForBiomarkers(IoModel.getURINE());
        }
    }

    public void defineCustomBiomarkerForArticles() {
        for (Article article : this.articleCollection) {
            article.processAbstractForBiomarkers(IoModel.getCUSTOM());
        }
    }

    public void removeCustomBiomarkerFromArticles() {
        for (Article article : this.articleCollection) {
            article.removeBiomarker(IoModel.getCUSTOM());
        }
    }
}
