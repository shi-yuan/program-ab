package org.alicebot;

import org.alicebot.utils.IOUtils;
import org.alicebot.utils.JapaneseUtils;

import java.io.*;

/**
 * Class encapsulating a chat session between a bot and a client
 */
public class Chat {
    public Bot bot;
    private boolean doWrites;
    public String customerId = MagicStrings.default_Customer_id;
    public History<History> thatHistory = new History<History>("that");
    public History<String> requestHistory = new History<String>("request");
    public History<String> responseHistory = new History<String>("response");
    // public History<String> repetitionHistory = new History<String>("repetition");
    public History<String> inputHistory = new History<String>("input");
    public Predicates predicates = new Predicates();
    public static String matchTrace = "";
    public static boolean locationKnown = false;
    public static String longitude;
    public static String latitude;
    public TripleStore tripleStore = new TripleStore("anon", this);

    /**
     * Constructor  (defualt customer ID)
     *
     * @param bot the bot to chat with
     */
    public Chat(Bot bot) {
        this(bot, true, "0");
    }

    public Chat(Bot bot, boolean doWrites) {
        this(bot, doWrites, "0");
    }

    /**
     * Constructor
     *
     * @param bot        bot to chat with
     * @param customerId unique customer identifier
     */
    private Chat(Bot bot, boolean doWrites, String customerId) {
        this.customerId = customerId;
        this.bot = bot;
        this.doWrites = doWrites;
        History<String> contextThatHistory = new History<String>();
        contextThatHistory.add(MagicStrings.default_that);
        thatHistory.add(contextThatHistory);
        addPredicates();
        addTriples();
        predicates.put("topic", MagicStrings.default_topic);
        predicates.put("jsenabled", MagicStrings.js_enabled);
        if (MagicBooleans.trace_mode) System.out.println("Chat Session Created for bot " + bot.name);
    }

    /**
     * Load all predicate defaults
     */
    private void addPredicates() {
        try {
            predicates.getPredicateDefaults(bot.config_path + "/predicates.txt");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load Triple Store knowledge base
     */
    private int addTriples() {
        int tripleCnt = 0;
        if (MagicBooleans.trace_mode) System.out.println("Loading Triples from " + bot.config_path + "/triples.txt");
        File f = new File(bot.config_path + "/triples.txt");
        if (f.exists())
            try {
                InputStream is = new FileInputStream(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String strLine;
                //Read File Line By Line
                while ((strLine = br.readLine()) != null) {
                    String[] triple = strLine.split(":");
                    if (triple.length >= 3) {
                        String subject = triple[0];
                        String predicate = triple[1];
                        String object = triple[2];
                        tripleStore.addTriple(subject, predicate, object);
                        //Log.i(TAG, "Added Triple:" + subject + " " + predicate + " " + object);
                        tripleCnt++;
                    }
                }
                is.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        if (MagicBooleans.trace_mode) System.out.println("Loaded " + tripleCnt + " triples");
        return tripleCnt;
    }

    /**
     * Chat session terminal interaction
     */
    public void chat() {
        BufferedWriter bw = null;
        String logFile = bot.log_path + "/log_" + customerId + ".txt";
        try {
            //Construct the bw object
            bw = new BufferedWriter(new FileWriter(logFile, true));
            String request = "SET PREDICATES";
            String response;
            while (!request.equals("quit")) {
                System.out.print("Human: ");
                request = IOUtils.readInputTextLine();
                response = multisentenceRespond(request);
                System.out.println("Robot: " + response);
                bw.write("Human: " + request);
                bw.newLine();
                bw.write("Robot: " + response);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
    private String respond(String input, String that, String topic, History<String> contextThatHistory) {
        //MagicBooleans.trace("chat.respond(input: " + input + ", that: " + that + ", topic: " + topic + ", contextThatHistory: " + contextThatHistory + ")");
        boolean repetition = true;
        //inputHistory.printHistory();
        for (int i = 0; i < MagicNumbers.repetition_count; i++) {
            //System.out.println(request.toUpperCase()+"=="+inputHistory.get(i)+"? "+request.toUpperCase().equals(inputHistory.get(i)));
            if (inputHistory.get(i) == null || !input.toUpperCase().equals(inputHistory.get(i).toUpperCase()))
                repetition = false;
        }
        if (input.equals(MagicStrings.null_input)) repetition = false;
        inputHistory.add(input);
        if (repetition) {
            input = MagicStrings.repetition_detected;
        }

        String response;

        response = AIMLProcessor.respond(input, that, topic, this);
        //MagicBooleans.trace("in chat.respond(), response: " + response);
        String normResponse = bot.preProcessor.normalize(response);
        //MagicBooleans.trace("in chat.respond(), normResponse: " + normResponse);
        if (MagicBooleans.jp_tokenize) normResponse = JapaneseUtils.tokenizeSentence(normResponse);
        String sentences[] = bot.preProcessor.sentenceSplit(normResponse);
        for (String sentence : sentences) {
            that = sentence;
            //System.out.println("That "+i+" '"+that+"'");
            if (that.trim().equals("")) that = MagicStrings.default_that;
            contextThatHistory.add(that);
        }
        //MagicBooleans.trace("in chat.respond(), returning: " + result);
        return response.trim() + "  ";
    }

    /**
     * Return bot response given an input and a history of "that" for the current conversational interaction
     *
     * @param input              client input
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(String input, History<String> contextThatHistory) {
        History hist = thatHistory.get(0);
        String that;
        if (hist == null) that = MagicStrings.default_that;
        else that = hist.getString(0);
        return respond(input, that, predicates.get("topic"), contextThatHistory);
    }

    /**
     * return a compound response to a multiple-sentence request. "Multiple" means one or more.
     *
     * @param request client's multiple-sentence input
     * @return
     */
    public String multisentenceRespond(String request) {

        //MagicBooleans.trace("chat.multisentenceRespond(request: " + request + ")");
        StringBuilder response = new StringBuilder();
        matchTrace = "";
        try {
            String normalized = bot.preProcessor.normalize(request);
            normalized = JapaneseUtils.tokenizeSentence(normalized);
            //MagicBooleans.trace("in chat.multisentenceRespond(), normalized: " + normalized);
            String sentences[] = bot.preProcessor.sentenceSplit(normalized);
            History<String> contextThatHistory = new History<String>("contextThat");
            for (String sentence : sentences) {
                //System.out.println("Human: "+sentences[i]);
                AIMLProcessor.trace_count = 0;
                String reply = respond(sentence, contextThatHistory);
                response.append("  ").append(reply);
                //System.out.println("Robot: "+reply);
            }
            requestHistory.add(request);
            responseHistory.add(response.toString());
            thatHistory.add(contextThatHistory);
            response = new StringBuilder(response.toString().replaceAll("[\n]+", "\n"));
            response = new StringBuilder(response.toString().trim());
        } catch (Exception ex) {
            ex.printStackTrace();
            return MagicStrings.error_bot_response;
        }

        if (doWrites) {
            bot.writeLearnfIFCategories();
        }
        //MagicBooleans.trace("in chat.multisentenceRespond(), returning: " + response);
        return response.toString();
    }

    public static void setMatchTrace(String newMatchTrace) {
        matchTrace = newMatchTrace;
    }
}
