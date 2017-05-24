package org.alicebot.ab.aiml.element;

import org.alicebot.ab.aiml.ParseState;
import org.w3c.dom.Node;

public interface AIMLElement {

    String evaluate(Node node, ParseState ps);
}
