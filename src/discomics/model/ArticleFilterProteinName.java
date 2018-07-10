package discomics.model;

import discomics.application.RomanDecoder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class ArticleFilterProteinName {

    private static List<String> clashingDictionaryNamePermittedNeighbors = new ArrayList<String>() {
        {
            try {
                InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/textmining/clashing_dictionary_name_permitted_neighbours.txt");
                addAll(IOUtils.readLines(inputStream));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    static Collection<Article> filterArticles(Collection<Article> articles, TextMinableInput textMinableInput, QuerySettings querySettings) {
        if (textMinableInput instanceof Protein) {
            return filterArticles(articles, (Protein) textMinableInput, querySettings);
        } else if (textMinableInput instanceof GeneFamily) {
            return filterArticles(articles, (GeneFamily) textMinableInput, querySettings);
        } else if (textMinableInput instanceof Drug) {
            return filterArticles(articles, (Drug) textMinableInput, querySettings);
        } else {
            return articles;
        }
    }

    /**
     * Method that filters the articles retrieved from PubMed and ePMC. Article passed through filtering if it contains at least
     * one of the protein names used in the search in its abstract
     */
    private static Collection<Article> filterArticles(Collection<Article> articles, Protein queryProtein, QuerySettings querySettings) {
        List<Article> filteredArticleList = new ArrayList<>();

        // construct protein names ArrayList with properly formatted names in an order to optimise for performance
        ArrayList<String> proteinNames = new ArrayList<>();
        //proteinNames.add(0, queryProtein.getProteinName().replaceAll("[\\p{Punct}]+", " ").replaceAll("  ", " ").toLowerCase());
        for (String protName : queryProtein.getLongNamesFiltering()) {
            proteinNames.add(protName.replaceAll("[\\p{Punct}]+", " ").replaceAll("  ", " "));
        }

        // remove root + subtype not 1
        //List<String> badProteinNames = queryProtein.getBadProteinNames();
        String queryProteinRoot_RootAndOne = queryProtein.getProteinNameObject().getRoot();
        boolean isStructureRootAndOne = queryProtein.getProteinNameObject().isStructureRootAndOne();

        // loop through articles and filter
        for (Article article : articles) {

            String articleAbstract = article.getTitleAbstractConcat().toLowerCase();
            String articleAbstractProcessed = articleAbstract;
            ArrayList<String> proteinNamesForLoop = new ArrayList<>(proteinNames);

            // if protein name has structure ROOT + ONE then remove all ROOT + NONONE substrings from abstract
            if (isStructureRootAndOne) {
                articleAbstractProcessed = RomanDecoder.replaceAllRomanToArabic(articleAbstract);

                // eliminate all ROOT + NR(NOT 1) words
                articleAbstractProcessed = articleAbstractProcessed.replaceAll(queryProteinRoot_RootAndOne + "[-|\\s]\\b(?!(?:1)\\b)\\d{1,2}\\b", "");

                if (articleAbstractProcessed.length() != articleAbstract.length()) // contains ROOT+NONONE, has to contain ROOT+1 (only ROOT not enough)
                    proteinNamesForLoop.remove(queryProteinRoot_RootAndOne);
            }

            String[] artAbstractSentences = articleAbstractProcessed.split("\n|\\.(?!\\d)|(?<!\\d)\\."); // split on new line and dot (not if number on both sides as with decimals)

            List<String> shortNames = queryProtein.getShortNamesFiltering();

            sentencesLoop:
            for (String sentence : artAbstractSentences) { // reformat abstract sentences

                String sentenceWithBrackets = sentence.replaceAll("[\\p{Punct}&&[^()\\[\\]'+\\-]]+", " ");
                String sentenceNoPunctuation = sentence.replaceAll("[\\p{Punct}]+", " "); // remove all punctuation

                // remove illegal protein names from sentence
                for (String illegalName : queryProtein.getIllegalNames()) {
                    sentenceNoPunctuation = sentenceNoPunctuation.replaceAll(illegalName, " ");
                }

                String[] sentenceNoPunctuationSplit = sentenceNoPunctuation.split("\\s+");

                // search all protein names in each abstract sentence. if name is found, add article to filtered list and break out of this loop
                for (String proteinName : proteinNamesForLoop) {
                    if (searchNameInSentence(proteinName.split("\\s+"), sentenceNoPunctuationSplit)) {
                        article.setFilteringPassed(true);
                        filteredArticleList.add(article);
                        break sentencesLoop;
                    }
                }

                // search short identifiers with permitted neighbours
//                for (String shortName: queryProtein.getShortNamesFiltering()) {
//                    boolean shortIdentifierPresent = searchNameInSentenceRequireNeighbour(shortName, sentenceNoPunctuationSplit, 2);
//                    if(shortIdentifierPresent) {
//                        article.setFilteringPassed(true);
//                        filteredArticleList.add(article);
//                        break sentencesLoop;
//                    }
//                }

                // search short identifiers; if name is found, add article to filtered list and break out of sentences loop
                Boolean shortIdentifierPresent;
                List<String> shortNamesCopy = new ArrayList<>(shortNames); // create copy to avoid concurrent modification by removal
                for (String shortName : shortNamesCopy) {
                    shortIdentifierPresent = searchGeneInSentence(shortName, queryProtein.getProteinName().split("\\s+"), sentenceWithBrackets.split("\\s+"), sentenceNoPunctuationSplit);

                    if (shortIdentifierPresent == null) {
                        shortNames.remove(shortName); // not searched for following sentences of same article because could be present outside brackets now
                    } else if (shortIdentifierPresent) {
                        article.setFilteringPassed(true);
                        filteredArticleList.add(article);
                        break sentencesLoop;
                    }
                }

                // search clashing dictionary names in sentence
                List<String> clashingProteinNames = queryProtein.getDictionaryClashingNames();
                for (String clashingName : clashingProteinNames) {
                    boolean clashingNamePassFiltering = searchNameInSentenceRequireNeighbour(clashingName, sentenceNoPunctuationSplit, 6);

                    if (clashingNamePassFiltering) {
                        article.setFilteringPassed(true);
                        filteredArticleList.add(article);
                        break sentencesLoop;
                    }
                }

                // search disease names taking into account allowed neighbours
                String diseaseProteinApprovedName = queryProtein.getDiseaseApprovedName();
                if (diseaseProteinApprovedName != null) {
                    boolean diseaseApprovedNamePassFiltering = searchNameInSentenceRequireNeighbour(diseaseProteinApprovedName, sentenceNoPunctuationSplit, 1);

                    if (diseaseApprovedNamePassFiltering) {
                        article.setFilteringPassed(true);
                        filteredArticleList.add(article);
                        break sentencesLoop;
                    }
                }
            }
        }
        if (querySettings.isNameFilteringEnable())
            return filteredArticleList;
        else
            return articles;
    }

    private static Collection<Article> filterArticles(Collection<Article> articles, GeneFamily family, QuerySettings querySettings) {
        List<Article> filteredArticles = new ArrayList<>();
        List<String> filteringTerms = family.getTextMiningNames(null);

        for (Article article : articles) {
            String artAbstract = article.getArtAbstract().toLowerCase();
            String[] artAbstractSentences = artAbstract.split("\n|\\.(?!\\d)|(?<!\\d)\\."); // split on new line and dot (not if number on both sides as with decimals)

            sentencesLoop:
            for (String sentence : artAbstractSentences) {
                String sentenceNoPunctuation = sentence.replaceAll("[\\p{Punct}]+", " "); // remove all punctuation

                String[] sentenceNoPunctuationSplit = sentenceNoPunctuation.split("\\s+");

                // search all protein names in each abstract sentence. if name is found, add article to filtered list and break out of this loop
                for (String proteinName : filteringTerms) {
                    if (searchNameInSentence(proteinName.split("\\s+"), sentenceNoPunctuationSplit)) {
                        article.setFilteringPassed(true);
                        filteredArticles.add(article);
                        break sentencesLoop;
                    }
                }
            }
        }
        if (querySettings.isNameFilteringEnable())
            return filteredArticles;
        else
            return articles;
    }

    private static Collection<Article> filterArticles(Collection<Article> articles, Drug drug, QuerySettings querySettings) {
        ArrayList<Article> outputArticleList = new ArrayList<>();



        return outputArticleList;
    }

    /**
     * part of the protein name filtering algorithm. Searches the presence of protein name words in a sentence of
     * the article abstract. These words have to be present in close proximity from one another within the sentence
     */
    private static boolean searchNameInSentence(String[] proteinNameWords, String[] sentenceWords) {
        int lookAhead = proteinNameWords.length + 2;
        int lookBehind = 2;

        for (int i = 0; i < sentenceWords.length; i++) {
            String artAbstractWord = sentenceWords[i];

            if (artAbstractWord.equals(proteinNameWords[0])) { // if locked into first word of name, the look ahead for the rest

                if (proteinNameWords.length == 1) // for single word names (and symbols)
                    return true;
                if (sentenceWords.length - 1 == i) // not single word name, reached end of sentence
                    return false;

                List<String> nextArtAbstractWords = new ArrayList<>(); // if name contains more words, look for other words as well
                if (i - lookBehind >= 0) {
                    nextArtAbstractWords.addAll(Arrays.asList(Arrays.copyOfRange(sentenceWords, i - lookBehind, i)));
                } else {
                    nextArtAbstractWords.addAll(Arrays.asList(Arrays.copyOfRange(sentenceWords, 0, i)));
                }

                if (i + 1 + lookAhead > sentenceWords.length) {
                    nextArtAbstractWords.addAll(Arrays.asList(Arrays.copyOfRange(sentenceWords, i + 1, sentenceWords.length)));
                } else {
                    nextArtAbstractWords.addAll(Arrays.asList(Arrays.copyOfRange(sentenceWords, i + 1, i + 1 + lookAhead)));
                }

                for (String nextProteinNameWord : Arrays.copyOfRange(proteinNameWords, 1, proteinNameWords.length)) { // if for loop finishes, return true
                    boolean contains = nextArtAbstractWords.contains(nextProteinNameWord);
                    if (!contains) { // doesn't contain
                        return searchNameInSentence(proteinNameWords, Arrays.copyOfRange(sentenceWords, i + 1, sentenceWords.length));
                    }
//                    else {
//                        List<String> proximalArtAbstractWords = new ArrayList<>(nextArtAbstractWords.subList(0, index));
//                        proximalArtAbstractWords.remove("and");
//                        proximalArtAbstractWords.remove("or");
//                        for(String proximalWord: proximalArtAbstractWords) {
//                            if(proximalWord.length() > 1 && nextProteinNameWord.length() == 1)
//                                return searchNameInSentence(proteinNameWords, Arrays.copyOfRange(sentenceWords, i + 1, sentenceWords.length));
//                        }
//                    }
                }
                return true;
            }
        }
        return false; // never locked into first word of name
    }

    private static Boolean searchGeneInSentence(String geneName, String[] mainProteinNameWords, String[] sentenceWords, String[] sentenceWordsNoPunct) {
        for (int i = 0; i < sentenceWords.length; i++) {

            String artAbstractWord = sentenceWords[i];

            // todo WHAT THE FUCK?
//            String regexString1 = geneName + "\\S";
//            String regexString2 = geneName + "\\S\\S";
//            String artAbstractWordAllPunctuationRemove = artAbstractWord.replaceAll("[\\p{Punct}]+", "");
//
//            if (artAbstractWordAllPunctuationRemove.matches(regexString1) || artAbstractWordAllPunctuationRemove.matches(regexString2))
//                return null;

            if (artAbstractWord.contains(geneName + "-")) // part of longer acronym
                return null;

            if (artAbstractWord.contains("-" + geneName)) // part of longer acronym
                return null;

            if (artAbstractWord.equalsIgnoreCase(geneName)) { // gene name without brackets has to have permitted neighbour
               return searchNameInSentenceRequireNeighbour(geneName, sentenceWordsNoPunct, 6);
            }

            if (artAbstractWord.equalsIgnoreCase("(" + geneName)) // gene name with bracket on right (may be a list of adjacent gene identifiers in brackets)
                return searchNameInSentenceRequireNeighbour(geneName, sentenceWordsNoPunct, 6);

            if (artAbstractWord.equalsIgnoreCase(geneName + ")")) // might be list of adjacent gene identifiers in brackets
                return searchNameInSentenceRequireNeighbour(geneName, sentenceWordsNoPunct, 6);

            if (artAbstractWord.equalsIgnoreCase("(" + geneName + ")") || artAbstractWord.equalsIgnoreCase("[" + geneName + "]")) { // current word is gene name in brackets
                // StringBuilder contains part of sentence preceding gene name in brackets
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < i; j++) {
                    sb.append(sentenceWords[j]).append(" ");
                }
                String precedingSentence = sb.toString();

                // if part of sentence preceding gene name in brackets contains first word of main protein name
                if (precedingSentence.toLowerCase().contains(mainProteinNameWords[0])) {
                    return true;
                } else {
                    return null; // if preceding part does not contain protein name reference, then drop gene name completely
                }
            }
        }
        return false;
    }

    private static boolean searchNameInSentenceRequireNeighbour(String clashingName, String[] sencenceWords, int wordNrLookAround) {
        if (wordNrLookAround < 1)
            return false;

        for (int i = 0; i < sencenceWords.length; i++) {
            String currentSentenceWord = sencenceWords[i];

            if (clashingName.equalsIgnoreCase(currentSentenceWord)) {
                for (String permittedNeighbour : clashingDictionaryNamePermittedNeighbors) {

                    // look neighbours before identified clashing protein name
                    for (int j = i - 1; j >= 0 && j >= i - wordNrLookAround; j--) {
                        String neighbouringSentenceWord = sencenceWords[j];
                        if (neighbouringSentenceWord.contains(permittedNeighbour))
                            return true;
                    }

                    // look neighbours after identified clashing protein name
                    for (int k = i + 1; k < sencenceWords.length && k <= i + wordNrLookAround; k++) {
                        String neighbouringSentenceWord = sencenceWords[k];
                        if (neighbouringSentenceWord.contains(permittedNeighbour))
                            return true;
                    }
                }
            }
        }
        return false;
    }

}
