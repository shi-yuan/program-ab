package org.alicebot.ab;

import org.alicebot.ab.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

public class AB {

    public static final Logger LOG = LoggerFactory.getLogger(AB.class);

    private static int leafPatternCnt = 0;
    private static int starPatternCnt = 0;

    private boolean offer_alice_responses = true;

    private String logfile = MagicStrings.root_path + "/data/" + MagicStrings.ab_sample_file; //normal.txt";

    private int runCompletedCnt;
    public Bot bot;
    public Bot alice;
    private AIMLSet passed;
    private AIMLSet testSet;

    private final Graphmaster inputGraph;
    private final Graphmaster patternGraph;
    private final Graphmaster deletedGraph;
    private ArrayList<Category> suggestedCategories;
    public static int limit = 500000;

    public AB(Bot bot, String sampleFile) {
        MagicStrings.ab_sample_file = sampleFile;
        logfile = MagicStrings.root_path + "/data/" + MagicStrings.ab_sample_file;
        System.out.println("AB with sample file " + logfile);
        this.bot = bot;
        this.inputGraph = new Graphmaster(bot, "input");
        this.deletedGraph = new Graphmaster(bot, "deleted");
        this.patternGraph = new Graphmaster(bot, "pattern");

        for (Category c : bot.brain.getCategories()) {
            patternGraph.addCategory(c);
        }

        this.suggestedCategories = new ArrayList<>();
        passed = new AIMLSet("passed", bot);
        testSet = new AIMLSet("1000", bot);

        readDeletedIFCategories();
    }

    /**
     * Calculates the botmaster's productivity rate in
     * categories/sec when using Pattern Suggestor to create content.
     *
     * @param runCompletedCnt number of categories completed in this run
     * @param timer           tells elapsed time in ms
     * @see AB
     */
    private void productivity(int runCompletedCnt, Timer timer) {
        float time = timer.elapsedTimeMins();
        LOG.debug("Completed {} in {} min. Productivity {} cat/min", runCompletedCnt, time, (float) runCompletedCnt / time);
    }

    private void readDeletedIFCategories() {
        bot.readCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
        LOG.debug("--- DELETED CATEGORIES -- read {} deleted categories", deletedGraph.getCategories().size());
    }

    private void writeDeletedIFCategories() {
        LOG.debug("--- DELETED CATEGORIES -- write");
        bot.writeCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
        LOG.debug("--- DELETED CATEGORIES -- write {} deleted categories", deletedGraph.getCategories().size());
    }

    /**
     * saves a new AIML category and increments runCompletedCnt
     *
     * @param pattern  the category's pattern (that and topic = *)
     * @param template the category's template
     * @param filename the filename for the category.
     */
    private void saveCategory(String pattern, String template, String filename) {
        String that = "*";
        String topic = "*";
        Category c = new Category(0, pattern, that, topic, template, filename);

        if (c.validate()) {
            bot.brain.addCategory(c);
            // bot.categories.add(c);
            bot.writeAIMLIFFiles();
            runCompletedCnt++;
        } else {
            LOG.debug("Invalid Category {}", c.validationMessage);
        }
    }

    /**
     * mark a category as deleted
     *
     * @param c the category
     */
    private void deleteCategory(Category c) {
        c.setFilename(MagicStrings.deleted_aiml_file);
        c.setTemplate(MagicStrings.deleted_template);
        deletedGraph.addCategory(c);
        LOG.debug("--- bot.writeDeletedIFCategories()");
        writeDeletedIFCategories();
    }

    public void abwq() throws IOException {
        Timer timer = new Timer();
        timer.start();
        classifyInputs(logfile);
        LOG.debug("{} classifying inputs", timer.elapsedTimeSecs());
        bot.writeQuit();
    }

    /**
     * read sample inputs from filename, turn them into Paths, and
     * add them to the graph.
     *
     * @param filename file containing sample inputs
     */
    private void graphInputs(String filename) throws IOException {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
            // Read File Line By Line
            String strLine;
            while ((strLine = br.readLine()) != null && count < limit) {
                // strLine = preProcessor.normalize(strLine);
                Category c = new Category(0, strLine, "*", "*", "nothing", MagicStrings.unknown_aiml_file);
                Nodemapper node = inputGraph.findNode(c);
                if (node == null) {
                    inputGraph.addCategory(c);
                    c.incrementActivationCnt();
                } else {
                    node.category.incrementActivationCnt();
                }
                count++;
            }
        }
    }

    /**
     * find suggested patterns in a graph of inputs
     */
    private void findPatterns() {
        findPatterns(inputGraph.root, "");
        LOG.debug("{} Leaf Patterns {} Star Patterns", leafPatternCnt, starPatternCnt);
    }

    /**
     * find patterns recursively
     *
     * @param node                    current graph node
     * @param partialPatternThatTopic partial pattern path
     */
    private void findPatterns(Nodemapper node, String partialPatternThatTopic) {
        if (NodemapperOperator.isLeaf(node)) {
            LOG.debug("LEAF: {}. {}", node.category.getActivationCnt(), partialPatternThatTopic);
            if (node.category.getActivationCnt() > MagicNumbers.node_activation_cnt) {
                // Start writing to the output stream
                LOG.debug("LEAF: {}. {} {}", node.category.getActivationCnt(), partialPatternThatTopic, node.shortCut);
                leafPatternCnt++;

                String categoryPatternThatTopic;
                if (node.shortCut) {
                    //System.out.println("Partial patternThatTopic = "+partialPatternThatTopic);
                    categoryPatternThatTopic = partialPatternThatTopic + " <THAT> * <TOPIC> *";
                } else {
                    categoryPatternThatTopic = partialPatternThatTopic;
                }

                Category c = new Category(0, categoryPatternThatTopic, MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                //if (brain.existsCategory(c)) System.out.println(c.inputThatTopic()+" Exists");
                //if (deleted.existsCategory(c)) System.out.println(c.inputThatTopic()+ " Deleted");
                if (!bot.brain.existsCategory(c) && !deletedGraph.existsCategory(c)/* && !unfinishedGraph.existsCategory(c)*/) {
                    patternGraph.addCategory(c);
                    suggestedCategories.add(c);
                }
            }
        }

        if (NodemapperOperator.size(node) > MagicNumbers.node_size) {
            //System.out.println("STAR: "+NodemapperOperator.size(node)+". "+partialPatternThatTopic+" * <that> * <topic> *");
            starPatternCnt++;

            Category c = new Category(0, partialPatternThatTopic + " * <THAT> * <TOPIC> *", MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
            //if (brain.existsCategory(c)) System.out.println(c.inputThatTopic()+" Exists");
            //if (deleted.existsCategory(c)) System.out.println(c.inputThatTopic()+ " Deleted");
            if (!bot.brain.existsCategory(c) && !deletedGraph.existsCategory(c)/* && !unfinishedGraph.existsCategory(c)*/) {
                patternGraph.addCategory(c);
                suggestedCategories.add(c);
            }
        }

        for (String key : NodemapperOperator.keySet(node)) {
            Nodemapper value = NodemapperOperator.get(node, key);
            findPatterns(value, partialPatternThatTopic + " " + key);
        }
    }

    /**
     * classify inputs into matching categories
     *
     * @param filename file containing sample normalized inputs
     */
    private void classifyInputs(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
            String strLine;
            int count = 0;
            while ((strLine = br.readLine()) != null && count < limit) {
                // Print the content on the console
                //System.out.println("Classifying "+strLine);

                if (strLine.startsWith("Human: ")) {
                    strLine = strLine.substring("Human: ".length(), strLine.length());
                }

                String sentences[] = bot.preProcessor.sentenceSplit(strLine);

                for (String sentence : sentences) {
                    if (sentence.length() > 0) {
                        Nodemapper match = patternGraph.match(sentence, "unknown", "unknown");

                        if (match == null) {
                            System.out.println(sentence + " null match");
                        } else {
                            match.category.incrementActivationCnt();
                            //System.out.println(count+". "+sentence+" matched "+match.category.inputThatTopic());
                        }
                        count += 1;
                        if (count % 10000 == 0) LOG.debug("{}", count);
                    }
                }
            }

            LOG.debug("Finished classifying {} inputs", count);
        }
    }

    /**
     * magically suggests new patterns for a bot.
     * Reads an input file of sample data called logFile.
     * Builds a graph of all the inputs.
     * Finds new patterns in the graph that are not already in the bot.
     * Classifies input log into those new patterns.
     */
    public void ab() throws IOException {
        String logFile = logfile;
        MagicBooleans.trace_mode = false;
        MagicBooleans.enable_external_sets = false;
        if (offer_alice_responses) alice = new Bot("alice");
        Timer timer = new Timer();
        bot.brain.nodeStats();
        if (bot.brain.getCategories().size() < MagicNumbers.brain_print_size) bot.brain.printgraph();
        timer.start();
        System.out.println("Graphing inputs");
        graphInputs(logFile);
        System.out.println(timer.elapsedTimeSecs() + " seconds Graphing inputs");
        inputGraph.nodeStats();
        if (inputGraph.getCategories().size() < MagicNumbers.brain_print_size) inputGraph.printgraph();
        //bot.inputGraph.printgraph();
        timer.start();
        System.out.println("Finding Patterns");
        findPatterns();
        System.out.println(suggestedCategories.size() + " suggested categories");
        System.out.println(timer.elapsedTimeSecs() + " seconds finding patterns");
        timer.start();
        patternGraph.nodeStats();
        if (patternGraph.getCategories().size() < MagicNumbers.brain_print_size) patternGraph.printgraph();
        System.out.println("Classifying Inputs from " + logFile);
        classifyInputs(logFile);
        System.out.println(timer.elapsedTimeSecs() + " classifying inputs");
    }

    private ArrayList<Category> nonZeroActivationCount(ArrayList<Category> suggestedCategories) {
        ArrayList<Category> result = new ArrayList<Category>();
        for (Category c : suggestedCategories) {
            if (c.getActivationCnt() > 0) result.add(c);
            // else     System.out.println("["+c.getActivationCnt()+"] "+c.inputThatTopic());
        }

        return result;
    }

    /**
     * train the bot through a terminal interaction
     */
    void terminalInteraction() {
        boolean firstInteraction = true;
        String alicetemplate = null;
        Timer timer;

        // if (sort_mode)
        suggestedCategories.sort(Category.ACTIVATION_COMPARATOR);
        ArrayList<Category> topSuggestCategories = new ArrayList<Category>();
        for (int i = 0; i < 10000 && i < suggestedCategories.size(); i++) {
            topSuggestCategories.add(suggestedCategories.get(i));
        }
        suggestedCategories = topSuggestCategories;
        Collections.shuffle(suggestedCategories);
        timer = new Timer();
        timer.start();
        runCompletedCnt = 0;
        ArrayList<Category> filteredAtomicCategories = new ArrayList<>();
        ArrayList<Category> filteredWildCategories = new ArrayList<>();
        for (Category c : suggestedCategories)
            if (!c.getPattern().contains("*")) filteredAtomicCategories.add(c);
            else filteredWildCategories.add(c);
        ArrayList<Category> browserCategories;
        browserCategories = suggestedCategories;
        // System.out.println(filteredAtomicCategories.size()+" filtered suggested categories");
        browserCategories = nonZeroActivationCount(browserCategories);
        for (Category c : browserCategories) {
            try {
                ArrayList<String> samples = new ArrayList<>(c.getMatches(bot));
                Collections.shuffle(samples);
                int sampleSize = Math.min(MagicNumbers.displayed_input_sample_size, c.getMatches(bot).size());
                for (int i = 0; i < sampleSize; i++) {
                    System.out.println("" + samples.get(i));
                }
                System.out.println("[" + c.getActivationCnt() + "] " + c.inputThatTopic());
                Nodemapper node;
                if (offer_alice_responses) {
                    node = alice.brain.findNode(c);
                    if (node != null) {
                        alicetemplate = node.category.getTemplate();
                        String displayAliceTemplate = alicetemplate;
                        displayAliceTemplate = displayAliceTemplate.replace("\n", " ");
                        if (displayAliceTemplate.length() > 200)
                            displayAliceTemplate = displayAliceTemplate.substring(0, 200);
                        System.out.println("ALICE: " + displayAliceTemplate);
                    } else alicetemplate = null;
                }

                String textLine = "" + IOUtils.readInputTextLine();
                if (firstInteraction) {
                    timer.start();
                    firstInteraction = false;
                }
                productivity(runCompletedCnt, timer);
                terminalInteractionStep(bot, "", textLine, c, alicetemplate);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Returning to Category Browser");
            }
        }
        System.out.println("No more samples");
        bot.writeAIMLFiles();
        bot.writeAIMLIFFiles();
    }

    /**
     * process one step of the terminal interaction
     *
     * @param bot      the bot being trained.
     * @param request  used when this routine is called by benchmark testSuite
     * @param textLine response typed by the botmaster
     * @param c        AIML category selected
     */
    private void terminalInteractionStep(Bot bot, String request, String textLine, Category c, String alicetemplate) {
        String template = null;
        if (textLine.contains("<pattern>") && textLine.contains("</pattern>")) {
            int index = textLine.indexOf("<pattern>") + "<pattern>".length();
            int jndex = textLine.indexOf("</pattern>");
            int kndex = jndex + "</pattern>".length();
            if (index < jndex) {
                String pattern = textLine.substring(index, jndex);
                c.setPattern(pattern);
                textLine = textLine.substring(kndex, textLine.length());
                System.out.println("Got pattern = " + pattern + " template = " + textLine);
            }
        }
        String botThinks = "";
        String[] pronouns = {"he", "she", "it", "we", "they"};
        for (String p : pronouns) {
            if (textLine.contains("<" + p + ">")) {
                textLine = textLine.replace("<" + p + ">", "");
                botThinks = "<think><set name=\"" + p + "\"><set name=\"topic\"><star/></set></set></think>";
            }
        }
        if (textLine.equals("q")) System.exit(0);       // Quit program
        else if (textLine.equals("wq")) {   // Write AIML Files and quit program
            bot.writeQuit();
            System.exit(0);
        } else if (textLine.equals("s") || textLine.equals("pass")) { //
            passed.add(request);
            AIMLSet difference = new AIMLSet("difference", bot);
            difference.addAll(testSet);
            difference.removeAll(passed);
            difference.writeAIMLSet();
            passed.writeAIMLSet();
        } else if (textLine.equals("a")) {
            template = alicetemplate;
            String filename;
            if (template.contains("<sr")) filename = MagicStrings.reductions_update_aiml_file;
            else filename = MagicStrings.personality_aiml_file;
            saveCategory(c.getPattern(), template, filename);
        } else if (textLine.equals("d")) { // delete this suggested category
            deleteCategory(c);
        } else if (textLine.equals("x")) {    // ask another bot
            template = "<sraix services=\"pannous\">" + c.getPattern().replace("*", "<star/>") + "</sraix>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.sraix_aiml_file);
        } else if (textLine.equals("p")) {   // filter inappropriate content
            template = "<srai>" + MagicStrings.inappropriate_filter + "</srai>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.inappropriate_aiml_file);
        } else if (textLine.equals("f")) { // filter profanity
            template = "<srai>" + MagicStrings.profanity_filter + "</srai>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.profanity_aiml_file);
        } else if (textLine.equals("i")) {
            template = "<srai>" + MagicStrings.insult_filter + "</srai>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.insult_aiml_file);
        } else if (textLine.contains("<srai>") || textLine.contains("<sr/>")) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.reductions_update_aiml_file);
        } else if (textLine.contains("<oob>")) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.oob_aiml_file);
        } else if (textLine.contains("<set name") || botThinks.length() > 0) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.predicates_aiml_file);
        } else if (textLine.contains("<get name") && !textLine.contains("<get name=\"name")) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.predicates_aiml_file);
        } else {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.personality_aiml_file);
        }
    }
}
