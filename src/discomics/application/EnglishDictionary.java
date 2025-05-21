package discomics.application;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Jure on 22/01/2017.
 */

public class EnglishDictionary {
    private static ArrayList<String> englishDictionary = new ArrayList<>();

    public EnglishDictionary() {
        BufferedReader br = null;

        //ClassLoader classloader = Main.classLoader;
        InputStream englishDictionary = Thread.currentThread().getContextClassLoader().getResourceAsStream("discomics/dictionary/wordsEnglish.txt");

        try {
            br = new BufferedReader(new InputStreamReader(englishDictionary));
            String word;
            while ((word = br.readLine()) != null) {
                EnglishDictionary.englishDictionary.add(word);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(EnglishDictionary.englishDictionary);
    }

    public static boolean isEnglish(String testWord) {
        return Collections.binarySearch(EnglishDictionary.englishDictionary, testWord.toLowerCase()) >= 0;
    }
}
