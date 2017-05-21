package org.alicebot;

import org.alicebot.constant.MagicBooleans;
import org.alicebot.constant.MagicStrings;
import org.alicebot.util.JapaneseUtils;

import java.io.*;
import java.util.HashMap;

/**
 * Manage client predicates
 */
public class Predicates extends HashMap<String, String> {

    /**
     * save a predicate value
     *
     * @param key   predicate name
     * @param value predicate value
     * @return predicate value
     */
    public String put(String key, String value) {
        //MagicBooleans.trace("predicates.put(key: " + key + ", value: " + value + ")");
        if (MagicBooleans.jp_tokenize) {
            if (key.equals("topic")) value = JapaneseUtils.tokenizeSentence(value);
        }
        if (key.equals("topic") && value.length() == 0) value = MagicStrings.default_get;
        if (value.equals(MagicStrings.too_much_recursion)) value = MagicStrings.default_list_item;
        // MagicBooleans.trace("Setting predicate key: " + key + " to value: " + value);
        //MagicBooleans.trace("in predicates.put, returning: " + result);
        return super.put(key, value);
    }

    /**
     * get a predicate value
     *
     * @param key predicate name
     * @return predicate value
     */
    public String get(String key) {
        //MagicBooleans.trace("predicates.get(key: " + key + ")");
        String result = super.get(key);
        if (result == null) result = MagicStrings.default_get;
        //MagicBooleans.trace("in predicates.get, returning: " + result);
        return result;
    }

    /**
     * Read predicate default values from an input stream
     *
     * @param in input stream
     */
    private void getPredicateDefaultsFromInputStream(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        try {
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                if (strLine.contains(":")) {
                    String property = strLine.substring(0, strLine.indexOf(":"));
                    String value = strLine.substring(strLine.indexOf(":") + 1);
                    put(property, value);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * read predicate defaults from a file
     *
     * @param filename name of file
     */
    public void getPredicateDefaults(String filename) {
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                getPredicateDefaultsFromInputStream(fstream);
                fstream.close();
            }
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }
}
