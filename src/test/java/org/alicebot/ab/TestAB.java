package org.alicebot.ab;

import org.alicebot.ab.utils.IOUtils;

import java.io.IOException;

/**
 * Created by User on 5/13/2014.
 */
public class TestAB {

    public static String sample_file = "sample.random.txt";

    public static void testChat(Bot bot, boolean doWrites, boolean traceMode) throws IOException {
        Chat chatSession = new Chat(bot, doWrites);
        bot.brain.nodeStats();

        MagicBooleans.trace_mode = traceMode;
        String textLine;
        while (true) {
            textLine = IOUtils.readInputTextLine("Human");

            if (textLine == null || textLine.length() < 1) {
                textLine = MagicStrings.null_input;
            }

            switch (textLine) {
                case "q":
                    System.exit(0);
                case "wq":
                    bot.writeQuit();
                    System.exit(0);
                case "iqtest":
                    ChatTest ct = new ChatTest(bot);
                    try {
                        ct.testMultisentenceRespond();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                case "ab":
                    testAB(bot, sample_file);
                    break;
                default:
                    if (MagicBooleans.trace_mode) {
                        System.out.println("STATE=" + textLine + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));
                    }

                    String response = chatSession.multisentenceRespond(textLine);
                    while (response.contains("&lt;")) response = response.replace("&lt;", "<");
                    while (response.contains("&gt;")) response = response.replace("&gt;", ">");
                    IOUtils.writeOutputTextLine("Robot", response);

                    break;
            }
        }
    }

    public static void runTests(Bot bot, boolean traceMode) throws IOException {
        MagicBooleans.qa_test_mode = true;
        Chat chatSession = new Chat(bot, false);
        //        bot.preProcessor.normalizeFile("c:/ab/bots/super/aiml/thats.txt", "c:/ab/bots/super/aiml/normalthats.txt");
        bot.brain.nodeStats();
        MagicBooleans.trace_mode = traceMode;
        IOUtils testInput = new IOUtils(MagicStrings.root_path + "/data/lognormal-500.txt", "read");
        //IOUtils testInput = new IOUtils(MagicStrings.root_path + "/data/callmom-inputs.txt", "read");
        IOUtils testOutput = new IOUtils(MagicStrings.root_path + "/data/lognormal-500-out.txt", "write");
        //IOUtils testOutput = new IOUtils(MagicStrings.root_path + "/data/callmom-outputs.txt", "write");
        String textLine = testInput.readLine();
        int i = 1;
        System.out.print(0);
        while (textLine != null) {
            if (textLine.length() < 1) textLine = MagicStrings.null_input;
            if (textLine.equals("q")) {
                System.exit(0);
            } else if (textLine.equals("wq")) {
                bot.writeQuit();
                System.exit(0);
            } else if (textLine.equals("ab")) {
                testAB(bot, sample_file);
            } else if (textLine.startsWith("#")) {
                testOutput.writeLine(textLine);
            } else {
                if (MagicBooleans.trace_mode) {
                    System.out.println("STATE=" + textLine + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));
                }

                String response = chatSession.multisentenceRespond(textLine);
                while (response.contains("&lt;")) {
                    response = response.replace("&lt;", "<");
                }

                while (response.contains("&gt;")) {
                    response = response.replace("&gt;", ">");
                }

                testOutput.writeLine("Robot: " + response);
            }
            textLine = testInput.readLine();

            System.out.print(".");
            if (i % 10 == 0) System.out.print(" ");
            if (i % 100 == 0) {
                System.out.println("");
                System.out.print(i + " ");
            }
            i++;
        }
        testInput.close();
        testOutput.close();
        System.out.println("");
    }

    public static void testAB(Bot bot, String sampleFile) throws IOException {
        MagicBooleans.trace_mode = true;
        AB ab = new AB(bot, sampleFile);
        ab.ab();
        System.out.println("Begin Pattern Suggestor Terminal Interaction");
        ab.terminalInteraction();
    }
}
