package org.alicebot.ab.aiml;

import org.w3c.dom.Node;

import java.util.Set;

/**
 * The interface needed to implement AIML Extension
 * <p>
 * A class implementing AIMLProcessorExtension should return
 * a Set of tag names and provide a function to recursively evaluate the
 * XML parse tree for each node associated with a new tag.
 */
public interface AIMLProcessorExtension {
    /**
     * provide the AIMLProcessor with a list of extension tag names.
     *
     * @return Set of extension tag names
     */
    Set<String> extensionTagSet();

    /**
     * recursively evaluate AIML from a node corresponding an extension tag
     *
     * @param node current XML parse node
     * @param ps   current parse state
     * @return result of evaluating AIML
     */
    String recursEval(Node node, ParseState ps);
}
