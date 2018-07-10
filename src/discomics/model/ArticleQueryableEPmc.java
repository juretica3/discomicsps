package discomics.model;

import discomics.application.DownloadFileHTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 4.9.2016.
 */
public abstract class ArticleQueryableEPmc extends ArticleQueryable implements Serializable {

    ArticleQueryableEPmc() {
        super();
    }

    /**
     * Main method that consructs query URL, downloads and parses Article search results. The query URL constuction methods
     * will be defined in subclasses that implement this class (ArticleCollectablePlys, ArticleCollectableBiom, ArticleCollectableCust)
     */
    List<Article> downloadArticles(TextMinableInput tmInput, QuerySettings querySettings)
            throws SocketException {
        try {
            List<String> proteinNamesSearch = new ArrayList<>();
//            if(querySettings.isSearchOnlyGeneName()) {
//                proteinNamesSearch.add(protein.getMainName());
//            } else {
//                proteinNamesSearch.addAll(protein.getProteinNomenclature().getNamesOnlineQuery());
//            }
            proteinNamesSearch.addAll(tmInput.getTextMiningNames(querySettings));
            String url = constructSearchQueryUrl(proteinNamesSearch, querySettings);
            List<Article> outputList = downloadParseArticles(url);
            this.textMiningSuccessful = true;
            return outputList;
            //return filterArticlesTmObjectNames(outputList, , querySettings);
        } catch (SocketException e) {
            e.printStackTrace();
            this.textMiningSuccessful = false;
            throw new SocketException();
        }
    }

//    /**
//     * Main method that consructs query URL, downloads and parses Article search results for a GENE FAMILY.
//     */
//    List<Article> downloadArticles(GeneFamily geneFamily, QuerySettings querySettings) throws SocketException {
//        try {
//            List<String> geneFamilyNamesSearch = geneFamily.getTextMiningNames();
//            String url = constructSearchQueryUrl(geneFamilyNamesSearch, querySettings);
//
//            List<Article> outputList = downloadParseArticles(url);
//            this.textMiningSuccessful = true;
//            return filterArticlesTmObjectNames(outputList, geneFamily, querySettings);
//
//        } catch (SocketException e) {
//            e.printStackTrace();
//            this.textMiningSuccessful = false;
//            throw new SocketException();
//        }
//    }

    /**
     * constructs base url for the ePMC RESTful service article query http://europepmc.org/restfulwebservice
     */
    public String constructBaseUrl() {
        String EPMC_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/";
        String EPMC_SEARCH = "search?";
        String EPMC_FORMAT = "json";
        String EPMC_RESULT_TYPE = "core";
        String EPMC_PAGE_SIZE = "1000";

        return EPMC_URL + EPMC_SEARCH +
                "format=" + EPMC_FORMAT +
                "&resulttype=" + EPMC_RESULT_TYPE +
                "&pageSize=" + EPMC_PAGE_SIZE +
                "&query=";
    }

    public String appendToBaseUrl(List<String> searchTerms, boolean encode, boolean quotations, boolean searchAbstractTitle) {
        if (searchAbstractTitle) {
            String titleStr = appendToBaseUrl(searchTerms, encode, quotations, "TITLE");
            String abstractStr = appendToBaseUrl(searchTerms, encode, quotations, "ABSTRACT");

            // format final query block - remove final bracket of first (title) block, add the " OR " string, and remove initial bracket of second (abstract) block
            return titleStr.substring(0, titleStr.length() - 3) + "%20OR%20" + abstractStr.substring(3, abstractStr.length());
        } else
            return appendToBaseUrl(searchTerms, encode, quotations, "");
    }

    /**
     * appends search terms to the base URL; the searchTerm list passed will be included in the same 'AND' block
     * ... AND (searchTerm0 OR searchTerm1 OR ... OR searchTermN) AND ...
     */
    private String appendToBaseUrl(List<String> searchTerms, boolean encode, boolean quotations, String searchField) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%28"); // open parenthesis
        for (int i = 0; i < searchTerms.size(); i++) {

            if (encode) {
                String str = "";

                try {
                    if (!searchField.isEmpty())
                        str = str + searchField + ":";

                    if (quotations)
                        str = str + "%22"; // quotes

                    str = str + URLEncoder.encode(searchTerms.get(i), "UTF-8");

                    if (quotations)
                        str = str + "%22"; // quotes

                    stringBuilder.append(str);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    stringBuilder.append("%22%22"); // double quotes
                }

            } else { // strings already URL encoded
                if (searchTerms.get(i).contains("%2A")) { // if search term contains wildcard *
                    if (!searchField.isEmpty())
                        stringBuilder.append(searchField).append(":");
                    stringBuilder.append(searchTerms.get(i).replaceAll(" ", "%20"));
                } else { // if search term doesn't contain wildcard
                    if (!searchField.isEmpty())
                        stringBuilder.append(searchField).append(":");
                    if (quotations)
                        stringBuilder.append("%22");

                    stringBuilder.append(searchTerms.get(i).replaceAll(" ", "%20"));
                    if (quotations)
                        stringBuilder.append("%22");
                }
            }

            if (i != (searchTerms.size() - 1)) {
                stringBuilder.append("%20OR%20");
            }
        }
        stringBuilder.append("%29"); // close parenthesis

        return stringBuilder.toString();
    }

    /**
     * use the search URL constructed via constructBaseURL() and appendToBaseURL() and pass it to this method to
     * retrieve Article search results
     */
    public List<Article> downloadParseArticles(String url) throws SocketException {

        if (url == null) {
            this.textMiningSuccessful = false;
            return new ArrayList<>();
        }

        List<Article> outputList = new ArrayList<>();
        int nrArticles;
        String nextCursorMark = "%2A"; // default value (asterisk *)

        do {
            String urlWithCursorMark = url + "&cursorMark=" + nextCursorMark;
            System.out.println("TEXTMINING EPMC: " + urlWithCursorMark);
            String JSONInput = DownloadFileHTTP.downloadOnlineFileNoHeader(urlWithCursorMark); // null returned by downloadOnlineFileNoHeader if errors occur
            JSONObject jsonObject = new JSONObject(JSONInput);

            nrArticles = jsonObject.getJSONObject("resultList").getJSONArray("result").length(); // number of articles retrieved
            nextCursorMark = jsonObject.getString("nextCursorMark");

            for (int i = 0; i < nrArticles; i++) { // parse article information, create Article objects and store them in output list
                Article art = new Article();

                JSONObject jsonArt = jsonObject.getJSONObject("resultList").getJSONArray("result").getJSONObject(i);

                if (jsonArt.has("pmid"))
                    art.setPmid(jsonArt.getString("pmid"));

                if (jsonArt.has("pmcid"))
                    art.setPmcid(jsonArt.getString("pmcid"));

                if (jsonArt.has("title"))
                    art.setTitle(jsonArt.getString("title"));

                if (jsonArt.has("authorString"))
                    art.setAuthorString(jsonArt.getString("authorString"));

                if (jsonArt.has("doi"))
                    art.setDoi(jsonArt.getString("doi"));

                if (jsonArt.has("authorList")) {
                    JSONArray jsonAuths = jsonArt.getJSONObject("authorList").getJSONArray("author");
                    for (int i1 = 0; i1 < jsonAuths.length(); i1++) {
                        String firstName = "";
                        if (jsonAuths.getJSONObject(i1).has("firstName"))
                            firstName = jsonAuths.getJSONObject(i1).getString("firstName");
                        else if (jsonAuths.getJSONObject(i1).has("initials"))
                            firstName = jsonAuths.getJSONObject(i1).getString("initials");
                        String lastName = "";
                        if (jsonAuths.getJSONObject(i1).has("lastName"))
                            lastName = jsonAuths.getJSONObject(i1).getString("lastName");
                        art.addArtAuthor(firstName, lastName);
                    }
                }

                String pubDate = null;
                if (jsonArt.has("firstPublicationDate")) {
                    pubDate = jsonArt.getString("firstPublicationDate");
                } else if (jsonArt.has("dateOfCreation")) {
                    pubDate = jsonArt.getString("dateOfCreation");
                } else if (jsonArt.has("dateOfRevision")) {
                    pubDate = jsonArt.getString("dateOfRevision");
                } else if (jsonArt.has("dateOfCompletion")) {
                    pubDate = jsonArt.getString("dateOfCompletion");
                }

                if (pubDate != null) {
                    String[] splitDate = pubDate.split("-");
                    if (pubDate.length() > 0)
                        art.setPubDateYear(splitDate[0]);
                    if (pubDate.length() > 1)
                        art.setPubDateMonthNr(splitDate[1]);
                    if (pubDate.length() > 2)
                        art.setPubDateDay(splitDate[2]);
                }

                if (jsonArt.has("journalInfo")) {

                    JSONObject jsonJour = jsonArt.getJSONObject("journalInfo");

                    if (jsonJour.has("issue"))
                        art.setJournalIssue(jsonJour.getString("issue"));

                    if (jsonJour.has("volume"))
                        art.setJournalVol(jsonJour.getString("volume"));

                    if (jsonJour.getJSONObject("journal").has("medlineAbbreviation"))
                        art.setJournalTitleShort(jsonJour.getJSONObject("journal").getString("medlineAbbreviation"));

                    if (jsonJour.getJSONObject("journal").has("title"))
                        art.setJournalTitle(jsonJour.getJSONObject("journal").getString("title"));

                }

                if (jsonArt.has("citedByCount")) {
                    art.setCitedByCount(jsonArt.getInt("citedByCount"));
                }

                if (jsonArt.has("abstractText"))
                    art.setArtAbstract(jsonArt.getString("abstractText"));

                JSONArray jsonPubType = null;

                if (jsonArt.has("pubTypeList")) {
                    if (jsonArt.getJSONObject("pubTypeList").has("pubType"))
                        jsonPubType = jsonArt.getJSONObject("pubTypeList").getJSONArray("pubType");
                }

                if (jsonPubType != null) {
                    for (int ii = 0; ii < jsonPubType.length(); ii++) {
                        String pubType = jsonPubType.getString(ii);
                        if (pubType.equalsIgnoreCase("review")) {
                            art.setReview(true);
                            break;
                        } else {
                            art.setReview(false);
                        }
                    }
                }

                JSONArray jsonFullTextURL = null;

                if (jsonArt.has("fullTextUrlList")) {
                    if (jsonArt.getJSONObject("fullTextUrlList").has("fullTextUrl"))
                        jsonFullTextURL = jsonArt.getJSONObject("fullTextUrlList").getJSONArray("fullTextUrl");
                }

                if (jsonFullTextURL != null) {
                    int jsonFullTextURLSize = jsonFullTextURL.length();

                    // loop through the available urls to see if OA or FREE versions available
                    for (int iii = 0; iii < jsonFullTextURLSize; iii++) {
                        String availabilityCode = jsonFullTextURL.getJSONObject(iii).getString("availabilityCode");

                        if (availabilityCode.equalsIgnoreCase("OA") || availabilityCode.equalsIgnoreCase("F")) {
                            art.setArtURL(jsonFullTextURL.getJSONObject(iii).getString("url"));
                            break;
                        }
                    }

                    // If OA or FREE version not available, grab first url in the list.
                    if (art.getUrl().isEmpty()) {
                        if (jsonFullTextURLSize != 0) {
                            art.setArtURL(jsonFullTextURL.getJSONObject(0).getString("url"));
                        }
                    }
                }
                outputList.add(art); // add the constructed object to the array
            }
        } while (nrArticles > 0);

        return outputList;
    }
}
