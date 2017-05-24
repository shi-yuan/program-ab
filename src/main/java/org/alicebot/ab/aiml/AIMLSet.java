package org.alicebot.ab.aiml;

import org.alicebot.ab.Bot;
import org.alicebot.ab.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * implements AIML Sets
 * <p>
 * The set element is used to set a predicate variable.
 * Predicates are not hardcoded like properties, and can be initialized during conversation.
 * This means that input from the user can be echoed in the value of a predicate.
 */
public class AIMLSet extends HashSet<String> {

    private static final Logger LOG = LoggerFactory.getLogger(AIMLSet.class);

    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");

    private String setName;
    private int maxLength = 1; // there are no empty sets
    private Bot bot;

    public int getMaxLength() {
        return maxLength;
    }

    /**
     * constructor
     *
     * @param name name of set
     */
    public AIMLSet(String name, Bot bot) {
        super();
        this.bot = bot;
        this.setName = name.toLowerCase();
        if (setName.equals(Constants.natural_number_set_name)) {
            maxLength = 1;
        }
    }

    @Override
    public boolean contains(Object obj) {
        String s = (String) obj;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Internal {} contains {} ?", setName, s);
        }

        if (setName.equals(Constants.natural_number_set_name)) {
            Boolean isanumber = PATTERN_NUMBER.matcher(s).matches();

            if (LOG.isDebugEnabled()) {
                LOG.debug("AIMLSet isanumber '{}' {}", s, isanumber);
            }

            return isanumber;
        } else {
            return super.contains(s);
        }
    }

    public void writeAIMLSet() throws IOException {
        LOG.info("Writing AIML Set {}", setName);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(getSetFile(bot.getSets_path())))) {
            for (String p : this) {
                out.write(p.trim());
                out.newLine();
            }
        }
    }

    public int readAIMLSet(Bot bot) throws IOException {
        int cnt = 0;

        File file = getSetFile(bot.getSets_path());

        LOG.info("Reading AIML Set {}", file);

        if (file.exists()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                cnt = readAIMLSetFromInputStream(stream);
            }
        } else {
            LOG.warn("{} not found", file);
        }

        return cnt;
    }

    private int readAIMLSetFromInputStream(InputStream in) throws IOException {
        int cnt = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String strLine;
            while ((strLine = br.readLine()) != null && strLine.length() > 0) {
                cnt++;
                //strLine = bot.preProcessor.normalize(strLine).toUpperCase();
                // assume the set is pre-normalized for faster loading

                strLine = strLine.toUpperCase().trim();
                String[] splitLine = strLine.split(" ");
                int length = splitLine.length;
                if (length > maxLength) {
                    maxLength = length;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("readAIMLSetFromInputStream {}", strLine);
                }

                add(strLine.trim());

                /*Category c = new Category(0, "ISA"+setName.toUpperCase()+" "+strLine.toUpperCase(), "*", "*", "true", MagicStrings.null_aiml_file);
                bot.brain.addCategory(c);*/
            }
        }

        return cnt;
    }

    private File getSetFile(String setsPath) {
        return new File(setsPath + "/" + setName + ".txt");
    }
}
