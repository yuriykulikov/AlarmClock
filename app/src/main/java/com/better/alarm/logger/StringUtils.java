package com.better.alarm.logger;


import android.os.Build;

import androidx.annotation.RequiresApi;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public final class StringUtils {

//    public static<Node extends ObservableStringTree2.ObservableStringNode<Node>> ObservableStringTree2<Node> getTree(
//            Class<Node> constructor,
//            Supplier<String> delimiterSupplier
//    ) {
//        return new ObservableStringTree2<>(
//                constructor,
//                delimiterSupplier);
//    }

    public static String[] split(String original, int index) {
        return new String[]{
                original.substring(0, index),
                original.substring(index)
        };
    }

    public static String[] split(String original, int ... points) {
        int length = points.length + 1;
        int lastPoint = 0;
        int lastPos = points.length;
        String[] result = new String[length];
        int lastSplittingPoint = points[lastPos - 1];
        if (!(lastSplittingPoint > original.length())) {
            for (int i = 0; i < length; i++) {

                if (i != lastPos) {
                    int currentPoint = points[i];
                    result[i] = original.substring(lastPoint, currentPoint);
                    lastPoint = currentPoint;
                } else {
                    result[i] = original.substring(lastPoint);
                }
            }
        } else {
            throw new IndexOutOfBoundsException("Last point: [" + lastSplittingPoint + "] is greater than the length of the original String: " + original +" [" + original.length() + "]. " );
        }
        return result;
    }

    public static String generateRandomHexToken(SecureRandom secureRandom, int byteLength) {
        byte[] token = new byte[byteLength];
        secureRandom.nextBytes(token);
        return new BigInteger(1, token).toString(16); // Hexadecimal encoding
    }

    public static String intToStringTransformer(int _int, int decimalPlaces, String prefix, String sufix) {
        String prevString = prefix == null ? "" : prefix;
        String precString = sufix == null ? "" : sufix;
        String wholeN = Integer.toString(_int);

        int stringLength = wholeN.length();

        int leftLength = stringLength - decimalPlaces;

        String leftString, rightString, commaString;

        leftString = leftLength < 1 ? "0" : wholeN.substring(0, leftLength);

        rightString = leftLength < 0 ? "0" + wholeN.charAt(0) : wholeN.substring(leftLength,stringLength);
//        rightString = leftLength < 0 ? "0" + wholeN.substring(0,1) : wholeN.substring(leftLength,stringLength);

        commaString = decimalPlaces == 0 ? "" : ",";

        return prevString + " " + leftString + commaString + rightString + " " + precString;
    }

    public static String intToStringTransformer(int _int, int decimalPlaces) {
        String wholeN = Integer.toString(_int);

        int stringLength = wholeN.length();

        int leftLength = stringLength - decimalPlaces;

        String leftString, rightString, commaString;

        leftString = leftLength < 1 ? "0" : wholeN.substring(0, leftLength);

        rightString = leftLength < 0 ? "0" + wholeN.charAt(0) : wholeN.substring(leftLength,stringLength);

        commaString = decimalPlaces == 0 ? "" : ",";

        return leftString + commaString + rightString;
    }

    public static String[] toStringArray(
            String fromString
    ) {
        if (fromString == null || fromString.length() < 2) {
            throw new IllegalStateException("Incompatible string: " + fromString);
        }
        return fromString.substring(1, fromString.length() - 1).split(", ");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String getStackTrace() {
        return Arrays.toString(Arrays.stream(Thread.currentThread().getStackTrace()).skip(3).toArray()).replace( ',', '\n' );
    }

    public static String getSingleStackTrace() {
        return Thread.currentThread().getStackTrace()[3].toString();
    }

    /**Includes comma (",") parsing*/
    public static double parseDouble(String aDouble) {
        String changed = aDouble;
        if (aDouble.contains(",")) {
            changed = aDouble.replace(',','.');
        }
        return Double.parseDouble(changed);
    }

//    /**@param digitPlace = If negative it will be decimals, if positive it will round up to the nearest 10*/
//    public static String parseDouble(double aDouble, int digitPlace) {
//        String res = Double.toString(ObjectUtils.round(aDouble, ObjectUtils.scale(1, 10, digitPlace)));
//        if (digitPlace < 0) {
//            int indexOfDot = res.indexOf(".");
//            int cropFrom = Math.min(indexOfDot + Math.abs(digitPlace) + 1, res.length());
//            return res.substring(0, cropFrom);
//        }
//        return res;
//    }

    private static final String space = "\\s+";
    public static String[] intoWords(String phrase) {
        return phrase.split(space);
    }
    public static String toPhrase(String... words) {
        return String.join(space, words);
    }
    public static String toPhrase(int start, int end, String ...words) {
        StringBuilder sb = new StringBuilder();
        int last = end - 1;
        assert last < words.length : "End index [" + end + "] greater than words length [" + words.length + "]";
        for (int i = start; i < last; i++) {
            sb.append(words[i]);
            sb.append(space);
        }
        sb.append(words[last]);
        return sb.toString();
    }

    /**
     * from A == 0
     * */
    public static char from(int alphabetIndex, boolean upperCase) {
        assert alphabetIndex >= 0 && alphabetIndex <= 25 : "Alphabet index must be between 0 and 25, inclusive.";
        char c = (char) ('a' + alphabetIndex);
        if (upperCase) {
            c -= 32;
        }
        return c;
    }

    public static String hexavigesimal(int number, boolean upperCase) {
        assert number > 0 : "Only positive numbers, not [" + number + "]";
        StringBuilder result = new StringBuilder();
        char base = upperCase ? 'A' : 'a';
        while (number > 0) {
            number--;
            result.insert(0, (char)(base + (number % 26)));
            number /= 26;
        }
        return result.toString();
    }
}
