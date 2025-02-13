package org.chai.util;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLUtil {
    public static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    public static final XMLSignatureFactory XML_SIGNATURE_FACTORY = XMLSignatureFactory.getInstance("DOM");

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(new URL("https://mc.chai.org/v0.1/schema.xsd"));

            dbf.setNamespaceAware(true);
            // required because INDENT="yes" in XML serialization:
            dbf.setIgnoringElementContentWhitespace(true);
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
            transformer = TRANSFORMER_FACTORY.newTransformer();
        } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        // TODO: standalone="yes" is not applied?
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        return transformer;
    }

    public static Node getXmlSignatureNode(final Element parent) {
        return parent.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);
    }

    public static Element getElement(final Element parent, final String name) {
        return (Element) parent.getElementsByTagName(name).item(0);
    }

    public static String getElementText(final Element parent, final String name) {
        return getElement(parent, name).getTextContent();
    }

    public static String getElementTextWithDefault(final Element parent, final String name, final String defaultValue) {
        final Node node = getElement(parent, name);
        return node == null ? defaultValue : node.getTextContent();
    }
}
