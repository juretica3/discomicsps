package discomics.model;

import java.io.Serializable;
import java.net.SocketException;
import java.util.*;

/**
 * Created by Jure on 24.12.2016.
 */
class ArticleCollectable implements Serializable {
    ArticleQueryableEPmc articleQueryableEPmc;
    ArticleQueryablePubmed articleQueryablePubmed;

    Set<Article> articleCollection;

    ArticleCollectable() {
        this.articleCollection = new HashSet<>();
    }

    void filterArticlesTmObjectNames(TextMinableInput tmInput, QuerySettings querySettings) {
        this.articleCollection = new HashSet<>(ArticleFilterProteinName.filterArticles(this.articleCollection, tmInput, querySettings));
    }

    int getNrRetrieved() {
        ArrayList<Article> uncompressedArticles = new ArrayList<>();
        this.articleCollection.forEach(article -> {
            if (!article.isCompressed())
                uncompressedArticles.add(article);
        });
        return uncompressedArticles.size();
    }

    public List<Article> getArticleCollection() {
        return new ArrayList<>(this.articleCollection);
    }

    public List<Article> getUncompressedArticleCollection() {
        List<Article> outputList = new ArrayList<>();
        for(Article article: this.articleCollection) {
            if(!article.isCompressed())
                outputList.add(article);
        }
        return outputList;
    }

    int getTotalHitCount() {
        return this.articleCollection.size();
    }

    void queryEPmc(TextMinableInput tmInput, QuerySettings querySettings) throws SocketException {
        HashSet<Article> articles = new HashSet<>();
        articles.addAll(articleQueryableEPmc.downloadArticles(tmInput, querySettings)); // query ePMC

        if (this.articleCollection == null)
            this.articleCollection = new HashSet<>();

        this.articleCollection.addAll(articles);
    }

    void queryPubmed(TextMinableInput tmInput, QuerySettings querySettings) throws SocketException {
        HashSet<Article> articles = new HashSet<>();
        articles.addAll(articleQueryablePubmed.downloadArticles(tmInput, querySettings)); // query PubMed

        if (this.articleCollection == null)
            this.articleCollection = new HashSet<>();

        this.articleCollection.addAll(articles);
    }

    private class ArticleFilterAndDateComparator implements Comparator<Article> {
        @Override
        public int compare(Article o1, Article o2) {
            if (o1.getFilteringPassed() && !o2.getFilteringPassed())
                return -1;
            else if (!o1.getFilteringPassed() && o2.getFilteringPassed())
                return 1;
            else {

                if (o1.getPubDateYear().isEmpty() && o2.getPubDateYear().isEmpty())
                    return 0;
                else if (o2.getPubDateYear().isEmpty())
                    return -1;
                else if (o1.getPubDateYear().isEmpty())
                    return +1;

                if (Integer.parseInt(o1.getPubDateYear()) > Integer.parseInt(o2.getPubDateYear()))
                    return -1;
                else if (Integer.parseInt(o1.getPubDateYear()) < Integer.parseInt(o2.getPubDateYear()))
                    return +1;

                if (o1.getPubDateMonthNr().isEmpty() && o2.getPubDateMonthNr().isEmpty())
                    return 0;
                else if (o2.getPubDateMonthNr().isEmpty())
                    return -1;
                else if (o1.getPubDateMonthNr().isEmpty())
                    return +1;

                if (Integer.parseInt(o1.getPubDateMonthNr()) > Integer.parseInt(o2.getPubDateMonthNr()))
                    return -1;
                else if (Integer.parseInt(o1.getPubDateMonthNr()) < Integer.parseInt(o2.getPubDateMonthNr()))
                    return +1;

                if (o1.getPubDateDay().isEmpty() && o2.getPubDateDay().isEmpty())
                    return 0;
                else if (o2.getPubDateDay().isEmpty())
                    return -1;
                else if (o1.getPubDateDay().isEmpty())
                    return +1;

                if (Integer.parseInt(o1.getPubDateDay()) > Integer.parseInt(o2.getPubDateDay()))
                    return -1;
                else if (Integer.parseInt(o1.getPubDateDay()) < Integer.parseInt(o2.getPubDateDay()))
                    return +1;

                return 0;
            }
        }
    }

//    void keepArticleSubset() {
//        int maxNrToKeep = 300; // set how many most recent articles should be kept for the user to view
//
//        if (maxNrToKeep >= this.articleCollection.size())
//            return;
//
//        ArrayList<Article> articlesToKeep = new ArrayList<>(this.articleCollection.subList(0, maxNrToKeep));
//        this.articleCollection = null;
//        this.articleCollection = articlesToKeep;
//    }

    void compressArticleSubset() {
        int maxNrToKeep = 300;

        if (maxNrToKeep >= this.articleCollection.size())
            return;

        ArrayList<Article> articles = new ArrayList<>(this.articleCollection);
        articles.sort(new ArticleFilterAndDateComparator());

        for (int i = maxNrToKeep; i < articles.size(); i++) {
            Article article = articles.get(i);

            article.setArtAbstract("");
            article.setTitle("");
            article.setPubDateYear("");
            article.setPubDateDay("");
            article.setPubDateMonthNr("");
            article.setPubDateMonthName("");
            article.setAuthorString("");
            article.setDoiAndUrl("");
            article.setJournalIssue("");
            article.setJournalTitle("");
            article.setJournalTitleShort("");
            article.setJournalVol("");
            article.setDoiAndUrl("");
            article.setCompressed(true);
        }

        this.articleCollection = new HashSet<>(articles);
    }

    boolean isTextMiningEPmcSuccessful() {
        return this.articleQueryableEPmc != null && this.articleQueryableEPmc.isTextMiningSuccessful();
    }

    boolean isTextMiningPubmedSuccessful() {
        return this.articleQueryablePubmed != null && this.articleQueryablePubmed.isTextMiningSuccessful();
    }

    public boolean removeArticlesFromCollection(List<Article> articles) {
        return this.articleCollection.removeAll(articles);
    }
}
