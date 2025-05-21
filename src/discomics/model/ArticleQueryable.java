package discomics.model;

import java.io.*;
import java.net.SocketException;
import java.util.*;

/**
 * Created by Jure on 22.12.2016.
 */
abstract class ArticleQueryable implements Serializable {
    static final transient long serialVersionUID = 72312399973256L;
    boolean textMiningSuccessful;

    abstract String constructBaseUrl();

    abstract String appendToBaseUrl(List<String> searchTerms, boolean encode, boolean quotationMarks, boolean searchAbstractTitle);

    abstract String constructSearchQueryUrl(List<String> searchTerms, QuerySettings querySettings);

    abstract List<Article> downloadParseArticles(String url) throws IOException;

    abstract List<Article> downloadArticles(TextMinableInput tmInput, QuerySettings querySettings) throws SocketException;

    //abstract List<Article> downloadParseFilterArticles(GeneFamily protein, QuerySettings querySettings) throws SocketException;

    // GETTERS AND SETTERS
    boolean isTextMiningSuccessful() {
        return textMiningSuccessful;
    }
}
