package org.chai.util;

import java.net.URL;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;

public class APIUtil {
    private static final URL VALIDATE_ENDPOINT;
    static {
        try {
            VALIDATE_ENDPOINT = new URL("https://func-mc-api-java.azurewebsites.net/api/ValidateXml");
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    private static final HttpHeaders VALIDATE_REQUEST_HEADERS = new HttpHeaders().add(HttpHeaderName.CONTENT_TYPE,
            "text/xml");

    public static HttpResponse sendValidateRequest(final String xml) {
        // TODO: Durable Functions with function chaining?
        final HttpRequest validateRequest = new HttpRequest(com.azure.core.http.HttpMethod.POST,
                VALIDATE_ENDPOINT,
                VALIDATE_REQUEST_HEADERS,
                BinaryData.fromString(xml));
        return HttpClient.createDefault().sendSync(validateRequest, Context.NONE);
    }
}
