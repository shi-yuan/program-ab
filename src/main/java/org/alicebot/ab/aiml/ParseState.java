package org.alicebot.ab.aiml;

import org.alicebot.ab.Chat;
import org.alicebot.ab.Nodemapper;
import org.alicebot.ab.Predicates;
import org.alicebot.ab.StarBindings;

/**
 * ParseState is a helper class for AIMLProcessor
 */
public class ParseState {

    private Nodemapper leaf;
    private String input;
    private String that;
    private String topic;
    private Chat chatSession;
    private int depth;
    private Predicates vars;
    private StarBindings starBindings;

    /**
     * Constructor - class has public members
     *
     * @param depth       depth in parse tree
     * @param chatSession client session
     * @param input       client input
     * @param that        bot's last sentence
     * @param topic       current topic
     * @param leaf        node containing the category processed
     */
    public ParseState(int depth, Chat chatSession, String input, String that, String topic, Nodemapper leaf) {
        this.chatSession = chatSession;
        this.input = input;
        this.that = that;
        this.topic = topic;
        this.leaf = leaf;
        this.depth = depth; // to prevent runaway recursion
        this.vars = new Predicates();
        this.starBindings = leaf.getStarBindings();
    }

    public Nodemapper getLeaf() {
        return leaf;
    }

    public String getInput() {
        return input;
    }

    public String getThat() {
        return that;
    }

    public String getTopic() {
        return topic;
    }

    public Chat getChatSession() {
        return chatSession;
    }

    public int getDepth() {
        return depth;
    }

    public Predicates getVars() {
        return vars;
    }

    public StarBindings getStarBindings() {
        return starBindings;
    }
}
