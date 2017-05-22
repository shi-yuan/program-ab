package org.alicebot.aiml;

import org.alicebot.Bot;
import org.alicebot.Inflector;
import org.alicebot.constant.MagicBooleans;
import org.alicebot.constant.MagicStrings;

import java.io.*;
import java.util.HashMap;

/**
 * implements AIML Map
 * <p>
 * A map is a function from one string set to another.
 * Elements of the domain are called keys and elements of the range are called values.
 */
public class AIMLMap extends HashMap<String, String> {
    private String mapName;
    private boolean isExternal = false;
    private Inflector inflector = new Inflector();
    Bot bot;

    /**
     * constructor to create a new AIML Map
     *
     * @param name the name of the map
     */
    public AIMLMap(String name, Bot bot) {
        super();
        this.bot = bot;
        this.mapName = name;
    }

    /**
     * return a map value given a key
     *
     * @param key the domain element
     * @return the range element or a string indicating the key was not found
     */
    public String get(String key) {
        String value;
        if (mapName.equals(MagicStrings.map_successor)) {
            try {
                int number = Integer.parseInt(key);
                return String.valueOf(number + 1);
            } catch (Exception ex) {
                return MagicStrings.default_map;
            }
        } else if (mapName.equals(MagicStrings.map_predecessor)) {
            try {
                int number = Integer.parseInt(key);
                return String.valueOf(number - 1);
            } catch (Exception ex) {
                return MagicStrings.default_map;
            }
        } else if (mapName.equals("singular")) {
            return inflector.singularize(key).toLowerCase();
        } else if (mapName.equals("plural")) {
            return inflector.pluralize(key).toLowerCase();
        } else if (isExternal && MagicBooleans.enable_external_sets) {
            //String[] split = key.split(" ");
            String query = mapName.toUpperCase() + " " + key;
            String response = null;//Sraix.sraix(null, query, MagicStrings.default_map, null, host, botid, null, "0");
            System.out.println("External " + mapName + "(" + key + ")=" + response);
            value = response;
        } else value = super.get(key);
        if (value == null) value = MagicStrings.default_map;
        //System.out.println("AIMLMap get "+key+"="+value);
        return value;
    }

    /**
     * put a new key, value pair into the map.
     *
     * @param key   the domain element
     * @param value the range element
     * @return the value
     */
    public String put(String key, String value) {
        //System.out.println("AIMLMap put "+key+"="+value);
        return super.put(key, value);
    }

    private int readAIMLMapFromInputStream(InputStream in, Bot bot) {
        int cnt = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        //Read File Line By Line
        try {
            while ((strLine = br.readLine()) != null && strLine.length() > 0) {
                String[] splitLine = strLine.split(":");
                //System.out.println("AIMLMap line="+strLine);
                if (splitLine.length >= 2) {
                    cnt++;
                    if (strLine.startsWith(MagicStrings.remote_map_key)) {
                        if (splitLine.length >= 3) {
                            String host = splitLine[1];
                            String botid = splitLine[2];
                            isExternal = true;
                            System.out.println("Created external map at " + host + " " + botid);
                        }
                    } else {
                        String key = splitLine[0].toUpperCase();
                        String value = splitLine[1];
                        // assume domain element is already normalized for speedier load
                        //key = bot.preProcessor.normalize(key).trim();
                        put(key, value);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cnt;
    }

    /**
     * read an AIML map for a bot
     *
     * @param bot the bot associated with this map.
     */
    public int readAIMLMap(Bot bot) {
        int cnt = 0;
        if (MagicBooleans.trace_mode) System.out.println("Reading AIML Map " + bot.getMapsPath() + "/" + mapName + ".txt");
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(bot.getMapsPath() + "/" + mapName + ".txt");
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(bot.getMapsPath() + "/" + mapName + ".txt");
                // Get the object
                cnt = readAIMLMapFromInputStream(fstream, bot);
                fstream.close();
            } else System.out.println(bot.getMapsPath() + "/" + mapName + ".txt not found");
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return cnt;

    }

}