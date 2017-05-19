package org.alicebot.ab;

import java.util.ArrayList;

/**
 * Array of values matching wildcards
 */
public class Stars extends ArrayList<String> {
    public String star(int i) {
        if (i < size())
            return get(i);
        else return null;
    }
}
