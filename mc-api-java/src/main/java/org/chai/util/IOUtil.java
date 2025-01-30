package org.chai.util;

import java.nio.charset.StandardCharsets;

import com.azure.core.util.serializer.JsonSerializer;

public class IOUtil {
    public static String jsonSerializeToString(JsonSerializer jsonSerializer, Object obj) {
        return new String(jsonSerializer.serializeToBytes(obj), StandardCharsets.UTF_8);
    } 
}
