package org.chai.util;

import java.nio.charset.StandardCharsets;

import com.azure.core.util.serializer.JsonSerializer;
import com.azure.core.util.serializer.JsonSerializerProviders;

public class IOUtil {
    public static final JsonSerializer JSON_SERIALIZER = JsonSerializerProviders.createInstance(true);

    public static String jsonSerializeToString(Object obj) {
        return new String(JSON_SERIALIZER.serializeToBytes(obj), StandardCharsets.UTF_8);
    }
}
