package org.alicebot;

/**
 * ParseState is a helper class for AIMLProcessor
 */
public class ParseState {
    public Nodemapper leaf;
    public String input;
    public String that;
    public String topic;
    public Chat chatSession;
    public int depth;
    public Predicates vars;
    public StarBindings starBindings;

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
        this.depth = depth;  // to prevent runaway recursion
        this.vars = new Predicates();
        this.starBindings = leaf.starBindings;
    }
}
