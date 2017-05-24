package org.alicebot.ab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linked list representation of Pattern Path and Input Path
 */
public class Path {

    private static final Logger LOG = LoggerFactory.getLogger(Path.class);

    private String word;
    private Path next;
    private int length;

    /**
     * Constructor - class has public members
     */
    private Path() {
        next = null;
        word = null;
        length = 0;
    }

    public String getWord() {
        return word;
    }

    public Path getNext() {
        return next;
    }

    public int getLength() {
        return length;
    }

    /**
     * convert a sentence (a string consisting of words separated by single spaces) into a Path
     *
     * @param sentence sentence to convert
     * @return sentence in Path form
     */
    public static Path sentenceToPath(String sentence) {
        return arrayToPath(sentence.trim().split(" "));
    }

    /**
     * The inverse of sentenceToPath
     *
     * @param path input path
     * @return sentence
     */
    public static String pathToSentence(Path path) {
        StringBuilder result = new StringBuilder();
        for (Path p = path; p != null; p = p.next) {
            result.append(" ").append(p.word);
        }
        return result.toString().trim();
    }

    /**
     * convert an array of strings to a Path
     *
     * @param array array of strings
     * @return sequence of strings as Path
     */
    private static Path arrayToPath(String[] array) {
        Path tail = null;
        Path head = null;
        for (int i = array.length - 1; i >= 0; i--) {
            head = new Path();
            head.word = array[i];
            head.next = tail;
            if (tail == null) {
                head.length = 1;
            } else {
                head.length = tail.length + 1;
            }
            tail = head;
        }

        return head;
    }

    /**
     * print a Path
     */
    public void print() {
        StringBuilder result = new StringBuilder();
        for (Path p = this; p != null; p = p.next) {
            result.append(p.word).append(",");
        }

        String str = result.toString();
        if (str.endsWith(",")) {
            str = str.substring(0, str.length() - 1);
        }

        LOG.info(str);
    }
}
