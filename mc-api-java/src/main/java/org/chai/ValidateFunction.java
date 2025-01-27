package org.chai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXParseException;

public class ValidateFunction {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * This function listens at endpoint "/api/ValidateXml". To invoke it using
     * "curl":
     * curl -X POST -H "Content-Type: text/xml" -d @input.xml /api/ValidateXml
     */
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

        context.getLogger().log(Level.INFO, "ENTRY " + request.getBody().orElse("null"));

        if (!request.getBody().isPresent()) {
            final Map<String, String> result = Collections.singletonMap("result", "NO_DOCUMENT");
            final String resultJson = GSON.toJson(result);
            context.getLogger().log(Level.INFO, "RETURN " + resultJson);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(resultJson)
                    .header("Content-Type", "application/json").build();
        }

        try {
            final String chaiMcXml = request.getBody().orElseThrow();

            final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = factory.newSchema(new URL("https://mc.chai.org/v0.1/schema.xsd"));
            final Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(chaiMcXml)));

            // TODO: validate digital signature

            final Map<String, Object> result = Collections.singletonMap("result", "VALID");
            final String resultJson = GSON.toJson(result);
            context.getLogger().log(Level.INFO, "RETURN " + resultJson);
            return request.createResponseBuilder(HttpStatus.OK).body(resultJson)
                    .header("Content-Type", "application/json").build();
        } catch (SAXParseException e) {
            final Map<String, String> result = new HashMap<String, String>();
            result.put("result", "INVALID");
            result.put("reason", e.getMessage());
            final String resultJson = GSON.toJson(result);
            context.getLogger().log(Level.INFO, "RETURN " + resultJson);
            return request.createResponseBuilder(HttpStatus.OK).body(resultJson)
                    .header("Content-Type", "application/json").build();
        } catch (Exception e) {
            final Map<String, String> result = new HashMap<String, String>();
            result.put("result", "ERROR");
            result.put("reason", e.getMessage());
            final String resultJson = GSON.toJson(result);
            context.getLogger().log(Level.INFO, "RETURN " + resultJson);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(resultJson)
                    .header("Content-Type", "application/json").build();
        }
    }
}
