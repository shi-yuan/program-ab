package org.alicebot.ab.aiml;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Graphmaster;
import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * The category element delimits a base unit of knowledge in an AIML-based chatbot.
 * In a very broad sense, a single category accepts an input, and returns an output.
 * All AIML elements (with the exception of the AIML root element and the topic element) must be contained within a category block.
 */
public class Category {

    private static final Logger LOG = LoggerFactory.getLogger(Category.class);

    private static int CATEGORY_COUNT = 0;

    private String pattern;
    private String that;
    private String topic;
    private String template;
    private String filename;
    private int activationCnt;
    private int categoryNumber; // for loading order
    private AIMLSet matches;

    /**
     * Constructor
     *
     * @param activationCnt category activation count
     * @param pattern       input pattern
     * @param that          that pattern
     * @param topic         topic pattern
     * @param template      AIML template
     * @param filename      AIML file name
     */
    public Category(int activationCnt, String pattern, String that, String topic, String template, String filename) {
        this.pattern = pattern.trim().toUpperCase();
        this.that = that.trim().toUpperCase();
        this.topic = topic.trim().toUpperCase();
        this.template = template.replace("& ", " and "); // XML parser treats & badly
        this.filename = filename;
        this.activationCnt = activationCnt;
        this.matches = null;
        this.categoryNumber = CATEGORY_COUNT++;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating {} {}", this.categoryNumber, inputThatTopic());
        }
    }

    /**
     * Constructor
     *
     * @param activationCnt    category activation count
     * @param patternThatTopic string representing Pattern Path
     * @param template         AIML template
     * @param filename         AIML category
     */
    public Category(int activationCnt, String patternThatTopic, String template, String filename) {
        this(activationCnt,
                patternThatTopic.substring(0, patternThatTopic.indexOf("<THAT>")),
                patternThatTopic.substring(patternThatTopic.indexOf("<THAT>") + "<THAT>".length(), patternThatTopic.indexOf("<TOPIC>")),
                patternThatTopic.substring(patternThatTopic.indexOf("<TOPIC>") + "<TOPIC>".length(), patternThatTopic.length()), template, filename);
    }

    /**
     * number of times a category was activated by inputs
     *
     * @return integer number of activations
     */
    private int getActivationCnt() {
        return activationCnt;
    }

    /**
     * get the index number of this category
     *
     * @return unique integer identifying this category
     */
    private int getCategoryNumber() {
        return categoryNumber;
    }

    /**
     * get category pattern
     *
     * @return pattern
     */
    public String getPattern() {
        return pattern == null ? "*" : pattern;
    }

    /**
     * get category that pattern
     *
     * @return that pattern
     */
    public String getThat() {
        return that == null ? "*" : that;
    }

    /**
     * get category topic pattern
     *
     * @return topic pattern
     */
    public String getTopic() {
        return topic == null ? "*" : topic;
    }

    /**
     * get category template
     *
     * @return template
     */
    public String getTemplate() {
        return template == null ? "" : template;
    }

    /**
     * get name of AIML file for this category
     *
     * @return file name
     */
    public String getFilename() {
        return filename == null ? Constants.unknown_aiml_file : filename;
    }

    /**
     * set category filename
     *
     * @param filename name of AIML file
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * set category template
     *
     * @param template AIML template
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * set category pattern
     *
     * @param pattern AIML pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * set category that pattern
     *
     * @param that AIML that pattern
     */
    public void setThat(String that) {
        this.that = that;
    }

    /**
     * set category topic
     *
     * @param topic AIML topic pattern
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * return a string representing the full pattern path as "{@code input pattern <THAT> that pattern <TOPIC> topic pattern}"
     *
     * @return
     */
    public String inputThatTopic() {
        return Graphmaster.inputThatTopic(pattern, that, topic);
    }

    /**
     * add a matching input to the matching input set
     *
     * @param input matching input
     */
    public void addMatch(String input, Bot bot) {
        if (matches == null) {
            String setName = this.inputThatTopic()
                    .replace("*", "STAR")
                    .replace("_", "UNDERSCORE")
                    .replace(" ", "-")
                    .replace("<THAT>", "THAT")
                    .replace("<TOPIC>", "TOPIC");

            LOG.info("Created match set {}", setName);

            matches = new AIMLSet(setName, bot);
        }
        matches.add(input);
    }

    /**
     * convert a template to a single-line representation by replacing "," with #Comma and newline with #Newline
     *
     * @param template original template
     * @return template on a single line of text
     */
    public static String templateToLine(String template) {
        return template
                .replaceAll("(\r\n|\n\r|\r|\n)", "\\#Newline")
                .replaceAll(Constants.aimlif_split_char, Constants.aimlif_split_char_name);
    }

    /**
     * convert a Category object to AIML syntax
     *
     * @param category Category object
     * @return AIML Category
     */
    public static String categoryToAIML(Category category) {
        String NL = "\n";

        String topicStart = "";
        String topicEnd = "";
        if (!category.getTopic().equals("*")) {
            topicStart = "<topic name=\"" + category.getTopic() + "\">" + NL;
            topicEnd = "</topic>" + NL;
        }

        String pattern = category.getPattern();
        if (pattern.contains("<SET>") || pattern.contains("<BOT")) {
            String[] splitPattern = pattern.split(" ");
            StringBuilder rpattern = new StringBuilder();
            for (String w : splitPattern) {
                if (w.startsWith("<SET>") || w.startsWith("<BOT") || w.startsWith("NAME=")) {
                    w = w.toLowerCase();
                }
                rpattern.append(" ").append(w);
            }
            pattern = rpattern.toString().trim();
        }

        String thatStatement = "";
        if (!category.getThat().equals("*")) {
            thatStatement = "<that>" + category.getThat() + "</that>";
        }

        return topicStart + "<category><pattern>" + pattern + "</pattern>" + thatStatement + NL +
                "<template>" + category.getTemplate() + "</template>" + NL +
                "</category>" + topicEnd;
    }

    /**
     * compare two categories for sorting purposes based on activation count
     */
    public static Comparator<Category> ACTIVATION_COMPARATOR = (c1, c2) -> c2.getActivationCnt() - c1.getActivationCnt();
    /**
     * compare two categories for sorting purposes based on alphabetical order of patterns
     */
    public static Comparator<Category> PATTERN_COMPARATOR = (c1, c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.inputThatTopic(), c2.inputThatTopic());
    /**
     * compare two categories for sorting purposes based on category index number
     */
    public static Comparator<Category> CATEGORY_NUMBER_COMPARATOR = Comparator.comparingInt(Category::getCategoryNumber);
}
