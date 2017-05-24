package org.alicebot.ab;

/**
 * structure to hold binding of wildcards in input pattern, that pattern and topic pattern
 */
public class StarBindings {

    private Stars inputStars;
    private Stars thatStars;
    private Stars topicStars;

    /**
     * Constructor  -- this class has public members
     */
    public StarBindings() {
        inputStars = new Stars();
        thatStars = new Stars();
        topicStars = new Stars();
    }

    public Stars getInputStars() {
        return inputStars;
    }

    public Stars getThatStars() {
        return thatStars;
    }

    public Stars getTopicStars() {
        return topicStars;
    }
}
