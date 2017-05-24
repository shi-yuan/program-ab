package org.alicebot.ab;

import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class encapsulating a chat session between a bot and a client
 */
public class Chat {

    private static final Logger LOG = LoggerFactory.getLogger(Chat.class);

    private Bot bot;
    private String customerId = Constants.default_Customer_id;
    private History<History<String>> thatHistory = new History<>("that");
    private History<String> requestHistory = new History<>("request");
    private History<String> responseHistory = new History<>("response");
    private History<String> inputHistory = new History<>("input");
    private Predicates predicates = new Predicates();

    /**
     * Constructor  (defualt customer ID)
     *
     * @param bot the bot to chat with
     */
    public Chat(Bot bot) throws IOException {
        this(bot, "0");
    }

    /**
     * Constructor
     *
     * @param bot        bot to chat with
     * @param customerId unique customer identifier
     */
    private Chat(Bot bot, String customerId) throws IOException {
        this.bot = bot;
        this.customerId = customerId;

        History<String> contextThatHistory = new History<>();
        contextThatHistory.add(Constants.default_that);
        thatHistory.add(contextThatHistory);

        // TODO:
        predicates.getPredicateDefaults(bot.getConfig_path() + "/predicates.txt");
        predicates.put("topic", Constants.default_topic);
        predicates.put("jsenabled", Constants.js_enabled);

        LOG.info("Chat Session Created for bot {}", bot.getName());
    }

    public Bot getBot() {
        return bot;
    }

    public String getCustomerId() {
        return customerId;
    }

    public History<History<String>> getThatHistory() {
        return thatHistory;
    }

    public History<String> getRequestHistory() {
        return requestHistory;
    }

    public History<String> getResponseHistory() {
        return responseHistory;
    }

    public History<String> getInputHistory() {
        return inputHistory;
    }

    public Predicates getPredicates() {
        return predicates;
    }

    /**
     * return a compound response to a multiple-sentence request. "Multiple" means one or more.
     *
     * @param request client's multiple-sentence input
     * @return
     */
    public String multisentenceRespond(String request) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("chat.multisentenceRespond(request: {})", request);
        }

        String response;
        try {
            String normalized = bot.getPreProcessor().normalize(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("in chat.multisentenceRespond(), normalized: {}", normalized);
            }

            History<String> contextThatHistory = new History<>("contextThat");
            StringBuilder sb = new StringBuilder();
            String sentences[] = bot.getPreProcessor().sentenceSplit(normalized);
            String reply;
            for (String sentence : sentences) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Human: {}", sentence);
                }

                reply = respond(sentence, contextThatHistory);
                sb.append("  ").append(reply);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Robot: {}", reply);
                }
            }
            requestHistory.add(request);
            responseHistory.add(sb.toString());
            thatHistory.add(contextThatHistory);

            response = sb.toString().replaceAll("[\n]+", "\n").trim();
        } catch (Exception ex) {
            LOG.error("Something is wrong with my brain: {}", ex);
            response = Constants.error_bot_response;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("in chat.multisentenceRespond(), returning: {}", response);
        }

        return response;
    }

    /**
     * Return bot response given an input and a history of "that" for the current conversational interaction
     *
     * @param input              client input
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(String input, History<String> contextThatHistory) throws Exception {
        String that;
        History hist = thatHistory.get(0);
        if (hist == null) {
            that = Constants.default_that;
        } else {
            that = hist.getString(0);
        }

        return respond(input, that, predicates.get("topic"), contextThatHistory);
    }

    /**
     * Return bot response to a single sentence input given conversation context
     *
     * @param input              client input
     * @param that               bot's last sentence
     * @param topic              current topic
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(String input, String that, String topic, History<String> contextThatHistory) throws Exception {
        boolean repetition = true;

        for (int i = 0; i < Constants.repetition_count; i++) {
            if (inputHistory.get(i) == null || !input.toUpperCase().equals(inputHistory.get(i).toUpperCase()))
                repetition = false;
        }

        if (input.equals(Constants.null_input)) {
            repetition = false;
        }

        inputHistory.add(input);

        if (repetition) {
            input = Constants.repetition_detected;
        }

        String response = bot.getAimlProcessor().respond(input, that, topic, this);

        String normResponse = bot.getPreProcessor().normalize(response);

        if (LOG.isDebugEnabled()) {
            LOG.debug("in chat.respond(), normResponse: {}", normResponse);
        }

        String sentences[] = bot.getPreProcessor().sentenceSplit(normResponse);
        for (String sentence : sentences) {
            that = sentence;
            if (that.trim().equals("")) {
                that = Constants.default_that;
            }
            contextThatHistory.add(that);
        }

        return response.trim() + "  ";
    }
}
