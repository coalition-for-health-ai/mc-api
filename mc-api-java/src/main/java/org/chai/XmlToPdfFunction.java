package org.chai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.chai.mc.Renderer;
import org.chai.mc.RendererFactory;
import org.chai.util.IOUtil;
import org.chai.util.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.core.util.serializer.TypeReference;
import com.microsoft.azure.functions.*;

public class XmlToPdfFunction {

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

    @FunctionName("XmlToPdf")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.OPTIONS, HttpMethod.POST
            }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        if (request.getHttpMethod() == HttpMethod.OPTIONS) {
            return request.createResponseBuilder(HttpStatus.NO_CONTENT).header("Allow",
                    "OPTIONS, POST")
                    .header("Accept-Post", "text/xml, application/xml").build();
        }

        if (!request.getHeaders().containsKey("content-type")) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .header("Accept-Post", "text/xml, application/xml").build();
        }
        final String contentTypeHeader = request.getHeaders().get("content-type");
        if (!contentTypeHeader.equals("text/xml") &&
                !contentTypeHeader.equals("application/xml")) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .header("Accept-Post", "text/xml, application/xml").build();
        }

        // TODO: is it safe to re-use any of the following?
        // - HttpClient
        // - Playwright / Browser

        context.getLogger().log(Level.INFO, "ENTRY " +
                request.getBody().orElse("null"));

        final String xml = request.getBody().orElse("");

        // TODO: Durable Functions with function chaining?
        final HttpRequest validateRequest = new HttpRequest(com.azure.core.http.HttpMethod.POST, VALIDATE_ENDPOINT,
                VALIDATE_REQUEST_HEADERS, BinaryData.fromString(xml));
        final HttpResponse validateResponse = HttpClient.createDefault().sendSync(validateRequest, Context.NONE);
        final Map<String, String> validateResponseBody = IOUtil.JSON_SERIALIZER
                .deserializeFromBytes(validateResponse.getBodyAsByteArray().block(),
                        new TypeReference<Map<String, String>>() {
                        });
        if (validateResponseBody.containsKey("result") &&
                validateResponseBody.get("result").equals("VALID")) {
            try {
                final Document doc = XMLUtil.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xml)));
                final Renderer renderer = RendererFactory.getRenderer("v0.1");
                final String html = renderer.render(doc.getDocumentElement());
                final PDDocument pdf = compileHTMLtoPDF(html);
                final Map<String, String> customProperties = new HashMap<>();
                customProperties.put("chaiMcXml", xml);
                customProperties.put("chaiMcSoftwareId", "mc-api 1.0.0");
                final PDDocument pdfWithProperties = addCustomPropertiesToPDF(pdf, customProperties);
                final byte[] pdfByteArray = convertPdfToByteArray(pdfWithProperties);
                pdfWithProperties.close();
                context.getLogger().log(Level.INFO, "RETURN " + html);
                return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/pdf")
                        .body(pdfByteArray)
                        .build();
            } catch (SAXException | IOException e) {
                final Map<String, String> result = new HashMap<String, String>();
                result.put("result", "ERROR");
                result.put("reason", e.getMessage());
                context.getLogger().log(Level.INFO, "RETURN " + result);
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(IOUtil.jsonSerializeToString(result))
                        .header("Content-Type", "application/json").build();
            }
        } else {
            final HttpResponseMessage.Builder responseBuilder = request
                    .createResponseBuilder(HttpStatus.valueOf(validateResponse.getStatusCode()))
                    .body(validateResponseBody);
            validateResponse.getHeaders()
                    .forEach(header -> responseBuilder.header(header.getName(),
                            header.getValue()));
            return responseBuilder.build();
        }
    }

    private static PDDocument compileHTMLtoPDF(final String html) throws IOException {
        try (final Playwright playwright = Playwright.create()) {
            final Browser browser = playwright.chromium().launch();
            final Page page = browser.newPage();
            page.setContent(html);
            final byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setMargin(new Margin().setTop("0.5in").setRight("0.5in").setBottom("0.5in").setLeft("0.5in"))
                    .setFormat("Letter"));
            browser.close();
            return Loader.loadPDF(pdf);
        }
    }

    private PDDocument addCustomPropertiesToPDF(final PDDocument pdf, final Map<String, String> properties) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            pdf.getDocumentInformation().setCustomMetadataValue(property.getKey(), property.getValue());
        }
        return pdf;
    }

    private byte[] convertPdfToByteArray(final PDDocument document) throws IOException {
        byte[] byteArray = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            document.save(byteArrayOutputStream);
            byteArray = byteArrayOutputStream.toByteArray();
        }
        return byteArray;
    }
}
