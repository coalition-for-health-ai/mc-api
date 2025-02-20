package org.chai;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.chai.util.APIUtil;
import org.chai.util.IOUtil;
import org.chai.util.KeyUtil;
import org.chai.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import reactor.core.publisher.Mono;

public class StampFunction {

    private static final PublicKey PUBLIC_KEY = KeyUtil.getPublicKey();
    private static final PrivateKey PRIVATE_KEY = KeyUtil.getPrivateKey();

    @FunctionName("StampXml")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.OPTIONS, HttpMethod.POST
            }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
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

        context.getLogger().log(Level.INFO, "ENTRY " + request.getBody().orElse("null"));

        final String xml = request.getBody().orElse("");
        return APIUtil.sendValidateRequest(xml).flatMap(validateResponse -> {
            return APIUtil.parseValidateResponseBody(validateResponse).flatMap(validateResponseBody -> {
                if (validateResponseBody.containsKey("result") &&
                        validateResponseBody.get("result").equals("VALID")) {
                    final Document doc;
                    try {
                        doc = XMLUtil.newDocumentBuilder()
                                .parse(new InputSource(new StringReader(xml)));
                    } catch (SAXException | IOException e) {
                        return Mono.error(e);
                    }

                    // If a Signature already exists, delete it:
                    final Node signatureNode = XMLUtil.getXmlSignatureNode(doc.getDocumentElement());
                    if (signatureNode != null) {
                        signatureNode.getParentNode().removeChild(signatureNode);
                    }

                    // Sign the document:
                    // TODO: add timestamp
                    // https://www.w3.org/Consortium/Offices/Presentations/XML_Signatures/slide5-0.htm
                    final String output;
                    try {
                        final DOMSignContext dsc = new DOMSignContext(PRIVATE_KEY,
                                doc.getDocumentElement());
                        final Reference ref = XMLUtil.XML_SIGNATURE_FACTORY.newReference("",
                                XMLUtil.XML_SIGNATURE_FACTORY.newDigestMethod(DigestMethod.SHA256, null),
                                Collections
                                        .singletonList(XMLUtil.XML_SIGNATURE_FACTORY.newTransform(Transform.ENVELOPED,
                                                (TransformParameterSpec) null)),
                                null, null);
                        final SignedInfo si = XMLUtil.XML_SIGNATURE_FACTORY.newSignedInfo(
                                XMLUtil.XML_SIGNATURE_FACTORY.newCanonicalizationMethod(
                                        CanonicalizationMethod.INCLUSIVE_11,
                                        (C14NMethodParameterSpec) null),
                                XMLUtil.XML_SIGNATURE_FACTORY
                                        .newSignatureMethod(
                                                "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                                                null),
                                Collections.singletonList(ref));
                        final KeyInfoFactory kif = XMLUtil.XML_SIGNATURE_FACTORY.getKeyInfoFactory();
                        final KeyValue kv = kif.newKeyValue(PUBLIC_KEY);
                        final KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));
                        // can't re-use `signature` because subsequent executions will
                        // cause a duplicate value in the `SignatureValue` element:
                        final XMLSignature signature = XMLUtil.XML_SIGNATURE_FACTORY.newXMLSignature(si, ki);
                        signature.sign(dsc);

                        final StringWriter writer = new StringWriter();
                        XMLUtil.newTransformer().transform(new DOMSource(doc), new StreamResult(writer));
                        output = writer.toString();
                    } catch (MarshalException | XMLSignatureException | NoSuchAlgorithmException
                            | InvalidAlgorithmParameterException | KeyException | TransformerException e) {
                        return Mono.error(e);
                    }

                    context.getLogger().log(Level.INFO, "RETURN " + output);
                    return Mono.just(request.createResponseBuilder(HttpStatus.OK)
                            .body(output)
                            .header("Content-Type", "text/xml").build());
                } else {
                    final HttpResponseMessage.Builder responseBuilder = request
                            .createResponseBuilder(HttpStatus.valueOf(validateResponse.getStatusCode()))
                            .body(validateResponseBody);
                    validateResponse.getHeaders()
                            .forEach(header -> responseBuilder.header(header.getName(),
                                    header.getValue()));
                    return Mono.just(responseBuilder.build());
                }
            });
        })
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(e -> {
                    final Map<String, String> result = new HashMap<String, String>();
                    result.put("result", "ERROR");
                    result.put("reason", e.getMessage());
                    context.getLogger().log(Level.INFO, "RETURN " + result);
                    return Mono.just(
                            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(IOUtil.jsonSerializeToString(result))
                                    .header("Content-Type", "application/json").build());
                }).block();
    }
}
