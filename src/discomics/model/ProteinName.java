package discomics.model;

import discomics.application.GreekAlphabet;
import discomics.application.RomanDecoder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by admin_desktop on 29/05/2017.
 */
class ProteinName implements Serializable {
    static final transient long serialVersionUID = 43312516L;

    // configuration files
    private static ArrayList<String> undesiredProteinNameParts = new ArrayList<String>() {{
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/undesired_protein_name_parts.txt");
        try {
            addAll(IOUtils.readLines(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }};

    private List<NamePart> nameParts1;
    private List<NamePart> nameParts2;

    private HashSet<String> allNames;

    private boolean isStructureRootAndOne = false;

    ProteinName() {
        this.nameParts1 = new ArrayList<>();
        this.nameParts2 = new ArrayList<>();
        this.allNames = new HashSet<>();
    }

    ProteinName(String name, String geneName) {
        this();

        String modifiedName = removeBracketContent(name);
        String modifiedName1 = removeUnnecessaryNameParts(modifiedName);

        allNames.add(modifiedName);
        allNames.add(modifiedName1);
        allNames.addAll(constructCollagenNames(name, geneName));

        this.nameParts1 = decomposeName(modifiedName);
        this.nameParts2 = decomposeName(modifiedName1);

        allNames.addAll(createRoSuSuPermutations(this.nameParts1));
        allNames.addAll(createRoSuSuPermutations(this.nameParts2));

        allNames.addAll(createRoGreekPermutations(this.nameParts1));
        allNames.addAll(createRoGreekPermutations(this.nameParts2));

        this.isStructureRootAndOne = this.isStructureRootAndOne(this.nameParts1);
//        if ()
//            allNames.add(this.nameParts1.get(0).root);

        // todo revise
        if(!this.isStructureRootAndOne) {
            if (this.isStructureRootAndOne = this.isStructureRootAndGreekSubtypeAndOne(this.nameParts1)) {
                allNames.add(this.nameParts1.get(0).root + " " + this.nameParts1.get(1).subtype);

                List<NamePart> namePartsOneRemoved = new ArrayList<>();
                namePartsOneRemoved.add(new NamePart(this.nameParts1.get(0).root, null));
                namePartsOneRemoved.add(new NamePart(null, this.nameParts1.get(1).subtype));
                allNames.addAll(createRoSuPermutations(namePartsOneRemoved));
            }
        }
        addRomanArabicSubtypeConversions(this.allNames);
        addGreekSubtypeConversions(this.allNames);
    }

    /**
     * Decomposes modifiedName into ROOT + SUBTYPE_ID + SUBTYPE structure
     */
    private List<NamePart> decomposeName(String name) {
        String[] splitName = name.split("\\s+");
        List<NamePart> nameParts = new ArrayList<>();

        String nameRoot = "";

        for (int i = 0; i < splitName.length; i++) {

            String namePart = splitName[i];

            String nextNamePart;
            if (i < splitName.length - 1)
                nextNamePart = splitName[i + 1];
            else
                nextNamePart = "";

            String twoForwardNamePart;
            if (i < splitName.length - 2)
                twoForwardNamePart = splitName[i + 2];
            else
                twoForwardNamePart = "";

            // check if current word is a subtype identifier
            if (namePart.equalsIgnoreCase("pseudogene") && isNamePartSubtype(nextNamePart)) { // pseudogene
                addRootSubtypeToList("pseudogene", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (namePart.equalsIgnoreCase("family")) { // family & family member
                if (nextNamePart.equalsIgnoreCase("member") && isNamePartSubtype(twoForwardNamePart)) {
                    addRootSubtypeToList("family member", twoForwardNamePart, nameParts, nameRoot);
                    i = i + 2;
                    nameRoot = "";
                } else if (isNamePartSubtype(nextNamePart)) {
                    addRootSubtypeToList("family", nextNamePart, nameParts, nameRoot);
                    i++;
                    nameRoot = "";
                }

            } else if (namePart.equalsIgnoreCase("subfamily")) { // subfamily and subfamily member
                if (nextNamePart.equalsIgnoreCase("member") && isNamePartSubtype(twoForwardNamePart)) {
                    addRootSubtypeToList("subfamily member", twoForwardNamePart, nameParts, nameRoot);
                    i = i + 2;
                    nameRoot = "";
                } else if (isNamePartSubtype(nextNamePart)) {
                    addRootSubtypeToList("subfamily", nextNamePart, nameParts, nameRoot);
                    i++;
                    nameRoot = "";
                }

            } else if (namePart.equalsIgnoreCase("domain") && isNamePartSubtype(nextNamePart)) { // domain
                addRootSubtypeToList("domain", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (namePart.equalsIgnoreCase("motif") && isNamePartSubtype(nextNamePart)) { // motif
                addRootSubtypeToList("motif", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (namePart.equalsIgnoreCase("subunit") && isNamePartSubtype(nextNamePart)) { // subunit
                addRootSubtypeToList("subunit", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (namePart.equalsIgnoreCase("isoform") && isNamePartSubtype(nextNamePart)) { // isoform
                addRootSubtypeToList("isoform", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (namePart.equalsIgnoreCase("class") && isNamePartSubtype(nextNamePart)) { // class
                addRootSubtypeToList("class", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (namePart.equalsIgnoreCase("chain") && isNamePartSubtype(nextNamePart)) { // chain
                // TODO

            } else if (namePart.equalsIgnoreCase("member") && isNamePartSubtype(nextNamePart)) { // member
                addRootSubtypeToList("member", nextNamePart, nameParts, nameRoot);
                nameRoot = "";
                i++;

            } else if (isNamePartSubtype(namePart)) { // subtype (roman, greek, arabic numeral and other combinations)
                addSubtypeToList(namePart, nameParts, nameRoot);
                nameRoot = "";

            } else { // if nothing then must be still part of modifiedName root
                if (nameRoot.isEmpty())
                    nameRoot = namePart;
                else
                    nameRoot = nameRoot + " " + namePart; // concatenate modifiedName root
            }

            if ((i == splitName.length - 1) && !nameRoot.isEmpty())
                nameParts.add(new NamePart(nameRoot, ""));
        }
        return nameParts;
    }

    private boolean isNamePartSubtype(String namePart) {
        if (GreekAlphabet.isGreekLetter(namePart)) {
            return true;
        }

        if (namePart.length() > 3) // if not greek letter and longer than 3 letters, not a subtype
            return false;

        if (namePart.matches("\\d+")) // if modifiedName part matches digit repeated once or more times
            return true;

        if (namePart.matches("\\d+[a-zA-Z]")) // format: 1A, 11B (three letters/digits and shorter)
            return true;

        if (namePart.matches("[a-zA-Z]\\d+")) // format C8 (letter and one or more digits)
            return true;

        if (namePart.matches("\\[a-zA-Z]\\d+\\[a-zA-Z]")) // format: C1s (letter+digit+letter)
            return true;

        if (namePart.matches("\\[A-Z]")) // check if single letter
            return true;

        return RomanDecoder.isRoman(namePart); // check if roman numeral
    }


    private void addRootSubtypeToList(String subtypeRoot, String subtype, List<NamePart> nameParts, String nameRoot) {
        if (!nameRoot.isEmpty()) {
            nameParts.add(new NamePart(nameRoot.trim(), ""));
        }

        nameParts.add(new NamePart(subtypeRoot.trim(), subtype.trim()));
    }

    private void addSubtypeToList(String subtype, List<NamePart> nameParts, String nameRoot) {
        if (!nameRoot.isEmpty()) {
            nameParts.add(new NamePart(nameRoot.trim(), ""));
        }

        nameParts.add(new NamePart("", subtype.trim()));
    }

    /**
     * Removes brackets and whatever characters are enclosed from the modifiedName
     */
    private String removeBracketContent(String str) {
        int openIndex = str.indexOf("(");
        int closeIndex = str.indexOf(")", openIndex);

        String output = str;
        if (openIndex >= 0 && closeIndex >= 0) {
            output = str.substring(0, openIndex);
            if (closeIndex != str.length() - 1)
                output = output + str.substring(closeIndex + 2, str.length());
        }
        return output;
    }

//    private String handleCommas(String name, String approvedSymbol) {
//        String[] approvedNameSplit = name.split(", ");
//
//        StringBuilder sb = new StringBuilder();
//        if ((approvedNameSplit[0].equalsIgnoreCase(approvedSymbol)
//                || approvedNameSplit[0].equalsIgnoreCase(approvedSymbol.substring(0, approvedSymbol.length() - 1) + " like"))
//                && approvedNameSplit.length > 1) { // some names contain gene symbol followed by comma, followed by actual modifiedName
//
//            for (int i = 1; i < approvedNameSplit.length; i++) { // remove pre-comma content
//                sb.append(approvedNameSplit[i]);
//            }
//
//        } else {
//            sb.append(approvedNameSplit[0]); // remove post-comma content
//            this.afterCommaContent = Arrays.copyOfRange(approvedNameSplit, 1, approvedNameSplit.length);
//        }
//
//        return sb.toString();
//    }

    ArrayList<String> getNames() {
        return new ArrayList<>(this.allNames);
    }

    static ArrayList<String> getUndesiredProteinNameParts() {
        return new ArrayList<>(undesiredProteinNameParts);
    }

    private List<String> createRoSuSuPermutations(List<NamePart> nameParts) {
        Set<String> output = new HashSet<>();

        if (!isStructureRoSuSu(nameParts))
            return new ArrayList<>(output);

        // example throughout comments: TUBB (tubulin beta class I, and TUBULIN BETA I)

        output.add(nameParts.get(1).subtype + nameParts.get(2).subtype + " " + nameParts.get(0).root); // betaI tubulin

        try {
            output.add(nameParts.get(1).subtype + RomanDecoder.toArabic(nameParts.get(2).subtype) + " " + nameParts.get(0).root); // beta1 tubulin
        } catch (IllegalArgumentException e) {
            // empty catch; term will simply not be added
        }

        if (GreekAlphabet.isGreekLetter(nameParts.get(1).subtype)) {
            String name2 = nameParts.get(1).subtype + " " + nameParts.get(2).subtype + " " + nameParts.get(0).root; // beta I tubulin, roman type will be added afterwards
            output.add(name2);
        }

        output.add(nameParts.get(0).root + " " + nameParts.get(1).subtype + nameParts.get(2).subtype); // tubulin betaI

        try {
            output.add(nameParts.get(0).root + " " + nameParts.get(1).subtype + RomanDecoder.toArabic(nameParts.get(2).subtype)); // tubulin beta1
        } catch (IllegalArgumentException e) {
            // empty catch block OK (if toArabic conversion fails
        }


//        if (GreekAlphabet.isGreekLetter(nameParts.get(1).subtype)) {
//            try {
//                Integer subtypeArabic2 = RomanDecoder.toArabic(nameParts.get(2).subtype);
//                output.add(nameParts.get(1).subtype + subtypeArabic2 + " " + nameParts.get(0).root); // beta1 tubulin
//                output.add(nameParts.get(1).subtype + " " + subtypeArabic2 + " " + nameParts.get(0).root); // beta 1 tubulin
//            } catch (IllegalArgumentException e) {
//                // empty catch block OK (if toArabic conversion fails)
//            }
//        }
//        if (GreekAlphabet.isGreekLetter(nameParts.get(2).subtype)) {
//            try {
//                Integer subtypeArabic1 = RomanDecoder.toArabic(nameParts.get(1).subtype);
//                output.add(subtypeArabic1 + nameParts.get(2).subtype + " " + nameParts.get(0).root);
//                output.add(subtypeArabic1 + " " + nameParts.get(2).subtype + " " + nameParts.get(0).root);
//            } catch (IllegalArgumentException e) {
//                // empty catch block OK (if toArabic conversion fails)
//            }
//        }

        return new ArrayList<>(output);
    }

    private void addRomanArabicSubtypeConversions(Set<String> nomenclatureSet) {
        Set<String> additionalNames = new HashSet<>();
        for (String name : nomenclatureSet) {
            additionalNames.add(RomanDecoder.replaceArabicToRoman(name));
            additionalNames.add(RomanDecoder.replaceRomanToArabic(name));
        }
        nomenclatureSet.addAll(additionalNames);
    }

    private void addGreekSubtypeConversions(Set<String> nomenclatureSet) {
        Set<String> additionalNames = new HashSet<>();
        for (String name : nomenclatureSet) {
            additionalNames.add(GreekAlphabet.replaceWithGreekLetters(name));
        }
        nomenclatureSet.addAll(additionalNames);
    }


    private Set<String> createRoSuPermutations(List<NamePart> nameParts) {
        Set<String> output = new HashSet<>();

        if (!isStructureRoSu(nameParts))
            return new HashSet<>();

        // example in comments GALACTOSIDASE CLASS BETA
        String name1 = nameParts.get(1).subtype + " " + nameParts.get(0).root; // beta galactosidase
        String name3 = nameParts.get(1).root + " " + nameParts.get(1).subtype + " " + nameParts.get(0).root; // class beta galactosidase
        output.add(name1);
        output.add(name3);

        return output;
    }

    private Set<String> createRoGreekPermutations(List<NamePart> nameParts) {
        Set<String> output = new HashSet<>();

        if (!isStructureRootAndGreekSubtype(nameParts))
            return new HashSet<>();

        String name1 = nameParts.get(1).subtype + " " + nameParts.get(0).root;
        output.add(name1);

        return output;
    }

    /**
     * isStructure methods return whether name conforms to a certain structure: 'Su' corresponds to Subtype, 'Ro' corresponds to root
     */
    private boolean isStructureRoSuSu(List<NamePart> nameParts) {
        if (nameParts.size() == 3) {
            return nameParts.get(0).isRootOnly()
                    && (nameParts.get(1).isSubtypeOnly() || nameParts.get(1).isRootAndSubtype())
                    && nameParts.get(2).isSubtypeOnly();
        } else
            return false;
    }

    private boolean isStructureRoSu(List<NamePart> nameParts) {
        if (nameParts.size() == 2)
            return nameParts.get(0).isRootOnly()
                    && nameParts.get(1).isSubtypeOnly() || nameParts.get(1).isRootAndSubtype();
        else
            return false;
    }

    /**
     * Identifies names that follow the pattern ROOT 1 (e.g. Fibronectin 1)
     **/

    private boolean isStructureRootAndOne(List<NamePart> nameParts) {
        if (nameParts.size() == 2) {
            boolean isRightStructure = nameParts.get(0).isRootOnly()
                    && nameParts.get(1).isSubtypeOnly(); // name contains root followed by subtype
            if (isRightStructure) {
                //int nrWordsInRoot = nameParts.get(0).root.split(" ").length; // compute number of words of root

                try {
                    boolean result = nameParts.get(1).subtype.equalsIgnoreCase("1");
                    return result; // return TRUE iff root contains one or two words AND subtype = 1
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Identifies names that follow the pattern ROOT GREEK 1 (e.g. Galactosidase beta 1)
     **/

    private boolean isStructureRootAndGreekSubtypeAndOne(List<NamePart> nameParts) {
        if (nameParts.size() == 3) {
            boolean isRightStructure = nameParts.get(0).isRootOnly()
                    && nameParts.get(1).isSubtypeOnly() && GreekAlphabet.isGreekLetter(nameParts.get(1).subtype)
                    && nameParts.get(2).isSubtypeOnly(); // compute if name structure is correct (ROOT GREEK 1)

            if (isRightStructure) {
                int nrWordsInRoot = nameParts.get(0).root.split(" ").length; // count number of words in root

                try {
                    return (Integer.parseInt(nameParts.get(2).subtype) == 1) && (nrWordsInRoot == 1 || nrWordsInRoot == 2);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isStructureRootAndGreekSubtype(List<NamePart> nameParts) {
        if (nameParts.size() == 2) {
            boolean isRightStructure = nameParts.get(0).isRootOnly()
                    && nameParts.get(1).isSubtypeOnly();
            if (isRightStructure) {
                return GreekAlphabet.isGreekLetter(nameParts.get(1).subtype);
            }
        }
        return false;
    }

    boolean isStructureRootAndOne() {
        return this.isStructureRootAndOne;
    }

    String getRoot() {
        if (this.nameParts1 != null && !this.nameParts1.isEmpty()) return this.nameParts1.get(0).root;
        else if (this.nameParts2 != null && !this.nameParts2.isEmpty()) return this.nameParts2.get(0).root;
        else return null;
    }

    private String removeUnnecessaryNameParts(String name) {
        String sModified = name;
        for (int i = 1; i < undesiredProteinNameParts.size(); i++) { // start from second entry, since first entry are instructions and notes
            sModified = sModified.replaceAll(undesiredProteinNameParts.get(i), "");
        }
        sModified = sModified.replaceAll("   ", " "); // replace any triple spaces with single
        return sModified.replaceAll("  ", " "); // replace double spaces with single and return
    }

    private List<String> constructCollagenNames(String name, String geneName) {
        List<String> output = new ArrayList<>();

        if (name == null)
            return output;

        boolean checkIfCorrectGene = geneName.equalsIgnoreCase("COL1A1") ||
                geneName.equalsIgnoreCase("COL1A2") ||
                geneName.equalsIgnoreCase("COL4A1") ||
                geneName.equalsIgnoreCase("COL6A1") ||
                geneName.equalsIgnoreCase("COL9A1");

        if (checkIfCorrectGene) {

            String[] nameWords = name.split(" ");
            String typeRoman = nameWords[2]; // position where collagen type stored as roman, see HGNC

            try {
                int typeInt = RomanDecoder.toArabic(typeRoman);
                output.add("collagen type " + typeInt);
                output.add("type " + typeInt + " collagen");
                output.add("collagen, type " + typeInt);
                output.add("collagen " + typeInt);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            output.add("collagen type " + typeRoman);
            output.add("type " + typeRoman + " collagen");
            output.add("collagen, type " + typeRoman);
            output.add("collagen " + typeRoman);
        }
        return output;
    }

    private class NamePart implements Serializable {
        static final transient long serialVersionUID = 2343564872L;

        String root = "";
        String subtype = "";

        public NamePart() {
        }

        private NamePart(String root, String subtype) {
            if (root != null)
                this.root = root;
            if (subtype != null)
                this.subtype = subtype.trim();
        }

        private NamePart(NamePart namePart) {
            if (namePart != null) {
                this.root = namePart.root;
                this.subtype = namePart.subtype;
            }
        }

        boolean isRootOnly() {
            return !root.isEmpty() && subtype.isEmpty();
        }

        boolean isSubtypeOnly() {
            return root.isEmpty() && !subtype.isEmpty();
        }

        boolean isRootAndSubtype() {
            return !root.isEmpty() && !subtype.isEmpty();
        }

        @Override
        public String toString() {
            return "NamePart{" +
                    "root='" + root + '\'' +
                    ", subtype='" + subtype + '\'' +
                    '}';
        }
    }
}
