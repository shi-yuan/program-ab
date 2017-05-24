package org.alicebot.ab.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class DomUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DomUtils.class);

    public static Node parseFile(String fileName) throws Exception {
        File file = new File(fileName);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        // from AIMLProcessor.evalTemplate and AIMLProcessor.validTemplate:
        //   dbFactory.setIgnoringComments(true); // fix this
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        return doc.getDocumentElement();
    }

    public static Node parseString(String string) throws Exception {
        try (InputStream is = new ByteArrayInputStream(string.getBytes("UTF-16"))) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            // from AIMLProcessor.evalTemplate and AIMLProcessor.validTemplate:
            //   dbFactory.setIgnoringComments(true); // fix this
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            return doc.getDocumentElement();
        }
    }

    /**
     * convert an XML node to an XML statement
     *
     * @param node current XML node
     * @return XML string
     */
    public static String nodeToString(Node node) throws TransformerException, IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("nodeToString(node: {})", node);
        }

        String result;
        try (StringWriter sw = new StringWriter()) {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(node), new StreamResult(sw));
            result = sw.toString();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("nodeToString() returning: {}", result);
        }

        return result;
    }

    public static String trimTag(String s, String tagName) {
        String stag = "<" + tagName + ">";
        String etag = "</" + tagName + ">";

        if (s.startsWith(stag) && s.endsWith(etag)) {
            s = s.substring(stag.length());
            s = s.substring(0, s.length() - etag.length());
        }

        return s.trim();
    }
}
