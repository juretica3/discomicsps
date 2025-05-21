package discomics.application;

import discomics.model.IoModel;
import discomics.model.ProteaseFamily;
import discomics.model.Protein;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Jure on 8.9.2016.
 */
public class ArticleScanProtease {

    private static final String wordBoundaryRegex = ".*\\b";
    private static final String wordBoundaryRegexEnd = "\\b.*";

    public static Set<Protein> processAbstractForProteases(String artAbstract, ProteaseFamily proteaseFamily) {
        Set<Protein> outputProteasesMentioned;

        if (proteaseFamily.equals(IoModel.getCTS())) {
            outputProteasesMentioned = scanAbstractForProteaseSubtypesCathepsin(artAbstract, IoModel.getProteasesCTS());
        } else if (proteaseFamily.equals(IoModel.getMMP())) {
            outputProteasesMentioned = scanAbstractProteaseSubtypesPass1(artAbstract, IoModel.getProteasesMMP());
            outputProteasesMentioned.addAll(scanAbstractForProteaseSubtypesPass2Metallo(artAbstract, proteaseFamily,
                    IoModel.getProteasesMMP()));
        } else {
            outputProteasesMentioned = scanAbstractProteaseSubtypesPass1(artAbstract, IoModel.getProteasesADAMTS());
            outputProteasesMentioned.addAll(scanAbstractProteaseSubtypesPass1(artAbstract, IoModel.getProteasesADAM()));
            outputProteasesMentioned.addAll(scanAbstractForProteaseSubtypesPass2Metallo(artAbstract, IoModel.getADAM(), IoModel.getProteasesADAM()));
            outputProteasesMentioned.addAll(scanAbstractForProteaseSubtypesPass2Metallo(artAbstract, IoModel.getADAMTS(), IoModel.getProteasesADAMTS()));
        }

        return outputProteasesMentioned;
    }

    private static HashSet<Protein> scanAbstractForProteaseSubtypesCathepsin(String artAbstract, List<Protein> proteases) {
        HashSet<Protein> proteasesMentioned = new HashSet<>();

        String abstractText = artAbstract;
        abstractText = abstractText.replaceAll("\\.A ", "");
        abstractText = abstractText.replaceAll("\\. A ", "");
        abstractText = abstractText.replaceAll("cathepsins", "cathepsin");

        Pattern undesirables = Pattern.compile("\\p{P}");
        abstractText = undesirables.matcher(abstractText).replaceAll(" ");

        abstractText = abstractText.replaceAll(" a ", "");
        abstractText = abstractText.toLowerCase();

        String[] words = abstractText.split("\\s+");

        for (Protein protease : proteases) {

            for (int i = 0; i < words.length; i++) {
                String root = null;

                if (words[i].toLowerCase().contains("cts") || words[i].toLowerCase().contains("cathepsin")) {

                    root = words[i]; // this could be ctsX; lets check

                    if (root.equals(protease.getMainName().toLowerCase())) {
                        proteasesMentioned.add(protease);
                        proteasesMentioned.addAll(checkSubtypeConcatenationCTS(words, i + 1, proteases, true));
                        continue;
                    }

                    if (i >= (words.length - 1))
                        break;

                    String root2 = root + (words[i + 1]); // this could be ctsX, cathepsinX, cathepsinsX; lets check

                    if (root2.equals(protease.getMainName().toLowerCase())) {
                        proteasesMentioned.add(protease);
                        proteasesMentioned.addAll(checkSubtypeConcatenationCTS(words, i + 1, proteases, true));
                        continue;
                    }

                    if (root2.equals("cathepsin" + protease.getProteaseSubtype().toLowerCase())) {
                        proteasesMentioned.add(protease);
                        proteasesMentioned.addAll(checkSubtypeConcatenationCTS(words, i + 1, proteases, true));
                        continue;
                    }

                    if (root2.equals("cathepsins" + protease.getProteaseSubtype().toLowerCase())) {
                        proteasesMentioned.add(protease);
                        proteasesMentioned.addAll(checkSubtypeConcatenationCTS(words, i + 1, proteases, true));
                    }

                }
            }
            for (int i = 0; i < protease.getTextMiningNames(null).size(); i++) {
                if (abstractText.contains(" " + protease.getTextMiningNames(null).get(i).toLowerCase() + " ")) {
                    proteasesMentioned.add(protease);
                }
            }
        }
        return proteasesMentioned;
    }

    private static HashSet<Protein> checkSubtypeConcatenationCTS(String[] words, int index, List<Protein> proteases, boolean fromParent) {

        HashSet<Protein> subtypes = new HashSet<>();

        if (index >= (words.length - 1)) {
            return subtypes;
        }

        String currentWord = words[index];
        for (Protein protease : proteases) {

            if (currentWord.equals("and") || currentWord.equals("or")) {
                subtypes.addAll(checkSubtypeConcatenationCTS(words, index + 1, proteases, false));
                break;
            }

            if (currentWord.equals(protease.getProteaseSubtype().toLowerCase())) {
                subtypes.add(protease);
                subtypes.addAll(checkSubtypeConcatenationCTS(words, index + 1, proteases, false));
            }

        }
        if (fromParent) {
            subtypes.addAll(checkSubtypeConcatenationCTS(words, index + 1, proteases, false));
        }

        return subtypes;
    }

    private static HashSet<Protein> scanAbstractProteaseSubtypesPass1(String artAbstract, List<Protein> proteases) {
        HashSet<Protein> outputSet = new HashSet<>();

        for (Protein protease : proteases) {
            outputSet.addAll(protease.getTextMiningNames(null).stream()
                    .filter(name -> artAbstract.matches(wordBoundaryRegex + name + wordBoundaryRegexEnd))
                    .map(name -> protease).collect(Collectors.toList()));
        }

        String abstractText = artAbstract;
        Pattern undesirables = Pattern.compile("[^A-Za-z0-9\\s+]");
        abstractText = undesirables.matcher(abstractText).replaceAll(" ");
        abstractText = abstractText.toLowerCase();
        String abstractTextMod = abstractText.replaceAll("  ", " ");

        for (Protein protease : proteases) {
            outputSet.addAll(protease.getTextMiningNames(null).stream()
                    .filter(name -> abstractTextMod.matches(wordBoundaryRegex + name.toLowerCase() + wordBoundaryRegexEnd))
                    .map(name -> protease).collect(Collectors.toList()));
        }

        abstractText = abstractText.replaceAll("and", "");
        abstractText = abstractText.replaceAll("or", "");
        abstractText = abstractText.replaceAll("but", "");
        abstractText = abstractText.replaceAll("not", "");
        String abstractTextMod2 = abstractText.replaceAll("  ", " ");

        for (Protein protease : proteases) {
            outputSet.addAll(protease.getTextMiningNames(null).stream()
                    .filter(name -> abstractTextMod2.matches(wordBoundaryRegex + name.toLowerCase() + wordBoundaryRegexEnd))
                    .map(name -> protease).collect(Collectors.toList()));
        }

        return outputSet;
    }


    private static HashSet<Protein> scanAbstractForProteaseSubtypesPass2Metallo(String artAbstract, ProteaseFamily proteaseFamily, List<Protein> proteases) {
        String abstractText = artAbstract;

        if (proteaseFamily.equals(IoModel.getADAMTS())) {
            abstractText = abstractText.replaceAll("ADAM-TS", "ADAMTS");
            abstractText = abstractText.replaceAll("ADAMT-S", "ADAMTS");
        }

        abstractText = abstractText.replaceAll("and", "");
        abstractText = abstractText.replaceAll("or", "");
        abstractText = abstractText.replaceAll("but", "");
        abstractText = abstractText.replaceAll("not", "");


        // remove all punctuation except hyphens
        //Pattern undesirables = Pattern.compile("\\p{P}");
        Pattern undesirables = Pattern.compile("[^A-Za-z0-9\\s+]");
        abstractText = undesirables.matcher(abstractText).replaceAll(" ");
        abstractText = abstractText.toLowerCase();
        abstractText = abstractText.replaceAll("  ", " ");

        abstractText = abstractText.replaceAll("aggrecanase 1", "");
        abstractText = abstractText.replaceAll("aggrecanase 2", "");
        abstractText = abstractText.replaceAll("tissue inhibitor of matrix metalloprot", "");
        abstractText = abstractText.replaceAll("tissue inhibitor of matrix metallopept", "");


        HashSet<String> subtypes = new HashSet<>();

        while (abstractText.contains(proteaseFamily.getStandardAbbreviation().toLowerCase())) {
            int indexStart = abstractText.indexOf(proteaseFamily.getStandardAbbreviation().toLowerCase());
            indexStart += proteaseFamily.getStandardAbbreviation().length();

            String substring = abstractText.substring(indexStart);

            int indexFirstNonDigit = 0;

            for (int i = 0; i < substring.length(); i++) {
                if (Character.isDigit(substring.charAt(i)) || Character.isSpaceChar(substring.charAt(i))) {
                    indexFirstNonDigit++;
                } else {
                    break;
                }
            }

            String subtypeSubstring = substring.substring(0, indexFirstNonDigit);
            String[] subtypeArray = subtypeSubstring.split("\\s+");
            subtypes.addAll(Arrays.asList(subtypeArray));

            abstractText = substring;
        }

        // convert from subtype string numbers to proteases
        HashSet<Protein> outputList = new HashSet<>();
        for (String subtype : subtypes) {
            outputList.addAll(proteases.stream().filter(protease -> subtype.equals(protease.getProteaseSubtype())).collect(Collectors.toList()));
        }

        return outputList;
    }
}
