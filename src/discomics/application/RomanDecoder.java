package discomics.application;

import java.util.TreeMap;

public final class RomanDecoder {

    private final static TreeMap<Integer, String> map = new TreeMap<Integer, String>();

    final static char symbol[] = {'M', 'D', 'C', 'L', 'X', 'V', 'I'};
    final static int value[] = {1000, 500, 100, 50, 10, 5, 1};

    public static int toArabic(String roman) throws IllegalArgumentException {
        roman = roman.toUpperCase();
        if (roman.length() == 0) return 0;
        for (int i = 0; i < symbol.length; i++) {
            int pos = roman.indexOf(symbol[i]);
            if (pos >= 0)
                return value[i] - toArabic(roman.substring(0, pos)) + toArabic(roman.substring(pos + 1));
        }
        throw new IllegalArgumentException("Invalid Roman Symbol.");
    }

    public static boolean isRoman(String roman) {
        try {
            toArabic(roman);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private static int[] numbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static String[] letters = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    public static String toRoman(int N) {
        String roman = "";
        for (int i = 0; i < numbers.length; i++) {
            while (N >= numbers[i]) {
                roman += letters[i];
                N -= numbers[i];
            }
        }
        return roman;
    }

    public static String toRoman(String number) {
        try {
            Integer integer = Integer.parseInt(number);
            return toRoman(integer);
        } catch (NumberFormatException e) {
            return number;
        }
    }

    /** replaces first roman number in a String to the arabic equivalent; returns input term if not replacements are made */
    public static String replaceRomanToArabic(String term) {

        String[] termSplit = term.split("\\s+");
        for (int i = 0; i < termSplit.length; i++) {
            try {
                String termWord = termSplit[i];
                Integer arabicInt = toArabic(termWord); // exception thrown if input is not roman
                termSplit[i] = Integer.toString(arabicInt);
                StringBuilder sb = new StringBuilder();

                for (String termOut: termSplit) {
                    sb.append(termOut).append(" ");
                }
                return sb.toString().trim();

            } catch (IllegalArgumentException e) {
                // termWord not arabic numeral
            }
        }
        return term;
    }

    public static String replaceArabicToRoman(String term) {
        String[] termSplit = term.split("\\s+");
        for (int i = 0; i < termSplit.length; i++) {
            try {
                String termWord = termSplit[i];
                String romanInt = toRoman(termWord); // exception thrown if input is not number
                termSplit[i] = romanInt;
                StringBuilder sb = new StringBuilder();

                for (String termOut: termSplit) {
                    sb.append(termOut).append(" ");
                }
                return sb.toString().trim();

            } catch (IllegalArgumentException e) {
                // termWord not arabic numeral
            }
        }
        return term;
    }

    public static String replaceAllRomanOneToArabic(String text) {
        return text.replaceAll("[\\s|-]I[\\s]", "1");
    }

    public static String replaceAllRomanToArabic(String text) {
        String textOut = text.replaceAll("[\\s|-]I[\\s]", "1");
        textOut = textOut.replaceAll("[\\s|-]II[\\s]", "2");
        textOut = textOut.replaceAll("[\\s|-]III[\\s]", "3");
        textOut = textOut.replaceAll("[\\s|-]IV[\\s]", "4");
        textOut = textOut.replaceAll("[\\s|-]V[\\s]", "5");
        textOut = textOut.replaceAll("[\\s|-]VI[\\s]", "6");
        textOut = textOut.replaceAll("[\\s|-]VII[\\s]", "7");
        textOut = textOut.replaceAll("[\\s|-]VIII[\\s]", "8");
        textOut = textOut.replaceAll("[\\s|-]IX[\\s]", "9");
        textOut = textOut.replaceAll("[\\s|-]X[\\s]", "10");
        return textOut;
    }

}
