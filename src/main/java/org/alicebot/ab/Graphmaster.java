package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLSet;
import org.alicebot.ab.aiml.Category;
import org.alicebot.ab.constant.Constants;
import org.alicebot.ab.util.NodemapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The AIML Pattern matching algorithm and data structure.
 */
public class Graphmaster {

    private static final Logger LOG = LoggerFactory.getLogger(Graphmaster.class);

    private static final String botPropRegex = "<bot name=\"(.*?)\"/>";
    private static final Pattern botPropPattern = Pattern.compile(botPropRegex, Pattern.CASE_INSENSITIVE);

    private Bot bot;
    private String name;
    private final Nodemapper root;
    private Set<String> vocabulary;
    private boolean enableShortCuts;

    private int leafCnt;
    private int nodeCnt;
    private long nodeSize;
    private int singletonCnt;
    private int shortCutCnt;
    private int naryCnt;

    /**
     * Constructor
     *
     * @param bot the bot the graph belongs to.
     */
    public Graphmaster(Bot bot, boolean enableShortCuts) {
        this(bot, "brain", enableShortCuts);
    }

    public Graphmaster(Bot bot, String name, boolean enableShortCuts) {
        this.root = new Nodemapper();
        this.bot = bot;
        this.name = name;
        this.vocabulary = new HashSet<>();
        this.enableShortCuts = enableShortCuts;
    }

    /**
     * Convert input, that and topic to a single sentence having the form
     * {@code input <THAT> that <TOPIC> topic}
     *
     * @param input input (or input pattern)
     * @param that  that (or that pattern)
     * @param topic topic (or topic pattern)
     * @return
     */
    public static String inputThatTopic(String input, String that, String topic) {
        return input.trim() + " <THAT> " + that.trim() + " <TOPIC> " + topic.trim();
    }

    /**
     * add an AIML category to this graph.
     *
     * @param category AIML Category
     */
    public void addCategory(Category category) {
        String inputThatTopic = inputThatTopic(category.getPattern(), category.getThat(), category.getTopic());

        if (LOG.isDebugEnabled()) {
            LOG.debug("addCategory: {}", inputThatTopic);
        }

        if (inputThatTopic.contains("<B")) {
            Matcher matcher = botPropPattern.matcher(inputThatTopic);
            String propName, property;
            while (matcher.find()) {
                propName = matcher.group(1).toLowerCase();
                property = bot.getProperties().get(propName).toUpperCase();
                inputThatTopic = inputThatTopic.replaceFirst("(?i)" + botPropRegex, property);
            }
        }

        //
        Path p = Path.sentenceToPath(inputThatTopic);
        addPath(p, category);
    }

    /**
     * add a path to the graph from the root to a Category
     *
     * @param path     Pattern path
     * @param category AIML category
     */
    private void addPath(Path path, Category category) {
        addPath(root, path, category);
    }

    /**
     * add a Path to the graph from a given node.
     * Shortcuts: Replace all instances of paths "<THAT> * <TOPIC> *" with a direct link to the matching category
     *
     * @param node     starting node in graph
     * @param path     Pattern path to be added
     * @param category AIML Category
     */
    private void addPath(Nodemapper node, Path path, Category category) {
        if (path == null) {
            node.setCategory(category);
            node.setHeight(0);
        } else if (enableShortCuts && thatStarTopicStar(path)) {
            node.setCategory(category);
            node.setHeight(Math.min(4, node.getHeight()));
            node.setShortCut(true);
        } else if (NodemapperUtils.containsKey(node, path.getWord())) {
            if (path.getWord().startsWith("<SET>")) {
                addSets(path.getWord(), bot, node, category.getFilename());
            }
            Nodemapper nextNode = NodemapperUtils.get(node, path.getWord());
            addPath(nextNode, path.getNext(), category);

            int offset = 1;
            if (path.getWord().equals("#") || path.getWord().equals("^")) {
                offset = 0;
            }

            node.setHeight(Math.min(offset + nextNode.getHeight(), node.getHeight()));
        } else {
            Nodemapper nextNode = new Nodemapper();
            if (path.getWord().startsWith("<SET>")) {
                addSets(path.getWord(), bot, node, category.getFilename());
            }

            if (node.getKey() != null) {
                NodemapperUtils.upgrade(node);
            }

            NodemapperUtils.put(node, path.getWord(), nextNode);
            addPath(nextNode, path.getNext(), category);

            int offset = 1;
            if (path.getWord().equals("#") || path.getWord().equals("^")) {
                offset = 0;
            }

            node.setHeight(Math.min(offset + nextNode.getHeight(), node.getHeight()));
        }
    }

    private boolean thatStarTopicStar(Path path) {
        String tail = Path.pathToSentence(path).trim();
        return tail.equals("<THAT> * <TOPIC> *");
    }

    private void addSets(String type, Bot bot, Nodemapper node, String filename) {
        String stag = "<SET>", etag = "</SET>";
        if (type.length() >= (stag + etag).length()) {
            type = type.substring(stag.length());
            type = type.substring(0, type.length() - etag.length());
        }

        String setName = type.toLowerCase();
        if (bot.getSetMap().containsKey(setName)) {
            if (node.getSets() == null) {
                node.setSets(new ArrayList<>());
            }

            if (!node.getSets().contains(setName)) {
                node.getSets().add(setName);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No AIML Set found for <set>{}</set> in {} {}", setName, bot.getName(), filename);
            }
        }
    }

    /**
     * Given a category, find the leaf node associated with this path.
     *
     * @param c AIML Category
     * @return leaf node or null if no matching node is found
     */
    public Nodemapper findNode(Category c) {
        Nodemapper result = findNode(root, Path.sentenceToPath(inputThatTopic(c.getPattern(), c.getThat(), c.getTopic())));

        if (LOG.isDebugEnabled()) {
            LOG.debug("findNode {} {}", inputThatTopic(c.getPattern(), c.getThat(), c.getTopic()), result);
        }

        return result;
    }

    /**
     * Recursively find a leaf node given a starting node and a path.
     *
     * @param node string node
     * @param path string path
     * @return the leaf node or null if no leaf is found
     */
    private Nodemapper findNode(Nodemapper node, Path path) {
        if (path == null && node != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("findNode: path is null, returning node {}", node.getCategory().inputThatTopic());
            }

            return node;
        } else if (Path.pathToSentence(path).trim().equals("<THAT> * <TOPIC> *") && node.isShortCut() && path.getWord().equals("<THAT>")) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("findNode: shortcut, returning {}", node.getCategory().inputThatTopic());
            }

            return node;
        } else if (NodemapperUtils.containsKey(node, path.getWord())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("findNode: node contains {}", path.getWord());
            }

            Nodemapper nextNode = NodemapperUtils.get(node, path.getWord().toUpperCase());
            return findNode(nextNode, path.getNext());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("findNode: returning null");
            }

            return null;
        }
    }

    /**
     * Find the matching leaf node given an input, that state and topic value
     *
     * @param input client input
     * @param that  bot's last sentence
     * @param topic current topic
     * @return matching leaf node or null if no match is found
     */
    public Nodemapper match(String input, String that, String topic) {
        Nodemapper n;
        try {
            String inputThatTopic = inputThatTopic(input, that, topic);
            Path p = Path.sentenceToPath(inputThatTopic);
            n = match(p, inputThatTopic);

            if (LOG.isDebugEnabled()) {
                if (n != null) {
                    LOG.debug("Matched: {} {}", n.getCategory().inputThatTopic(), n.getCategory().getFilename());
                } else {
                    LOG.debug("No match.");
                }
            }
        } catch (Exception ex) {
            LOG.error("Match[input={}, that={}, topic={}] error: {}", input, that, topic, ex);
            n = null;
        }

        return n;
    }

    /**
     * Find the matching leaf node given a path of the form "{@code input <THAT> that <TOPIC> topic}"
     *
     * @param path
     * @param inputThatTopic
     * @return matching leaf node or null if no match is found
     */
    private Nodemapper match(Path path, String inputThatTopic) {
        try {
            String[] inputStars = new String[Constants.max_stars];
            String[] thatStars = new String[Constants.max_stars];
            String[] topicStars = new String[Constants.max_stars];
            String starState = "inputStar";
            String matchTrace = "";

            Nodemapper n = match(path, root, inputThatTopic, starState, 0, inputStars, thatStars, topicStars, matchTrace);
            if (n != null) {
                StarBindings sb = new StarBindings();
                for (int i = 0; inputStars[i] != null && i < Constants.max_stars; ++i) {
                    sb.getInputStars().add(inputStars[i]);
                }
                for (int i = 0; thatStars[i] != null && i < Constants.max_stars; ++i) {
                    sb.getThatStars().add(thatStars[i]);
                }
                for (int i = 0; topicStars[i] != null && i < Constants.max_stars; ++i) {
                    sb.getTopicStars().add(topicStars[i]);
                }
                n.setStarBindings(sb);
            }

            if (n != null) {
                n.getCategory().addMatch(inputThatTopic, bot);
            }

            return n;
        } catch (Exception ex) {
            LOG.error("Match[path={}, inputThatTopic={}] error: {}", path, inputThatTopic, ex);
            return null;
        }
    }

    /**
     * Depth-first search of the graph for a matching leaf node.
     * At each node, the order of search is
     * 1. $WORD  (high priority exact word match)
     * 2. # wildcard  (zero or more word match)
     * 3. _ wildcard (one or more words match)
     * 4. WORD (exact word match)
     * 5. {@code <set></set>} (AIML Set match)
     * 6. shortcut (graph shortcut when that pattern = * and topic pattern = *)
     * 7. ^ wildcard  (zero or more words match)
     * 8. * wildcard (one or more words match)
     *
     * @param path           remaining path to be matched
     * @param node           current search node
     * @param inputThatTopic original input, that and topic string
     * @param starState      tells whether wildcards are in input pattern, that pattern or topic pattern
     * @param starIndex      index of wildcard
     * @param inputStars     array of input pattern wildcard matches
     * @param thatStars      array of that pattern wildcard matches
     * @param topicStars     array of topic pattern wildcard matches
     * @param matchTrace     trace of match path for debugging purposes
     * @return matching leaf node or null if no match is found
     */
    private Nodemapper match(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        Nodemapper matchedNode;
        if ((matchedNode = nullMatch(path, node, matchTrace)) != null) {
            return matchedNode;
        } else if (path.getLength() < node.getHeight()) {
            return null;
        } else if ((matchedNode = dollarMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = sharpMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = underMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = wordMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = setMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = shortCutMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = caretMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = starMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else {
            return null;
        }
    }

    /**
     * a match is found if the end of the path is reached and the node is a leaf node
     *
     * @param path       remaining path
     * @param node       current search node
     * @param matchTrace trace of match for debugging purposes
     * @return matching leaf node or null if no match found
     */
    private Nodemapper nullMatch(Path path, Nodemapper node, String matchTrace) {
        if (path == null && node != null && NodemapperUtils.isLeaf(node) && node.getCategory() != null) {
            return node;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Match failed (null) {}", matchTrace);
            }
            return null;
        }
    }

    private Nodemapper shortCutMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        if (node != null && node.isShortCut() && path.getWord().equals("<THAT>") && node.getCategory() != null) {
            String tail = Path.pathToSentence(path).trim();
            String that = tail.substring(tail.indexOf("<THAT>") + "<THAT>".length(), tail.indexOf("<TOPIC>")).trim();
            String topic = tail.substring(tail.indexOf("<TOPIC>") + "<TOPIC>".length(), tail.length()).trim();
            thatStars[0] = that;
            topicStars[0] = topic;
            return node;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Match failed (shortCut) {}", matchTrace);
            }
            return null;
        }
    }

    private Nodemapper wordMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        try {
            String uword = path.getWord().toUpperCase();
            if (uword.equals("<THAT>")) {
                starIndex = 0;
                starState = "thatStar";
            } else if (uword.equals("<TOPIC>")) {
                starIndex = 0;
                starState = "topicStar";
            }

            Nodemapper matchedNode;
            if (NodemapperUtils.containsKey(node, uword) &&
                    (matchedNode = match(path.getNext(), NodemapperUtils.get(node, uword), inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
                return matchedNode;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Match failed (word) {}[{}, {}]", matchTrace, uword, uword);
                }
                return null;
            }
        } catch (Exception ex) {
            LOG.error("wordMatch[{}] error: {}", Path.pathToSentence(path), ex);
            return null;
        }
    }

    private Nodemapper dollarMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        String uword = "$" + path.getWord().toUpperCase();
        Nodemapper matchedNode;
        if (NodemapperUtils.containsKey(node, uword) &&
                (matchedNode = match(path.getNext(), NodemapperUtils.get(node, uword), inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Match failed (dollar) {}", matchTrace);
            }
            return null;
        }
    }

    private Nodemapper starMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "*", matchTrace);
    }

    private Nodemapper underMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "_", matchTrace);
    }

    private Nodemapper caretMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        Nodemapper matchedNode;
        matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
        if (matchedNode != null) {
            return matchedNode;
        } else {
            return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
        }
    }

    private Nodemapper sharpMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        Nodemapper matchedNode;
        matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
        if (matchedNode != null) {
            return matchedNode;
        } else {
            return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
        }
    }

    private Nodemapper zeroMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String wildcard, String matchTrace) {
        if (path != null && NodemapperUtils.containsKey(node, wildcard)) {
            setStars(bot.getProperties().get(Constants.null_star), starIndex, starState, inputStars, thatStars, topicStars);
            Nodemapper nextNode = NodemapperUtils.get(node, wildcard);
            return match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Match failed (zero) {}[{}]", matchTrace, wildcard);
            }
            return null;
        }
    }

    private Nodemapper wildMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String wildcard, String matchTrace) {
        Nodemapper matchedNode;
        if (path.getWord().equals("<THAT>") || path.getWord().equals("<TOPIC>")) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Match failed (wild1 {}) {}", wildcard, matchTrace);
            }
            return null;
        }

        try {
            if (NodemapperUtils.containsKey(node, wildcard)) {
                matchTrace += "[" + wildcard + "," + path.getWord() + "]";

                Nodemapper nextNode = NodemapperUtils.get(node, wildcard);
                if (NodemapperUtils.isLeaf(nextNode) && !nextNode.isShortCut()) {
                    setStars(Path.pathToSentence(path), starIndex, starState, inputStars, thatStars, topicStars);
                    return nextNode;
                }

                String currentWord = path.getWord();
                StringBuilder starWords = new StringBuilder(currentWord + " ");
                for (path = path.getNext(); path != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>"); path = path.getNext()) {
                    matchTrace += "[" + wildcard + "," + path.getWord() + "]";

                    if ((matchedNode = match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
                        setStars(starWords.toString(), starIndex, starState, inputStars, thatStars, topicStars);
                        return matchedNode;
                    }

                    starWords.append(currentWord = path.getWord()).append(" ");
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Match failed (wild2 {}) {}", wildcard, matchTrace);
                }

                return null;
            }
        } catch (Exception ex) {
            LOG.error("wildMatch[{}] error: {}", Path.pathToSentence(path), ex);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Match failed (wild3 {}) {}", wildcard, matchTrace);
        }

        return null;
    }

    private Nodemapper setMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Graphmaster.setMatch(path: {}, node: {}, input: {}, starState: {}, starIndex: {}, inputStars, thatStars, topicStars, matchTrace: {})", path, node, input, starState, starIndex, matchTrace);
        }

        if (node.getSets() == null || path.getWord().equals("<THAT>") || path.getWord().equals("<TOPIC>")) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("in Graphmaster.setMatch, setMatch sets = {}", node.getSets());
        }

        int length;
        AIMLSet aimlSet;
        String currentWord, starWords, phrase;
        Nodemapper nextNode, matchedNode, bestMatchedNode;
        for (String setName : node.getSets()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("in Graphmaster.setMatch, setMatch trying type {}", setName);
            }

            nextNode = NodemapperUtils.get(node, "<SET>" + setName.toUpperCase() + "</SET>");
            aimlSet = bot.getSetMap().get(setName);
            bestMatchedNode = null;
            currentWord = path.getWord();
            starWords = currentWord + " ";
            length = 1;
            matchTrace += "[<set>" + setName + "</set>," + path.getWord() + "]";

            if (LOG.isDebugEnabled()) {
                LOG.debug("in Graphmaster.setMatch, setMatch starWords =\"{}\"", starWords);
            }

            for (Path qath = path.getNext(); qath != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>") && length <= aimlSet.getMaxLength(); qath = qath.getNext()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("in Graphmaster.setMatch, qath.word = {}", qath.getWord());
                }

                phrase = bot.getPreProcessor().normalize(starWords.trim()).toUpperCase();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("in Graphmaster.setMatch, setMatch trying \"{}\" in {}", phrase, setName);
                }

                if (aimlSet.contains(phrase) && (matchedNode = match(qath, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
                    setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("in Graphmaster.setMatch, setMatch found {} in {}", phrase, setName);
                    }

                    bestMatchedNode = matchedNode;
                }
                // else if (qath.word.equals("<THAT>") || qath.word.equals("<TOPIC>")) return null;

                length = length + 1;
                currentWord = qath.getWord();
                starWords += currentWord + " ";
            }

            if (bestMatchedNode != null) {
                return bestMatchedNode;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Match failed (set) {}", matchTrace);
        }

        return null;
    }

    private void setStars(String starWords, int starIndex, String starState, String[] inputStars, String[] thatStars, String[] topicStars) {
        if (starIndex < Constants.max_stars) {
            starWords = starWords.trim();
            switch (starState) {
                case "inputStar":
                    inputStars[starIndex] = starWords;
                    break;
                case "thatStar":
                    thatStars[starIndex] = starWords;
                    break;
                case "topicStar":
                    topicStars[starIndex] = starWords;
                    break;
            }
        }
    }

    public void printgraph() {
        printgraph(root, "");
    }

    private void printgraph(Nodemapper node, String partial) {
        if (node == null) {
            LOG.info("Null graph");
        } else {
            String template;
            if (NodemapperUtils.isLeaf(node) || node.isShortCut()) {
                template = Category.templateToLine(node.getCategory().getTemplate());
                template = template.substring(0, Math.min(16, template.length()));
                if (node.isShortCut()) {
                    LOG.info("{}({}[{}])--<THAT>-->X(1)--*-->X(1)--<TOPIC>-->X(1)--*-->{}...", partial, NodemapperUtils.size(node), node.getHeight(), template);
                } else {
                    LOG.info("{}({}[{}]) {}...", partial, NodemapperUtils.size(node), node.getHeight(), template);
                }
            }
            for (String key : NodemapperUtils.keySet(node)) {
                printgraph(NodemapperUtils.get(node, key), partial + "(" + NodemapperUtils.size(node) + "[" + node.getHeight() + "])--" + key + "-->");
            }
        }
    }

    public List<Category> getCategories() {
        List<Category> categories = new ArrayList<>();
        getCategories(root, categories);
        return categories;
    }

    private void getCategories(Nodemapper node, List<Category> categories) {
        if (node != null) {
            if (NodemapperUtils.isLeaf(node) || node.isShortCut()) {
                if (node.getCategory() != null) {
                    // node.category == null when the category is deleted.
                    categories.add(node.getCategory());
                }
            }

            for (String key : NodemapperUtils.keySet(node)) {
                getCategories(NodemapperUtils.get(node, key), categories);
            }
        }
    }

    public void nodeStats() {
        leafCnt = 0;
        nodeCnt = 0;
        nodeSize = 0;
        singletonCnt = 0;
        shortCutCnt = 0;
        naryCnt = 0;
        nodeStatsGraph(root);
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} ({}): {} categories {} nodes {} singletons {} leaves {} shortcuts {} n-ary {} branches {} average branching",
                    bot.getName(), name, getCategories().size(), nodeCnt, singletonCnt, leafCnt, shortCutCnt, naryCnt, nodeSize, (float) nodeSize / (float) nodeCnt);
        }
    }

    private void nodeStatsGraph(Nodemapper node) {
        if (node != null) {
            nodeCnt++;
            nodeSize += NodemapperUtils.size(node);
            if (NodemapperUtils.size(node) == 1) {
                singletonCnt += 1;
            }
            if (NodemapperUtils.isLeaf(node) && !node.isShortCut()) {
                leafCnt++;
            }
            if (NodemapperUtils.size(node) > 1) {
                naryCnt += 1;
            }
            if (node.isShortCut()) {
                shortCutCnt += 1;
            }
            for (String key : NodemapperUtils.keySet(node)) {
                nodeStatsGraph(NodemapperUtils.get(node, key));
            }
        }
    }

    public Set<String> getVocabulary() {
        vocabulary = new HashSet<>();
        getBrainVocabulary(root);
        for (String set : bot.getSetMap().keySet()) {
            vocabulary.addAll(bot.getSetMap().get(set));
        }
        return vocabulary;
    }

    private void getBrainVocabulary(Nodemapper node) {
        if (node != null) {
            for (String key : NodemapperUtils.keySet(node)) {
                vocabulary.add(key);
                getBrainVocabulary(NodemapperUtils.get(node, key));
            }
        }
    }
}
