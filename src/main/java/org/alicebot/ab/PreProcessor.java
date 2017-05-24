package org.alicebot.ab;

import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AIML Preprocessor and substitutions
 */
public class PreProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PreProcessor.class);

    private static final Pattern PATTERN = Pattern.compile("\"(.*?)\",\"(.*?)\"", Pattern.DOTALL);

    private int normalCount = 0;
    private Pattern[] normalPatterns = new Pattern[Constants.max_substitutions];
    private String[] normalSubs = new String[Constants.max_substitutions];

    private int denormalCount = 0;
    private Pattern[] denormalPatterns = new Pattern[Constants.max_substitutions];
    private String[] denormalSubs = new String[Constants.max_substitutions];

    private int personCount = 0;
    private Pattern[] personPatterns = new Pattern[Constants.max_substitutions];
    private String[] personSubs = new String[Constants.max_substitutions];

    private int person2Count = 0;
    private Pattern[] person2Patterns = new Pattern[Constants.max_substitutions];
    private String[] person2Subs = new String[Constants.max_substitutions];

    private int genderCount = 0;
    private Pattern[] genderPatterns = new Pattern[Constants.max_substitutions];
    private String[] genderSubs = new String[Constants.max_substitutions];

    /**
     * Constructor given bot
     *
     * @param bot AIML bot
     */
    public PreProcessor(Bot bot) throws IOException {
        // TODO:
        normalCount = readSubstitutions(bot.getConfig_path() + "/normal.txt", normalPatterns, normalSubs);
        denormalCount = readSubstitutions(bot.getConfig_path() + "/denormal.txt", denormalPatterns, denormalSubs);
        personCount = readSubstitutions(bot.getConfig_path() + "/person.txt", personPatterns, personSubs);
        person2Count = readSubstitutions(bot.getConfig_path() + "/person2.txt", person2Patterns, person2Subs);
        genderCount = readSubstitutions(bot.getConfig_path() + "/gender.txt", genderPatterns, genderSubs);

        LOG.info("Preprocessor: {} norms {} denorms {} persons {} person2 {} genders", normalCount, denormalCount, personCount, person2Count, genderCount);
    }

    /**
     * apply normalization substitutions to a request
     *
     * @param request client input
     * @return normalized client input
     */
    public String normalize(String request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("PreProcessor.normalize(request: {})", request);
        }

        String result = substitute(request, normalPatterns, normalSubs, normalCount);
        result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");

        if (LOG.isDebugEnabled()) {
            LOG.debug("PreProcessor.normalize() returning: {}", result);
        }

        return result;
    }

    /**
     * apply denormalization substitutions to a request
     *
     * @param request client input
     * @return normalized client input
     */
    public String denormalize(String request) {
        return substitute(request, denormalPatterns, denormalSubs, denormalCount);
    }

    /**
     * personal pronoun substitution for {@code <person></person>} tag
     *
     * @param input sentence
     * @return sentence with pronouns swapped
     */
    public String person(String input) {
        return substitute(input, personPatterns, personSubs, personCount);
    }

    /**
     * personal pronoun substitution for {@code <person2></person2>} tag
     *
     * @param input sentence
     * @return sentence with pronouns swapped
     */
    public String person2(String input) {
        return substitute(input, person2Patterns, person2Subs, person2Count);
    }

    /**
     * personal pronoun substitution for {@code <gender>} tag
     *
     * @param input sentence
     * @return sentence with pronouns swapped
     */
    public String gender(String input) {
        return substitute(input, genderPatterns, genderSubs, genderCount);
    }

    /**
     * Apply a sequence of subsitutions to an input string
     *
     * @param request  input request
     * @param patterns array of patterns to match
     * @param subs     array of substitution values
     * @param count    number of patterns and substitutions
     * @return result of applying substitutions to input
     */
    private String substitute(String request, Pattern[] patterns, String[] subs, int count) {
        String result = " " + request + " ";
        String replacement;
        Matcher m;
        for (int i = 0; i < count; i++) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("{} {}-->{}", i, patterns[i].pattern(), subs[i]);
            }

            replacement = subs[i];
            m = patterns[i].matcher(result);
            if (m.find()) {
                result = m.replaceAll(replacement);
            }
        }
        while (result.contains("  ")) {
            result = result.replace("  ", " ");
        }

        return result.trim();
    }

    /**
     * read substitutions from a file
     *
     * @param filename name of substitution file
     * @param patterns array of patterns
     * @param subs     array of substitution values
     * @return number of patterns and substitutions read
     */
    private int readSubstitutions(String filename, Pattern[] patterns, String[] subs) throws IOException {
        int subCount = 0;
        File file = new File(filename);

        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String strLine;
                Matcher matcher;

                while ((strLine = br.readLine()) != null) {
                    strLine = strLine.trim();

                    if (!strLine.startsWith(Constants.text_comment_mark)) {
                        matcher = PATTERN.matcher(strLine);

                        if (matcher.find() && subCount < Constants.max_substitutions) {
                            patterns[subCount] = Pattern.compile(Pattern.quote(matcher.group(1)), Pattern.CASE_INSENSITIVE);
                            subs[subCount] = matcher.group(2);

                            subCount++;
                        }
                    }
                }
            }
        }

        return subCount;
    }

    /**
     * Split an input into an array of sentences based on sentence-splitting characters.
     *
     * @param line input text
     * @return array of sentences
     */
    public String[] sentenceSplit(String line) {
        line = line.replace("。", ".");
        line = line.replace("？", "?");
        line = line.replace("！", "!");

        String result[] = line.split("[\\.!\\?]");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }

        return result;
    }
}
