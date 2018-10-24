package application.android.com.zhaozehong.utils;

import android.util.Log;


import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class HanziToPinyinUtils {

    public static String[] onHanyu2Pinyin(String chinese) {
        String[] arr = new String[3];

        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        StringBuffer jpbuffer = new StringBuffer();
        StringBuffer buffer = new StringBuffer();
        char[] nameChar = chinese.toCharArray();
        for (int i = 0; i < nameChar.length; i++) {
            if (nameChar[i] > 128) {
                try {
                    String s = PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat)[0];
                    buffer.append(s);
                    jpbuffer.append(char2digit(s.toCharArray()[0]));
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            }
        }

        arr[0] = buffer.toString();
        Log.e("zhaozehong", "[HanziToPinyinUtils][pinyin] " + arr[0]);
        arr[1] = pinyin2digits(arr[0]);
        Log.e("zhaozehong", "[HanziToPinyinUtils][quanping] " + arr[1]);
        arr[2] = pinyin2digits(jpbuffer.toString());
        Log.e("zhaozehong", "[HanziToPinyinUtils][jianpin] " + arr[2]);
        return arr;
    }

    public static String pinyin2digits(String pinyin) {
        StringBuilder digits = new StringBuilder();
        char[] pinyinChar = pinyin.toCharArray();
        for (char c : pinyinChar) {
            digits.append(char2digit(c));
        }
        return digits.toString();
    }

    public static String char2digit(char c) {
        if (c == 'a' || c == 'b' || c == 'c' || c == 'A' || c == 'B' || c == 'C') {
            return "2";
        } else if (c == 'd' || c == 'e' || c == 'f' || c == 'D' || c == 'E' || c == 'F') {
            return "3";
        } else if (c == 'g' || c == 'h' || c == 'i' || c == 'H' || c == 'G' || c == 'I') {
            return "4";
        } else if (c == 'j' || c == 'k' || c == 'l' || c == 'J' || c == 'K' || c == 'L') {
            return "5";
        } else if (c == 'm' || c == 'n' || c == 'o' || c == 'M' || c == 'N' || c == 'O') {
            return "6";
        } else if (c == 'p' || c == 'q' || c == 'r' || c == 's' || c == 'P' || c == 'Q' || c == 'R' || c == 'S') {
            return "7";
        } else if (c == 't' || c == 'u' || c == 'v' || c == 'T' || c == 'U' || c == 'V') {
            return "8";
        } else if (c == 'w' || c == 'x' || c == 'y' || c == 'z' || c == 'W' || c == 'X' || c == 'Y' || c == 'Z') {
            return "9";
        } else {
            return String.valueOf(c);
        }
    }
}
