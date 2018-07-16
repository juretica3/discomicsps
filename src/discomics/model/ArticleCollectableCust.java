package discomics.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by Jure on 20/11/2016.
 */
public class ArticleCollectableCust extends ArticleCollectable implements Serializable {
    static final transient long serialVersionUID = 3259034276204456L;

    private List<CustomInputTermBlock> searchTerms = new ArrayList<>();

    ArticleCollectableCust() {
        super();
    }

    ArticleCollectableCust(List<CustomInputTermBlock> searchTerms) {
        this();
        this.searchTerms = searchTerms;

        articleQueryableEPmc = new ArticleQueryableEPmc() {
//            @Override
//            List<Article> downloadArticles(Protein protein, QuerySettings querySettings) throws SocketException {
//                List<Article> articles = super.downloadParseFilterArticles(protein, querySettings);
//                return filterArticlesCustomTerms(articles);
//            }

            @Override
            public String constructSearchQueryUrl(List<String> searchTermProtNames, QuerySettings querySettings) {
                return constructSearchQueryUrlCustom(searchTermProtNames, this, querySettings);
            }
        };

        this.articleQueryablePubmed = new ArticleQueryablePubmed() {
//            @Override
//            List<Article> downloadParseFilterArticles(Protein protein, QuerySettings querySettings) throws SocketException {
//                List<Article> articles = super.downloadParseFilterArticles(protein, querySettings);
//                return filterArticlesCustomTerms(articles);
//            }

            @Override
            public String constructSearchQueryUrl(List<String> searchTermProtNames, QuerySettings querySettings) {
                return constructSearchQueryUrlCustom(searchTermProtNames, this, querySettings);
            }
        };
    }

    @Override
    void filterArticlesTmObjectNames(TextMinableInput tmInput, QuerySettings querySettings) {
        filterArticlesCustomTerms(); // filter according to tmInput names

        super.filterArticlesTmObjectNames(tmInput, querySettings); // filter according to custom terms
    }

    /**
     * Constructs PubMed or ePMC query URL with constructBaseUrl() and appendToBaseUrl() methods defined in
     * ArticleQueryablePubmed and ArticleQueryableEPmc
     */
    private String constructSearchQueryUrlCustom(List<String> tmNames, ArticleQueryable articleQueryable, QuerySettings querySettings) {
        StringBuilder sb = new StringBuilder();
        sb.append(articleQueryable.constructBaseUrl());

        sb.append(articleQueryable.appendToBaseUrl(tmNames, true, true, !querySettings.isSearchFullText()));
        sb.append("%20AND%20");

        for (int i = 0; i < searchTerms.size(); i++) {
            sb.append(articleQueryable.appendToBaseUrl(searchTerms.get(i).getCustomInputTermsAsStrings(), false, false, !querySettings.isSearchFullText()));

            if (i != (searchTerms.size() - 1))
                sb.append("%20AND%20");
        }
        System.out.println("TEXTMINING CUSTOM: " + sb.toString());
        return sb.toString();
    }

    void finalisePostQuery(int maxArticlesRetrieved) {
        compressArticleSubset(maxArticlesRetrieved);
    }

    private void filterArticlesCustomTerms() {
        List<Article> outputFilteredList = new ArrayList<>(); // list of articles that pass filtering

        for (Article article : this.articleCollection) {
            String artAbstract = article.getTitleAbstractConcat().toLowerCase();

            andTermLoop:
            for (int i = 0; i < searchTerms.size(); i++) { // loop through AND blocks in custom user defined query
                CustomInputTermBlock termBlock = searchTerms.get(i);

                orTermLoop:
                for (int j = 0; j < termBlock.getCustomInputTerms().size(); j++) { // loop through OR linked terms within AND block
                    CustomInputTermBlock.CustomInputTerm term = termBlock.getCustomInputTerms().get(j);

                    if (term.isPhrase()) { // if the term searched as whole phrase, attempt to match whole phrase in abstract
                        Pattern pattern = Pattern.compile(".*\\b" + term.getSearchTerm() + "\\b.*");
                        Matcher matcher = pattern.matcher(artAbstract);
                        if (matcher.find())
                            break;

                        if (j == termBlock.getCustomInputTerms().size() - 1)
                            break andTermLoop; // if all terms in AND block have been scanned and no match was found, skip to next article, and don't pass the current one through filtering

                    } else { // if the term not searched as whole phrase, break string into words, and match all of the words

                        String[] splitSearchTerms = term.getSearchTerm().split("[\\p{Punct}\\s]+");

                        for (int k = 0; k < splitSearchTerms.length; k++) { // all terms have to be matched for article to pass filtering
                            Pattern pattern = Pattern.compile(".*\\b" + splitSearchTerms[k] + "\\b.*");
                            Matcher matcher = pattern.matcher(artAbstract);
                            if (!matcher.find()) // if a term not matched, break out of loop
                                break;

                            if (k == splitSearchTerms.length - 1) // only if loop hasn't been broken out for any of the words article passes through filtering
                                break orTermLoop; // when article is passed through filter, stop searching with other terms
                        }
                        if (j == termBlock.getCustomInputTerms().size() - 1)
                            break andTermLoop; // if all terms in AND block have been scanned and no match was found (never broken out of the loop), don't pass article through filtering
                    }
                }

                if (i == searchTerms.size() - 1) // only if all blocks have been satisfied (AND blocks), pass article through filtering
                    outputFilteredList.add(article);
            }
        }
        this.articleCollection = new HashSet<>(outputFilteredList); // update article collection
    }
}
