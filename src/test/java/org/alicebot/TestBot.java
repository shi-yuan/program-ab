package org.alicebot;

import org.alicebot.constant.MagicNumbers;
import org.alicebot.constant.MagicStrings;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestBot {

    private Chat chatSession;

    @Before
    public void setUp() {
        String path = "src/test/resources";
        //AIMLProcessor.extension = new PCAIMLProcessorExtension();

        System.out.println("Working Directory = " + MagicStrings.root_path);

        Graphmaster.enableShortCuts = true;

        String botName = "test";

        Bot bot = new Bot(
                path + "/aiml",
                path + "/config",
                path + "/log",
                path + "/sets",
                path + "/maps",
                botName);

        if (bot.getBrain().getCategories().size() < MagicNumbers.brain_print_size) {
            bot.getBrain().printgraph();
        }

        chatSession = new Chat(bot, true);
        bot.getBrain().nodeStats();
    }

    @Test
    public void testIQ() throws Exception {
        String pairs[][] = {
                {"Hello Alice", "Hello User"},

                {"I like mango", "I too like mango."},
                {"A mango is a fruit", "How mango can not be a fruit?"},

                {"Do you know who Albert Einstein is", "Albert Einstein was a German physicist."},
                {"Bye", "Good Bye!"},
                {"Bye Alice!", "Good Bye!"},
                {"Factory", "Development Center!"},
                {"Industry", "Development Center!"},
                {"I love going to school daily.", "School is an important institution in a child's life."},
                {"I like my school.", "School is an important institution in a child's life."},

                {"I am Mahesh", "Hello Mahesh!"},
                {"Good Night", "Hi Mahesh! Thanks for the conversation!"},

                {"What about movies?", "Do you like comedy movies"},
                {"No", "Ok! But I like comedy movies."},

                {"let discuss movies", "Yes movies"},
                {"Comedy movies are nice to watch", "Watching good movie refreshes our minds."},
                {"I like watching comedy", "I too like watching comedy."},

                {"My name is Mahesh", "Hello!"},
                {"Byeee", "Hi Mahesh Thanks for the conversation!"},

                {"how are you feeling today", "I am happy!"}
        };

        String request, expected, response;
        for (String[] pair : pairs) {
            request = pair[0];
            expected = pair[1];

            System.out.println("STATE=" + request + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));

            response = chatSession.multisentenceRespond(request);

            assertThat(response, containsString(expected));
        }

        request = "Hi";
        response = chatSession.multisentenceRespond(request);
        System.out.println("STATE=" + request + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));
        anyOf(containsString("Hi! Nice to meet you!"), containsString("Hello!")).matches(response);
    }
}
