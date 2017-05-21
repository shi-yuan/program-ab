package org.alicebot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IOUtils {

    private static final Logger LOG = LoggerFactory.getLogger(IOUtils.class);

    private BufferedReader reader;
    private BufferedWriter writer;

    public IOUtils(String filePath, String mode) throws IOException {
        if (mode.equals("read")) {
            reader = new BufferedReader(new FileReader(filePath));
        } else if (mode.equals("write")) {
            Files.delete(Paths.get(filePath));
            writer = new BufferedWriter(new FileWriter(filePath, true));
        }
    }

    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
    }

    public static String readInputTextLine() throws IOException {
        return readInputTextLine(null);
    }

    private static String readInputTextLine(String prompt) throws IOException {
        if (prompt != null) {
            System.out.print(prompt + ": ");
        }

        BufferedReader lineOfText = new BufferedReader(new InputStreamReader(System.in));
        return lineOfText.readLine();
    }

    public static File[] listFiles(File dir) {
        return dir.listFiles();
    }

    public static String system(String evaluatedContents, String failedString) throws IOException {
        LOG.debug("System {}", evaluatedContents);

        StringBuilder result = new StringBuilder();
        Process p = Runtime.getRuntime().exec(evaluatedContents);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String data;
            while ((data = br.readLine()) != null) {
                result.append(data).append("\n");
            }
        } finally {
            p.destroy();
        }

        LOG.debug("Result = {}", result);

        return result.toString();
    }

    public static String evalScript(String engineName, String script) throws Exception {
        LOG.debug("evaluating {}", script);

        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        return "" + engine.eval(script);
    }
}
