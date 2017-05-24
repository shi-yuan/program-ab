package org.alicebot;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.constant.Constants;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestBot {

    private Chat chatSession;

    @Before
    public void setUp() throws Exception {
        //AIMLProcessor.extension = new PCAIMLProcessorExtension();

        String botName = "test";

        String path = "src/test/resources";
        Bot bot = new Bot(botName, path + "/aiml",
                path + "/config",
                path + "/sets",
                path + "/maps",
                path + "/config/properties.txt");

        if (bot.getBrain().getCategories().size() < Constants.brain_print_size) {
            bot.getBrain().printgraph();
        }

        chatSession = new Chat(bot);
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

            System.out.println("STATE=" + request + ":THAT=" + chatSession.getThatHistory().get(0).get(0) + ":TOPIC=" + chatSession.getPredicates().get("topic"));

            response = chatSession.multisentenceRespond(request);

            assertThat(response, containsString(expected));
        }

        request = "Hi";
        response = chatSession.multisentenceRespond(request);
        System.out.println("STATE=" + request + ":THAT=" + chatSession.getThatHistory().get(0).get(0) + ":TOPIC=" + chatSession.getPredicates().get("topic"));
        anyOf(containsString("Hi! Nice to meet you!"), containsString("Hello!")).matches(response);
    }
}
