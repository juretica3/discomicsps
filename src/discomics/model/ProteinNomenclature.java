package discomics.model;

import discomics.application.EnglishDictionary;
import discomics.application.GreekAlphabet;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Created by admin_desktop on 19/06/2017.
 */
public class ProteinNomenclature implements Serializable {
    static final transient long serialVersionUID = 222273256L;
    private static int SHORT_ID_MAX_LENGTH = 4;

    // configuration files
    private static ArrayList<String> clashingSynonymsRules = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/clashing_synonyms.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static ArrayList<String> manuallyAddedProteinNames = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/manually_added_protein_names.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static ArrayList<String> manuallyRemovedProteinNames = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/manually_removed_protein_names.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static ArrayList<String> forceApprovedNameFile = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/force_approved_name.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static List<String> diseaseSynonymNames = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/disease_synonym_names_genes.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private static List<String> diseaseApprovedNames = new ArrayList<String>() {{
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/disease_approved_name_genes.txt");
            addAll(IOUtils.readLines(is));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private List<String> synonymSymbols;
    private List<String> synonymNames;
    private List<String> addedNames;

    private ProteinName proteinName;

    private String approvedProteinName = "";
    private String geneName = "";

    private List<String> illegalNames;

    ProteinNomenclature() {
        this.synonymNames = new ArrayList<>();
        this.synonymSymbols = new ArrayList<>();
        this.addedNames = new ArrayList<>();
        this.proteinName = new ProteinName();
        this.illegalNames = new ArrayList<>();
    }

    ProteinNomenclature(String geneName) {
        this();
        this.geneName = geneName;
    }

    ProteinNomenclature(String[] hgncEntrySplit) {
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
        this.geneName = hgncEntrySplit[4].replaceAll("\"", "");

        String proteinName = hgncEntrySplit[5].replaceAll("\"", "");
        this.approvedProteinName = proteinName;
        this.proteinName = new ProteinName(proteinName, this.geneName);

        try {
            String[] synonymSymbolsSplit = hgncEntrySplit[7].split(", ");
            for (String synonymSymbol : synonymSymbolsSplit) {
                addSynonymSymbol(synonymSymbol.replaceAll("\"", ""));
            }

            String[] synonymNamesSplit = hgncEntrySplit[8].split(", \"");
            for (String s : synonymNamesSplit) {
                this.synonymNames.add(s.replaceAll("\"", ""));
            }

            String[] addedNamesSplit = hgncEntrySplit[9].split(", \"");
            for (String s : addedNamesSplit) {
                this.addedNames.add(s.replaceAll("\"", ""));
            }

            String[] illegalNames = hgncEntrySplit[10].split(", \"");
            for (String s : illegalNames) {
                this.illegalNames.add(s.replaceAll("\"", ""));
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            //empty catch block. when information is lacking in the local database, the array might be shorter than predicted; not a problem at all
        }
    }

    ProteinName getProteinName() {
        return proteinName;
    }

    public String getGeneName() {
        return geneName;
    }

    void addSynonymSymbol(String synonymSymbol) {
        if (synonymSymbol.length() > 3) // synonym symbol has to be longer than three letters; high risk of nonspecific abbreviation matches
            this.synonymSymbols.add(synonymSymbol);
    }

    void addSynonymName(String synonymName) {
        this.synonymNames.add(synonymName);
    }

    void setProteinName(String proteinName) {
        this.approvedProteinName = proteinName;
        this.proteinName = new ProteinName(proteinName, this.geneName);
    }

    void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    void addManualNameRules() {
        String symbolId = "(symbol)";
        String nameId = "(name)";
        // manually add nomenclature
        List<String> allRules = manuallyAddedProteinNames; // list of all rules
        for (int i = 1; i < allRules.size(); i++) { // first line skipped, since instruction line
            String[] ruleSplit = allRules.get(i).split("\\|");

            if (ruleSplit[0].toLowerCase().contains(nameId)) { // if geneId contains nameId by accident, remove it
                ruleSplit[0] = ruleSplit[0].replace(nameId, "").trim();
            }
            if (ruleSplit[0].toLowerCase().contains(symbolId)) { // if geneId contains symbolId by accident, remove it
                ruleSplit[0] = ruleSplit[0].replace(symbolId, "").trim();
            }

            if (this.geneName.equalsIgnoreCase(ruleSplit[0])) { // if gene name is equal to gene name of rule
                for (int j = 1; j < ruleSplit.length; j++) {// loop through all names to be added and add them
                    String nameToBeAdded = ruleSplit[j];
                    if (nameToBeAdded.contains(nameId)) { // if name to be added contains nameId add it to synonymNames
                        this.synonymNames.add(nameToBeAdded.replace(nameId, "").trim());
                    } else if (nameToBeAdded.contains(symbolId)) { // if name to be added contains symbolId add it to synonymSymbols
                        this.synonymSymbols.add(nameToBeAdded.replace(symbolId, "").trim());
                    } else { // if nameId or symbolId tag is not present, assume it is a name
                        this.synonymNames.add(nameToBeAdded.trim());
                    }
                }
            }
        }
    }

    void removeManualNameRules() {
        // manually remove nomenclature
        List<String> allRules = manuallyRemovedProteinNames; // list of all rules
        for (int i = 1; i < allRules.size(); i++) { // first line skipped, since instruction line
            String[] ruleSplit = allRules.get(i).split("\\|");

            if (this.geneName.equalsIgnoreCase(ruleSplit[0])) { // if gene name is equal to gene name of rule
                for (int j = 1; j < ruleSplit.length; j++) {// loop through all names to be added and add them
                    String nameToBeRemoved = ruleSplit[j];
                    this.synonymNames.remove(nameToBeRemoved);
                    this.synonymSymbols.remove(nameToBeRemoved);
                }
            }
        }

        // remove illegal disease names part of synonym names
        for (String diseaseSynonymName : diseaseSynonymNames) {
            String[] ruleSplit = diseaseSynonymName.split("\\|");

            if (this.geneName.equalsIgnoreCase(ruleSplit[0])) {
                this.synonymNames.remove(ruleSplit[1]);
            }
        }
    }

    void removeClashingNames() {
        // first column of file denotes the gene symbol, second column denotes the synonym to be removed
        clashingSynonymsRules.forEach(str -> {
            String[] valuePair = str.split("\t");
            if (this.geneName.equalsIgnoreCase(valuePair[0])) {
                this.synonymNames.remove(valuePair[1]);
                this.synonymSymbols.remove(valuePair[1]);
            }
        });
    }

    void defineIllegalNames() {
        // define illegal nomenclature rules, do not include punctuation
        if (this.geneName.equalsIgnoreCase("SCPEP1")) {
            this.illegalNames.add("serine carboxypeptidase cathepsin");
            this.illegalNames.add("serine carboxypeptidase A");
        }
        if (this.geneName.equalsIgnoreCase("THM")) {
            this.illegalNames.add("trihalomethane");
            this.illegalNames.add("trihalomethanes");
        }
        if (this.geneName.equalsIgnoreCase("BRPF1")) {
            this.illegalNames.add("peregrin falcon");
        }
        if (this.geneName.equalsIgnoreCase("FGF1")) {
            this.illegalNames.add("basic fibroblast growth factor");
            this.illegalNames.add("bFGF");
            this.illegalNames.add("PD-ECGF");
        }
        if (this.geneName.equalsIgnoreCase("FGF2")) {
            this.illegalNames.add("acidic fibroblast growth factor");
            this.illegalNames.add("aFGF");
        }
    }

    void addGreekSymbols() {
        ArrayList<String> greekNames = new ArrayList<>();
        for (String name : this.synonymNames) {
            greekNames.add(GreekAlphabet.replaceWithGreekLetters(name));
        }
        for (String name : this.synonymSymbols) {
            greekNames.add(GreekAlphabet.replaceWithGreekLetters(name));
        }
        for (String name : this.proteinName.getNames()) {
            greekNames.add(GreekAlphabet.replaceWithGreekLetters(name));
        }
        greekNames.add(GreekAlphabet.replaceWithGreekLetters(this.approvedProteinName));
        this.addedNames.addAll(greekNames);
    }

    void addHyphenatedGeneName() {
        for (int i = this.geneName.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(this.geneName.charAt(i))) {
                if (i != this.geneName.length() - 1) {
                    this.synonymSymbols.add(this.geneName.substring(0, i + 1) + "-" + this.geneName.substring(i + 1, this.geneName.length()));
                }
                return;
            }
        }
    }

    void forceApprovedName() {
        List<String> forceApprovedNameRules = forceApprovedNameFile;
        for (String forceRule : forceApprovedNameRules) {
            String[] ruleSplit = forceRule.split("\\|");
            if (ruleSplit[0].equalsIgnoreCase(this.geneName)) {
                this.setProteinName(ruleSplit[1]);
                return;
            }
        }
    }

    void addUnneccessaryPartsRemovedSynonyms() {
        ArrayList<String> undesiredParts = ProteinName.getUndesiredProteinNameParts();
        Set<String> output = new HashSet<>();

        for (String name : synonymNames) {
            String sModified = name;
            for (int i = 1; i < undesiredParts.size(); i++) { // start from second entry, since first entry are instructions and notes
                sModified = sModified.replaceAll(undesiredParts.get(i), "");
            }
            sModified = sModified.replaceAll("\\s+", " "); // replace any spaces with single
            for (int i = 0; i < sModified.length(); i++) { // trim leading spaces
                if (sModified.charAt(i) != ' ') {
                    sModified = sModified.substring(i, sModified.length());
                    break;
                }
            }
            if (!sModified.equalsIgnoreCase(name))
                output.add(sModified);
        }
        this.synonymNames.addAll(output);
    }

    boolean isSuccessBuildingNomenclature() {
        return !getNamesOnlineQuery().contains("entry withdrawn") && !approvedProteinName.isEmpty()
                && !geneName.isEmpty();
    }

    String getApprovedProteinName() {
        return approvedProteinName;
    }

    List<String> getIllegalNames() {
        return new ArrayList<>(illegalNames);
    }

    public List<String> getNamesOnlineQuery() {
        ProteinNameSet output = new ProteinNameSet(this.proteinName.getNames());
        output.addAll(this.synonymNames);
        output.addAll(this.addedNames);
        output.addAll(this.synonymSymbols);
        output.add(this.geneName);

        if (this.proteinName.isStructureRootAndOne())
            output.add(this.proteinName.getRoot());

        output.forEach(s -> s = s.toLowerCase());
        return new ArrayList<>(output);
    }

    List<String> getLongIdentifiersFiltering() {
        List<String> allNames = this.getNamesOnlineQuery(); // get all names
        List<String> longNames = new ArrayList<>();
        for (String name : allNames) { // filter only names longer than 3 characters
            if (name.length() > SHORT_ID_MAX_LENGTH)
                longNames.add(name);
        }
        List<String> longNamesNoEnglishNoDisease = new ArrayList<>();
        for (String longName : longNames) { // remove all english terms
            if (!EnglishDictionary.isEnglish(longName))
                longNamesNoEnglishNoDisease.add(longName);
        }
        String diseaseName = this.getDiseaseApprovedName(); // remove approved name if this is disease name
        if (diseaseName != null) {
            longNamesNoEnglishNoDisease.remove(diseaseName);
        }
        return longNamesNoEnglishNoDisease;
    }

    List<String> getShortIdentifiersFiltering() {
        List<String> allNames = this.getNamesOnlineQuery();
        List<String> shortNames = new ArrayList<>();
        for (String name : allNames) {
            if (name.length() <= SHORT_ID_MAX_LENGTH)
                shortNames.add(name);
        }
        return shortNames;
    }

    List<String> getDictionaryClashingTerms() {
        Set<String> allNames = new HashSet<>(this.proteinName.getNames());
        allNames.add(this.geneName);
        allNames.addAll(this.synonymNames);
        allNames.addAll(this.synonymSymbols);

        List<String> clashingTerms = new ArrayList<>();
        allNames.forEach(s -> {
            if (EnglishDictionary.isEnglish(s)) {
                clashingTerms.add(s);
            }
        });
        return clashingTerms;
    }

    String getDiseaseApprovedName() {
        for (String diseaseApprovedNameGene : diseaseApprovedNames) {
            String[] splitEntry = diseaseApprovedNameGene.split("\\|");
            if (this.geneName.equalsIgnoreCase(splitEntry[0])) {
                return splitEntry[1];
            }
        }
        return null;
    }

    class ProteinNameSet extends HashSet<String> implements Serializable {
        static final transient long serialVersionUID = 23423413872L;

        ProteinNameSet(Collection<? extends String> c) {
            this.addAll(c);
        }

        ProteinNameSet() {
            super();
        }

        @Override
        public boolean add(String s) {
            if (s == null)
                return false;

            s = s.toLowerCase();
//            if (EnglishDictionary.isEnglish(s)) // if it is an english word don't include, exceptions implemented!!!
//                return false;

            if (s.length() <= 2 || s.length() > 55)
                return false;

            s = s.trim();
            s = s.replaceAll("  ", " ");

            return super.add(s);
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            boolean changed = false;
            for (String s : c)
                if (this.add(s))
                    changed = true;
            return changed;
        }
    }
}
