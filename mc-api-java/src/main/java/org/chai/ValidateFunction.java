package org.chai;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.chai.util.BibTeXUtil;
import org.chai.util.IOUtil;
import org.chai.util.KeyUtil;
import org.chai.util.XMLUtil;
import org.jbibtex.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ValidateFunction {

    private static final PublicKey PUBLIC_KEY = KeyUtil.getPublicKey();

    @FunctionName("ValidateXml")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.OPTIONS, HttpMethod.POST
            }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        if (request.getHttpMethod() == HttpMethod.OPTIONS) {
            return request.createResponseBuilder(HttpStatus.NO_CONTENT).header("Allow", "OPTIONS, POST")
                    .header("Accept-Post", "text/xml, application/xml").build();
        }

        if (!request.getHeaders().containsKey("content-type")) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .header("Accept-Post", "text/xml, application/xml").build();
        }
        final String contentTypeHeader = request.getHeaders().get("content-type");
        if (!contentTypeHeader.equals("text/xml") && !contentTypeHeader.equals("application/xml")) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .header("Accept-Post", "text/xml, application/xml").build();
        }

        // TODO: is it safe to re-use any of the following?
        // - JsonSerializer
        // - DocumentBuilder or DocumentBuilderFactory

        context.getLogger().log(Level.INFO, "ENTRY " + request.getBody().orElse("null"));

        if (!request.getBody().isPresent()) {
            final Map<String, String> result = Collections.singletonMap("result", "NO_DOCUMENT");
            context.getLogger().log(Level.INFO, "RETURN " + result);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(IOUtil.jsonSerializeToString(result))
                    .header("Content-Type", "application/json").build();
        }

        final String chaiMcXml = request.getBody().orElseThrow();
        StringReader chaiMcXmlReader = new StringReader(chaiMcXml);

        // XML Schema Validation:
        try {
            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(new URL("https://mc.chai.org/v0.1/schema.xsd"));
            final Validator validator = schema.newValidator();
            validator.validate(new StreamSource(chaiMcXmlReader));
        } catch (SAXException | IOException e) {
            final Map<String, String> result = new HashMap<String, String>();
            result.put("result", "INVALID");
            result.put("reason", "XML Schema Validation failed: " + e.getMessage());
            context.getLogger().log(Level.INFO, "RETURN " + result);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(IOUtil.jsonSerializeToString(result))
                    .header("Content-Type", "application/json").build();
        }

        // XML Signature Validation:
        try {
            chaiMcXmlReader.reset();
        } catch (IOException e) {
            chaiMcXmlReader.close();
            chaiMcXmlReader = new StringReader(chaiMcXml);
        }
        try {
            final Document doc = XMLUtil.newDocumentBuilder().parse(new InputSource(chaiMcXmlReader));

            try {
                BibTeXUtil.throwIfInvalidBibTeX(XMLUtil.getElementText(doc.getDocumentElement(), "Bibliography"));
            } catch (ParseException e) {
                final Map<String, String> result = new HashMap<String, String>();
                result.put("result", "INVALID");
                result.put("reason", "Bibliography element BibTeX validation failed: " + e.getMessage());
                context.getLogger().log(Level.INFO, "RETURN " + result);
                return request.createResponseBuilder(HttpStatus.OK)
                        .body(IOUtil.jsonSerializeToString(result))
                        .header("Content-Type", "application/json").build();
            }

            final Node signatureNode = XMLUtil.getXmlSignatureNode(doc.getDocumentElement());
            if (signatureNode != null) {
                // TODO: X.509 certificate + KeySelector?
                final DOMValidateContext valContext = new DOMValidateContext(PUBLIC_KEY, signatureNode);

                final XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
                final XMLSignature signature = factory.unmarshalXMLSignature(valContext);
                final boolean coreValidity = signature.validate(valContext);

                if (!coreValidity) {
                    final Map<String, String> result = new HashMap<String, String>();
                    result.put("result", "INVALID");

                    // Narrow down the cause of the failure:
                    final StringBuilder reasonBuilder = new StringBuilder();
                    if (!signature.getSignatureValue().validate(valContext)) {
                        reasonBuilder.append("Signature validation failed. ");
                    }
                    final List<Reference> refs = signature.getSignedInfo().getReferences();
                    for (int i = 0; i < refs.size(); i++) {
                        final Reference ref = refs.get(i);
                        if (!ref.validate(valContext)) {
                            reasonBuilder.append("Reference " + (i + 1) + " of "
                                    + refs.size() + " failed; has digest value "
                                    + Arrays.toString(ref.getDigestValue()) + " but expected "
                                    + Arrays.toString(ref.getCalculatedDigestValue()) + ". ");
                        }
                    }
                    result.put("reason", reasonBuilder.toString());

                    context.getLogger().log(Level.INFO, "RETURN " + result);
                    return request.createResponseBuilder(HttpStatus.OK)
                            .body(IOUtil.jsonSerializeToString(result))
                            .header("Content-Type", "application/json").build();
                }
            }
        } catch (SAXException | IOException | MarshalException | XMLSignatureException e) {
            final Map<String, String> result = new HashMap<String, String>();
            result.put("result", "ERROR");
            result.put("reason", e.getMessage());
            context.getLogger().log(Level.INFO, "RETURN " + result);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(IOUtil.jsonSerializeToString(result))
                    .header("Content-Type", "application/json").build();
        }

        final Map<String, Object> result = Collections.singletonMap("result", "VALID");
        context.getLogger().log(Level.INFO, "RETURN " + result);
        return request.createResponseBuilder(HttpStatus.OK)
                .body(IOUtil.jsonSerializeToString(result))
                .header("Content-Type", "application/json").build();
    }
}
