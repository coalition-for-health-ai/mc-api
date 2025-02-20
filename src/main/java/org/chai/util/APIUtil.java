package org.chai.util;

import java.net.URL;
import java.util.Map;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.core.util.serializer.TypeReference;

import reactor.core.publisher.Mono;

public class APIUtil {
    private static final HttpClient HTTP_CLIENT = HttpClient.createDefault();

    private static final URL VALIDATE_ENDPOINT;
    static {
        try {
            VALIDATE_ENDPOINT = new URL("https://func-mc-api.azurewebsites.net/api/ValidateXml");
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    private static final HttpHeaders VALIDATE_REQUEST_HEADERS = new HttpHeaders().add(HttpHeaderName.CONTENT_TYPE,
            "text/xml");

    public static Mono<Map<String, String>> parseValidateResponseBody(final HttpResponse validateResponse) {
        return validateResponse.getBodyAsByteArray().map(body -> IOUtil.JSON_SERIALIZER.deserializeFromBytes(body,
                new TypeReference<Map<String, String>>() {
                }));
    }

    public static Mono<HttpResponse> sendValidateRequest(final String xml) {
        // TODO: Durable Functions with function chaining?
        return HTTP_CLIENT.send(new HttpRequest(com.azure.core.http.HttpMethod.POST,
                VALIDATE_ENDPOINT,
                VALIDATE_REQUEST_HEADERS,
                BinaryData.fromString(xml)));
    }
}
