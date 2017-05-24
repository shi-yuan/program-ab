package org.alicebot.ab;

import org.alicebot.ab.constant.Constants;

import java.io.*;
import java.util.HashMap;

/**
 * Bot Properties
 */
public class Properties extends HashMap<String, String> {

    /**
     * get the value of a bot property.
     *
     * @param key property name
     * @return property value or a string indicating the property is undefined
     */
    @Override
    public String get(Object key) {
        String result = super.get(key);
        if (result == null) {
            result = Constants.default_property;
        }

        return result;
    }

    /**
     * Read bot properties from a file.
     *
     * @param filename file containing bot properties
     */
    public void getProperties(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                int index;
                String strLine;
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
