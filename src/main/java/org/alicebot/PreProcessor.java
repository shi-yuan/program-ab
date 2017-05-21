package org.alicebot;

import org.alicebot.constant.MagicBooleans;
import org.alicebot.constant.MagicNumbers;
import org.alicebot.constant.MagicStrings;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AIML Preprocessor and substitutions
 */
public class PreProcessor {
	static private boolean DEBUG = false;

    private int normalCount = 0;
    private int denormalCount = 0;
    private int personCount = 0;
    private int person2Count = 0;
    private int genderCount = 0;
    private String[] normalSubs = new String[MagicNumbers.max_substitutions];
    private Pattern[] normalPatterns = new Pattern[MagicNumbers.max_substitutions];
    private String[] denormalSubs = new String[MagicNumbers.max_substitutions];
    private Pattern[] denormalPatterns = new Pattern[MagicNumbers.max_substitutions];
    private String[] personSubs = new String[MagicNumbers.max_substitutions];
    private Pattern[] personPatterns = new Pattern[MagicNumbers.max_substitutions];
    private String[] person2Subs = new String[MagicNumbers.max_substitutions];
    private Pattern[] person2Patterns = new Pattern[MagicNumbers.max_substitutions];
    private String[] genderSubs = new String[MagicNumbers.max_substitutions];
    private Pattern[] genderPatterns = new Pattern[MagicNumbers.max_substitutions];

    /**
     * Constructor given bot
     *
     * @param bot       AIML bot
     */
    public PreProcessor (Bot bot) {

        normalCount = readSubstitutions(bot.getConfigPath()+"/normal.txt", normalPatterns, normalSubs);
        denormalCount = readSubstitutions(bot.getConfigPath()+"/denormal.txt", denormalPatterns, denormalSubs);
        personCount = readSubstitutions(bot.getConfigPath() +"/person.txt", personPatterns, personSubs);
        person2Count = readSubstitutions(bot.getConfigPath() +"/person2.txt", person2Patterns, person2Subs);
        genderCount = readSubstitutions(bot.getConfigPath() +"/gender.txt", genderPatterns, genderSubs);
        if (MagicBooleans.trace_mode) System.out.println("Preprocessor: "+normalCount+" norms "+personCount+" persons "+person2Count+" person2 ");
    }

    /**
     * apply normalization substitutions to a request
     *
     * @param request  client input
     * @return         normalized client input
     */
    public String normalize (String request) {
		if (DEBUG) System.out.println("PreProcessor.normalize(request: " + request + ")");
		String result = substitute(request,  normalPatterns, normalSubs, normalCount);
        result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
		if (DEBUG) System.out.println("PreProcessor.normalize() returning: " + result);
        return result;
    }
    /**
     * apply denormalization substitutions to a request
     *
     * @param request  client input
     * @return         normalized client input
     */
    public String denormalize (String request) {
        return substitute(request,  denormalPatterns, denormalSubs, denormalCount);
    }
    /**
     * personal pronoun substitution for {@code <person></person>} tag
     * @param input  sentence
     * @return       sentence with pronouns swapped
     */
    public String person (String input) {
        return substitute(input, personPatterns, personSubs, personCount);

    }
    /**
     * personal pronoun substitution for {@code <person2></person2>} tag
     * @param input  sentence
     * @return       sentence with pronouns swapped
     */
    public String person2 (String input) {
        return substitute(input, person2Patterns, person2Subs, person2Count);

    }
    /**
     * personal pronoun substitution for {@code <gender>} tag
     * @param input  sentence
     * @return       sentence with pronouns swapped
     */
    public String gender (String input) {
        return substitute(input, genderPatterns, genderSubs, genderCount);

    }

    /**
     * Apply a sequence of subsitutions to an input string
     *
     * @param request      input request
     * @param patterns     array of patterns to match
     * @param subs         array of substitution values
     * @param count        number of patterns and substitutions
     * @return             result of applying substitutions to input
     */
    private String substitute(String request, Pattern[] patterns, String[] subs, int count) {
        String result = " "+request+" ";
        int index=0;
        try {
        for (int i = 0; i < count; i++) {
            index = i;
            String replacement =  subs[i];
            Pattern p = patterns[i];
            Matcher m = p.matcher(result);
            //System.out.println(i+" "+patterns[i].pattern()+"-->"+subs[i]);
            if (m.find()) {
                //System.out.println(i+" "+patterns[i].pattern()+"-->"+subs[i]);
                //System.out.println(m.group());
                result = m.replaceAll(replacement);
            }

            //System.out.println(result);
        }
        while (result.contains("  ")) result = result.replace("  "," ");
        result = result.trim();
        //System.out.println("Normalized: "+result);
        } catch (Exception ex)    {
            ex.printStackTrace();
            System.out.println("Request "+request+" Result "+result+" at "+index+" "+patterns[index]+" "+subs[index]);
        }
        return result.trim();
    }

    /**
     * read substitutions from input stream
     *
     * @param in        input stream
     * @param patterns  array of patterns
     * @param subs      array of substitution values
     * @return          number of patterns substitutions read
     */
    private int readSubstitutionsFromInputStream(InputStream in, Pattern[] patterns, String[] subs) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        //Read File Line By Line
        int subCount = 0;
        try {
            while ((strLine = br.readLine()) != null)   {
                //System.out.println(strLine);
                strLine = strLine.trim();
                if (!strLine.startsWith(MagicStrings.text_comment_mark)) {
                    Pattern pattern = Pattern.compile("\"(.*?)\",\"(.*?)\"", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(strLine);
                    if (matcher.find() && subCount < MagicNumbers.max_substitutions) {
                        subs[subCount] = matcher.group(2);
                        String quotedPattern = Pattern.quote(matcher.group(1));
                        //System.out.println("quoted pattern="+quotedPattern);
                        patterns[subCount] = Pattern.compile(quotedPattern, Pattern.CASE_INSENSITIVE);
                        subCount++;
                    }
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return subCount;
    }

    /**
     * read substitutions from a file
     *
     * @param filename  name of substitution file
     * @param patterns  array of patterns
     * @param subs      array of substitution values
     * @return          number of patterns and substitutions read
     */
    private int readSubstitutions(String filename, Pattern[] patterns, String[] subs) {
        int subCount = 0;
        try{

            // Open the file that is the first
            // command line parameter
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object of DataInputStream
                subCount = readSubstitutionsFromInputStream(fstream, patterns, subs);
                //Close the input stream
                fstream.close();
            }
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return (subCount);
    }

    /**
     * Split an input into an array of sentences based on sentence-splitting characters.
     *
     * @param line   input text
     * @return       array of sentences
     */
    public String[] sentenceSplit(String line) {
        line = line.replace("。",".");
        line = line.replace("？","?");
        line = line.replace("！","!");
        //System.out.println("Sentence split "+line);
        String result[] = line.split("[\\.!\\?]");
        for (int i = 0; i < result.length; i++) result[i] = result[i].trim();
        return result;
    }

    /**
     * normalize a file consisting of sentences, one sentence per line.
     *
     * @param infile           input file
     * @param outfile          output file to write results
     */
    public void normalizeFile (String infile, String outfile) {
        try{
            BufferedWriter bw = null;
            FileInputStream fstream = new FileInputStream(infile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            bw = new BufferedWriter(new FileWriter(outfile)) ;
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                strLine = strLine.trim();
                if (strLine.length() > 0) {
                    String norm = normalize(strLine).toUpperCase();
                    String sentences[] = sentenceSplit(norm); {
                        if (sentences.length > 1) {
                            for (String s : sentences)
                                System.out.println(norm+"-->"+s);
                        }
                        for (String sentence : sentences) {
                            sentence = sentence.trim();
                            if (sentence.length() > 0) {
                                //System.out.println("'"+strLine+"'-->'"+norm+"'");
                                bw.write(sentence);
                                bw.newLine();
                            }
                        }
                    }
                }
            }
            bw.close();
            br.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
