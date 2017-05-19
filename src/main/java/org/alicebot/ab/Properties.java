package org.alicebot.ab;

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
    public String get(String key) {
        String result = super.get(key);
        if (result == null) return MagicStrings.default_property;
        else return result;
    }

    /**
     * Read bot properties from an input stream.
     *
     * @param in Input stream
     */
    private int getPropertiesFromInputStream(InputStream in) {
        int cnt = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        //Read File Line By Line
        try {
            while ((strLine = br.readLine()) != null) {
                if (strLine.contains(":")) {
                    String property = strLine.substring(0, strLine.indexOf(":"));
                    String value = strLine.substring(strLine.indexOf(":") + 1);
                    put(property, value);
                    cnt++;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cnt;
    }

    /**
     * Read bot properties from a file.
     *
     * @param filename file containing bot properties
     */
    public int getProperties(String filename) {
        int cnt = 0;
        if (MagicBooleans.trace_mode) System.out.println("Get Properties: " + filename);
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(filename);
            if (file.exists()) {
                if (MagicBooleans.trace_mode) System.out.println("Exists: " + filename);
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                cnt = getPropertiesFromInputStream(fstream);
                //Close the input stream
                fstream.close();
            }
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return cnt;
    }
}
