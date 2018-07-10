package discomics.model;

import discomics.application.DownloadFileHTTP;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jure on 19/11/2016.
 */
public class Drug implements TextMinableInput, Serializable {
    static final transient long serialVersionUID = 1252234224984456L;

    //private static String PUBCHEM_BASE_URL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/substance/name/";
    //private static String PUBCHEM_SUFFIX_URL = "/cids/XML";

    private static String STITCH_BASE_URL = "http://stitch.embl.de/api/json/resolve?format=only-ids&identifier=";

    private ArrayList<String> interactionTypes;
    private String drugName;
    //private String drugChemblId;
    private String drugId = null; // either DrugBank ID or STITCH ID (PubChem CID)

    Drug(String drugChemblId, ArrayList<String> interactionTypes, String drugName) throws SocketException {
        this.interactionTypes = interactionTypes;

        // in some names these tags are used to denote superscript/subscript letters/numbers. remove those
        drugName = drugName.replace("<SUP>", "");
        drugName = drugName.replace("</SUP>", "");
        drugName = drugName.replace("<SUB>", "");
        drugName = drugName.replace("</SUB>", "");

        drugName = drugName.replaceAll("\\(CHEMBL[\\d]+\\)$", "").trim();

        this.drugName = drugName;

        if (!drugName.isEmpty()) {
            this.drugId = queryDrugBankCsvForId(drugName); // first from DrugBank CSV file (faster)
            if (this.drugId == null) // if not found, query STITCH
                this.drugId = downloadDrugIdStitch(drugChemblId);
        }
    }

//    private List<String> downloadDrugIdsPubchem(String drugName) {
//        String url = PUBCHEM_BASE_URL + drugName + PUBCHEM_SUFFIX_URL;
//        HashSet<String> idSet = new HashSet<>();
//
//        try {
//            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//            Document doc = dBuilder.parse(new URL(url).openStream());
//            doc.getDocumentElement().normalize();
//
//            NodeList idList = doc.getElementsByTagName("CID");
//            for (int i = 0; i < idList.getLength(); i++) {
//                idSet.add("CID" + idList.item(i).getTextContent());
//                if (idSet.size() >= 2) // stop once two IDs have been collected
//                    break;
//            }
//        } catch (ParserConfigurationException | SAXException | IOException e) {
//            e.printStackTrace();
//        }
//        return new ArrayList<>(idSet);
//    }

    private String downloadDrugIdStitch(String drugName) throws SocketException {
        String url = "";

        try {
            url = STITCH_BASE_URL + URLEncoder.encode(drugName, "UTF-8");
            System.out.println("DRUG ID STITCH " + drugName + ": " + url);
            String drugId = DownloadFileHTTP.downloadOnlineFileNoHeader(url);

            if (drugId == null || drugId.contains("Error"))
                return null;

            JSONArray idArray = new JSONArray(drugId);

            if (idArray.length() > 1)
                return null;

            if (drugId.contains("CID"))
                return idArray.getString(0).substring(drugId.indexOf("CID") - "CID".length() + 1);
            else
                return idArray.getString(0);

        } catch (UnsupportedEncodingException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String queryDrugBankCsvForId(String drugName) {
        try {
            InputStream drugbankVocabulary = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/db/drugbankVocabulary.csv");

            String drugBankCsv = IOUtils.toString(drugbankVocabulary, "UTF-8");
            String[] drugbankCsvArray = drugBankCsv.split("\\n");

            for (int i = 1; i < drugbankCsvArray.length; i++) {
                String currentRow = drugbankCsvArray[i];
                if (currentRow.toLowerCase().contains(drugName.toLowerCase())) {
                    return currentRow.substring(0, currentRow.indexOf(','));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<String> getInteractionType() {
        return new ArrayList<>(interactionTypes);
    }

    public String getMainName() { // TABLE FIELD 1
        return drugName;
    }

    public String getDrugId() {
        return drugId;
    }

    public List<String> getTextMiningNames(QuerySettings querySettings) {
        List<String> outputNames = new ArrayList<>();
        outputNames.add(this.drugName);

        // pyruvic acid -> pyruvate (contribution of ionised acid name)
        String acidSuffix = "ic acid";
        String drugNameSuffix = this.drugName.substring(this.drugName.length() - acidSuffix.length(), this.drugName.length());
        if(drugNameSuffix.equalsIgnoreCase(acidSuffix)) {
            String drugNamePrefix = this.drugName.substring(0, this.drugName.length() - acidSuffix.length());
            outputNames.add(drugNamePrefix + "ate");
        }

        return outputNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Drug that = (Drug) o;

        return drugName != null ? drugName.equals(that.drugName) : that.drugName == null;
    }

    @Override
    public int hashCode() {
        return drugName != null ? drugName.hashCode() : 0;
    }
}
