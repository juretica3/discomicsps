package discomics.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 22.12.2016.
 */
public abstract class ArticleQueryablePubmed extends ArticleQueryable {
    private static String DATABASE = "pubmed";
    private static final String PMC_SEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/" +
            "esearch.fcgi?db=" + DATABASE + "&usehistory=y" + "&term="; // search articles by query
    private static String PMC_FETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=" + DATABASE + "&retmode=xml"; // fetch article by id


    ArticleQueryablePubmed() {
        super();
    }

    /**
     * Main method that takes Protein as input, downloads articles, parses them, filters them and returns the output Article List
     * URL query construction rules defined in subclasses that implement this abstract class (ArticleCollectableBiom, ArticleCollectablePlys, ArticleCollectableCust)
     */
    List<Article> downloadArticles(TextMinableInput tmInput, QuerySettings querySettings) throws SocketException {
        try {
            List<String> protNameSearchTerms = new ArrayList<>();
            protNameSearchTerms.addAll(tmInput.getTextMiningNames(querySettings));
//            if(querySettings.isSearchOnlyGeneName()) {
//                protNameSearchTerms.add(protein.getMainName());
//            } else {
//                protNameSearchTerms.addAll(protein.getProteinNomenclature().getNamesOnlineQuery()); // preliminary search to retrieve protein names with no results
//            }
            String urlSearch1 = constructSearchQueryUrl(protNameSearchTerms, querySettings);
            List<String> badSearchNameTerms = this.preliminarySearch(urlSearch1);

            protNameSearchTerms.removeAll(badSearchNameTerms); // removal of protein names with no results
            if (protNameSearchTerms.size() == 0) { // if protein name list empty after removal of protein names with no results, then no articles found
                this.textMiningSuccessful = true;
                return new ArrayList<>();
            }

            String urlSearch2 = constructSearchQueryUrl(protNameSearchTerms, querySettings); // search repetition, main search without protein names with no hits
            ESearchResult eSearchResult = this.parsePmidSearchResults(urlSearch2);

            List<Article> outputList;

            if (eSearchResult.hitCount > 0) { // parse articles only if one hit or more, otherwise exit TRY block and return empty ArrayList
                String urlFetch = constructFetchUrl(eSearchResult);
                outputList = downloadParseArticles(urlFetch);

                this.textMiningSuccessful = true;
                return outputList;
            } else {
                this.textMiningSuccessful = true;
                return new ArrayList<>();
            }

        } catch (MalformedURLException e) {
            this.textMiningSuccessful = false;
            e.printStackTrace();
        } catch (IOException e) {
            if (e instanceof InterruptedIOException || e instanceof SocketException) {
                this.textMiningSuccessful = false;
                return new ArrayList<>();
                //throw new SocketException("PMC downloadParseArticles failed");
            }
        }
        this.textMiningSuccessful = false; // if search succeeds method would have returned by now
        return new ArrayList<>();
    }

//    List<Article> downloadParseFilterArticles(GeneFamily geneFamily, QuerySettings querySettings) throws SocketException {
//        try {
//            List<String> searchTerms = geneFamily.getTextMiningNames();
//
//            String urlSearch1 = constructSearchQueryUrl(searchTerms, querySettings);
//            List<String> badSearchNameTerms = this.preliminarySearch(urlSearch1);
//
//            searchTerms.removeAll(badSearchNameTerms); // removal of protein names with no results
//            if (searchTerms.size() == 0) { // if protein name list empty after removal of protein names with no results, then no articles found
//                this.textMiningSuccessful = true;
//                return new ArrayList<>();
//            }
//
//            String urlSearch2 = constructSearchQueryUrl(searchTerms, querySettings); // search repetition, main search without protein names with no hits
//            ESearchResult eSearchResult = this.parsePmidSearchResults(urlSearch2);
//
//            List<Article> outputList;
//
//            if (eSearchResult.hitCount > 0) { // parse articles only if one hit or more, otherwise exit TRY block and return empty ArrayList
//                String urlFetch = constructFetchUrl(eSearchResult);
//                outputList = downloadParseArticles(urlFetch);
//
//                //outputList = new ArrayList<>(this.filterArticlesTmObjectNames(outputList, geneFamily, querySettings));
//
//                this.textMiningSuccessful = true;
//                return outputList;
//            } else {
//                this.textMiningSuccessful = true;
//                return new ArrayList<>();
//            }
//
//        } catch (MalformedURLException e) {
//            this.textMiningSuccessful = false;
//            e.printStackTrace();
//        } catch (IOException e) {
//            if (e instanceof InterruptedIOException || e instanceof SocketException) {
//                this.textMiningSuccessful = false;
//                return new ArrayList<>();
//                //throw new SocketException("PMC downloadParseArticles failed");
//            }
//        }
//        this.textMiningSuccessful = false; // if search succeeds method would have returned by now
//        return new ArrayList<>();
//    }

    /**
     * Constructs the query URL for the Entrez ESearch service to retrieve PMIDs of search result articles
     */
    public String constructBaseUrl() {
        return PMC_SEARCH_URL;
    }

    /**
     * Constructs the query URL for the Entrez ESearch service to retrieve PMIDs of search result articles
     * To be used in conjunction with constructBaseUrl() method
     */
    public String appendToBaseUrl(List<String> searchTerms, boolean encode, boolean quotations, boolean searchTitleAbstract) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("%28"); // open parenthesis

        for (int i = 0; i < searchTerms.size(); i++) {

            if (encode) { // if encode = true, then use URLEncoder to encode them
                String str = "";
                try {
                    if (quotations) // if quotations = true, surround the search term with quotation marks (as phrase)
                        str = str + "%22";

                    str = str + URLEncoder.encode(searchTerms.get(i), "UTF-8"); // encode and append search term

                    if (quotations) // close quotations
                        str = str + "%22";

                    if (searchTitleAbstract)
                        str = str + "%5BTitle%2FAbstract%5D"; // [Title/Abstract] after search term if search to be restricted

                    stringBuilder.append(str);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    stringBuilder.append("%22%22"); // if encoding fails, append open&close quotations with nothing in between
                }

            } else { // URL encoding not required
                if (searchTerms.get(i).contains("%2A")) { // contains wildcard %2A = *
                    stringBuilder.append(searchTerms.get(i).replaceAll(" ", "%20")); // replaces spaces with URL encoded spaces
                    if (searchTitleAbstract) // retrict search if required
                        stringBuilder.append("%5BTitle%2FAbstract%5D");
                } else {
                    if (quotations) // never surround wildcarded term with quotations
                        stringBuilder.append("%22");
                    stringBuilder.append(searchTerms.get(i).replaceAll(" ", "%20"));
                    if (quotations)
                        stringBuilder.append("%22");
                    if (searchTitleAbstract)
                        stringBuilder.append("%5BTitle%2FAbstract%5D");
                }
            }

            if (i != (searchTerms.size() - 1)) { // if dealing with final term, do not append " OR "
                stringBuilder.append("%20OR%20");
            }
        }
        stringBuilder.append("%29"); // close parentheses

        return stringBuilder.toString();
    }

    /**
     * Preliminary search focuses on identifying quoted search terms for which nothing was found in the PubMed
     * database, so they can be eliminated from the main search (since they lead to unwanted articles being retrieved
     * down the line
     */
    private List<String> preliminarySearch(String url) throws IOException {
        List<String> outputBadTermList = new ArrayList<>();

        InputStream xmlInputStream = null;

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

            xmlInputStream = new URL(url).openStream();
            XMLEventReader eventReader = factory.createXMLEventReader(xmlInputStream);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();
                    String startElementName = startElement.getName().getLocalPart();

                    if (startElementName.equals("QuotedPhraseNotFound")) {
                        event = eventReader.nextEvent();
                        if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                            outputBadTermList.add(event.asCharacters().getData());
                    }
                }
            }

        } catch (XMLStreamException e) {
            this.textMiningSuccessful = false;
            e.printStackTrace();
        } finally {
            if (xmlInputStream != null)
                xmlInputStream.close();
        }

//        try {
//            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder db = dbFactory.newDocumentBuilder();
//            Document document = db.parse(new URL(url).openStream());
//            document.getDocumentElement().normalize();
//
//            NodeList phrasesNotFound = document.getElementsByTagName("QuotedPhraseNotFound");
//            for (int i = 0; i < phrasesNotFound.getLength(); i++) {
//                outputBadTermList.add(phrasesNotFound.item(i).getTextContent().replaceAll("\"", ""));
//            }
//        } catch (ParserConfigurationException | SAXException e) {
//            e.printStackTrace();
//        }
        return outputBadTermList;
    }

    /**
     * Parses ESearch results (article PMIDs), which can be fed into the EFetch tool to retrieve more detailed article information
     */
//    private List<String> parsePmidSearchResults(String url) throws IOException {
//        List<String> outputList = new ArrayList<>();
//        try {
//            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//            Document doc = dBuilder.parse(new URL(url).openStream()); // create loop if doesn't go
//            doc.getDocumentElement().normalize();
//
//            String hitCount = doc.getElementsByTagName("Count").item(0).getTextContent(); // total hit count (not everything retrieved, limited by 'retmax' parameter)
//            this.hitCount = Integer.parseInt(hitCount);
//
//            Node pubmedArticleIds = doc.getElementsByTagName("IdList").item(0); // list of PubmedArticle ids
//            NodeList pubmedArticleIdList = ((Element) pubmedArticleIds).getElementsByTagName("Id");
//
//            for (int i = 0; i < pubmedArticleIdList.getLength(); i++) {
//                outputList.add(pubmedArticleIdList.item(i).getTextContent());
//            }
//
//        } catch (ParserConfigurationException | SAXException e) {
//            e.printStackTrace();
//            this.textMiningSuccessful = false;
//        }
//        return outputList;
//    }
    private ESearchResult parsePmidSearchResults(String url) throws IOException {
        String queryKey = null;
        String webEnv = null;
        int hitCount = 0;

        InputStream xmlInputStream = null;

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

            xmlInputStream = new URL(url).openStream();
            XMLEventReader eventReader = factory.createXMLEventReader(xmlInputStream, "UTF-8");

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    StartElement startElement = event.asStartElement();
                    String startElementName = startElement.getName().getLocalPart();

                    switch (startElementName) {
                        case "Count":
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                hitCount = Integer.parseInt(event.asCharacters().getData());
                            break;
                        case "QueryKey":
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                queryKey = event.asCharacters().getData();
                            break;
                        case "WebEnv":
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                webEnv = event.asCharacters().getData();
                            break;
                    }
                }
            }

        } catch (XMLStreamException e) {
            e.printStackTrace();
            this.textMiningSuccessful = false;
        } finally {
            if (xmlInputStream != null)
                xmlInputStream.close();
        }

//        try {
//            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//            Document doc = dBuilder.parse(new URL(url).openStream());
//            doc.getDocumentElement().normalize();
//
//            String hitCountStr = doc.getElementsByTagName("Count").item(0).getTextContent(); // total hit count (not everything retrieved, limited by 'retmax' parameter)
//            hitCount = Integer.parseInt(hitCountStr);
//
//            queryKey = doc.getElementsByTagName("QueryKey").item(0).getTextContent();
//            webEnv = doc.getElementsByTagName("WebEnv").item(0).getTextContent();
//
//        } catch (ParserConfigurationException | SAXException e) {
//            e.printStackTrace();
//            this.textMiningSuccessful = false;
//        }
        return new ESearchResult(webEnv, queryKey, hitCount);
    }

    private class ESearchResult {
        private String webEnv;
        private String queryKey;
        private int hitCount;

        private ESearchResult(String webEnv, String queryKey, int hitCount) {
            this.webEnv = webEnv;
            this.queryKey = queryKey;
            this.hitCount = hitCount;
        }
    }

    /**
     * Constructs query URL for the EFetch service given the PMIDs retrieved via the ESearch service
     */
//    private String constructFetchUrl(List<String> pmids) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(PMC_FETCH_URL).append("&id=");
//
//        for (int i = 0; i < pmids.size(); i++) {
//            sb.append(pmids.get(i));
//
//            if (i != pmids.size() - 1)
//                sb.append(",");
//        }
//        System.out.println(sb.toString());
//        return sb.toString();
//    }
    private String constructFetchUrl(ESearchResult eSearchResult) {
        return PMC_FETCH_URL +
                "&query_key=" + eSearchResult.queryKey +
                "&WebEnv=" + eSearchResult.webEnv;
    }

    /**
     * Retrieves articles using Entrez EFetch https://www.ncbi.nlm.nih.gov/books/NBK25499/
     * As input pass the url constructed by constructFetchUrl() method
     */
    public List<Article> downloadAndParseDOM(String url) throws IOException {
        List<Article> outputList = new ArrayList<>();
        System.out.println("TEXTMINING PUBMED: " + url);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new URL(url).openStream()); // create loop if doesn't go
            doc.getDocumentElement().normalize();

            NodeList pubmedArticles = doc.getElementsByTagName("PubmedArticle"); // list of PubmedArticles

            for (int i = 0; i < pubmedArticles.getLength(); i++) { // loop through retrieved articles
                Article outputArticle = new Article();

                Node pubmedArticle = pubmedArticles.item(i); // gets article

                Node medlineCitation = ((Element) pubmedArticle).getElementsByTagName("MedlineCitation").item(0);
                outputArticle.setPmid((((Element) medlineCitation).getElementsByTagName("PMID").item(0).getTextContent()));

                Node article = ((Element) medlineCitation).getElementsByTagName("Article").item(0);

                // JOURNAL INFO
                Node journal = ((Element) article).getElementsByTagName("Journal").item(0);

                NodeList journalVol = ((Element) journal).getElementsByTagName("Volume");
                if (journalVol.getLength() > 0)
                    outputArticle.setJournalVol(journalVol.item(0).getTextContent());

                NodeList journalIssue = ((Element) journal).getElementsByTagName("Issue");
                if (journalIssue.getLength() > 0)
                    outputArticle.setJournalIssue(((Element) journal).getElementsByTagName("Issue").item(0).getTextContent());

                NodeList journalTitle = ((Element) journal).getElementsByTagName("Title");
                if (journalTitle.getLength() > 0)
                    outputArticle.setJournalTitle(journalTitle.item(0).getTextContent());

                NodeList journalTitleShort = ((Element) journal).getElementsByTagName("ISOAbbreviation");
                if (journalTitleShort.getLength() > 0)
                    outputArticle.setJournalTitleShort(journalTitleShort.item(0).getTextContent());

                Node pubDate = ((Element) journal).getElementsByTagName("PubDate").item(0);

                NodeList pubDateYear = ((Element) pubDate).getElementsByTagName("Year");
                if (pubDateYear.getLength() > 0)
                    outputArticle.setPubDateYear(pubDateYear.item(0).getTextContent());

                NodeList pubDateMonth = ((Element) pubDate).getElementsByTagName("Month");
                if (pubDateMonth.getLength() > 0)
                    outputArticle.setPubDateMonthName(pubDateMonth.item(0).getTextContent());

                // TITLE AND ABSTRACT
                NodeList articleTitle = ((Element) article).getElementsByTagName("ArticleTitle");
                if (articleTitle.getLength() > 0)
                    outputArticle.setTitle(articleTitle.item(0).getTextContent());

                NodeList articleAbstract = ((Element) article).getElementsByTagName("AbstractText");
                if (articleAbstract.getLength() > 0)
                    outputArticle.setArtAbstract(articleAbstract.item(0).getTextContent());

                // AUTHORS
                NodeList authors = ((Element) article).getElementsByTagName("Author");

                for (int k = 0; k < authors.getLength(); k++) {
                    Element currentAuthor = (Element) authors.item(k);

                    if (currentAuthor.getAttribute("ValidYN").equals("Y")) {
                        NodeList lastName = currentAuthor.getElementsByTagName("LastName");
                        NodeList firstName = currentAuthor.getElementsByTagName("ForeName");
                        if (lastName.getLength() > 0 && firstName.getLength() > 0)
                            outputArticle.addArtAuthor(firstName.item(0).getTextContent(), lastName.item(0).getTextContent());
                    }
                }

                // DOI
                if (((Element) pubmedArticle).getElementsByTagName("ArticleIdList").getLength() > 0) {
                    Node articleIdList = ((Element) pubmedArticle).getElementsByTagName("ArticleIdList").item(0);
                    NodeList articleIds = articleIdList.getChildNodes();

                    for (int h = 0; h < articleIds.getLength(); h++) {
                        Node currentNode = articleIds.item(h);
                        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element currentId = (Element) currentNode;
                            if (currentId.getAttribute("IdType").equals("doi")) {
                                outputArticle.setDoiAndUrl(currentId.getTextContent());
                            }
                        }
                    }
                }

                // ARTICLE TYPOLOGY INFO
                if (((Element) article).getElementsByTagName("PublicationTypeList").getLength() > 0) {
                    Node publicationTypeList = ((Element) article).getElementsByTagName("PublicationTypeList").item(0);
                    NodeList publicationTypes = publicationTypeList.getChildNodes();

                    for (int h = 0; h < publicationTypes.getLength(); h++) {
                        Node currentNode = publicationTypes.item(h);
                        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element currentType = (Element) currentNode;
                            if (currentType.getTextContent().equalsIgnoreCase("review"))
                                outputArticle.setReview(true);
                        }
                    }
                }

                outputList.add(outputArticle);
            }
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            this.textMiningSuccessful = false;
        }
        return outputList;
    }

    /**
     * Download article data from PubMed and parse the XML file with StAX parser
     **/

    public List<Article> downloadParseArticles(String url) throws IOException {
        List<Article> outputList = new ArrayList<>();
        InputStream xmlInputStream = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            Thread.sleep(1000);
            xmlInputStream = new URL(url).openStream();
            XMLEventReader eventReader = factory.createXMLEventReader(xmlInputStream);

            Article article = null;

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT: {
                        StartElement startElement = event.asStartElement();
                        String startElementName = startElement.getName().getLocalPart();

                        if (startElementName.equals("PubmedArticle") || startElementName.equals("PubmedBookArticle"))
                            article = new Article();
                        else if (startElementName.equals("PMID")) {
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS) {
                                if (article == null)
                                    article = new Article();
                                else
                                    article.setPmid(event.asCharacters().getData());
                            }
                        } else if (startElementName.equals("Journal")) {
                            journalLoop:
                            while (true) {
                                event = eventReader.nextEvent();

                                switch (event.getEventType()) {
                                    case XMLStreamConstants.START_ELEMENT:
                                        startElement = event.asStartElement();
                                        startElementName = startElement.getName().getLocalPart();

                                        if (startElementName.equals("Volume")) {
                                            event = eventReader.nextEvent();
                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                article.setJournalVol(event.asCharacters().getData());
                                        } else if (startElementName.equals("Issue")) {
                                            event = eventReader.nextEvent();
                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                article.setJournalIssue(event.asCharacters().getData());
                                        } else if (startElementName.equals("Title")) {
                                            event = eventReader.nextEvent();
                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                article.setJournalTitle(event.asCharacters().getData());
                                        } else if (startElementName.equals("ISOAbbreviation")) {
                                            event = eventReader.nextEvent();
                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                article.setJournalTitleShort(event.asCharacters().getData());
                                        } else if (startElementName.equals("PubDate")) {
                                            pubDateLoop:
                                            while (true) {
                                                event = eventReader.nextEvent();

                                                switch (event.getEventType()) {
                                                    case XMLStreamConstants.START_ELEMENT:
                                                        startElement = event.asStartElement();
                                                        startElementName = startElement.getName().getLocalPart();

                                                        if (startElementName.equals("Year")) {
                                                            event = eventReader.nextEvent();
                                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                                article.setPubDateYear(event.asCharacters().getData());
                                                        } else if (startElementName.equals("Month")) {
                                                            event = eventReader.nextEvent();
                                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                                article.setPubDateMonthName(event.asCharacters().getData());
                                                        } else if (startElementName.equals("Day")) {
                                                            event = eventReader.nextEvent();
                                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                                article.setPubDateDay(event.asCharacters().getData());
                                                        }
                                                        break;
                                                    case XMLStreamConstants.END_ELEMENT:
                                                        EndElement endElement = event.asEndElement();
                                                        if (endElement.getName().getLocalPart().equals("PubDate"))
                                                            break pubDateLoop;
                                                        break;
                                                }
                                            }
                                        }
                                        break;

                                    case XMLStreamConstants.END_ELEMENT:
                                        EndElement endElement = event.asEndElement();
                                        if (endElement.getName().getLocalPart().equals("Journal"))
                                            break journalLoop;
                                        break;
                                }
                            }
                        } else if (startElementName.equals("ArticleTitle")) {
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                article.setTitle(event.asCharacters().getData());
                        } else if (startElementName.equals("AbstractText")) {
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                article.setArtAbstract(event.asCharacters().getData());
                        } else if (startElementName.equals("AuthorList")) {
                            String firstName = null;
                            String lastName = null;

                            authorLoop:
                            while (true) {
                                event = eventReader.nextEvent();

                                switch (event.getEventType()) {
                                    case (XMLStreamConstants.START_ELEMENT): {
                                        startElement = event.asStartElement();
                                        startElementName = startElement.getName().getLocalPart();
                                        if (startElementName.equals("Author")) {
                                            firstName = "";
                                            lastName = "";
                                        } else if (startElementName.equals("ForeName")) {
                                            event = eventReader.nextEvent();
                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                firstName = event.asCharacters().getData();
                                        } else if (startElementName.equals("LastName")) {
                                            event = eventReader.nextEvent();
                                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                                lastName = event.asCharacters().getData();
                                        }
                                        break;
                                    }
                                    case (XMLStreamConstants.END_ELEMENT): {
                                        EndElement endElement = event.asEndElement();
                                        String endElementName = endElement.getName().getLocalPart();
                                        if (endElementName.equals("AuthorList"))
                                            break authorLoop;
                                        else if (endElementName.equals("Author")) {
                                            article.addArtAuthor(firstName, lastName);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else if (startElementName.equals("ArticleId")) {
                            Attribute idAttr = startElement.getAttributeByName(new QName("IdType"));
                            if (idAttr != null) {
                                if (idAttr.getValue().equals("doi")) {
                                    event = eventReader.nextEvent();
                                    if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                        article.setDoiAndUrl(event.asCharacters().getData());
                                }
                            }
                        } else if (startElementName.equals("PublicationType")) {
                            event = eventReader.nextEvent();
                            if (event.getEventType() == XMLStreamConstants.CHARACTERS)
                                if (event.asCharacters().getData().equalsIgnoreCase("review"))
                                    article.setReview(true);
                        }

                        break;
                    }
                    case XMLStreamConstants.END_ELEMENT: {
                        EndElement endElement = event.asEndElement();
                        String endElementName = endElement.getName().getLocalPart();

                        if (endElementName.equals("PubmedArticle"))
                            outputList.add(article);

                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.textMiningSuccessful = false;
            if (!(e instanceof MalformedURLException))
                throw e;
        } catch (XMLStreamException e) {
            e.printStackTrace();
            this.textMiningSuccessful = false;
        } catch (InterruptedException e) {
            this.textMiningSuccessful = false;
            e.printStackTrace();
        } finally {
            if (xmlInputStream != null)
                xmlInputStream.close();
        }
        return outputList;
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
