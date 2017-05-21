package org.alicebot.util;

import net.reduls.sanmoku.Morpheme;
import net.reduls.sanmoku.Tagger;
import org.alicebot.aiml.AIMLProcessor;
import org.alicebot.constant.MagicBooleans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class JapaneseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JapaneseUtils.class);

    /**
     * Morphological analysis of an input sentence that contains an AIML pattern.
     *
     * @param sentence
     * @return morphed sentence with one space between words, preserving XML markup and AIML $ operation
     */
    public static String tokenizeSentence(String sentence) {
        LOG.debug("tokenizeSentence {}", sentence);

        if (!MagicBooleans.jp_tokenize) return sentence;

        String result;
        result = tokenizeXML(sentence);

        while (result.contains("$ ")) result = result.replace("$ ", "$");
        while (result.contains("  ")) result = result.replace("  ", " ");
        while (result.contains("anon ")) result = result.replace("anon ", "anon"); // for Triple Store

        result = result.trim();

        LOG.debug("tokenizeSentence '{}'-->'{}'", sentence, result);

        return result;
    }

    /**
     * Tokenize a fragment of the input that contains only text
     *
     * @param fragment fragment of input containing only text and no XML tags
     * @return tokenized fragment
     */
    private static String tokenizeFragment(String fragment) {
        LOG.debug("buildFragment {}", fragment);

        StringBuilder result = new StringBuilder();
        for (Morpheme e : Tagger.parse(fragment)) {
            result.append(e.surface).append(" ");

            LOG.debug("Feature {} Surface={}", e.feature, e.surface);
        }

        return result.toString().trim();
    }

    private static String tokenizeXML(String xmlExpression) {
        LOG.debug("tokenizeXML {}", xmlExpression);

        xmlExpression = "<sentence>" + xmlExpression + "</sentence>";
        Node root = DomUtils.parseString(xmlExpression);
        String response = recursEval(root);

        return AIMLProcessor.trimTag(response, "sentence");
    }

    private static String recursEval(Node node) {
        String nodeName = node.getNodeName();

        LOG.debug("recursEval {}", nodeName);

        switch (nodeName) {
            case "#text":
                return tokenizeFragment(node.getNodeValue());
            case "sentence":
                return evalTagContent(node);
            default:
                return (genericXML(node));
        }
    }

    private static String genericXML(Node node) {
        LOG.debug("genericXML {}", node.getNodeName());

        String result = evalTagContent(node);
        return unevaluatedXML(result, node);
    }

    private static String evalTagContent(Node node) {
        LOG.debug("evalTagContent {}", node.getNodeName());

        StringBuilder result = new StringBuilder();
        NodeList childList = node.getChildNodes();
        Node child;
        for (int i = 0; i < childList.getLength(); i++) {
            child = childList.item(i);
            result.append(recursEval(child));
        }

        return result.toString();
    }

    private static String unevaluatedXML(String result, Node node) {
        StringBuilder attributes = new StringBuilder();
        if (node.hasAttributes()) {
            NamedNodeMap XMLAttributes = node.getAttributes();
            for (int i = 0; i < XMLAttributes.getLength(); i++) {
                attributes.append(" ").append(XMLAttributes.item(i).getNodeName()).append("=\"").append(XMLAttributes.item(i).getNodeValue()).append("\"");
            }
        }

        String nodeName = node.getNodeName();
        if (result.equals("")) {
            return " <" + nodeName + attributes + "/> ";
        } else {
            // add spaces
            return " <" + nodeName + attributes + ">" + result + "</" + nodeName + "> ";
        }
    }
}
