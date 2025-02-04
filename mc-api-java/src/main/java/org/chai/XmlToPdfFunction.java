package org.chai;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

import org.chai.mc.Renderer;
import org.chai.mc.RendererFactory;
import org.chai.util.IOUtil;
import org.chai.util.XMLUtil;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.footnotes.FootnotesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
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
import com.azure.core.util.serializer.JsonSerializer;
import com.azure.core.util.serializer.JsonSerializerProviders;
import com.azure.core.util.serializer.TypeReference;
import com.microsoft.azure.functions.*;

class FirstTagInlineAttributeProvider implements AttributeProvider {
    private boolean isFirstTag = true;

    @Override
    public void setAttributes(final Node node, final String tagName, final Map<String, String> attributes) {
        if (isFirstTag) {
            attributes.put("style", "display: inline");
            isFirstTag = false;
        }
    }
}

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

    private static final List<Extension> COMMONMARK_EXTENSIONS = Arrays.asList(AutolinkExtension.create(),
            StrikethroughExtension.create(), FootnotesExtension.create());
    private static final Parser MARKDOWN_PARSER = Parser.builder().extensions(COMMONMARK_EXTENSIONS).build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().extensions(COMMONMARK_EXTENSIONS)
            .omitSingleParagraphP(true).attributeProviderFactory(context -> new FirstTagInlineAttributeProvider())
            .build();

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
        // - JsonSerializer
        // - HttpClient
        // - Playwright / Browser

        context.getLogger().log(Level.INFO, "ENTRY " +
                request.getBody().orElse("null"));

        final String xml = request.getBody().orElse("");
        final JsonSerializer jsonSerializer = JsonSerializerProviders.createInstance(true);

        // TODO: Durable Functions with function chaining?
        final HttpRequest validateRequest = new HttpRequest(com.azure.core.http.HttpMethod.POST, VALIDATE_ENDPOINT,
                VALIDATE_REQUEST_HEADERS, BinaryData.fromString(xml));
        final HttpResponse validateResponse = HttpClient.createDefault().sendSync(validateRequest, Context.NONE);
        final Map<String, String> validateResponseBody = jsonSerializer
                .deserializeFromBytes(validateResponse.getBodyAsByteArray().block(),
                        new TypeReference<Map<String, String>>() {
                        });
        if (validateResponseBody.containsKey("result") &&
                validateResponseBody.get("result").equals("VALID")) {
            final Document doc;
            try {
                doc = XMLUtil.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xml)));
            } catch (SAXException | IOException e) {
                final Map<String, String> result = new HashMap<String, String>();
                result.put("result", "ERROR");
                result.put("reason", e.getMessage());
                context.getLogger().log(Level.INFO, "RETURN " + result);
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(IOUtil.jsonSerializeToString(jsonSerializer, result))
                        .header("Content-Type", "application/json").build();
            }

            final Renderer renderer = RendererFactory.getRenderer("v0.1", MARKDOWN_PARSER, HTML_RENDERER);

            final String html = renderer.render(doc.getDocumentElement());
            final byte[] pdf = compileHTMLtoPDF(html);
            final Map<String, String> customProperties = new HashMap<>();
            customProperties.put("chaiMcXml", xml);
            customProperties.put("chaiMcSoftwareId", "mc-api 1.0.0");
            final byte[] pdfWithProperties = addCustomPropertiesToPDF(pdf, customProperties);
            // TODO: digitally sign PDF
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/pdf")
                    .body(pdfWithProperties)
                    .build();
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

    private static byte[] compileHTMLtoPDF(final String html) {
        try (final Playwright playwright = Playwright.create()) {
            final Browser browser = playwright.chromium().launch();
            final Page page = browser.newPage();
            page.setContent(html);
            final byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setMargin(new Margin().setTop("0.5in").setRight("0.5in").setBottom("0.5in").setLeft("0.5in"))
                    .setFormat("Letter"));
            browser.close();
            return pdf;
        }
    }

    private byte[] addCustomPropertiesToPDF(final byte[] pdf, final Map<String, String> properties) {
        return pdf;
    }
}
