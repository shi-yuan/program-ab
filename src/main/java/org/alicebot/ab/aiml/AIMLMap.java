package org.alicebot.ab.aiml;

import org.alicebot.ab.Bot;
import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

/**
 * implements AIML Map
 * <p>
 * The map element is used to reference a .map file, which attempts to match the map element's contents to one of its own properties,
 * returning the property's value. Maps are data structures that provide key-value pairs.
 */
public class AIMLMap extends HashMap<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AIMLMap.class);

    private static final String SPLIT_STR = ":";

    private String mapName;
    private Bot bot;

    /**
     * constructor to create a new AIML Map
     *
     * @param name the name of the map
     */
    public AIMLMap(String name, Bot bot) {
        super();
        this.mapName = name;
        this.bot = bot;
    }

    /**
     * return a map value given a key
     *
     * @param key the domain element
     * @return the range element or a string indicating the key was not found
     */
    @Override
    public String get(Object key) {
        String value;
        if (mapName.equals(Constants.map_successor)) {
            try {
                int number = Integer.parseInt((String) key);
                return String.valueOf(number + 1);
            } catch (Exception ex) {
                return Constants.default_map;
            }
        } else if (mapName.equals(Constants.map_predecessor)) {
            try {
                int number = Integer.parseInt((String) key);
                return String.valueOf(number - 1);
            } catch (Exception ex) {
                return Constants.default_map;
            }
        } else {
            value = super.get(key);
        }

        if (value == null) {
            value = Constants.default_map;
        }

        return value;
    }

    public void writeAIMLMap() throws IOException {
        LOG.info("Writing AIML Map {}", mapName);

        try (BufferedWriter out = new BufferedWriter(new FileWriter(getMapFile(bot.getMaps_path())))) {
            for (String p : keySet()) {
                p = p.trim();
                out.write(p + SPLIT_STR + get(p).trim());
                out.newLine();
            }
        }
    }

    /**
     * read an AIML map for a bot
     *
     * @param bot the bot associated with this map.
     */
    public int readAIMLMap(Bot bot) throws IOException {
        int cnt = 0;

        File file = getMapFile(bot.getMaps_path());

        LOG.info("Reading AIML Map {}", file);

        if (file.exists()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                cnt = readAIMLMapFromInputStream(stream);
            }
        } else {
            LOG.warn("{} not found", file);
        }

        return cnt;
    }

    private int readAIMLMapFromInputStream(InputStream in) throws IOException {
        int cnt = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String strLine;
            String[] splitLine;
            while ((strLine = br.readLine()) != null && strLine.length() > 0) {
                splitLine = strLine.split(SPLIT_STR);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("AIMLMap line={}", strLine);
                }

                if (splitLine.length >= 2) {
                    cnt++;

                    String key = splitLine[0].toUpperCase();
                    String value = splitLine[1];
                    // assume domain element is already normalized for speedier load
                    //key = bot.preProcessor.normalize(key).trim();
                    put(key, value);
                }
            }
        }

        return cnt;
    }

    private File getMapFile(String mapsPath) {
        return new File(mapsPath + "/" + mapName + ".txt");
    }
}
