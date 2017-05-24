package org.alicebot.ab;

import org.alicebot.ab.constant.Constants;

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
    @Override
    public String put(String key, String value) {
        if (key.equals("topic") && value.length() == 0) {
            value = Constants.default_get;
        }
        if (value.equals(Constants.too_much_recursion)) {
            value = Constants.default_list_item;
        }

        return super.put(key, value);
    }

    /**
     * get a predicate value
     *
     * @param key predicate name
     * @return predicate value
     */
    @Override
    public String get(Object key) {
        String result = super.get(key);
        if (result == null) {
            result = Constants.default_get;
        }

        return result;
    }

    /**
     * read predicate defaults from a file
     *
     * @param filename name of file
     */
    public void getPredicateDefaults(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String strLine;
                int index;
                while ((strLine = br.readLine()) != null) {
                    if (strLine.contains(":")) {
                        index = strLine.indexOf(":");
                        put(strLine.substring(0, index), strLine.substring(index + 1));
                    }
                }
            }
        }
    }
}
