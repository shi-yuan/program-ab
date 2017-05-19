import org.alicebot.ab.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        MagicStrings.setRootPath();
        AIMLProcessor.extension = new PCAIMLProcessorExtension();
        mainFunction(args);
    }

    private static void mainFunction(String[] args) throws IOException {
        String botName = "alice2";
        String action = "chat";

        MagicBooleans.jp_tokenize = false;
        MagicBooleans.trace_mode = true;
        System.out.println(MagicStrings.program_name_version);

        String[] splitArg;
        String option, value;
        for (String s : args) {
            splitArg = s.split("=");
            if (splitArg.length >= 2) {
                option = splitArg[0];
                value = splitArg[1];

                switch (option) {
                    case "bot":
                        botName = value;
                        break;
                    case "action":
                        action = value;
                        break;
                    case "trace":
                        MagicBooleans.trace_mode = value.equals("true");
                        break;
                    case "morph":
                        MagicBooleans.jp_tokenize = value.equals("true");
                        break;
                }
            }
        }

        if (MagicBooleans.trace_mode) {
            System.out.println("Working Directory = " + MagicStrings.root_path);
        }

        Graphmaster.enableShortCuts = true;

        Bot bot = new Bot(botName, MagicStrings.root_path, action);

        if (bot.brain.getCategories().size() < MagicNumbers.brain_print_size) {
            bot.brain.printgraph();
        }

        if (MagicBooleans.trace_mode) {
            System.out.println("Action = '" + action + "'");
        }

        if (action.equals("chat") || action.equals("chat-app")) {
            boolean doWrites = !action.equals("chat-app");
            TestAB.testChat(bot, doWrites, MagicBooleans.trace_mode);
        } else if (action.equals("ab")) {
            TestAB.testAB(bot, TestAB.sample_file);
        } else if (action.equals("abwq")) {
            AB ab = new AB(bot, TestAB.sample_file);
            //ab.abwq();
        } else if (action.equals("test")) {
            TestAB.runTests(bot, MagicBooleans.trace_mode);
        } else if (action.equals("shadow")) {
            MagicBooleans.trace_mode = false;
            bot.shadowChecker();
        } else if (action.equals("iqtest")) {
            ChatTest ct = new ChatTest(bot);
            try {
                ct.testMultisentenceRespond();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Unrecognized action " + action);
        }
    }
}
