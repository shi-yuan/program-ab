package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLMap;
import org.alicebot.ab.aiml.AIMLProcessor;
import org.alicebot.ab.aiml.AIMLSet;
import org.alicebot.ab.aiml.Category;
import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Class representing the AIML bot
 */
public class Bot {

    private static final Logger LOG = LoggerFactory.getLogger(Bot.class);

    private static final File[] EMPTY_FILES = new File[0];

    private Properties properties = new Properties();
    private PreProcessor preProcessor;
    private AIMLProcessor aimlProcessor;

    private Graphmaster brain;
    private Graphmaster learnfGraph;
    private Graphmaster learnGraph;

    private String name = Constants.default_bot_name;

    private Map<String, AIMLSet> setMap = new HashMap<>();
    private Map<String, AIMLMap> mapMap = new HashMap<>();
    private Set<String> pronounSet = new HashSet<>();

    private String aiml_path;
    private String config_path;
    private String sets_path;
    private String maps_path;

    public Properties getProperties() {
        return properties;
    }

    public PreProcessor getPreProcessor() {
        return preProcessor;
    }

    public AIMLProcessor getAimlProcessor() {
        return aimlProcessor;
    }

    public Graphmaster getBrain() {
        return brain;
    }

    public Graphmaster getLearnfGraph() {
        return learnfGraph;
    }

    public Graphmaster getLearnGraph() {
        return learnGraph;
    }

    public String getName() {
        return name;
    }

    public Map<String, AIMLSet> getSetMap() {
        return setMap;
    }

    public Map<String, AIMLMap> getMapMap() {
        return mapMap;
    }

    public Set<String> getPronounSet() {
        return pronounSet;
    }

    public String getAiml_path() {
        return aiml_path;
    }

    public String getConfig_path() {
        return config_path;
    }

    public String getSets_path() {
        return sets_path;
    }

    public String getMaps_path() {
        return maps_path;
    }

    /**
     * Constructor
     *
     * @param name name of bot
     */
    public Bot(String name, String aiml_path, String config_path, String sets_path, String maps_path, String props_path) throws Exception {
        this.name = name;
        this.aiml_path = aiml_path;
        this.config_path = config_path;
        this.sets_path = sets_path;
        this.maps_path = maps_path;

        this.brain = new Graphmaster(this, true);
        this.learnfGraph = new Graphmaster(this, "learnf", true);
        this.learnGraph = new Graphmaster(this, "learn", true);

        preProcessor = new PreProcessor(this);
        aimlProcessor = new AIMLProcessor();

        properties.getProperties(props_path);

        pronounSet = getPronouns();

        setMap.put(Constants.natural_number_set_name, new AIMLSet(Constants.natural_number_set_name, this));
        addAIMLSets();

        mapMap.put(Constants.map_successor, new AIMLMap(Constants.map_successor, this));
        mapMap.put(Constants.map_predecessor, new AIMLMap(Constants.map_predecessor, this));
        mapMap.put(Constants.map_singular, new AIMLMap(Constants.map_singular, this));
        mapMap.put(Constants.map_plural, new AIMLMap(Constants.map_plural, this));
        addAIMLMaps();

        addCategoriesFromAIML();

        Category b = new Category(0, "PROGRAM VERSION", "*", "*", Constants.program_name_version, "update.aiml");
        brain.addCategory(b);
        brain.nodeStats();
        learnfGraph.nodeStats();
    }

    private Set<String> getPronouns() throws IOException {
        Set<String> pronounSet = new HashSet<>();

        File file = new File(config_path + "/pronouns.txt");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String strLine;

                while ((strLine = br.readLine()) != null) {
                    if (!strLine.startsWith(Constants.text_comment_mark)) {
                        if (strLine.length() > 0) {
                            pronounSet.add(strLine.trim());
                        }
                    }
                }
            }
        }

        return pronounSet;
    }

    /**
     * Load all brain categories from AIML directory
     */
    private int addCategoriesFromAIML() throws Exception {
        int cnt = 0;

        File folder = new File(aiml_path);
        if (folder.exists()) {

            LOG.info("Loading AIML files from {}", aiml_path);

            String file;
            List<Category> moreCategories;
            for (File listOfFile : Optional.ofNullable(folder.listFiles()).orElse(EMPTY_FILES)) {
                if (listOfFile.isFile()) {
                    file = listOfFile.getName();
                    if (file.toLowerCase().endsWith(".aiml")) {
                        moreCategories = Optional.ofNullable(aimlProcessor.aimlToCategories(aiml_path, file)).orElse(new ArrayList<>());
                        addMoreCategories(file, moreCategories);
                        cnt += moreCategories.size();
                    }
                }
            }
        }

        LOG.info("Loaded {} categories", cnt);

        return cnt;
    }

    /**
     * add an array list of categories with a specific file name
     *
     * @param file           name of AIML file
     * @param moreCategories list of categories
     */
    private void addMoreCategories(String file, List<Category> moreCategories) {
        if (file.contains(Constants.learnf_aiml_file)) {
            for (Category c : moreCategories) {
                brain.addCategory(c);
                learnfGraph.addCategory(c);
            }
        } else {
            for (Category c : moreCategories) {
                brain.addCategory(c);
            }
        }
    }

    /**
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    public void writeAIMLFiles() throws IOException {
        Map<String, BufferedWriter> fileMap = new HashMap<>();
        Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);

        List<Category> brainCategories = brain.getCategories();
        brainCategories.sort(Category.CATEGORY_NUMBER_COMPARATOR);

        BufferedWriter bw;
        String fileName;
        for (Category c : brainCategories) {
            if (!c.getFilename().equals(Constants.null_aiml_file)) {
                fileName = c.getFilename();

                if (fileMap.containsKey(fileName)) {
                    bw = fileMap.get(fileName);
                } else {
                    bw = new BufferedWriter(new FileWriter(aiml_path + "/" + fileName));
                    bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<aiml>\n");
                    fileMap.put(fileName, bw);
                }

                bw.write(Category.categoryToAIML(c) + "\n");
            }
        }

        for (Map.Entry<String, BufferedWriter> entry : fileMap.entrySet()) {
            try (BufferedWriter bw2 = entry.getValue()) {
                bw2.write("</aiml>\n");
            }
        }
    }

    /**
     * Load all AIML Sets
     */
    private int addAIMLSets() throws IOException {
        int cnt = 0;

        File folder = new File(sets_path);
        if (folder.exists()) {

            LOG.info("Loading AIML Sets files from {}", sets_path);

            String file;
            AIMLSet aimlSet;
            String setName;
            for (File listOfFile : Optional.ofNullable(folder.listFiles()).orElse(EMPTY_FILES)) {
                if (listOfFile.isFile()) {
                    file = listOfFile.getName();
                    if (file.toLowerCase().endsWith(".txt")) {
                        setName = file.substring(0, file.length() - ".txt".length());

                        aimlSet = new AIMLSet(setName, this);
                        cnt += aimlSet.readAIMLSet(this);
                        setMap.put(setName, aimlSet);
                    }
                }
            }
        }

        LOG.info("Loaded {} sets", cnt);

        return cnt;
    }

    /**
     * Load all AIML Maps
     */
    private int addAIMLMaps() throws IOException {
        int cnt = 0;

        File folder = new File(maps_path);
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();

            LOG.info("Loading AIML Map files from {}", maps_path);

            String file;
            String mapName;
            AIMLMap aimlMap;
            for (File listOfFile : Optional.ofNullable(listOfFiles).orElse(EMPTY_FILES)) {
                if (listOfFile.isFile()) {
                    file = listOfFile.getName();
                    if (file.toLowerCase().endsWith(".txt")) {
                        mapName = file.substring(0, file.length() - ".txt".length());

                        aimlMap = new AIMLMap(mapName, this);
                        cnt += aimlMap.readAIMLMap(this);
                        mapMap.put(mapName, aimlMap);
                    }
                }
            }
        }

        LOG.info("Loaded {} maps", cnt);

        return cnt;
    }

    public void deleteLearnfCategories() {
        Nodemapper n;
        for (Category c : learnfGraph.getCategories()) {
            n = brain.findNode(c);
            if (n != null) {
                n.setCategory(null);
            }
        }
        learnfGraph = new Graphmaster(this, true);
    }

    public void deleteLearnCategories() {
        Nodemapper n;
        for (Category c : learnGraph.getCategories()) {
            n = brain.findNode(c);
            if (n != null) {
                n.setCategory(null);
            }
        }
        learnGraph = new Graphmaster(this, true);
    }
}
