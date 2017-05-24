package org.alicebot.ab;

import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * History object to maintain history of input, that request and response
 *
 * @param <T> type of history object
 */
public class History<T> {

    private static final Logger LOG = LoggerFactory.getLogger(History.class);

    private Object[] history;
    private String name;

    /**
     * Constructor with default history name
     */
    public History() {
        this("unknown");
    }

    /**
     * Constructor with history name
     *
     * @param name name of history
     */
    public History(String name) {
        this.name = name;
        history = new Object[Constants.max_history];
    }

    /**
     * add an item to history
     *
     * @param item history item to add
     */
    public void add(T item) {
        System.arraycopy(history, 0, history, 1, Constants.max_history - 1);
        history[0] = item;
    }

    /**
     * get an item from history
     *
     * @param index history index
     * @return history item
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        return index < Constants.max_history ? (T) history[index] : null;
    }

    /**
     * get a String history item
     *
     * @param index history index
     * @return history item
     */
    public String getString(int index) {
        if (index < Constants.max_history) {
            return history[index] == null ? Constants.unknown_history_item : (String) history[index];
        } else {
            return null;
        }
    }

    /**
     * print history
     */
    public void printHistory() {
        T obj;
        for (int i = 0; (obj = get(i)) != null; i++) {
            LOG.info("{} History {} = {}", name, (i + 1), obj);
            if (obj instanceof History) {
                ((History) obj).printHistory();
            }
        }
    }
}
