package net.afnf.and.utils;

import java.util.List;

public class MyStringUtlis {

    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }
        else if (str.length() == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public static boolean eqauls(String s1, String s2) {
        boolean b1 = isEmpty(s1);
        boolean b2 = isEmpty(s2);
        if (b1 && b2) {
            return true;
        }
        else if (b1 || b2) {
            return false;
        }
        return s1.equals(s2);
    }

    public static String normalize(String str) {
        if (isEmpty(str)) {
            return "";
        }

        StringBuilder sb = new StringBuilder(str.length());
        char[] charArray = str.toCharArray();
        for (char d : charArray) {
            if (d == ' ' || d == '\t' || d == '\n' || d == '\r') {
                // do nothing
            }
            else if (d == '　') {
                sb.append(" ");
            }
            else {
                sb.append(d);
            }
        }
        return sb.toString();
    }

    public static int count(String str, char c) {
        if (isEmpty(str)) {
            return 0;
        }

        int count = 0;
        char[] charArray = str.toCharArray();
        for (char d : charArray) {
            if (d == c) {
                count++;
            }
        }
        return count;
    }

    public static String replaceFirst(String str, String from, String to) {
        if (isEmpty(str) || isEmpty(from)) {
            return str;
        }
        return str.replaceFirst(from, to != null ? to : "");
    }

    public static String subStringBefore(String str, String sep) {
        if (isEmpty(str)) {
            return "";
        }

        int pos = str.indexOf(sep);
        if (pos == -1) {
            return str;
        }
        else {
            return str.substring(0, pos);
        }
    }

    public static int toInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        }
        catch (Throwable e) {
            return defaultValue;
        }
    }

    public static String toString(List<Double> list) {
        StringBuilder sb = new StringBuilder();
        for (Double d : list) {
            sb.append((int) (d * 1000));
            sb.append(",");
        }
        return sb.toString();
    }

    public static double round3(double d) {
        return ((int) (d * 1000)) / 1000.0;
    }

    public static String trimQuote(String str) {
        if (MyStringUtlis.isEmpty(str) == false && str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }
}
