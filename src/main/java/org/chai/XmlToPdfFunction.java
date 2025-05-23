package org.chai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.chai.mc.Renderer;
import org.chai.mc.RendererFactory;
import org.chai.util.APIUtil;
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
import com.microsoft.playwright.options.WaitUntilState;

import reactor.core.publisher.Mono;

import com.microsoft.azure.functions.*;

public class XmlToPdfFunction {

    @FunctionName("XmlToPdf")
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
        if (!contentTypeHeader.equals("text/xml") &&
                !contentTypeHeader.equals("application/xml")) {
            return request.createResponseBuilder(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .header("Accept-Post", "text/xml, application/xml").build();
        }

        // TODO: is it safe to re-use any of the following?
        // - Playwright / Browser

        context.getLogger().log(Level.INFO, "ENTRY " +
                request.getBody().orElse("null"));

        final String xml = request.getBody().orElse("");
        return APIUtil.sendValidateRequest(xml).flatMap(validateResponse -> {
            return APIUtil.parseValidateResponseBody(validateResponse).flatMap(validateResponseBody -> {
                if (validateResponseBody.containsKey("result") &&
                        validateResponseBody.get("result").equals("VALID")) {
                    try {
                        final Document doc = XMLUtil.newDocumentBuilder()
                                .parse(new InputSource(new StringReader(xml)));
                        final Renderer renderer = RendererFactory.getRenderer("v0.1");
                        final String html = renderer.render(doc.getDocumentElement());
                        final PDDocument pdf = compileHTMLtoPDF(html);
                        pdf.getDocumentInformation().setCustomMetadataValue("chaiMcXml", xml);
                        pdf.getDocumentInformation().setCustomMetadataValue("chaiMcSoftwareId", "mc-api 1.0.0");
                        final byte[] pdfByteArray = convertPDFToByteArray(pdf);
                        try {
                            context.getLogger().log(Level.INFO, "RETURN " + html);
                            return Mono
                                    .just(request.createResponseBuilder(HttpStatus.OK)
                                            .header("Content-Type", "application/pdf")
                                            .body(pdfByteArray)
                                            .build());
                        } finally {
                            pdf.close();
                        }
                    } catch (SAXException | IOException e) {
                        return Mono.error(e);
                    }
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
                .timeout(Duration.ofSeconds(30))
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

    private static PDDocument compileHTMLtoPDF(final String html) throws IOException {
        try (final Playwright playwright = Playwright.create()) {
            final Browser browser = playwright.chromium().launch();
            final Page page = browser.newPage();
            page.navigate("about:blank");
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForFunction("document.fonts.ready");
            final byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setMargin(new Margin().setTop("0.5in").setRight("0.5in").setBottom("0.5in").setLeft("0.5in"))
                    .setFormat("Letter"));
            page.close();
            browser.close();
            return Loader.loadPDF(pdf);
        }
    }

    private byte[] convertPDFToByteArray(final PDDocument document) throws IOException {
        byte[] byteArray = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            document.save(byteArrayOutputStream);
            byteArray = byteArrayOutputStream.toByteArray();
        }
        return byteArray;
    }
}
