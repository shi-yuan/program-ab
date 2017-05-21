package org.alicebot;

import org.alicebot.constant.MagicStrings;
import org.alicebot.util.CalendarUtils;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;

public class Utilities {

    /**
     * Excel sometimes adds mysterious formatting to CSV files.
     * This function tries to clean it up.
     *
     * @param line line from AIMLIF file
     * @return reformatted line
     */
    public static String fixCSV(String line) {
        while (line.endsWith(";")) line = line.substring(0, line.length() - 1);
        if (line.startsWith("\"")) line = line.substring(1, line.length());
        if (line.endsWith("\"")) line = line.substring(0, line.length() - 1);
        line = line.replaceAll("\"\"", "\"");
        return line;
    }

    public static String tagTrim(String xmlExpression, String tagName) {
        String stag = "<" + tagName + ">";
        String etag = "</" + tagName + ">";
        if (xmlExpression.length() >= (stag + etag).length()) {
            xmlExpression = xmlExpression.substring(stag.length());
            xmlExpression = xmlExpression.substring(0, xmlExpression.length() - etag.length());
        }
        return xmlExpression;
    }

    public static HashSet<String> stringSet(String... strings) {
        HashSet<String> set = new HashSet<>();
        Collections.addAll(set, strings);
        return set;
    }

    private static String getFileFromInputStream(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        //Read File Line By Line
        String contents = "";
        try {
            while ((strLine = br.readLine()) != null) {
                if (!strLine.startsWith(MagicStrings.text_comment_mark)) {
                    if (strLine.length() == 0) contents += "\n";
                    else contents += strLine + "\n";
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return contents.trim();
    }

    public static String getFile(String filename) {
        String contents = "";
        try {
            File file = new File(filename);
            if (file.exists()) {
                //System.out.println("Found file "+filename);
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                contents = getFileFromInputStream(fstream);
                fstream.close();
            }
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        //System.out.println("getFile: "+contents);
        return contents;
    }

    public static String getCopyright(Bot bot, String AIMLFilename) {
        StringBuilder copyright = new StringBuilder();
        String year = CalendarUtils.year();
        String date = CalendarUtils.date();
        try {
            copyright = new StringBuilder(getFile(bot.getConfigPath() + "/copyright.txt"));
            String[] splitCopyright = copyright.toString().split("\n");
            copyright = new StringBuilder();
            for (String aSplitCopyright : splitCopyright) {
                copyright.append("<!-- ").append(aSplitCopyright).append(" -->\n");
            }
            copyright = new StringBuilder(copyright.toString().replace("[url]", bot.getProperties().get("url")));
            copyright = new StringBuilder(copyright.toString().replace("[date]", date));
            copyright = new StringBuilder(copyright.toString().replace("[YYYY]", year));
            copyright = new StringBuilder(copyright.toString().replace("[version]", bot.getProperties().get("version")));
            copyright = new StringBuilder(copyright.toString().replace("[botname]", bot.getName().toUpperCase()));
            copyright = new StringBuilder(copyright.toString().replace("[filename]", AIMLFilename));
            copyright = new StringBuilder(copyright.toString().replace("[botmaster]", bot.getProperties().get("botmaster")));
            copyright = new StringBuilder(copyright.toString().replace("[organization]", bot.getProperties().get("organization")));
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        copyright.append("<!--  -->\n");
        //System.out.println("Copyright: "+copyright);
        return copyright.toString();
    }

    /**
     * Returns if a character is one of Chinese-Japanese-Korean characters.
     *
     * @param c the character to be tested
     * @return true if CJK, false otherwise
     */
    public static boolean isCharCJK(final char c) {
        if ((Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
                || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS)) {
            return true;
        }
        return false;
    }
}
