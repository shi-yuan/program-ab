package org.alicebot.ab;

import org.alicebot.ab.aiml.Category;
import org.alicebot.ab.constant.Constants;

import java.util.List;
import java.util.Map;

/**
 * Nodemapper data structure.  In order to minimize memory overhead this class has no methods.
 * Operations on Nodemapper objects are performed by NodemapperUtils class
 */
public class Nodemapper {

    private Category category;
    private int height = Constants.max_graph_height;
    private StarBindings starBindings;
    private Map<String, Nodemapper> map;
    private String key;
    private Nodemapper value;
    private boolean shortCut = false;
    private List<String> sets;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public StarBindings getStarBindings() {
        return starBindings;
    }

    public void setStarBindings(StarBindings starBindings) {
        this.starBindings = starBindings;
    }

    public Map<String, Nodemapper> getMap() {
        return map;
    }

    public void setMap(Map<String, Nodemapper> map) {
        this.map = map;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Nodemapper getValue() {
        return value;
    }

    public void setValue(Nodemapper value) {
        this.value = value;
    }

    public boolean isShortCut() {
        return shortCut;
    }

    public void setShortCut(boolean shortCut) {
        this.shortCut = shortCut;
    }

    public List<String> getSets() {
        return sets;
    }

    public void setSets(List<String> sets) {
        this.sets = sets;
    }
}
