package discomics.model;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by Jure on 4.9.2016.
 */
public class IoModel implements Serializable {

    private ProteinCollection proteinCollection;

    private List<TextMinedProtein> textMinedProteins;
    private List<TextMinedProtein> textMinedProteinsDeepSearch;
    private List<TextMinedGeneFamily> textMinedGeneFamilies;
    private List<TextMinedDrug> textMinedDrugs;

    private List<CustomInputTermBlock> customSearchTerms;

    private ProteinInteractionNetwork proteolysisFullPpi;
    private ProteinInteractionNetwork proteolysisStringentPpi;
    private ProteinInteractionNetwork biomarkerFullPpi;
    private ProteinInteractionNetwork biomarkerBloodPpi;
    private ProteinInteractionNetwork biomarkerUrinePpi;
    private ProteinInteractionNetwork biomarkerSalivaPpi;
    private ProteinInteractionNetwork customFullPpi;

    private transient ProteinDrugInteractionNetwork proteinDrugInteractionNetwork;
    private transient ProteinDrugInteractionNetwork proteinDrugProteolysisStringentNetwork;
    private transient ProteinDrugInteractionNetwork proteinDrugCustomNetwork;

    private QuerySettings querySettings;

    public IoModel() { // no argument constructor important for serialisation
        this.proteinCollection = new ProteinCollection();
        this.textMinedProteins = new ArrayList<>();
        this.textMinedGeneFamilies = new ArrayList<>();
        this.textMinedProteinsDeepSearch = new ArrayList<>();
        this.textMinedDrugs = new ArrayList<>();
        this.customSearchTerms = new ArrayList<>();

        initialiseProteaseFamilies();
        initialiseModelBiomarkers();

        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/proteaseData.obj");
            ois = new ObjectInputStream(is);

            IoModel.proteasesMMP = (ArrayList<Protein>) ois.readObject();
            IoModel.proteasesADAM = (ArrayList<Protein>) ois.readObject();
            IoModel.proteasesADAMTS = (ArrayList<Protein>) ois.readObject();
            IoModel.proteasesCTS = (ArrayList<Protein>) ois.readObject();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

//
//        try {
//            initialiseProteases();
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//        File file = new File("resources/discomics/proteaseData.obj");
//        OutputStream outputStream = null;
//        ObjectOutputStream oos = null;
//        try {
//            outputStream = new FileOutputStream(file);
//            oos = new ObjectOutputStream(outputStream);
//            oos.writeObject(proteasesMMP);
//            oos.writeObject(proteasesADAM);
//            oos.writeObject(proteasesADAMTS);
//            oos.writeObject(proteasesCTS);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (outputStream != null) {
//                try {
//                    outputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (oos != null) {
//                try {
//                    oos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    private static Biomarker URINE;
    private static Biomarker SALIVA;
    private static Biomarker BLOOD;
    private static Biomarker CUSTOM;

    private void initialiseModelBiomarkers() {
        ArrayList<String> urineSearchTerms = new ArrayList<>();
        urineSearchTerms.add("urine");
        URINE = new Biomarker("urine", urineSearchTerms);

        ArrayList<String> salivaSearchTerms = new ArrayList<>();
        salivaSearchTerms.add("saliva");
        SALIVA = new Biomarker("plasma", salivaSearchTerms);

        ArrayList<String> bloodSearchTerms = new ArrayList<>();
        bloodSearchTerms.add("blood");
        bloodSearchTerms.add("serum");
        bloodSearchTerms.add("plasma");
        BLOOD = new Biomarker("blood", bloodSearchTerms);

        CUSTOM = new Biomarker("custom");
    }

    private static ProteaseFamily MMP;
    private static ProteaseFamily ADAM;
    private static ProteaseFamily CTS;
    private static ProteaseFamily ADAMTS;

    private static ProteaseFamily proteaseFamilySelected;

    private static ArrayList<Protein> proteasesMMP;
    private static ArrayList<Protein> proteasesADAMTS;
    private static ArrayList<Protein> proteasesADAM;
    private static ArrayList<Protein> proteasesCTS;


    private static final List<String> searchVerbs = new ArrayList<String>() {{
        add("degrad%2A");
        add("cleav%2A");
        add("proteoly%2A");
        add("breakdown");
        add("hydroly%2A");
        add("catabolis%2A");
        add("cataboliz%2A");
        add("fragment%2A");
        add("peptid%2A");
        add("substrat%2A");
    }};

    private final List<String> searchVerbsNoWildcard = new ArrayList<String>() {{
        add("degradation");
        add("degrade");
        add("degrades");
        add("degraded");
        add("degrading");
        add("cleavage");
        add("cleaves");
        add("cleaved");
        add("cleaving");
        add("proteolysis");
        add("proteolysed");
        add("proteolyzed");
        add("proteolysing");
        add("proteolyzing");
        add("breakdown");
        add("breaks down");
        add("broken down");
        add("breaking down");
        add("hydrolysis");
        add("hydrolysed");
        add("hydrolyzed");
        add("hydrolysing");
        add("hydrolyzing");
        add("catabolised");
        add("catabolized");
        add("catabolism");
        add("catabolisis");
        add("catabolizing");
        add("catabolising");
        add("fragment");
        add("fragments");
        add("fragmentation");
        add("fragmenting");
        add("fragmented");
        add("peptid");
        add("substrat");
    }};

    private static final List<String> proteolysisBadVerbs = new ArrayList<String>() {{
        add("dna fragment");
        add("nucleic acid fragment");
        add("rna fragment");

        add("peptic fragment");
        add("fragmentation by pepsin");
        add("fragmented by pepsin");
//        add("digestion by pepsin");
//        add("digested by pepsin");

        add("tryptic fragment");
        add("fragmentation by trypsin");
        add("fragmentated by trypsin");
//        add("digestion by trypsin");
//        add("digested by trypsin");
    }};

    private void initialiseProteaseFamilies() {

        ArrayList<String> namesMMP = new ArrayList<>();
        namesMMP.add("MMP%2A");
        namesMMP.add("matrix metalloprotease");
        namesMMP.add("matrix metallopeptidase");
        namesMMP.add("matrix metalloproteinase");
        namesMMP.add("collagenase"); // MMP1,8,13
        namesMMP.add("gelatinase"); // MMP2,9
        namesMMP.add("stromelysin"); // MMP3
        ArrayList<String> badSynonymsMMP = new ArrayList<>();
        badSynonymsMMP.add("mitochondrial membrane potential");

        String standardAbbreviationMMP = "MMP";

        ArrayList<String> highlightTermsMMP = new ArrayList<>();
        highlightTermsMMP.add(standardAbbreviationMMP);
        highlightTermsMMP.addAll(namesMMP.stream().filter(name -> !name.contains("%2A")).collect(Collectors.toList()));

        MMP = new ProteaseFamily(standardAbbreviationMMP, namesMMP, badSynonymsMMP, highlightTermsMMP);

        ArrayList<String> namesADAM = new ArrayList<>();
        namesADAM.add("ADAM%2A");
        namesADAM.add("disintegrin and metalloproteinase");
        namesADAM.add("disintegrin and metalloprotease");
        namesADAM.add("disintegrin and metallopeptidase");
        namesADAM.add("aggrecanase"); // ADAMTS1,4,5
        ArrayList<String> badSynonymADAM = new ArrayList<>();

        String standardAbbreviationADAM = "ADAM";

        ArrayList<String> highlightTermsADAM = new ArrayList<>();
        highlightTermsADAM.add(standardAbbreviationADAM);
        highlightTermsADAM.add("ADAMTS");
        highlightTermsADAM.addAll(namesADAM.stream().filter(name -> !name.contains("%2A")).collect(Collectors.toList()));

        ADAM = new ProteaseFamily(standardAbbreviationADAM, namesADAM, badSynonymADAM, highlightTermsADAM);

        ArrayList<String> namesADAMTS = new ArrayList<>(); // ADAM used to query PMC
        ArrayList<String> badSynonymADAMTS = new ArrayList<>(); // namesADAMTS and badSynonymADAMTS not used
        ArrayList<String> highlightTermsADAMTS = new ArrayList<>(); // not used
        String standardAbbreviationADAMTS = "ADAMTS";

        ADAMTS = new ProteaseFamily(standardAbbreviationADAMTS, namesADAMTS, badSynonymADAMTS, highlightTermsADAMTS);

        ArrayList<String> namesCTS = new ArrayList<>();
        namesCTS.add("CTS%2A");
        namesCTS.add("cathepsin%2A");
        ArrayList<String> badSynonymsCTS = new ArrayList<>();
        badSynonymsCTS.add("carpal tunnel syndrome");

        String standardAbbreviationCTS = "CTS";

        ArrayList<String> highlightTermsCTS = new ArrayList<>();
        highlightTermsCTS.add(standardAbbreviationCTS);
        highlightTermsCTS.add("cathepsin");

        CTS = new ProteaseFamily(standardAbbreviationCTS, namesCTS, badSynonymsCTS, highlightTermsCTS);
    }

    private void initialiseProteases() throws SocketException {
        JSONArray adamtsJsonArray = readProteaseGeneData("discomics/adamts_subtypes_json.txt");
        proteasesADAMTS = new ArrayList<>();

        for (int i = 0; i < adamtsJsonArray.length(); i++) {
            proteasesADAMTS.add(new Protein(adamtsJsonArray.getJSONObject(i)));
        }

        JSONArray adamJsonArray = readProteaseGeneData("discomics/adam_subtypes_json.txt");
        proteasesADAM = new ArrayList<>();

        for (int i = 0; i < adamJsonArray.length(); i++) {
            proteasesADAM.add(new Protein(adamJsonArray.getJSONObject(i)));
        }

        JSONArray mmpJsonArray = readProteaseGeneData("discomics/mmp_subtypes_json.txt");
        proteasesMMP = new ArrayList<>();

        for (int i = 0; i < mmpJsonArray.length(); i++) {
            proteasesMMP.add(new Protein(mmpJsonArray.getJSONObject(i)));
        }

        JSONArray ctsJsonArray = readProteaseGeneData("discomics/cts_subtypes_json.txt");
        proteasesCTS = new ArrayList<>();

        for (int i = 0; i < ctsJsonArray.length(); i++) {
            proteasesCTS.add(new Protein(ctsJsonArray.getJSONObject(i)));
        }
    }

    private static JSONArray readProteaseGeneData(String fileName) {

        //Get file from resources folder
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(fileName);

        StringWriter writer = new StringWriter();
        String inputJSON;

        try {
            IOUtils.copy(is, writer, "UTF-8");
            inputJSON = writer.toString();
        } catch (IOException e) {
            inputJSON = "";
            e.printStackTrace();
        }

        return new JSONArray(inputJSON);
    }

    public void constructAllPpis() throws SocketException {
        ExecutorService executorService = Executors.newCachedThreadPool();

        Future<ProteinInteractionNetwork> future0 = null; // proteolysis networks
        Future<ProteinInteractionNetwork> future1 = null;

        Future<ProteinInteractionNetwork> future2 = null; // biomarker networks
        Future<ProteinInteractionNetwork> future3 = null;
        Future<ProteinInteractionNetwork> future4 = null;
        Future<ProteinInteractionNetwork> future5 = null;

        Future<ProteinInteractionNetwork> future6 = null; // custom network

        Future<ProteinDrugInteractionNetwork> future7 = null; // drug interaction network
        Future<ProteinDrugInteractionNetwork> future8 = null;
        Future<ProteinDrugInteractionNetwork> future9 = null;

        if (this.querySettings.isProteaseSearch()) {
            Callable<ProteinInteractionNetwork> task0 = this::constructProteolysisFullPpi;
            future0 = executorService.submit(task0);

            Callable<ProteinInteractionNetwork> task1 = this::constructProteolysisStringentPpi;
            future1 = executorService.submit(task1);

            Callable<ProteinDrugInteractionNetwork> task9 = this::constructStringentProteolysisDrugNetwork;
            future9 = executorService.submit(task9);
        }
        if (this.querySettings.isBiomarkerSearch()) {
            Callable<ProteinInteractionNetwork> task2 = this::constructBiomarkerFullPpi;
            future2 = executorService.submit(task2);

            Callable<ProteinInteractionNetwork> task3 = () -> constructBiomarkerTissuePpi(IoModel.getBLOOD());
            future3 = executorService.submit(task3);

            Callable<ProteinInteractionNetwork> task4 = () -> constructBiomarkerTissuePpi(IoModel.getSALIVA());
            future4 = executorService.submit(task4);

            Callable<ProteinInteractionNetwork> task5 = () -> constructBiomarkerTissuePpi(IoModel.getURINE());
            future5 = executorService.submit(task5);
        }
        if (this.querySettings.isCustomSearch()) {
            Callable<ProteinInteractionNetwork> task6 = this::constructCustomFullPpi;
            future6 = executorService.submit(task6);
            Callable<ProteinDrugInteractionNetwork> task8 = this::constructCustomDrugNetwork;
            future8 = executorService.submit(task8);
        }

        Callable<ProteinDrugInteractionNetwork> task7 = this::constructDrugInteractionNetwork;
        future7 = executorService.submit(task7);

        try {
            if (future0 != null)
                this.proteolysisFullPpi = future0.get();
            if (future1 != null)
                this.proteolysisStringentPpi = future1.get();
            if (future2 != null)
                this.biomarkerFullPpi = future2.get();
            if (future3 != null)
                this.biomarkerBloodPpi = future3.get();
            if (future4 != null)
                this.biomarkerSalivaPpi = future4.get();
            if (future5 != null)
                this.biomarkerUrinePpi = future5.get();
            if (future6 != null)
                this.customFullPpi = future6.get();
            if (future7 != null)
                this.proteinDrugInteractionNetwork = future7.get();
            if (future8 != null)
                this.proteinDrugCustomNetwork = future8.get();
            if (future9 != null)
                this.proteinDrugProteolysisStringentNetwork = future9.get();
        } catch (ExecutionException e) {
            if (e.getLocalizedMessage().equals("java.net.SocketException"))
                throw new SocketException();
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }

    private ProteinInteractionNetwork constructProteolysisFullPpi() throws SocketException {
        Set<Protein> combinedList = new HashSet<>();

        for (TextMinedProtein tmProt : this.textMinedProteins) {
            List<Protein> proteasesMentioned = new ArrayList<>();
            // add proteases to list
            for (Article art : tmProt.getArticleCollectablePlys().getArticleCollection()) {
                proteasesMentioned.addAll(art.getProteasesMentioned());
            }
            combinedList.addAll(proteasesMentioned);
            // add proteins where proteases were detected to list, and where string ID is defined
            if (tmProt.getTextMinableInput().isSuccessBuildingSTRING() && !proteasesMentioned.isEmpty())
                combinedList.add(tmProt.getTextMinableInput());
        }
        ProteinInteractionNetwork network = new ProteinInteractionNetwork(new ArrayList<>(combinedList));
        network.build();
        return network;
    }

    private ProteinInteractionNetwork constructProteolysisStringentPpi() throws SocketException {

        Set<String> geneNames = new HashSet<>();
        for (ArticleCollectablePlys articleCollectablePlys : getArticleCollProteolysisList()) {
            geneNames.addAll(articleCollectablePlys.getPhysicalInteractions().stream().map(NetworkEdge::getNode1).collect(Collectors.toList()));
            geneNames.addAll(articleCollectablePlys.getPhysicalInteractions().stream().map(NetworkEdge::getNode2).collect(Collectors.toList()));
        }
        List<Protein> proteinList = getProteinsFromGeneNames(geneNames);
        ProteinInteractionNetwork network = new ProteinInteractionNetwork(proteinList);
        network.build();
        return network;
    }

    private ProteinInteractionNetwork constructBiomarkerFullPpi() throws SocketException {
        List<Protein> ppiListWithHits = textMinedProteins.stream()
                .filter(textMinedProtein -> textMinedProtein.getArticleCollectableBiom().getTotalHitCount() > 0)
                .map(TextMinedProtein::getTextMinableInput)
                .collect(Collectors.toList());

//        List<Protein> ppiListWithHits = getArticleCollBiomarkerList().stream()
//                .filter(articleCollBiomarker -> articleCollBiomarker.getNrRetrieved() > 0)
//                .map(ArticleCollectableBiom::getProtein)
//                .collect(Collectors.toList());

        ProteinInteractionNetwork network = new ProteinInteractionNetwork(ppiListWithHits);
        network.build();
        return network;
    }

    private ProteinInteractionNetwork constructBiomarkerTissuePpi(Biomarker biomarker) throws SocketException {
        List<Protein> proteinListPpi = new ArrayList<>();
        for (TextMinedProtein textMinedProtein: this.textMinedProteins) {
            List<Biomarker> biomarkersMentioned = new ArrayList<>();
            for (Article article : textMinedProtein.getArticleCollectableBiom().getArticleCollection()) {
                biomarkersMentioned.addAll(article.getBiomarkersMentioned());
            }
            if (biomarkersMentioned.contains(biomarker))
                proteinListPpi.add(textMinedProtein.getTextMinableInput());
        }
        ProteinInteractionNetwork network = new ProteinInteractionNetwork(proteinListPpi);
        network.build();
        return network;
    }

    private ProteinInteractionNetwork constructCustomFullPpi() throws SocketException {
        List<Protein> ppiListWithHits = textMinedProteins.stream()
                .filter(textMinedProtein -> textMinedProtein.getArticleCollectableCust().getTotalHitCount() > 0)
                .map(TextMinedProtein::getTextMinableInput)
                .collect(Collectors.toList());

        ProteinInteractionNetwork network = new ProteinInteractionNetwork(ppiListWithHits);
        network.build();
        return network;
    }

    private ProteinDrugInteractionNetwork constructDrugInteractionNetwork() throws SocketException {
        List<Protein> proteinsWithDrugs = new ArrayList<>();
        for (Protein protein : proteinCollection.getOutputProteinList()) {
            if (protein.getDrugs().size() > 0)
                proteinsWithDrugs.add(protein);
        }
        ProteinDrugInteractionNetwork network = new ProteinDrugInteractionNetwork(proteinsWithDrugs);
        network.build();
        return network;
    }

    private ProteinDrugInteractionNetwork constructStringentProteolysisDrugNetwork() throws SocketException {
        List<Protein> proteinsWithDrugs = new ArrayList<>();
        List<NetworkEdge> allProteaseInteractions = new ArrayList<>();
        for (TextMinedProtein tmProt : textMinedProteins) {
            List<NetworkEdge> proteaseInteractions = tmProt.getArticleCollectablePlys().getPhysicalInteractions();
            if (proteaseInteractions.size() > 0) {
                proteinsWithDrugs.add(tmProt.getTextMinableInput());
                allProteaseInteractions.addAll(proteaseInteractions);
            }
        }

        List<Protein> proteases = new ArrayList<>();
        if (proteaseFamilySelected.equals(IoModel.getMMP()))
            proteases.addAll(IoModel.getProteasesMMP());

        if (proteaseFamilySelected.equals(IoModel.getADAMTS())) {
            proteases.addAll(IoModel.getProteasesADAMTS());
            proteases.addAll(IoModel.getProteasesADAM());
        }
        if (proteaseFamilySelected.equals(IoModel.getCTS()))
            proteases.addAll(IoModel.getProteasesCTS());

        List<Protein> presentPhysicalInteractions = new ArrayList<>();
        for (NetworkEdge proteaseIneteraction : allProteaseInteractions) {
            for (Protein protease: proteases) {
                if (protease.getMainName().equalsIgnoreCase(proteaseIneteraction.getNode1())) {
                    presentPhysicalInteractions.add(protease);
                    break;
                }
                if (protease.getMainName().equalsIgnoreCase(proteaseIneteraction.getNode2())) {
                    presentPhysicalInteractions.add(protease);
                    break;
                }
            }
        }
        proteinsWithDrugs.addAll(presentPhysicalInteractions);
        ProteinDrugInteractionNetwork network = new ProteinDrugInteractionNetwork(proteinsWithDrugs);
        network.build();
        return network;
    }

    private ProteinDrugInteractionNetwork constructCustomDrugNetwork() throws SocketException {
        List<Protein> proteinsWithDrugs = new ArrayList<>();
        for (TextMinedProtein tmProt : textMinedProteins) {
            if (tmProt.getArticleCollectableCust().getTotalHitCount() > 0) {
                proteinsWithDrugs.add(tmProt.getTextMinableInput());
            }
        }
        ProteinDrugInteractionNetwork network = new ProteinDrugInteractionNetwork(proteinsWithDrugs);
        network.build();
        return network;
    }

    public List<ProteaseCount> getProteaseCountTableEntries(List<TextMinedObject> selectedProteins) { // if input is null, all proteins taken in consideration

        HashSet<Article> allArticles = new HashSet<>();
        HashSet<Article> allUncompressedArticles = new HashSet<>();
        HashSet<Protein> allProteases = new HashSet<>();

        //if (selectedProteins != null) { // proteins were selected, so construct only for selected proteins
            for (TextMinedObject tmObject : selectedProteins) {
                allArticles.addAll(tmObject.getArticleCollectablePlys().getAllClassifiedArticles());
                allUncompressedArticles.addAll(tmObject.getArticleCollectablePlys().getClassifiedArticlesUncompressed());
            }
//        } else { // if null parameter passed, do for all proteins
//            for (TextMinedProtein textMinedProtein : this.textMinedProteins) {
//                allArticles.addAll(textMinedProtein.getArticleCollectablePlys().getAllClassifiedArticles());
//                allUncompressedArticles.addAll(textMinedProtein.getArticleCollectablePlys().getClassifiedArticlesUncompressed());
//            }
//        }

        // collect all mentioned proteases into list
        for (Article article : allArticles) {
            allProteases.addAll(article.getProteasesMentioned());
        }

        // construct protease count objects
        ArrayList<ProteaseCount> proteaseCounts = new ArrayList<>();
        int totalHits;
        int nrRetrieved;
        for (Protein protease : allProteases) {
            totalHits = 0;
            nrRetrieved = 0;

            // all articles (compressed and uncompressed) contribute to total hit count
            for (Article article : allArticles) {
                if (article.getProteasesMentioned().contains(protease))
                    totalHits++;
            }

            // only uncompressed articles contribute to nr retrieved count
            for (Article article : allUncompressedArticles) {
                if (article.getProteasesMentioned().contains(protease))
                    nrRetrieved++;
            }
            proteaseCounts.add(new ProteaseCount(protease, totalHits, nrRetrieved));
        }
        return proteaseCounts;
    }

    private List<Protein> getProteinsFromGeneNames(Set<String> geneNames) {

        Set<Protein> proteinList = new HashSet<>();
        geneNames.forEach(s -> {
            for (Protein protein : proteinCollection.getOutputProteinList()) {
                if (s.equalsIgnoreCase(protein.getMainName())) {
                    proteinList.add(protein);
                    return;
                }
            }
            for (Protein protease : getProteasesMMP()) {
                if (s.equalsIgnoreCase(protease.getMainName())) {
                    proteinList.add(protease);
                    return;
                }
            }
            for (Protein protease : getProteasesADAM()) {
                if (s.equalsIgnoreCase(protease.getMainName())) {
                    proteinList.add(protease);
                    return;
                }
            }
            for (Protein protease : getProteasesADAMTS()) {
                if (s.equalsIgnoreCase(protease.getMainName())) {
                    proteinList.add(protease);
                    return;
                }
            }
            for (Protein protease : getProteasesCTS()) {
                if (s.equalsIgnoreCase(protease.getMainName())) {
                    proteinList.add(protease);
                    return;
                }
            }
        });
        if (geneNames.size() != proteinList.size()) {
            System.err.print("Warning: logical test after protein list to gene name conversion has failed.\n");
        }
        return new ArrayList<>(proteinList);
    }

    public void addTextMinedProtein(TextMinedProtein tmProt) {
        this.textMinedProteins.add(tmProt);
    }

    public void addTextMinedGeneFamily(TextMinedGeneFamily tmGeneFamily) {this.textMinedGeneFamilies.add(tmGeneFamily);}

    public void addTextMinedProteinDeepSearch(TextMinedProtein tmProtAdd) {
        List<TextMinedProtein> temporaryList = new ArrayList<>(textMinedProteinsDeepSearch);

        for(TextMinedProtein tmProt: temporaryList) { // remove text mined protein if already present
            if(tmProt.getTextMinableInput().getMainName().equalsIgnoreCase(tmProtAdd.getMainName())) {
                this.textMinedProteinsDeepSearch.remove(tmProt);
            }
        }
        this.textMinedProteinsDeepSearch.add(tmProtAdd); // add freshly searched text mined protein
    }

    public void addTextMinedDrug(TextMinedDrug textMinedDrug) {
        List<TextMinedDrug> temporaryList = new ArrayList<>(textMinedDrugs);

        for(TextMinedDrug tmDrug: temporaryList) { // remove text mined drug if already present
            if(tmDrug.getTextMinableInput().equals(textMinedDrug.getTextMinableInput())) {
                this.textMinedDrugs.remove(tmDrug);
            }
        }
        this.textMinedDrugs.add(textMinedDrug); // add freshly searched text mined drug
    }

    //public void clearTextMinedProteinDeepSearchList() {
    //    this.textMinedProteinsDeepSearch.clear();
    //}

    public void setProteaseFamilySelected(ProteaseFamily proteaseFamilySelected) {
        IoModel.proteaseFamilySelected = proteaseFamilySelected;
    }

    public static ProteaseFamily getMMP() {
        return MMP;
    }

    public static ProteaseFamily getADAM() {
        return ADAM;
    }

    public static ProteaseFamily getCTS() {
        return CTS;
    }

    public static ProteaseFamily getADAMTS() {
        return ADAMTS;
    }

    public static ArrayList<Protein> getProteasesMMP() {
        return proteasesMMP;
    }

    public static ArrayList<Protein> getProteasesADAMTS() {
        return proteasesADAMTS;
    }

    public static ArrayList<Protein> getProteasesADAM() {
        return proteasesADAM;
    }

    public static ArrayList<Protein> getProteasesCTS() {
        return proteasesCTS;
    }

    public static Biomarker getURINE() {
        return URINE;
    }

    public static Biomarker getSALIVA() {
        return SALIVA;
    }

    public static Biomarker getBLOOD() {
        return BLOOD;
    }

    public static Biomarker getCUSTOM() {
        return CUSTOM;
    }

    public static List<String> getSearchVerbs() {
        return searchVerbs;
    }

    public List<ArticleCollectablePlys> getArticleCollProteolysisList() {
        return this.textMinedProteins.stream()
                .map(TextMinedProtein::getArticleCollectablePlys)
                .collect(Collectors.toList());
    }

    public List<ArticleCollectableBiom> getArticleCollBiomarkerList() {
        return this.textMinedProteins.stream()
                .map(TextMinedProtein::getArticleCollectableBiom)
                .collect(Collectors.toList());
    }

    public List<ArticleCollectableCust> getArticleCollCustomList() {
        return this.textMinedProteins.stream()
                .map(TextMinedProtein::getArticleCollectableCust)
                .collect(Collectors.toList());
    }

    public List<String> getSearchVerbsNoWildcard() {
        return searchVerbsNoWildcard;
    }

    public static List<String> getProteolysisBadVerbs() {
        return proteolysisBadVerbs;
    }

    public static List<String> getSearchVerbRoots() {
        List<String> verbs = new ArrayList<>();

        for (String verb : searchVerbs) {
            verb = verb.replace("%2A", "");
            verbs.add(verb);
        }
        return verbs;
    }

    public static ProteaseFamily getProteaseFamilySelected() {
        return proteaseFamilySelected;
    }

    public ProteinInteractionNetwork getProteolysisFullPpi() {
        return proteolysisFullPpi;
    }

    public ProteinInteractionNetwork getProteolysisStringentPpi() {
        return proteolysisStringentPpi;
    }

    public ProteinInteractionNetwork getBiomarkerFullPpi() {
        return biomarkerFullPpi;
    }

    public ProteinInteractionNetwork getBiomarkerBloodPpi() {
        return biomarkerBloodPpi;
    }

    public ProteinInteractionNetwork getBiomarkerUrinePpi() {
        return biomarkerUrinePpi;
    }

    public ProteinInteractionNetwork getBiomarkerSalivaPpi() {
        return biomarkerSalivaPpi;
    }

    public ProteinInteractionNetwork getCustomFullPpi() {
        return customFullPpi;
    }

    public ProteinDrugInteractionNetwork getProteinDrugInteractionNetwork() {
        return proteinDrugInteractionNetwork;
    }

    public ProteinDrugInteractionNetwork getProteinDrugProteolysisStringentNetwork() {
        return proteinDrugProteolysisStringentNetwork;
    }

    public ProteinDrugInteractionNetwork getProteinDrugCustomNetwork() {
        return proteinDrugCustomNetwork;
    }

    public ProteinCollection getProteinCollection() {
        return proteinCollection;
    }

    public void setProteinCollection(ProteinCollection proteinCollection) {
        this.proteinCollection = proteinCollection;
    }

    public List<TextMinedObject> getTextMinedProteins() {
        return new ArrayList<>(textMinedProteins);
    }

    public List<TextMinedObject> getTextMinedGeneFamilies() {
        return new ArrayList<>(textMinedGeneFamilies);
    }

    public List<TextMinedObject> getTextMinedProteinsDeepSearch() {
        return new ArrayList<>(textMinedProteinsDeepSearch);
    }

    public List<TextMinedObject> getTextMinedDrugs() {
        return new ArrayList<>(textMinedDrugs);
    }

    public QuerySettings getQuerySettings() {
        return querySettings;
    }

    public void setQuerySettings(QuerySettings querySettings) {
        this.querySettings = querySettings;
    }

    public void setCustomSearchTerms(List<CustomInputTermBlock> customSearchTerms) {
        this.customSearchTerms = customSearchTerms;
    }

    public List<CustomInputTermBlock> getCustomSearchTerms() {
        return customSearchTerms;
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (this.querySettings.isProteaseSearch()) { // define static field proteaseFamilySelected in IoModel after loading object
            ArticleCollectablePlys artColl = this.textMinedProteins.get(0).getArticleCollectablePlys();
            IoModel.proteaseFamilySelected = artColl.getProteaseFamily();
        }
    }

}
