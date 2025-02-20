package org.chai.mc;

public class RendererFactory {
    public static Renderer getRenderer(final String version) {
        if ("v0.1".equals(version)) {
            return new RendererV01();
        } else {
            throw new IllegalArgumentException("Unknown version: " + version);
        }
    }
}
