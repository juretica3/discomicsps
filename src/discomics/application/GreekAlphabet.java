package discomics.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin_desktop on 13/05/2017.
 */

public class GreekAlphabet {

    private static List<GreekLetter> alphabet = new ArrayList<>();

    static {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream greekAlphabetIs = classLoader.getResourceAsStream("discomics/greek/greekAlphabet.txt");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(greekAlphabetIs));
            String line;

            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split(",");
                if (lineSplit.length == 4) {
                    alphabet.add(new GreekLetter(lineSplit[0].replaceAll(",", ""),
                            lineSplit[1].replaceAll(",", ""),
                            lineSplit[2].replaceAll(",", ""),
                            lineSplit[3].replaceAll(",", "")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isGreekLetter(String word) {
        for (GreekLetter greekLetter : alphabet) {
            if (word.trim().equalsIgnoreCase(greekLetter.getLetter())) {
                return true;
            }
        }
        return false;
    }

    public static String identifyGreekLetterReturnLatin(String phrase) {
        for (GreekLetter greekLetter : alphabet) {
            if(phrase.contains(greekLetter.getUnicodeLower()) || phrase.contains(greekLetter.getUnicodeUpper()))
                return greekLetter.latinEquivalent;
        }
        return null;
    }


    public static String replaceWithGreekLetters(String s) {
        for (GreekLetter greekLetter : alphabet) {
            s = s.replaceAll("\\b" + greekLetter.getLetter(), greekLetter.getUnicodeLower());
            s = s.replaceAll(greekLetter.getLetter() + "\\b", greekLetter.getUnicodeLower());
        }
        return s;
    }


    private static class GreekLetter {
        private String letter;
        private String latinEquivalent;
        private String unicodeLower;
        private String unicodeUpper;

        GreekLetter(String letter, String latinEquivalent, String unicodeLower, String unicodeUpper) {
            this.letter = letter;
            this.latinEquivalent = latinEquivalent;
            this.unicodeLower = unicodeLower;
            this.unicodeUpper = unicodeUpper;
        }

        String getLetter() {
            return letter;
        }

        String getLatinEquivalent() { return latinEquivalent; }

        String getUnicodeLower() {
            return unicodeLower;
        }

        String getUnicodeUpper() {
            return unicodeUpper;
        }
    }
}
