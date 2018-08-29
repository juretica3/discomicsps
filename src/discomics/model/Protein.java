package discomics.model;

import discomics.application.DownloadFileHTTP;
import discomics.application.MyLogger;
import org.apache.log4j.varia.NullAppender;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by Jure on 4.9.2016.
 */
public class Protein implements TextMinableInput, Serializable {
    static final transient long serialVersionUID = 675735546256L;
    private static final String CLASS_NAME = Protein.class.getName();

    private static final String queryStemHGNC = "https://rest.genenames.org";
    private static final String queryPathFetchHGNC = "/fetch/";
    private static final String queryPathSearchHGNC = "/search/";
    private static final String queryIDGeneHGNC = "symbol/";
    private static final String querySynonymGeneHGNC = "alias_symbol/";
    private static final String queryPastNameGeneHGNC = "prev_symbol/";
    private static final String queryIDUniProtHGNC = "uniprot_ids/";
    private static final String queryIDEnsemblHGNC = "ensembl_gene_id/";

    //private static final String queryUniProtStem = "http://www.uniprot.org/uniprot/";
    private static final String queryUniProtStem2 = "https://www.ebi.ac.uk/proteins/api/proteins/";

    private final static String mapStringIdToUniprotIdStem = "http://www.uniprot.org/uniprot/?format=json&columns=id,database%28string%29&query=string%2B";

    private static final String queryStemDgiDb = "http://www.dgidb.org/api/v2/interactions.json?genes=";

    private static final String queryStemSTRING = "https://string-db.org/api/json/resolve?";
    private static final String queryIdentifierRootSTRING = "identifier=";
    private static final String querySpeciesRootSTRING = "species=";

    private static final String homoSapiensTaxonId = "9606"; // homo sapiens
    private static final String rattusNorvegicusTaxonId = "10116";
    private static final String musMusculusTaxonId = "10090";

    private String queryInputGene = "";
    private GeneFamily geneFamily; // empty gene family default
    private ProteinNomenclature proteinNomenclature;

    private String hgncId;
    private String uniprotId = "";
    private String ensemblId = "";
    private String stringId = "";

    private ArrayList<GoAnnotation> goAnnotations;
    private Set<Drug> drugs;

    private String proteaseSubtype; // only proteases

    private boolean successBuildingHGNC = false;
    private boolean ambiguousHGNC = false;
    private boolean successBuildingSTRING = false;
    private boolean successBuildingDGI = false;
    private boolean successBuildingUniProt = false;

    private NetworkNodeProtein rawNetworkNode;

    public Protein(String queryInputGene) throws SocketException, MalformedURLException {
        this();
        this.queryInputGene = queryInputGene;
        this.proteinNomenclature = new ProteinNomenclature(queryInputGene);

        query(queryInputGene);

        proteinNomenclature.addManualNameRules();
        proteinNomenclature.removeManualNameRules();
        proteinNomenclature.removeClashingNames();
        proteinNomenclature.forceApprovedName();
        proteinNomenclature.addHyphenatedGeneName();

        proteinNomenclature.addUnneccessaryPartsRemovedSynonyms();
        proteinNomenclature.addGreekSymbols();

        proteinNomenclature.defineIllegalNames();

        defineHgncId();
    }

    public Protein(String[] hgncDatabaseRowSplit) throws SocketException {
        // HGNC databse row structure:
        // COL0: HGNC ID
        // COL1: Ensembl Gene ID
        // COL2: UniProt ID
        // COL3: STRING ID
        // COL4: Approved symbol (ProteinNomenclature)
        // COL5: Approved name (ProteinNomenclature)
        // COL6: Gene Family
        // COL7: Synonym Symbols (ProteinNomenclature)
        // COL8: Synonym Names (ProteinNomenclature)
        // COL9: Added Names (ProteinNomenclature)
        // COL10: Illegal Names (ProteinNomenclature)

        this();
        this.proteinNomenclature = new ProteinNomenclature(hgncDatabaseRowSplit);
        this.hgncId = hgncDatabaseRowSplit[0];
        try {
            this.ensemblId = hgncDatabaseRowSplit[1];
            this.uniprotId = hgncDatabaseRowSplit[2];
            this.stringId = hgncDatabaseRowSplit[3];

            String geneFamily = hgncDatabaseRowSplit[6];
            if (!geneFamily.isEmpty())
                this.geneFamily = new GeneFamily(geneFamily);

        } catch (ArrayIndexOutOfBoundsException e) {
            //empty catch block. when information is lacking in the local database, the array might be shorter than predicted; not a problem at all
        }

        // check if all fields defined; if not search online
        if (this.uniprotId.isEmpty() && !this.stringId.isEmpty()) {
            mapStringIdToUniprotId(this.stringId);
        }

    }


    public Protein(JSONObject jsonObject) throws SocketException { // only used to define proteases when starting program
        this();
        this.proteinNomenclature = new ProteinNomenclature();

        parseJsonHGNC(jsonObject);
        if (jsonObject.has("stringId")) {
            this.stringId = jsonObject.getString("stringId"); //MMP28 only with no stringId
        }

        modifyAndBuildCustomNameRulesProtease();
        queryDgi(proteinNomenclature.getGeneName());
    }

    public Protein() {
        this.drugs = new HashSet<>();
        this.goAnnotations = new ArrayList<>();

        org.apache.log4j.BasicConfigurator.configure(new NullAppender()); // to show MyLogger output remove new NullAppender()
    }

    private void query(String queryInputGene) throws SocketException, MalformedURLException {
        queryNomenclature1(queryInputGene);

        if (!this.successBuildingHGNC) {
            queryNomenclature2(queryInputGene); // defines nomenclature, string Id
            this.successBuildingUniProt = queryUniProt(this.uniprotId);
            this.successBuildingDGI = queryDgi(this.proteinNomenclature.getGeneName());
            return;
        }

        this.successBuildingUniProt = queryUniProt(this.uniprotId);
        this.successBuildingSTRING = querySTRINGId(this.proteinNomenclature.getGeneName());
        this.successBuildingDGI = queryDgi(this.proteinNomenclature.getGeneName());
    }

    private void queryNomenclature1(String queryInputGene) throws SocketException, MalformedURLException {

        if (queryInputGene.length() > 14 && queryInputGene.toLowerCase().contains("ens")) { // ensembl id
            this.successBuildingHGNC = queryFetchHgnc(queryInputGene, queryIDEnsemblHGNC);
        } else if (queryInputGene.length() == 6 || queryInputGene.length() == 10) { // uniprot id
            if (!(this.successBuildingHGNC = queryFetchHgnc(queryInputGene, queryIDUniProtHGNC))) {
                this.successBuildingHGNC = queryFetchHgnc(queryInputGene, queryIDGeneHGNC);
            }
        } else { // gene name
            this.successBuildingHGNC = queryFetchHgnc(queryInputGene, queryIDGeneHGNC);
        }

        // if FETCH request not successful, try SEARCH request
        if (!this.successBuildingHGNC)
            this.successBuildingHGNC = querySearchHgnc(queryInputGene, queryIDGeneHGNC); // search geneId field
        if (!this.successBuildingHGNC)
            this.successBuildingHGNC = querySearchHgnc(queryInputGene, querySynonymGeneHGNC); // search synonym field
        if (!this.successBuildingHGNC)
            this.successBuildingHGNC = querySearchHgnc(queryInputGene, queryPastNameGeneHGNC); // search past name field
    }

    private void queryNomenclature2(String queryInputGene) throws SocketException, MalformedURLException {
        this.successBuildingSTRING = queryNomenclatureSTRING(queryInputGene); // try retrieving nomeclature from STRING

        if (this.successBuildingSTRING) // if retrieving nomenclature from STRING is successful, try searching for STRING retrieved gene name on HGNC
            if (!(this.successBuildingHGNC = queryFetchHgnc(this.proteinNomenclature.getGeneName(), queryIDGeneHGNC)))
                this.successBuildingHGNC = this.successBuildingSTRING;

        mapStringIdToUniprotId(stringId);
    }

    String convertPseudogeneToParentGene() {
        String geneName = this.proteinNomenclature.getGeneName();
        String proteinName = this.proteinNomenclature.getApprovedProteinName();

        boolean isPseudogene = proteinName.toLowerCase().contains("pseudogene");
        if (isPseudogene) {
            for (int i = geneName.length() - 1; i >= 0; i--) {
                if (!Character.isDigit(geneName.charAt(i)))
                    if (geneName.charAt(i) == 'P' || geneName.charAt(i) == 'p') {
                        return geneName.substring(0, i);
                    } else {
                        return null;
                    }
            }
        }
        return null;
    }

    // check hgncId is defined and not null
    private void defineHgncId() {
        if (this.hgncId == null) {

            if (!this.uniprotId.isEmpty())
                this.hgncId = this.uniprotId;
            else if (!this.ensemblId.isEmpty())
                this.hgncId = this.ensemblId;
            else if (!this.stringId.isEmpty())
                this.hgncId = this.stringId;
            else if (!this.proteinNomenclature.getGeneName().isEmpty())
                this.hgncId = this.proteinNomenclature.getGeneName();
            else
                this.hgncId = "";
        }
    }

    private boolean queryFetchHgnc(String queryInputGene, String searchIDParameter) throws SocketException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        String url = queryStemHGNC + queryPathFetchHGNC + searchIDParameter + queryInputGene;
        String stringJSON = DownloadFileHTTP.downloadOnlineFileWithHeader(url);

        System.out.println("HGNC QUERY " + queryInputGene + ": " + url);

        JSONObject objJSON;
        try {
            objJSON = new JSONObject(stringJSON); // null pointer if no internet, caught in Controller
        } catch (JSONException e) {
            e.printStackTrace();
            System.err.println("ERROR WITH " + queryInputGene);
            return false;
        }

        int numFound = objJSON.getJSONObject("response").getInt("numFound");
        if (numFound == 0) {
            MyLogger.log(Level.WARNING, "No genes matching query were found; this might be an error in input");
            return false;
        } else if (numFound > 1) {
            MyLogger.log(Level.WARNING, "Multiple genes matching query were found, only first will be considered");
        }

        JSONObject obj = objJSON.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
        return parseJsonHGNC(obj);
    }

    private boolean querySearchHgnc(String queryInputGene, String searchField) throws SocketException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        String url = queryStemHGNC + queryPathSearchHGNC + searchField + queryInputGene;
        String stringJSON = DownloadFileHTTP.downloadOnlineFileWithHeader(url);

        System.out.println("HGNC RETRY QUERY " + queryInputGene + ": " + url);

        JSONObject objJSON;
        try {
            objJSON = new JSONObject(stringJSON); // null pointer if no internet, caught in Controller
        } catch (JSONException e) {
            e.printStackTrace();
            System.err.println("ERROR WITH " + queryInputGene);
            return false;
        }

        int numFound = objJSON.getJSONObject("response").getInt("numFound");
        if (numFound == 0) {
            MyLogger.log(Level.WARNING, "No genes matching query were found; this might be an error in input");
            return false;
        } else if (numFound == 1) {
            JSONObject obj = objJSON.getJSONObject("response").getJSONArray("docs").getJSONObject(0);
            if (obj.has("symbol")) {
                String geneSymbol = obj.getString("symbol");
                MyLogger.exiting(CLASS_NAME, methodName);
                return queryFetchHgnc(geneSymbol, queryIDGeneHGNC);
            } else {
                this.ambiguousHGNC = true;
                MyLogger.log(Level.WARNING, "Error parsing downloaded HGNC JSON");
                MyLogger.exiting(CLASS_NAME, methodName);
                return false;
            }
        } else {
            MyLogger.log(Level.WARNING, "Ambiguous find, nothing retrieved");
            return false;
        }
    }

    private boolean parseJsonHGNC(JSONObject jsonObject) {

        if (jsonObject.has("hgnc_id")) {
            this.hgncId = jsonObject.getString("hgnc_id");
        }

        if (jsonObject.has("name")) {
            this.proteinNomenclature.setProteinName(jsonObject.getString("name"));
        }

        if (jsonObject.has("symbol")) {
            this.proteinNomenclature.setGeneName(jsonObject.getString("symbol"));
        }

        if (jsonObject.has("uniprot_ids")) {
            JSONArray uniprotIDs = jsonObject.getJSONArray("uniprot_ids");
            this.uniprotId = uniprotIDs.getString(0);
        }

        if (jsonObject.has("ensembl_gene_id")) {
            this.ensemblId = jsonObject.getString("ensembl_gene_id");
        }

        // alias names were never approved. They are likely to cause non-specific articles to be picked up
        if (jsonObject.has("alias_name")) {
            JSONArray aliasNames = jsonObject.getJSONArray("alias_name");
            for (int i = 0; i < aliasNames.length(); i++) {
                this.proteinNomenclature.addSynonymName(aliasNames.getString(i));
            }
        }
        if (jsonObject.has("alias_symbol")) {
            JSONArray aliasSymbols = jsonObject.getJSONArray("alias_symbol");
            for (int i = 0; i < aliasSymbols.length(); i++) {
                this.proteinNomenclature.addSynonymSymbol(aliasSymbols.getString(i));
            }
        }
        if(jsonObject.has("gene_family")) {
            JSONArray geneFamily = jsonObject.getJSONArray("gene_family");
            this.geneFamily = new GeneFamily(geneFamily.getString(0));
        }
//        if (jsonObject.has("prev_name")) {
//            JSONArray prevNames = jsonObject.getJSONArray("prev_name");
//            for (int i = 0; i < prevNames.length(); i++) {
//                String name = prevNames.getString(i);
//                if (name.contains("(") && name.contains(")")) {
//                    int openIndex = name.indexOf("(");
//                    int closeIndex = name.indexOf(")");
//                    String nameBefore = name.substring(0, openIndex - 1);
//                    String nameAfter = name.substring(closeIndex + 1);
//                    name = nameBefore + nameAfter;
//                }
//                this.proteinNomenclature.addPastName(name.trim());
//            }
//        }
//        if (jsonObject.has("prev_symbol")) {
//            JSONArray prevSymbols = jsonObject.getJSONArray("prev_symbol");
//            for (int i = 0; i < prevSymbols.length(); i++) {
//                if (prevSymbols.getString(i).length() > 3)
//                    this.proteinNomenclature.addPastSymbol(prevSymbols.getString(i));
//            }
//        }
        return true;
    }

    private boolean queryUniProt(String uniprotId) throws SocketException {
        if (uniprotId.isEmpty())
            return false;

        String url = queryUniProtStem2 + uniprotId;
        System.out.println("UNIPROT QUERY: " + url);
        String stringJSON = DownloadFileHTTP.downloadOnlineFileWithHeader(url);

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(stringJSON);
            JSONArray dbReferences = jsonObject.getJSONArray("dbReferences");
            for (int i = 0; i < dbReferences.length(); i++) {
                JSONObject dbRef = dbReferences.getJSONObject(i);

                if (dbRef.has("type")) {
                    String dbRefType = dbRef.getString("type");
                    if (dbRefType.equals("GO")) {
                        String goId = dbRef.getString("id");
                        String goTerm = dbRef.getJSONObject("properties").getString("term");
                        GoAnnotation goAnnotation = new GoAnnotation(goId, goTerm);
                        this.goAnnotations.add(goAnnotation);
                    } else if (dbRefType.equals("STRING")) {
                        if (dbRef.has("id"))
                            this.stringId = dbRef.getString("id");
                    }
                }
            }
            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

//    private boolean queryUniProt(String uniprotId) throws SocketException {
//        if (uniprotId.isEmpty())
//            return false;
//
//        InputStream inputStream = null;
//        try {
//            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//            documentBuilderFactory.setNamespaceAware(false);
//            documentBuilderFactory.setValidating(false);
//            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
//            String queryUrl = queryUniProtStem + uniprotId + ".xml";
//            System.out.println("UNIPROT QUERY " + queryInputGene + ": " + queryUrl);
//
//            for (int i = 0; i < 20; i++) {
//                try {
//                    inputStream = new URL(queryUrl).openStream();
//                    break;
//                } catch (UnknownHostException | SocketException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (inputStream == null) {
//                throw new SocketException();
//            }
//
//            Document document = documentBuilder.parse(inputStream);
//            document.getDocumentElement().normalize();
//
//            // add UniProt nomenclature
////            Node proteinNames = document.getElementsByTagName("protein").item(0);
////            NodeList recommendedNames = ((Element) proteinNames).getElementsByTagName("recommendedName");
////            for (int i = 0; i < recommendedNames.getLength(); i++) {
////                Node recommendedName = recommendedNames.item(i);
////                parseUniProtNameBlock(recommendedName);
////            }
////
////            NodeList alternativeNames = ((Element) proteinNames).getElementsByTagName("alternativeName");
////            for (int i = 0; i < alternativeNames.getLength(); i++) {
////                Node alternativeName = alternativeNames.item(i);
////                parseUniProtNameBlock(alternativeName);
////            }
//
////            Node gene = document.getElementsByTagName("gene").item(0);
////            NodeList geneNames = ((Element) gene).getElementsByTagName("name");
////            for (int i = 0; i < geneNames.getLength(); i++) {
////                Node geneName = geneNames.item(i);
////                if (((Element) geneName).getAttribute("type").equalsIgnoreCase("primary")) {
////                    this.geneName = geneName.getTextContent();
////                } else if (((Element) geneName).getAttribute("type").equalsIgnoreCase("synonym")) {
////                    String geneNameStr = geneName.getTextContent();
////                    this.alternativeGeneNames.add(geneNameStr);
////                    if (geneNameStr.length() > 3) // dont add abbreviations shorter than 3 letter; specificity issues
////                        this.proteinNamesQuery.add(geneNameStr);
////                }
////            }
//
//            // parse GO terminology
//            NodeList dbReferences = document.getElementsByTagName("dbReference");
//            for (int i = 0; i < dbReferences.getLength(); i++) {
//                Node dbReference = dbReferences.item(i);
//                String dbRefType = ((Element) dbReference).getAttribute("type");
//                if (dbRefType.equalsIgnoreCase("GO")) {
//                    String goId = ((Element) dbReference).getAttribute("id");
//                    String goTerm = "";
//                    String goEvidence = "";
//
//                    NodeList properties = ((Element) dbReference).getElementsByTagName("property");
//                    for (int j = 0; j < properties.getLength(); j++) {
//                        Node property = properties.item(j);
//                        if (((Element) property).getAttribute("type").equalsIgnoreCase("term")) {
//                            goTerm = ((Element) property).getAttribute("value");
//                        } else if (((Element) property).getAttribute("type").equalsIgnoreCase("evidence")) {
//                            goEvidence = ((Element) property).getAttribute("value");
//                        }
//                    }
//
//                    GoAnnotation goAnnotation = new GoAnnotation(goId, goTerm);
//                    this.goAnnotations.add(goAnnotation);
//                }
//            }
//            return true;
//        } catch (SAXException | ParserConfigurationException | IOException e) {
//            e.printStackTrace();
//            if (e instanceof UnknownHostException || e instanceof SocketException)
//                throw new SocketException();
//            return false;
//        } finally {
//            try {
//                if (inputStream != null)
//                    inputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void parseUniProtNameBlock(Node nameBlock) {
        NodeList fullNames = ((Element) nameBlock).getElementsByTagName("fullName");
        for (int j = 0; j < fullNames.getLength(); j++) {
            String fullName = fullNames.item(j).getTextContent();
            this.proteinNomenclature.addSynonymName(fullName);
        }
        NodeList shortNames = ((Element) nameBlock).getElementsByTagName("shortName");
        for (int j = 0; j < shortNames.getLength(); j++) {
            String shortName = shortNames.item(j).getTextContent();
            this.proteinNomenclature.addSynonymName(shortName);
        }
    }


    private boolean queryDgi(String queryInputGene) throws SocketException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        try {

            String stringJSON;
            String url = queryStemDgiDb + queryInputGene;
            stringJSON = DownloadFileHTTP.downloadOnlineFileWithHeader(url);

            System.out.println("DGI QUERY " + queryInputGene + ": " + url);

            if (stringJSON.toLowerCase().contains("error")) {
                MyLogger.log(Level.SEVERE, "HGNC File downloaded is null" + queryInputGene);
                return false;
            }
            JSONObject objJSON = new JSONObject(stringJSON); // null pointer if no internet, caught in Controller

            JSONArray array;
            if (objJSON.has("matchedTerms"))
                array = objJSON.getJSONArray("matchedTerms");
            else
                return false;

            if (array.length() == 0) {
                MyLogger.log(Level.FINE, "No drugs were found for this gene");
                return true;
            }

            parseJsonDgiDb(array.getJSONObject(0).getJSONArray("interactions"));

            MyLogger.log(Level.FINE, "Gene standard nomenclature retrieved from HGNC successfully");
            MyLogger.exiting(CLASS_NAME, methodName);

            return true;

        } catch (JSONException | NullPointerException e) { // JSONException occurs if error message retrieved when creating JSONArray
            e.printStackTrace();
            System.err.print("EXCEPTION WITH: " + queryInputGene);
            return false;
        }
    }

    private void parseJsonDgiDb(JSONArray jsonArray) throws SocketException {

        for (int i = 0; i < jsonArray.length(); i++) {
            String drugChemblId = "";
            String drugName = "";
            JSONArray interactionTypes;
            ArrayList<String> interactionTypesProcessed = new ArrayList<>();

            JSONObject drugObject = jsonArray.getJSONObject(i);

            if (drugObject.has("interactionTypes")) {
                interactionTypes = drugObject.getJSONArray("interactionTypes");
                for (int j = 0; j < interactionTypes.length(); j++) {
                    interactionTypesProcessed.add(interactionTypes.getString(j));
                }
            }
            if (drugObject.has("drugName"))
                drugName = drugObject.getString("drugName");
            if (drugObject.has("drugChemblId"))
                drugChemblId = drugObject.getString("drugChemblId");

            if (!drugName.isEmpty() || !drugName.equals("null")) {
                Drug interaction = new Drug(drugChemblId, interactionTypesProcessed, drugName);
                this.drugs.add(interaction);
            }
        }
    }

    private boolean queryNomenclatureSTRING(String queryInputGene) throws SocketException {

        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        MyLogger.entering(CLASS_NAME, methodName);

        try {
            String stringJSON;
            String url = queryStemSTRING + queryIdentifierRootSTRING + queryInputGene;
            stringJSON = DownloadFileHTTP.downloadOnlineFileNoHeader(url);

            System.out.println("STRING NOMENCLATURE QUERY " + queryInputGene + ": " + url);

            if (stringJSON == null || stringJSON.contains("Error"))
                return false;

            JSONArray array = new JSONArray(stringJSON);

            if (array.length() == 0)
                return false;

            return parseJsonNomenclatureSTRING(array, queryInputGene);

        } catch (JSONException | NullPointerException e) { // JSONException if error is returned, when creating JSONArray
            e.printStackTrace();
            return false;
        }
    }


    private boolean parseJsonNomenclatureSTRING(JSONArray jsonArray, String queryInputGene) {
        ArrayList<StringEntryForParsing> stringEntries = new ArrayList<>();
        // first iteration, add only if queryInputGene equals to what is retrieved from STRING
        for (int i = 0; i < jsonArray.length(); i++) { // iterate through retrieved entries

            // add all entries to list for parsing
            JSONObject object = jsonArray.getJSONObject(i);
            if (object.has("ncbiTaxonId") && object.has("preferredName") && object.has("stringId")) {

                String proteinName = "";
                if (object.has("annotation")) {
                    String annotationField = object.getString("annotation");
                    if (annotationField.contains(";"))  // name is written before first semicolon, after which description follows
                        proteinName = annotationField.substring(0, annotationField.indexOf(';'));
                    else // if annotation field doesn't contain ';', then description after ';' is absent, only name present
                        proteinName = annotationField;

                    proteinName = proteinName.trim();
                    int openBracketIndex = proteinName.indexOf('(');
                    int closedBracketIndex = proteinName.indexOf(')');
                    if (openBracketIndex >= 0)
                        proteinName = proteinName.substring(0, openBracketIndex - 1) + proteinName.substring(closedBracketIndex + 1, proteinName.length()); //remove bracket content
                    proteinName = proteinName.substring(0, 1).toUpperCase() + proteinName.substring(1); // first letter uppercase
                } else {
                    proteinName = object.getString("preferredName");
                }

                StringEntryForParsing stringEntryForParsing = new StringEntryForParsing(object.getInt("ncbiTaxonId"),
                        object.getString("preferredName"), proteinName, object.getString("stringId"));
                stringEntries.add(stringEntryForParsing);
            }
        }

        // below nomenclature retrieval attempts arranged by priority; if nothing found for one, move to next, otherwise stop searching
        if (readWriteStringEntryNames(queryInputGene, stringEntries, homoSapiensTaxonId, true))
            return true;

        if (readWriteStringEntryNames(queryInputGene, stringEntries, musMusculusTaxonId, true))
            return true;

        if (readWriteStringEntryNames(queryInputGene, stringEntries, rattusNorvegicusTaxonId, true))
            return true;

        if (readWriteStringEntryNames(queryInputGene, stringEntries, homoSapiensTaxonId, false))
            return true;

        if (readWriteStringEntryNames(queryInputGene, stringEntries, musMusculusTaxonId, false))
            return true;

        return readWriteStringEntryNames(queryInputGene, stringEntries, rattusNorvegicusTaxonId, false);

    }

    private boolean readWriteStringEntryNames(String queryInputGene, List<StringEntryForParsing> stringEntries, String taxonId, boolean hasToEqualUserInputGene) {
        List<StringEntryForParsing> listEntries = new ArrayList<>();
        // in first iteration check that queryInputGene equals String record gene
        for (StringEntryForParsing stringEntry : stringEntries) {
            if (stringEntry.speciesId == Integer.parseInt(taxonId)) { // check taxonId
                if (hasToEqualUserInputGene) { // check if has to equal user input gene
                    if (stringEntry.geneName.equalsIgnoreCase(queryInputGene)) { // check if equals user input gene
                        listEntries.add(stringEntry);
                    }
                } else { // if doesnt have to equal user input gene
                    listEntries.add(stringEntry);
                }
            }
        }
        if (listEntries.size() > 1) { // ambiguous, found more than 1, not sure which one is correct, so fail search
            this.ambiguousHGNC = true;
            return false;
        } else if (listEntries.size() == 1) { // if one only result found, then that's it
            this.proteinNomenclature.setGeneName(listEntries.get(0).geneName);
            this.stringId = listEntries.get(0).stringId;
            this.proteinNomenclature.setProteinName(listEntries.get(0).proteinName);
            this.ambiguousHGNC = false;
            return true;
        } else { // when size = 0
            return false;
        }
    }

    private class StringEntryForParsing {
        private int speciesId;
        private String geneName;
        private String proteinName;
        private String stringId;

        private StringEntryForParsing(int speciesId, String geneName, String proteinName, String stringId) {
            this.speciesId = speciesId;
            this.geneName = geneName;
            this.proteinName = proteinName;
            this.stringId = stringId;
        }
    }

    private boolean mapStringIdToUniprotId(String stringId) throws SocketException {
        try {
            if (!isSuccessBuildingSTRING())
                return false;

            String url = mapStringIdToUniprotIdStem + stringId;
            System.out.println("STRING TO UNIPROT ID MAPPING: " + url);
            String jsonString = DownloadFileHTTP.downloadOnlineFileNoHeader(url);

            if (jsonString == null || jsonString.contains("Error"))
                return false;

            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject jsonObject = jsonArray.getJSONObject(0);

            if (jsonObject.has("id")) {
                this.uniprotId = jsonObject.getString("id");
                return true;
            } else
                return false;

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }


//    private boolean queryUniProt(String uniprotId) {
//
//        if (uniprotId == null)
//            return false;
//
//        URL url = null;
//        InputStream is = null;
//
//        try {
//            url = new URL(queryUniProtStem + uniprotId + ".xml");
//            System.out.println("UNIPROT QUERY " + queryInputGene + ": " + url.toString());
//
//            is = url.openStream();
//
//            XMLInputFactory factory = XMLInputFactory.newInstance();
//            XMLEventReader reader = factory.createXMLEventReader(is);
//
//            while (reader.hasNext()) {
//                XMLEvent event = reader.nextEvent();
//
//                if (event.isStartElement()) {
//                    StartElement element = event.asStartElement();
//
//                    if (element.getName().getLocalPart().equals(""))
//
//                        if (element.getName().getLocalPart().equals("dbReference")) {
//                            String type = element.getAttributeByName(new QName(null, "type")).getValue();
//
//                            switch (type) {
////                            case "STRING":
////                                String testID = element.getAttributeByName(new QName(null, "id")).getValue();
////                                System.out.println("TEST STRING ID " + testID);
////                                this.stringId = testID;
////                                break;
//
//                                case "GO":
//                                    String goId = element.getAttributeByName(new QName(null, "id")).getValue();
//                                    String goTerm = "";
//                                    String goEvidence = "";
//
//                                    while (true) {
//                                        event = reader.nextEvent();
//
//                                        StartElement element1;
//                                        if (event.isStartElement())
//                                            element1 = event.asStartElement();
//                                        else if (event.isEndElement()) {
//                                            if (event.asEndElement().getName().getLocalPart().equals("dbReference"))
//                                                break;
//                                            else
//                                                continue;
//                                        } else
//                                            continue;
//
//                                        String type1 = element1.getAttributeByName(new QName(null, "type")).getValue();
//
//                                        if (type1.equals("term") && element1.getName().getLocalPart().equals("property"))
//                                            goTerm = element1.getAttributeByName(new QName(null, "value")).getValue();
//                                        else if (type1.equals("evidence") && element1.getName().getLocalPart().equals("property"))
//                                            goEvidence = element1.getAttributeByName(new QName(null, "value")).getValue();
//
//                                    }
//                                    GoAnnotation goAnnotation = new GoAnnotation(goId, goTerm, goEvidence);
//                                    this.goAnnotations.add(goAnnotation);
//                                    break;
//                            }
//                        }
//
////                    case XMLStreamConstants.CHARACTERS:
////                        tagContent = reader.getText().trim();
////                        break;
////
////                    case XMLStreamConstants.END_ELEMENT:
////                        switch (reader.getLocalName()) {
////                            case "employee":
////                                empList.add(currEmp);
////                                break;
////                            case "firstName":
////                                currEmp.firstName = tagContent;
////                                break;
////                            case "lastName":
////                                currEmp.lastName = tagContent;
////                                break;
////                            case "location":
////                                currEmp.location = tagContent;
////                                break;
////                        }
////                        break;
////
////                    case XMLStreamConstants.START_DOCUMENT:
////                        empList = new ArrayList<>();
////                        break;
//                }
//
//            }
//            return true;
//
//        } catch (IOException | XMLStreamException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    private boolean querySTRINGId(String queryInputGene) throws SocketException {

        String stringJSON;
        StringBuilder sb = new StringBuilder();
        try {
            if (!ensemblId.isEmpty()) {
                sb.append(queryStemSTRING)
                        .append(queryIdentifierRootSTRING)
                        .append(ensemblId)
                        .append("&")
                        .append(querySpeciesRootSTRING)
                        .append(homoSapiensTaxonId);

                stringJSON = DownloadFileHTTP.downloadOnlineFileNoHeader(sb.toString());
                System.out.println("STRING QUERY " + queryInputGene + ": " + sb.toString());

                if (stringJSON.contains("Error"))
                    return false;

                JSONArray arrayJSON = new JSONArray(stringJSON);

                if (arrayJSON.getJSONObject(0).has("stringId")) {
                    stringId = arrayJSON.getJSONObject(0).getString("stringId");
                }
            } else {
                sb.append(queryStemSTRING)
                        .append(queryIdentifierRootSTRING)
                        .append(queryInputGene)
                        .append("&")
                        .append(querySpeciesRootSTRING)
                        .append(homoSapiensTaxonId);

                stringJSON = DownloadFileHTTP.downloadOnlineFileNoHeader(sb.toString());
                System.out.println("STRING QUERY " + queryInputGene + ": " + sb.toString());

                if (stringJSON.contains("Error"))
                    return false;

                JSONArray arrayJSON = new JSONArray(stringJSON);

                for (int i = 0; i < arrayJSON.length(); i++) {
                    if (arrayJSON.getJSONObject(i).getString("preferredName").toLowerCase().equals(queryInputGene.toLowerCase())) {
                        if (arrayJSON.getJSONObject(i).has("stringId")) {
                            stringId = arrayJSON.getJSONObject(i).getString("stringId");
                            break;
                        }
                    }
                }
            }
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        return !stringId.isEmpty();
    }


    private void modifyAndBuildCustomNameRulesProtease() {

        if (proteinNomenclature.getGeneName().contains("CTS")) {
            this.proteaseSubtype = proteinNomenclature.getGeneName().replace("CTS", "");
            proteinNomenclature.addSynonymSymbol("CAT" + this.proteaseSubtype);
            proteinNomenclature.addSynonymSymbol("CATH" + this.proteaseSubtype);
        } else if (proteinNomenclature.getGeneName().contains("ADAM")) {
            if (proteinNomenclature.getGeneName().contains("ADAMTS")) {
                this.proteaseSubtype = proteinNomenclature.getGeneName().replace("ADAMTS", "");
                proteinNomenclature.addSynonymSymbol("ADAMT-S" + this.proteaseSubtype);
                proteinNomenclature.addSynonymSymbol("ADAM-TS" + this.proteaseSubtype);
                proteinNomenclature.addSynonymSymbol("ADAMTS-" + this.proteaseSubtype);
            } else {
                this.proteaseSubtype = proteinNomenclature.getGeneName().replace("ADAM", "");
                proteinNomenclature.addSynonymSymbol("ADAM" + this.proteaseSubtype);
                proteinNomenclature.addSynonymSymbol("ADAM-" + this.proteaseSubtype);
            }
        } else if (proteinNomenclature.getGeneName().contains("MMP")) {
            this.proteaseSubtype = proteinNomenclature.getGeneName().replace("MMP", "");
            proteinNomenclature.addSynonymSymbol("MMP-" + this.proteaseSubtype);
            proteinNomenclature.addSynonymName("matrix metalloproteinase " + this.proteaseSubtype);
            proteinNomenclature.addSynonymName("matrix metalloprotease " + this.proteaseSubtype);
        }
    }


    // GETTERS FOR IDENTIFIERS
    public String getEnsemblId() {
        return ensemblId;
    }

    public String getHgncId() {
        return hgncId;
    }

    public String getUniprotId() {
        return uniprotId;
    }

    public String getStringId() {
        return stringId;
    }

    public String getQueryInputGene() {
        return queryInputGene;
    }


    // GETTERS FOR QUERY OUTCOME MARKERS
    boolean isSuccessBuildingNomenclature() {
        return proteinNomenclature.isSuccessBuildingNomenclature();
    }

    public boolean isAmbiguousHGNC() {
        return ambiguousHGNC;
    }

    public boolean isSuccessBuildingDGI() {
        return successBuildingDGI;
    }

    public boolean isSuccessBuildingUniProt() {
        return successBuildingUniProt;
    }

    public boolean isSuccessBuildingSTRING() {
        return !stringId.isEmpty();
    }


    // OTHER GETTERS
    public String getProteaseSubtype() {
        return proteaseSubtype;
    }

    public List<Drug> getDrugs() {
        return new ArrayList<>(this.drugs);
    }

    public ArrayList<GoAnnotation> getGoAnnotations() {
        return new ArrayList<>(goAnnotations);
    }

    public static String getHomoSapiensTaxonId() {
        return homoSapiensTaxonId;
    }

    public static String getRattusNorvegicusTaxonId() {
        return rattusNorvegicusTaxonId;
    }

    public static String getMusMusculusTaxonId() {
        return musMusculusTaxonId;
    }

    void setRawNetworkNode(NetworkNodeProtein networkNode) {
        this.rawNetworkNode = networkNode;
    }

    NetworkNodeProtein getRawNetworkNode() {
        return rawNetworkNode;
    }

    public GeneFamily getGeneFamily() {
        return geneFamily;
    }


    // RETRIEVE PROTEIN NOMENCLATURE
    public String getProteinName() {
        return proteinNomenclature.getApprovedProteinName();
    }

    @Override
    public String getMainName() {
        return proteinNomenclature.getGeneName();
    }

    @Override
    public List<String> getTextMiningNames(QuerySettings querySettings) {
        List<String> outputNames = new ArrayList<>();
        if (querySettings == null || !querySettings.isSearchOnlyGeneName()) {
            outputNames.addAll(proteinNomenclature.getNamesOnlineQuery());
        } else {
            outputNames.add(proteinNomenclature.getGeneName());
        }
        return outputNames;
    }

    public List<String> getLongNamesFiltering() {
        return proteinNomenclature.getLongIdentifiersFiltering();
    }

    public List<String> getShortNamesFiltering() {
        return proteinNomenclature.getShortIdentifiersFiltering();
    }

    public List<String> getDictionaryClashingNames() {
        return proteinNomenclature.getDictionaryClashingTerms();
    }

    public ProteinName getProteinNameObject() {
        return proteinNomenclature.getProteinName();
    }

    public String getDiseaseApprovedName() {
        return proteinNomenclature.getDiseaseApprovedName();
    }

    public List<String> getIllegalNames() {
        return proteinNomenclature.getIllegalNames();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Protein protein = (Protein) o;

        if (!hgncId.equalsIgnoreCase(protein.getHgncId())) return false;
        if (!proteinNomenclature.getGeneName().equalsIgnoreCase(protein.proteinNomenclature.getGeneName()))
            return false;
        if (!uniprotId.equalsIgnoreCase(protein.uniprotId)) return false;
        if (!ensemblId.equalsIgnoreCase(protein.ensemblId)) return false;
        return stringId.equalsIgnoreCase(protein.stringId);
    }

    @Override
    public int hashCode() {
        return uniprotId != null ? uniprotId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Protein{" +
                ", uniprotId='" + uniprotId + '\'' +
                ", ensemblId='" + ensemblId + '\'' +
                ", stringId='" + stringId + '\'' +
                '}';
    }
}
