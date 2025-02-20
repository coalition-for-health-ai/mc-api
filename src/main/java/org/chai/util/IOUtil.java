package org.chai.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64.Encoder;

import com.azure.core.util.serializer.JsonSerializer;
import com.azure.core.util.serializer.JsonSerializerProviders;

public class IOUtil {
    public static final Encoder BASE64_ENCODER = java.util.Base64.getEncoder();

    public static final JsonSerializer JSON_SERIALIZER = JsonSerializerProviders.createInstance(true);

    public static String getResourceAsBase64(final String resourceName) throws IOException {
        return BASE64_ENCODER.encodeToString(IOUtil.class.getResourceAsStream(resourceName).readAllBytes());
    }

    public static String jsonSerializeToString(Object obj) {
        return new String(JSON_SERIALIZER.serializeToBytes(obj), StandardCharsets.UTF_8);
    }
}
