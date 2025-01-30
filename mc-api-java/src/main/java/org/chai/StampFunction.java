package org.chai;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.chai.util.IOUtil;
import org.chai.util.KeyUtil;
import org.chai.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.core.util.serializer.JsonSerializer;
import com.azure.core.util.serializer.JsonSerializerProviders;
import com.azure.core.util.serializer.TypeReference;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class StampFunction {

    private static URL VALIDATE_ENDPOINT;
    static {
        try {
            VALIDATE_ENDPOINT = new URL("https://func-mc-api-java.azurewebsites.net/api/ValidateXml");
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    private static final HttpHeaders VALIDATE_REQUEST_HEADERS = new HttpHeaders().add(HttpHeaderName.CONTENT_TYPE,
            "text/xml");

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

        // TODO: is it safe to re-use any of the following?
        // - JsonSerializer
        // - HttpClient
        // - XMLSignatureFactory
        // - Transformer

        context.getLogger().log(Level.INFO, "ENTRY " + request.getBody().orElse("null"));

        final JsonSerializer jsonSerializer = JsonSerializerProviders.createInstance(true);

        // TODO: Durable Functions with function chaining?
        final HttpRequest validateRequest = new HttpRequest(com.azure.core.http.HttpMethod.POST,
                VALIDATE_ENDPOINT,
                VALIDATE_REQUEST_HEADERS,
                BinaryData.fromString(request.getBody().orElse("")));
        final HttpResponse validateResponse = HttpClient.createDefault().sendSync(validateRequest, Context.NONE);
        final Map<String, String> validateResponseBody = jsonSerializer
                .deserializeFromBytes(validateResponse.getBodyAsByteArray().block(),
                        new TypeReference<Map<String, String>>() {
                        });
        if (validateResponseBody.containsKey("result") && validateResponseBody.get("result").equals("VALID")) {
            final Document doc;
            try {
                doc = XMLUtil.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(request.getBody().orElse(""))));
            } catch (SAXException | IOException e) {
                final Map<String, String> result = new HashMap<String, String>();
                result.put("result", "ERROR");
                result.put("reason", e.getMessage());
                context.getLogger().log(Level.INFO, "RETURN " + result);
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(IOUtil.jsonSerializeToString(jsonSerializer, result))
                        .header("Content-Type", "application/json").build();
            }

            // If a Signature already exists, delete it:
            final NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nl.getLength() > 0) {
                final Node signature = nl.item(0);
                signature.getParentNode().removeChild(signature);
            }

            // Sign the document:
            // TODO: add timestamp
            // https://www.w3.org/Consortium/Offices/Presentations/XML_Signatures/slide5-0.htm
            final String output;
            try {
                final DOMSignContext dsc = new DOMSignContext(PRIVATE_KEY,
                        doc.getDocumentElement());
                final XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
                final Reference ref = fac.newReference("",
                        fac.newDigestMethod(DigestMethod.SHA256, null),
                        Collections.singletonList(fac.newTransform(Transform.ENVELOPED,
                                (TransformParameterSpec) null)),
                        null, null);
                final SignedInfo si = fac.newSignedInfo(
                        fac.newCanonicalizationMethod(
                                CanonicalizationMethod.INCLUSIVE_11,
                                (C14NMethodParameterSpec) null),
                        fac
                                .newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                        Collections.singletonList(ref));
                final KeyInfoFactory kif = fac.getKeyInfoFactory();
                final KeyValue kv = kif.newKeyValue(PUBLIC_KEY);
                final KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));
                // can't re-use `signature` because subsequent executions will
                // cause a duplicate value in the `SignatureValue` element:
                final XMLSignature signature = fac.newXMLSignature(si, ki);
                signature.sign(dsc);

                final StringWriter writer = new StringWriter();
                XMLUtil.newTransformer().transform(new DOMSource(doc), new StreamResult(writer));
                output = writer.toString();
            } catch (MarshalException | XMLSignatureException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | KeyException | TransformerException e) {
                final Map<String, String> result = new HashMap<String, String>();
                result.put("result", "ERROR");
                result.put("reason", e.getMessage());
                context.getLogger().log(Level.INFO, "RETURN " + result);
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(IOUtil.jsonSerializeToString(jsonSerializer, result))
                        .header("Content-Type", "application/json").build();
            }

            context.getLogger().log(Level.INFO, "RETURN " + output);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(output)
                    .header("Content-Type", "text/xml").build();
        } else {
            final HttpResponseMessage.Builder responseBuilder = request
                    .createResponseBuilder(HttpStatus.valueOf(validateResponse.getStatusCode()))
                    .body(validateResponseBody);
            validateResponse.getHeaders()
                    .forEach(header -> responseBuilder.header(header.getName(), header.getValue()));
            return responseBuilder.build();
        }
    }
}
