package org.alicebot.ab.aiml;

import org.alicebot.ab.Chat;
import org.alicebot.ab.History;
import org.alicebot.ab.Nodemapper;
import org.alicebot.ab.constant.Constants;
import org.alicebot.ab.util.DomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.transform.TransformerException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The core AIML parser and interpreter.
 * Implements the AIML 2.0 specification as described in
 * AIML 2.0 Working Draft document
 * https://docs.google.com/document/d/1wNT25hJRyupcG51aO89UcQEiG-HkXRXusukADpFnDs4/pub
 */
public class AIMLProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AIMLProcessor.class);

    private static final ScriptEngine JS_ENGINE = new ScriptEngineManager().getEngineByName("js");

    private AIMLProcessorExtension extension;

    /**
     * convert an AIML file to a list of categories.
     *
     * @param directory directory containing the AIML file.
     * @param aimlFile  AIML file name.
     * @return list of categories.
     */
    public List<Category> aimlToCategories(String directory, String aimlFile) throws Exception {
        List<Category> categories = new ArrayList<>();

        NodeList nodelist = DomUtils.parseFile(directory + "/" + aimlFile).getChildNodes();
        Node n;
        String topic;
        NodeList children;
        for (int i = 0, len = nodelist.getLength(); i < len; ++i) {
            n = nodelist.item(i);

            if (n.getNodeName().equals("category")) {
                processCategory(n, categories, "*", aimlFile);
            } else if (n.getNodeName().equals("topic")) {
                topic = n.getAttributes().getNamedItem("name").getTextContent();
                children = n.getChildNodes();

                for (int j = 0, len2 = children.getLength(); j < len2; j++) {
                    n = children.item(j);
                    if (n.getNodeName().equals("category")) {
                        processCategory(n, categories, topic, aimlFile);
                    }
                }
            }
        }

        return categories;
    }

    /**
     * when parsing an AIML file, process a category element.
     *
     * @param n          current XML parse node.
     * @param categories list of categories found so far.
     * @param topic      value of topic in case this category is wrapped in a <topic> tag
     * @param aimlFile   name of AIML file being parsed.
     */
    private void processCategory(Node n, List<Category> categories, String topic, String aimlFile) throws TransformerException, IOException {
        String pattern = "*", that = "*", template = "";
        Node m;
        String mName;
        NodeList children = n.getChildNodes();
        for (int j = 0, len = children.getLength(); j < len; ++j) {
            m = children.item(j);
            mName = m.getNodeName();

            switch (mName) {
                case "#text": /*skip*/
                    break;
                case "pattern":
                    pattern = DomUtils.nodeToString(m);
                    break;
                case "that":
                    that = DomUtils.nodeToString(m);
                    break;
                case "topic":
                    topic = DomUtils.nodeToString(m);
                    break;
                case "template":
                    template = DomUtils.nodeToString(m);
                    break;
                default:
                    LOG.warn("processCategory: unexpected {} in {}", mName, DomUtils.nodeToString(m));
                    break;
            }
        }

        pattern = DomUtils.trimTag(pattern, "pattern");
        that = DomUtils.trimTag(that, "that");
        topic = DomUtils.trimTag(topic, "topic");

        pattern = cleanPattern(pattern);
        that = cleanPattern(that);
        topic = cleanPattern(topic);

        template = DomUtils.trimTag(template, "template");

        Category c = new Category(0, pattern, that, topic, template, aimlFile);

        if (template.length() == 0) {
            LOG.warn("Category {} discarded due to blank or missing <template>.", c.inputThatTopic());
        } else {
            categories.add(c);
        }
    }

    private String cleanPattern(String pattern) {
        return pattern.replaceAll("(\r\n|\n\r|\r|\n)", " ").replaceAll(" {2}", " ").trim();
    }

    /**
     * generate a bot response to a single sentence input.
     *
     * @param input       input statement.
     * @param that        bot's last reply.
     * @param topic       current topic.
     * @param chatSession current client chat session.
     * @return bot's reply.
     */
    public String respond(String input, String that, String topic, Chat chatSession) throws Exception {

        if (LOG.isDebugEnabled()) {
            LOG.debug("input: {}, that: {}, topic: {}, chatSession: {}", input, that, topic, chatSession);
        }

        Nodemapper leaf = chatSession.getBot().getBrain().match(input, that, topic);
        if (leaf == null) {
            return Constants.default_bot_response;
        }

        ParseState ps = new ParseState(0, chatSession, input, that, topic, leaf);
        String template = leaf.getCategory().getTemplate();
        return evalTemplate(template, ps);
    }

    // ====================
    // Parsing and evaluation functions
    // ====================

    /**
     * evaluate the contents of an AIML tag.
     * calls recursEval on child tags.
     *
     * @param node             the current parse node.
     * @param ps               the current parse state.
     * @param ignoreAttributes tag names to ignore when evaluating the tag.
     * @return the result of evaluating the tag contents.
     */
    private String evalTagContent(Node node, ParseState ps, Set<String> ignoreAttributes) throws Exception {
        NodeList childList = node.getChildNodes();
        Node child;
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = childList.getLength(); i < len; ++i) {
            child = childList.item(i);
            if (ignoreAttributes == null || !ignoreAttributes.contains(child.getNodeName())) {
                sb.append(recursEval(child, ps));
            }
        }
        return sb.toString();
    }

    /**
     * pass thru generic XML (non-AIML tags, such as HTML) as unevaluated XML
     *
     * @param node current parse node
     * @param ps   current parse state
     * @return unevaluated generic XML string
     */
    private String genericXML(Node node, ParseState ps) throws Exception {
        String evalResult = evalTagContent(node, ps, null);
        return unevaluatedXML(evalResult, node);
    }

    /**
     * return a string of unevaluated XML.      When the AIML parser
     * encounters an unrecognized XML tag, it simply passes through the
     * tag in XML form.  For example, if the response contains HTML
     * markup, the HTML is passed to the requesting process.    However if that
     * markup contains AIML tags, those tags are evaluated and the parser
     * builds the result.
     *
     * @param node current parse node.
     * @return the unevaluated XML string
     */
    private String unevaluatedXML(String resultIn, Node node) {
        StringBuilder attributes = new StringBuilder();
        if (node.hasAttributes()) {
            NamedNodeMap attrs = node.getAttributes();
            for (int i = 0, len = attrs.getLength(); i < len; ++i) {
                attributes.append(" ").append(attrs.item(i).getNodeName()).append("=\"").append(attrs.item(i).getNodeValue()).append("\"");
            }
        }

        String nodeName = node.getNodeName();
        return !resultIn.equals("") ? "<" + nodeName + attributes + ">" + resultIn + "</" + nodeName + ">" : "<" + nodeName + attributes + "/>";
    }

    /**
     * implements AIML <srai> tag
     *
     * @param node current parse node.
     * @param ps   current parse state.
     * @return the result of processing the <srai>
     */
    private String srai(Node node, ParseState ps) throws Exception {
        if (ps.getDepth() > Constants.max_recursion_depth) {
            return Constants.too_much_recursion;
        }

        String result = evalTagContent(node, ps, null).trim().replaceAll("(\r\n|\n\r|\r|\n)", " ");
        result = ps.getChatSession().getBot().getPreProcessor().normalize(result);

        String topic = ps.getChatSession().getPredicates().get("topic");

        Nodemapper leaf = ps.getChatSession().getBot().getBrain().match(result, ps.getThat(), topic);
        if (leaf == null) {
            return Constants.default_bot_response;
        }

        String response = evalTemplate(leaf.getCategory().getTemplate(), new ParseState(ps.getDepth() + 1, ps.getChatSession(), ps.getInput(), ps.getThat(), topic, leaf));

        return response.trim();
    }

    /**
     * in AIML 2.0, an attribute value can be specified by either an XML attribute value
     * or a subtag of the same name.  This function tries to read the value from the XML attribute first,
     * then tries to look for the subtag.
     *
     * @param node          current parse node.
     * @param ps            current parse state.
     * @param attributeName the name of the attribute.
     * @return the attribute value.
     */
    private String getAttributeOrTagValue(Node node, ParseState ps, String attributeName) throws Exception {
        String result;
        Node n = node.getAttributes().getNamedItem(attributeName);
        if (n == null) {
            // no attribute or tag named attributeName
            result = null;

            NodeList childList = node.getChildNodes();
            for (int i = 0, len = childList.getLength(); i < len; ++i) {
                n = childList.item(i);

                if (n.getNodeName().equals(attributeName)) {
                    result = evalTagContent(n, ps, null);
                }
            }
        } else {
            result = n.getNodeValue();
        }

        return result;
    }

    /**
     * map an element of one string set to an element of another
     * Implements <map name="mapname"></map>   and <map><name>mapname</name></map>
     *
     * @param node current XML parse node
     * @param ps   current AIML parse state
     * @return the map result or a string indicating the key was not found
     */
    private String map(Node node, ParseState ps) throws Exception {
        String result = Constants.default_map;

        Set<String> attributeNames = Collections.singleton("name");
        String contents = evalTagContent(node, ps, attributeNames);
        contents = contents.trim();

        String mapName = getAttributeOrTagValue(node, ps, "name");
        if (mapName == null) {
            // this is an OOB map tag (no attribute)
            result = "<map>" + contents + "</map>";
        } else {
            AIMLMap map = ps.getChatSession().getBot().getMapMap().get(mapName);
            if (map != null) {
                result = map.get(contents.toUpperCase());
            }

            if (result == null) {
                result = Constants.default_map;
            }

            result = result.trim();
        }

        return result;
    }

    /**
     * set the value of an AIML predicate.
     * Implements <set name="predicate"></set> and <set var="varname"></set>
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the result of the <set> operation
     */
    private String set(Node node, ParseState ps) throws Exception {
        Set<String> attributeNames = Stream.of("name", "var").collect(Collectors.toSet());
        String result = evalTagContent(node, ps, attributeNames).trim().replaceAll("(\r\n|\n\r|\r|\n)", " ");

        String predicateName = getAttributeOrTagValue(node, ps, "name");
        if (predicateName != null) {
            ps.getChatSession().getPredicates().put(predicateName, result);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Set predicate {} to {} in {}", predicateName, result, ps.getLeaf().getCategory().inputThatTopic());
            }
        }

        String varName = getAttributeOrTagValue(node, ps, "var");
        if (varName != null) {
            ps.getVars().put(varName, result);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Set var {} to {} in {}", varName, result.trim(), ps.getLeaf().getCategory().inputThatTopic());
            }
        }

        if (ps.getChatSession().getBot().getPronounSet().contains(predicateName)) {
            result = predicateName;
        }

        return result;
    }

    /**
     * get the value of an AIML predicate.
     * implements <get name="predicate"></get>  and <get var="varname"></get>
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the result of the <get> operation
     */
    private String get(Node node, ParseState ps) throws Exception {
        String result = Constants.default_get;
        String predicateName = getAttributeOrTagValue(node, ps, "name");
        String varName = getAttributeOrTagValue(node, ps, "var");
        if (predicateName != null) {
            result = ps.getChatSession().getPredicates().get(predicateName).trim();
        } else if (varName != null) {
            result = ps.getVars().get(varName).trim();
        }

        return result;
    }

    /**
     * return the value of a bot property.
     * implements {{{@code <bot name="property"/>}}}
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the bot property or a string indicating the property was not found.
     */
    private String bot(Node node, ParseState ps) throws Exception {
        String result = Constants.default_property;
        String propertyName = getAttributeOrTagValue(node, ps, "name");
        if (propertyName != null) {
            result = ps.getChatSession().getBot().getProperties().get(propertyName).trim();
        }
        return result;
    }

    /**
     * implements formatted date tag <date jformat="format"/> and <date format="format"/>
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the formatted date
     */
    private String date(Node node, ParseState ps) throws Exception {
        String jformat = getAttributeOrTagValue(node, ps, "jformat");
        /*String locale = getAttributeOrTagValue(node, ps, "locale");
        String timezone = getAttributeOrTagValue(node, ps, "timezone");*/

        return new SimpleDateFormat(jformat).format(new Date());
    }

    /**
     * get the value of an index attribute and return it as an integer.
     * if it is not recognized as an integer, return 0
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the the integer intex value
     */
    private int getIndexValue(Node node, ParseState ps) throws Exception {
        int index = 0;

        String value = getAttributeOrTagValue(node, ps, "index");
        if (value != null) {
            index = Integer.parseInt(value) - 1;
        }

        return index;
    }

    /**
     * implements {@code <star index="N"/>}
     * returns the value of input words matching the Nth wildcard (or AIML Set).
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the word sequence matching a wildcard
     */
    private String inputStar(Node node, ParseState ps) throws Exception {
        int index = getIndexValue(node, ps);
        String result = ps.getStarBindings().getInputStars().star(index);
        return result == null ? "" : result.trim();
    }

    /**
     * implements {@code <thatstar index="N"/>}
     * returns the value of input words matching the Nth wildcard (or AIML Set) in <that></that>.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the word sequence matching a wildcard
     */
    private String thatStar(Node node, ParseState ps) throws Exception {
        int index = getIndexValue(node, ps);
        String result = ps.getStarBindings().getThatStars().star(index);
        return result == null ? "" : result.trim();
    }

    /**
     * implements <topicstar/> and <topicstar index="N"/>
     * returns the value of input words matching the Nth wildcard (or AIML Set) in a topic pattern.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the word sequence matching a wildcard
     */
    private String topicStar(Node node, ParseState ps) throws Exception {
        int index = getIndexValue(node, ps);
        String result = ps.getStarBindings().getTopicStars().star(index);
        return result == null ? "" : result.trim();
    }

    /**
     * return the client ID.
     * implements {@code <id/>}
     *
     * @param ps AIML parse state
     * @return client ID
     */
    private String id(ParseState ps) {
        return ps.getChatSession().getCustomerId();
    }

    /**
     * return the size of the robot brain (number of AIML categories loaded).
     * implements {@code <size/>}
     *
     * @param ps AIML parse state
     * @return bot brain size
     */
    private String size(ParseState ps) {
        return String.valueOf(ps.getChatSession().getBot().getBrain().getCategories().size());
    }

    /**
     * return the size of the robot vocabulary (number of words the bot can recognize).
     * implements {@code <vocabulary/>}
     *
     * @param ps AIML parse state
     * @return bot vocabulary size
     */
    private String vocabulary(ParseState ps) {
        return String.valueOf(ps.getChatSession().getBot().getBrain().getVocabulary().size());
    }

    /**
     * return a string indicating the name and version of the AIML program.
     * implements {@code <program/>}
     *
     * @return AIML program name and version.
     */
    private String program() {
        return Constants.program_name_version;
    }

    /**
     * implements the (template-side) {@code <that index="M,N"/>}    tag.
     * returns a normalized sentence.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the nth last sentence of the bot's mth last reply.
     */
    private String that(Node node, ParseState ps) throws Exception {
        int i = 0;
        int j = 0;
        String value = getAttributeOrTagValue(node, ps, "index");
        if (value != null) {
            String[] pair = value.split(",");
            i = Integer.parseInt(pair[0]) - 1;
            j = Integer.parseInt(pair[1]) - 1;

            if (LOG.isDebugEnabled()) {
                LOG.debug("That index={},{}", i, j);
            }
        }

        String that = Constants.unknown_history_item;
        History hist = ps.getChatSession().getThatHistory().get(i);
        if (hist != null) {
            that = (String) hist.get(j);
        }

        return that.trim();
    }

    /**
     * implements {@code <input index="N"/>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the nth last sentence input to the bot
     */
    private String input(Node node, ParseState ps) throws Exception {
        int index = getIndexValue(node, ps);
        return ps.getChatSession().getInputHistory().getString(index);
    }

    /**
     * implements {@code <request index="N"/>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the nth last multi-sentence request to the bot.
     */
    private String request(Node node, ParseState ps) throws Exception {
        int index = getIndexValue(node, ps);
        return ps.getChatSession().getRequestHistory().getString(index).trim();
    }

    /**
     * implements {@code <response index="N"/>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the bot's Nth last multi-sentence response.
     */
    private String response(Node node, ParseState ps) throws Exception {
        int index = getIndexValue(node, ps);
        return ps.getChatSession().getResponseHistory().getString(index).trim();
    }

    /**
     * implements {@code <system>} tag.
     * Evaluate the contents, and try to execute the result as
     * a command in the underlying OS shell.
     * Read back and return the result of this command.
     * <p>
     * The timeout parameter allows the botmaster to set a timeout
     * in ms, so that the <system></system>   command returns eventually.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the result of executing the system command or a string indicating the command failed.
     */
    private String system(Node node, ParseState ps) throws Exception {
        Set<String> attributeNames = Collections.singleton("timeout");
        String evaluatedContents = evalTagContent(node, ps, attributeNames);

        if (LOG.isDebugEnabled()) {
            LOG.debug("System {}", evaluatedContents);
        }

        Process p = null;
        try {
            p = Runtime.getRuntime().exec(evaluatedContents);

            StringBuilder result = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String data;
                while ((data = br.readLine()) != null) {
                    result.append(data).append("\n");
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Result = {}", result);
            }

            return result.toString();
        } catch (Exception ex) {
            LOG.error("System [{}] error: {}", evaluatedContents, ex);
            return Constants.system_failed;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /**
     * implements {@code <think>} tag
     * <p>
     * Evaluate the tag contents but return a blank.
     * "Think but don't speak."
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return a blank empty string
     */
    private String think(Node node, ParseState ps) throws Exception {
        evalTagContent(node, ps, null);
        return "";
    }

    /**
     * Transform a string of words (separtaed by spaces) into
     * a string of individual characters (separated by spaces).
     * Explode "ABC DEF" = "A B C D E F".
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return exploded string
     */
    private String explode(Node node, ParseState ps) throws Exception {
        String str = evalTagContent(node, ps, null);

        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray()) {
            sb.append(" ").append(ch);
        }

        str = sb.toString();
        while (str.contains("  ")) {
            str = str.replace("  ", " ");
        }

        return str.trim();
    }

    /**
     * apply the AIML normalization pre-processor to the evaluated tag content.
     * implements {@code <normalize>} tag.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return normalized string
     */
    private String normalize(Node node, ParseState ps) throws Exception {
        String result = evalTagContent(node, ps, null);
        return ps.getChatSession().getBot().getPreProcessor().normalize(result);
    }

    /**
     * apply the AIML denormalization pre-processor to the evaluated tag content.
     * implements {@code <normalize>} tag.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return denormalized string
     */
    private String denormalize(Node node, ParseState ps) throws Exception {
        String result = evalTagContent(node, ps, null);
        return ps.getChatSession().getBot().getPreProcessor().denormalize(result);
    }

    /**
     * evaluate tag contents and return result in upper case
     * implements {@code <uppercase>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return uppercase string
     */
    private String uppercase(Node node, ParseState ps) throws Exception {
        return evalTagContent(node, ps, null).toUpperCase();
    }

    /**
     * evaluate tag contents and return result in lower case
     * implements {@code <lowercase>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return lowercase string
     */
    private String lowercase(Node node, ParseState ps) throws Exception {
        return evalTagContent(node, ps, null).toLowerCase();
    }

    /**
     * evaluate tag contents and capitalize each word.
     * implements {@code <formal>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return capitalized string
     */
    private String formal(Node node, ParseState ps) throws Exception {
        String string = evalTagContent(node, ps, null);

        boolean found = false;
        char[] chars = string.toLowerCase().toCharArray();
        for (int i = 0, len = chars.length; i < len; ++i) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i])) {
                found = false;
            }
        }

        return String.valueOf(chars);
    }

    /**
     * evaluate tag contents and capitalize the first word.
     * implements {@code <sentence>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return string with first word capitalized
     */
    private String sentence(Node node, ParseState ps) throws Exception {
        String result = evalTagContent(node, ps, null);
        return result.length() > 1 ? result.substring(0, 1).toUpperCase() + result.substring(1, result.length()) : "";
    }

    /**
     * evaluate tag contents and swap 1st and 2nd person pronouns
     * implements {@code <person>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return sentence with pronouns swapped
     */
    private String person(Node node, ParseState ps) throws Exception {
        String result = node.hasChildNodes() ? evalTagContent(node, ps, null) : ps.getStarBindings().getInputStars().star(0);
        result = " " + result + " ";
        result = ps.getChatSession().getBot().getPreProcessor().person(result);
        return result.trim();
    }

    /**
     * evaluate tag contents and swap 1st and 3rd person pronouns
     * implements {@code <person2>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return sentence with pronouns swapped
     */
    private String person2(Node node, ParseState ps) throws Exception {
        String result = node.hasChildNodes() ? evalTagContent(node, ps, null) : ps.getStarBindings().getInputStars().star(0);
        result = " " + result + " ";
        result = ps.getChatSession().getBot().getPreProcessor().person2(result);
        return result.trim();
    }

    /**
     * implements {@code <gender>} tag
     * swaps gender pronouns
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return sentence with gender ronouns swapped
     */
    private String gender(Node node, ParseState ps) throws Exception {
        String result = evalTagContent(node, ps, null);
        result = " " + result + " ";
        result = ps.getChatSession().getBot().getPreProcessor().gender(result);
        return result.trim();
    }

    /**
     * implements {@code <random>} tag
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return response randomly selected from the list
     */
    private String random(Node node, ParseState ps) throws Exception {
        NodeList childList = node.getChildNodes();
        List<Node> liList = new ArrayList<>();
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeName().equals("li")) {
                liList.add(childList.item(i));
            }
        }

        int index = (int) (Math.random() * liList.size());
        return evalTagContent(liList.get(index), ps, null);
    }

    private String unevaluatedAIML(Node node, ParseState ps) throws Exception {
        String result = learnEvalTagContent(node, ps);
        return unevaluatedXML(result, node);
    }

    private String recursLearn(Node node, ParseState ps) throws Exception {
        String nodeName = node.getNodeName();
        switch (nodeName) {
            case "#text":
                return node.getNodeValue();
            case "eval":
                return evalTagContent(node, ps, null);
            default:
                return unevaluatedAIML(node, ps);
        }
    }

    private String learnEvalTagContent(Node node, ParseState ps) throws Exception {
        StringBuilder result = new StringBuilder();
        Node child;
        NodeList childList = node.getChildNodes();
        for (int i = 0, len = childList.getLength(); i < len; ++i) {
            child = childList.item(i);
            result.append(recursLearn(child, ps));
        }
        return result.toString();
    }

    private String learn(Node node, ParseState ps) throws Exception {
        String pattern = "";
        String that = "*";
        String template = "";
        Category c;
        NodeList childList = node.getChildNodes(), grandChildList;
        for (int i = 0, len = childList.getLength(); i < len; ++i) {
            if (childList.item(i).getNodeName().equals("category")) {
                grandChildList = childList.item(i).getChildNodes();

                for (int j = 0, len2 = grandChildList.getLength(); j < len2; ++j) {
                    if (grandChildList.item(j).getNodeName().equals("pattern")) {
                        pattern = recursLearn(grandChildList.item(j), ps);
                    } else if (grandChildList.item(j).getNodeName().equals("that")) {
                        that = recursLearn(grandChildList.item(j), ps);
                    } else if (grandChildList.item(j).getNodeName().equals("template")) {
                        template = recursLearn(grandChildList.item(j), ps);
                    }
                }

                pattern = pattern.substring("<pattern>".length(), pattern.length() - "</pattern>".length());
                pattern = pattern.toUpperCase().replaceAll("\n", " ").replaceAll("[ ]+", " ");

                if (that.length() >= "<that></that>".length()) {
                    that = that.substring("<that>".length(), that.length() - "</that>".length());
                }
                that = that.toUpperCase().replaceAll("\n", " ").replaceAll("[ ]+", " ");

                if (template.length() >= "<template></template>".length()) {
                    template = template.substring("<template>".length(), template.length() - "</template>".length());
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Learn Pattern = {}, That = {}, Template = {}", pattern, that, template);
                }

                if (node.getNodeName().equals("learn")) {
                    c = new Category(0, pattern, that, "*", template, Constants.null_aiml_file);
                    ps.getChatSession().getBot().getLearnGraph().addCategory(c);
                } else {
                    c = new Category(0, pattern, that, "*", template, Constants.learnf_aiml_file);
                    ps.getChatSession().getBot().getLearnfGraph().addCategory(c);
                }
                ps.getChatSession().getBot().getBrain().addCategory(c);
            }
        }

        return "";
    }

    /**
     * implements {@code <condition> with <loop/>}
     * re-evaluate the conditional statement until the response does not contain {@code <loop/>}
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return result of conditional expression
     */
    private String loopCondition(Node node, ParseState ps) throws Exception {
        boolean loop = true;
        int loopCnt = 0;
        StringBuilder sb = new StringBuilder();
        String loopResult;
        while (loop && loopCnt < Constants.max_loops) {
            loopResult = condition(node, ps);
            if (loopResult.trim().equals(Constants.too_much_recursion)) {
                return Constants.too_much_recursion;
            }

            if (loopResult.contains("<loop/>")) {
                loopResult = loopResult.replace("<loop/>", "");
                loop = true;
            } else {
                loop = false;
            }

            sb.append(loopResult);
            ++loopCnt;
        }

        String result = sb.toString();
        if (loopCnt >= Constants.max_loops) {
            result = Constants.too_much_looping;
        }
        return result;
    }

    /**
     * implements all 3 forms of the {@code <condition> tag}
     * In AIML 2.0 the conditional may return a {@code <loop/>}
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return result of conditional expression
     */
    private String condition(Node node, ParseState ps) throws Exception {
        NodeList childList = node.getChildNodes();
        List<Node> liList = new ArrayList<>();
        // First check if the <condition> has an attribute "name".  If so, get the predicate name.
        for (int i = 0, len = childList.getLength(); i < len; ++i) {
            if (childList.item(i).getNodeName().equals("li")) {
                liList.add(childList.item(i));
            }
        }

        String varName, predicate, value;
        varName = getAttributeOrTagValue(node, ps, "var");
        predicate = getAttributeOrTagValue(node, ps, "name");
        Set<String> attributeNames = Stream.of("name", "var", "value").collect(Collectors.toSet());
        int size = liList.size();
        // if there are no <li> nodes, this is a one-shot condition.
        if (size == 0 &&
                (value = getAttributeOrTagValue(node, ps, "value")) != null &&
                predicate != null &&
                ps.getChatSession().getPredicates().get(predicate).equalsIgnoreCase(value)) {
            return evalTagContent(node, ps, attributeNames);
        } else if (size == 0 &&
                (value = getAttributeOrTagValue(node, ps, "value")) != null &&
                varName != null &&
                ps.getVars().get(varName).equalsIgnoreCase(value)) {
            return evalTagContent(node, ps, attributeNames);
        } else {
            String liPredicate;
            String liVarName;
            // otherwise this is a <condition> with <li> items
            for (Node n : liList) {
                liPredicate = predicate == null ? getAttributeOrTagValue(n, ps, "name") : predicate;
                liVarName = varName == null ? getAttributeOrTagValue(n, ps, "var") : varName;

                value = getAttributeOrTagValue(n, ps, "value");
                if (value != null) {
                    // if the predicate equals the value, return the <li> item.
                    if (liPredicate != null &&
                            (ps.getChatSession().getPredicates().get(liPredicate).equalsIgnoreCase(value) ||
                                    ps.getChatSession().getPredicates().containsKey(liPredicate) && value.equals("*"))) {
                        return evalTagContent(n, ps, attributeNames);
                    } else if (liVarName != null &&
                            (ps.getVars().get(liVarName).equalsIgnoreCase(value) ||
                                    ps.getVars().containsKey(liPredicate) && value.equals("*"))) {
                        return evalTagContent(n, ps, attributeNames);
                    }
                } else {
                    // this is a terminal <li> with no predicate or value, i.e. the default condition.
                    return evalTagContent(n, ps, attributeNames);
                }
            }
        }

        return "";
    }

    private String javascript(Node node, ParseState ps) throws Exception {
        String script = evalTagContent(node, ps, null);

        if (LOG.isDebugEnabled()) {
            LOG.debug("evaluating {}", script);
        }

        return "" + JS_ENGINE.eval(script);
    }

    private String firstWord(String sentence) {
        String content = sentence == null ? "" : sentence;
        content = content.trim();
        if (content.contains(" ")) {
            return content.substring(0, content.indexOf(" "));
        } else if (content.length() > 0) {
            return content;
        } else {
            return Constants.default_list_item;
        }
    }

    private String restWords(String sentence) {
        String content = sentence == null ? "" : sentence;
        content = content.trim();
        if (content.contains(" ")) {
            return content.substring(content.indexOf(" ") + 1, content.length());
        } else {
            return Constants.default_list_item;
        }
    }

    public String first(Node node, ParseState ps) throws Exception {
        String content = evalTagContent(node, ps, null);
        return firstWord(content);
    }

    public String rest(Node node, ParseState ps) throws Exception {
        String content = evalTagContent(node, ps, null);
        content = ps.getChatSession().getBot().getPreProcessor().normalize(content);
        return restWords(content);
    }

    private String resetlearnf(ParseState ps) {
        ps.getChatSession().getBot().deleteLearnfCategories();
        return "Deleted Learnf Categories";
    }

    private String resetlearn(ParseState ps) {
        ps.getChatSession().getBot().deleteLearnCategories();
        return "Deleted Learn Categories";
    }

    /**
     * Recursively descend the XML DOM tree, evaluating AIML and building a response.
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     */
    private String recursEval(Node node, ParseState ps) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("AIMLProcessor.recursEval(node: {}, ps: {}, nodeName: {}, node string: {})", node, ps, node.getNodeName(), DomUtils.nodeToString(node));
        }

        String nodeName = node.getNodeName();
        if (nodeName.equals("#text")) {
            return node.getNodeValue();
        } else if (nodeName.equals("#comment")) {
            //MagicBooleans.trace("in AIMLProcessor.recursEval(), comment = "+node.getTextContent());
            return "";
        } else if (nodeName.equals("template")) {
            return evalTagContent(node, ps, null);
        } else if (nodeName.equals("random")) {
            return random(node, ps);
        } else if (nodeName.equals("condition")) {
            return loopCondition(node, ps);
        } else if (nodeName.equals("srai")) {
            return srai(node, ps);
        } else if (nodeName.equals("sr")) {
            return respond(ps.getStarBindings().getInputStars().star(0), ps.getThat(), ps.getTopic(), ps.getChatSession());
        } else if (nodeName.equals("sraix")) {
            return null;
        } else if (nodeName.equals("set")) {
            return set(node, ps);
        } else if (nodeName.equals("get")) {
            return get(node, ps);
        } else if (nodeName.equals("map")) {
            return map(node, ps);
        } else if (nodeName.equals("bot")) {
            return bot(node, ps);
        } else if (nodeName.equals("id")) {
            return id(ps);
        } else if (nodeName.equals("size")) {
            return size(ps);
        } else if (nodeName.equals("vocabulary")) {
            return vocabulary(ps);
        } else if (nodeName.equals("program")) {
            return program();
        } else if (nodeName.equals("date")) {
            return date(node, ps);
        }/* else if (nodeName.equals("interval")) {
            return interval(node, ps);
        }*/ else if (nodeName.equals("think")) {
            return think(node, ps);
        } else if (nodeName.equals("system")) {
            return system(node, ps);
        } else if (nodeName.equals("explode")) {
            return explode(node, ps);
        } else if (nodeName.equals("normalize")) {
            return normalize(node, ps);
        } else if (nodeName.equals("denormalize")) {
            return denormalize(node, ps);
        } else if (nodeName.equals("uppercase")) {
            return uppercase(node, ps);
        } else if (nodeName.equals("lowercase")) {
            return lowercase(node, ps);
        } else if (nodeName.equals("formal")) {
            return formal(node, ps);
        } else if (nodeName.equals("sentence")) {
            return sentence(node, ps);
        } else if (nodeName.equals("person")) {
            return person(node, ps);
        } else if (nodeName.equals("person2")) {
            return person2(node, ps);
        } else if (nodeName.equals("gender")) {
            return gender(node, ps);
        } else if (nodeName.equals("star")) {
            return inputStar(node, ps);
        } else if (nodeName.equals("thatstar")) {
            return thatStar(node, ps);
        } else if (nodeName.equals("topicstar")) {
            return topicStar(node, ps);
        } else if (nodeName.equals("that")) {
            return that(node, ps);
        } else if (nodeName.equals("input")) {
            return input(node, ps);
        } else if (nodeName.equals("request")) {
            return request(node, ps);
        } else if (nodeName.equals("response")) {
            return response(node, ps);
        } else if (nodeName.equals("learn") || nodeName.equals("learnf")) {
            return learn(node, ps);
        } else if (nodeName.equals("javascript")) {
            return javascript(node, ps);
        } else if (nodeName.equals("first")) {
            return first(node, ps);
        } else if (nodeName.equals("rest")) {
            return rest(node, ps);
        } else if (nodeName.equals("resetlearnf")) {
            return resetlearnf(ps);
        } else if (nodeName.equals("resetlearn")) {
            return resetlearn(ps);
        } else if (extension != null && extension.extensionTagSet().contains(nodeName)) {
            return extension.recursEval(node, ps);
        } else {
            return (genericXML(node, ps));
        }
    }

    /**
     * evaluate an AIML template expression
     *
     * @param template AIML template contents
     * @param ps       AIML Parse state
     * @return result of evaluating template.
     */
    private String evalTemplate(String template, ParseState ps) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("AIMLProcessor.evalTemplate(template: {}, ps: {})", template, ps);
        }

        template = "<template>" + template + "</template>";
        Node root = DomUtils.parseString(template);
        String response = recursEval(root, ps);

        if (LOG.isDebugEnabled()) {
            LOG.debug("in AIMLProcessor.evalTemplate() returning: {}", response);
        }

        return response;
    }
}
