package org.alicebot;

import org.alicebot.aiml.AIMLMap;
import org.alicebot.aiml.AIMLProcessor;
import org.alicebot.aiml.AIMLSet;
import org.alicebot.aiml.Category;
import org.alicebot.constant.MagicBooleans;
import org.alicebot.constant.MagicStrings;
import org.alicebot.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Class representing the AIML bot
 */
public class Bot {

    private static final Logger LOG = LoggerFactory.getLogger(Bot.class);

    private HashMap<String, AIMLSet> setMap = new HashMap<>();
    private HashMap<String, AIMLMap> mapMap = new HashMap<>();
    private HashSet<String> pronounSet = new HashSet<>();

    private String aimlPath;
    private String configPath;
    private String logPath;
    private String setsPath;
    private String mapsPath;

    private String name;

    private Graphmaster brain;
    private Graphmaster learnfGraph;
    private Graphmaster learnGraph;

    private PreProcessor preProcessor;

    private Properties properties;

    public Bot(String aimlPath, String configPath, String logPath, String setsPath, String mapsPath, String name) {
        this.aimlPath = aimlPath;
        this.configPath = configPath;
        this.logPath = logPath;
        this.setsPath = setsPath;
        this.mapsPath = mapsPath;

        this.name = name;

        this.brain = new Graphmaster(this);
        this.learnfGraph = new Graphmaster(this, "learnf");
        this.learnGraph = new Graphmaster(this, "learn");

        this.preProcessor = new PreProcessor(this);

        this.properties = new Properties();
        addProperties();

        int cnt = addAIMLSets();

        LOG.debug("Loaded {} set elements.", cnt);

        cnt = addAIMLMaps();

        LOG.debug("Loaded {} map elements.", cnt);

        this.pronounSet = getPronouns();
        AIMLSet number = new AIMLSet(MagicStrings.natural_number_set_name, this);
        setMap.put(MagicStrings.natural_number_set_name, number);
        AIMLMap successor = new AIMLMap(MagicStrings.map_successor, this);
        mapMap.put(MagicStrings.map_successor, successor);
        AIMLMap predecessor = new AIMLMap(MagicStrings.map_predecessor, this);
        mapMap.put(MagicStrings.map_predecessor, predecessor);
        AIMLMap singular = new AIMLMap(MagicStrings.map_singular, this);
        mapMap.put(MagicStrings.map_singular, singular);
        AIMLMap plural = new AIMLMap(MagicStrings.map_plural, this);
        mapMap.put(MagicStrings.map_plural, plural);
        //System.out.println("setMap = "+setMap);

        addCategoriesFromAIML();

        Category b = new Category(0, "PROGRAM VERSION", "*", "*", MagicStrings.program_name_version, "update.aiml");
        brain.addCategory(b);
        brain.nodeStats();
        learnfGraph.nodeStats();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAimlPath() {
        return aimlPath;
    }

    public void setAimlPath(String aimlPath) {
        this.aimlPath = aimlPath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getSetsPath() {
        return setsPath;
    }

    public void setSetsPath(String setsPath) {
        this.setsPath = setsPath;
    }

    public String getMapsPath() {
        return mapsPath;
    }

    public void setMapsPath(String mapsPath) {
        this.mapsPath = mapsPath;
    }

    public Properties getProperties() {
        return properties;
    }

    public HashMap<String, AIMLSet> getSetMap() {
        return setMap;
    }

    public PreProcessor getPreProcessor() {
        return preProcessor;
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

    public HashMap<String, AIMLMap> getMapMap() {
        return mapMap;
    }

    public HashSet<String> getPronounSet() {
        return pronounSet;
    }

    private HashSet<String> getPronouns() {
        HashSet<String> pronounSet = new HashSet<>();
        String pronouns = Utilities.getFile(configPath + "/pronouns.txt");
        String[] splitPronouns = pronouns.split("\n");
        for (String splitPronoun : splitPronouns) {
            String p = splitPronoun.trim();
            if (p.length() > 0) pronounSet.add(p);
        }
        if (MagicBooleans.trace_mode) System.out.println("Read pronouns: " + pronounSet);
        return pronounSet;
    }

    /**
     * add an array list of categories with a specific file name
     *
     * @param file           name of AIML file
     * @param moreCategories list of categories
     */
    private void addMoreCategories(String file, ArrayList<Category> moreCategories) {
        if (file.contains(MagicStrings.deleted_aiml_file)) {
           /* for (Category c : moreCategories) {
                //System.out.println("Delete "+c.getPattern());
                deletedGraph.addCategory(c);
            }*/

        } else if (file.contains(MagicStrings.learnf_aiml_file)) {
            if (MagicBooleans.trace_mode) System.out.println("Reading Learnf file");
            for (Category c : moreCategories) {
                brain.addCategory(c);
                learnfGraph.addCategory(c);
                //patternGraph.addCategory(c);
            }
            //this.categories.addAll(moreCategories);
        } else {
            for (Category c : moreCategories) {
                //System.out.println("Brain size="+brain.root.size());
                //brain.printgraph();
                brain.addCategory(c);
                //patternGraph.addCategory(c);
                //brain.printgraph();
            }
            //this.categories.addAll(moreCategories);
        }
    }

    /**
     * Load all brain categories from AIML directory
     */
    private int addCategoriesFromAIML() {
        org.alicebot.Timer timer = new org.alicebot.Timer();
        timer.start();
        int cnt = 0;
        try {
            // Directory path here
            String file;
            File folder = new File(aimlPath);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                if (MagicBooleans.trace_mode) System.out.println("Loading AIML files from " + aimlPath);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".aiml") || file.endsWith(".AIML")) {
                            if (MagicBooleans.trace_mode) System.out.println(file);
                            try {
                                ArrayList<Category> moreCategories = AIMLProcessor.AIMLToCategories(aimlPath, file);
                                addMoreCategories(file, moreCategories);
                                cnt += moreCategories.size();
                            } catch (Exception iex) {
                                System.out.println("Problem loading " + file);
                                iex.printStackTrace();
                            }
                        }
                    }
                }
            } else System.out.println("addCategoriesFromAIML: " + aimlPath + " does not exist.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (MagicBooleans.trace_mode)
            System.out.println("Loaded " + cnt + " categories in " + timer.elapsedTimeSecs() + " sec");
        return cnt;
    }

    /**
     * csv2aiml
     * <p>
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    private void writeAIMLFiles() {
        if (MagicBooleans.trace_mode) System.out.println("writeAIMLFiles");
        HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
        Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        //b = new Category(0, "PROGRAM VERSION", "*", "*", MagicStrings.program_name_version, "update.aiml");
        //brain.addCategory(b);
        ArrayList<Category> brainCategories = brain.getCategories();
        brainCategories.sort(Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {

            if (!c.getFilename().equals(MagicStrings.null_aiml_file))
                try {
                    //System.out.println("Writing "+c.getCategoryNumber()+" "+c.inputThatTopic());
                    BufferedWriter bw;
                    String fileName = c.getFilename();
                    if (fileMap.containsKey(fileName)) bw = fileMap.get(fileName);
                    else {
                        String copyright = Utilities.getCopyright(this, fileName);
                        bw = new BufferedWriter(new FileWriter(aimlPath + "/" + fileName));
                        fileMap.put(fileName, bw);
                        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                                "<aiml>\n");
                        bw.write(copyright);
                        //bw.newLine();
                    }
                    bw.write(Category.categoryToAIML(c) + "\n");
                    //bw.newLine();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
        }
        Set<String> set = fileMap.keySet();
        for (String aSet : set) {
            BufferedWriter bw = fileMap.get(aSet);
            //Close the bw
            try {
                if (bw != null) {
                    bw.write("</aiml>\n");
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            }

        }
        File dir = new File(aimlPath);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * load bot properties
     */
    private void addProperties() {
        try {
            properties.getProperties(configPath + "/properties.txt");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load all AIML Sets
     */
    private int addAIMLSets() {
        int cnt = 0;
        org.alicebot.Timer timer = new org.alicebot.Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(setsPath);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                if (MagicBooleans.trace_mode) System.out.println("Loading AIML Sets files from " + setsPath);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            if (MagicBooleans.trace_mode) System.out.println(file);
                            String setName = file.substring(0, file.length() - ".txt".length());
                            if (MagicBooleans.trace_mode) System.out.println("Read AIML Set " + setName);
                            AIMLSet aimlSet = new AIMLSet(setName, this);
                            cnt += aimlSet.readAIMLSet(this);
                            setMap.put(setName, aimlSet);
                        }
                    }
                }
            } else {
                System.out.println("addAIMLSets: " + setsPath + " does not exist.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cnt;
    }

    /**
     * Load all AIML Maps
     */
    private int addAIMLMaps() {
        int cnt = 0;
        org.alicebot.Timer timer = new org.alicebot.Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(mapsPath);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                if (MagicBooleans.trace_mode) System.out.println("Loading AIML Map files from " + mapsPath);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            if (MagicBooleans.trace_mode) System.out.println(file);
                            String mapName = file.substring(0, file.length() - ".txt".length());
                            if (MagicBooleans.trace_mode) System.out.println("Read AIML Map " + mapName);
                            AIMLMap aimlMap = new AIMLMap(mapName, this);
                            cnt += aimlMap.readAIMLMap(this);
                            mapMap.put(mapName, aimlMap);
                        }
                    }
                }
            } else System.out.println("addAIMLMaps: " + mapsPath + " does not exist.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return cnt;
    }

    public void deleteLearnfCategories() {
        ArrayList<Category> learnfCategories = learnfGraph.getCategories();
        for (Category c : learnfCategories) {
            Nodemapper n = brain.findNode(c);
            System.out.println("Found node " + n + " for " + c.inputThatTopic());
            if (n != null) n.category = null;
        }
        learnfGraph = new Graphmaster(this);
    }

    public void deleteLearnCategories() {
        ArrayList<Category> learnCategories = learnGraph.getCategories();
        for (Category c : learnCategories) {
            Nodemapper n = brain.findNode(c);
            System.out.println("Found node " + n + " for " + c.inputThatTopic());
            if (n != null) n.category = null;
        }
        learnGraph = new Graphmaster(this);
    }

    /**
     * check Graphmaster for shadowed categories
     */
    public void shadowChecker() {
        shadowChecker(brain.root);
    }

    /**
     * traverse graph and test all categories found in leaf nodes for shadows
     *
     * @param node
     */
    private void shadowChecker(Nodemapper node) {
        if (NodemapperOperator.isLeaf(node)) {
            String input = node.category.getPattern();
            input = brain.replaceBotProperties(input);
            input = input.replace("*", "XXX").replace("_", "XXX").replace("^", "").replace("#", "");
            String that = node.category.getThat().replace("*", "XXX").replace("_", "XXX").replace("^", "").replace("#", "");
            String topic = node.category.getTopic().replace("*", "XXX").replace("_", "XXX").replace("^", "").replace("#", "");
            input = instantiateSets(input);
            System.out.println("shadowChecker: input=" + input);
            Nodemapper match = brain.match(input, that, topic);
            if (match != node) {
                System.out.println("" + Graphmaster.inputThatTopic(input, that, topic));
                System.out.println("MATCHED:     " + match.category.inputThatTopic());
                System.out.println("SHOULD MATCH:" + node.category.inputThatTopic());
            }
        } else {
            for (String key : NodemapperOperator.keySet(node)) {
                shadowChecker(NodemapperOperator.get(node, key));
            }
        }
    }

    private String instantiateSets(String pattern) {
        String[] splitPattern = pattern.split(" ");
        StringBuilder patternBuilder = new StringBuilder();
        for (String x : splitPattern) {
            if (x.startsWith("<SET>")) {
                String setName = AIMLProcessor.trimTag(x, "SET");
                AIMLSet set = setMap.get(setName);
                if (set != null) x = "FOUNDITEM";
                else x = "NOTFOUND";
            }
            patternBuilder.append(" ").append(x);
        }
        pattern = patternBuilder.toString();
        return pattern.trim();
    }
}
