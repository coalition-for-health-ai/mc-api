package org.chai.util;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

public class XMLUtil {
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(new URL("https://mc.chai.org/v0.1/schema.xsd"));

            dbf.setNamespaceAware(true);
            dbf.setIgnoringElementContentWhitespace(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setSchema(schema);
            // dbf.setValidating(true); // TODO
            return dbf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DocumentBuilder newDocumentBuilder() {
        final DocumentBuilderFactory dbf = newDocumentBuilderFactory();
        try {
            return dbf.newDocumentBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Transformer newTransformer() {
        final Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(e);
        }
        // INDENT="yes" causes XML Signature validation to fail:
        // transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        // TODO: standalone="yes" is not applied?
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        return transformer;
    }
}
