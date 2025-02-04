package org.chai.mc;

public class RendererFactory {
    public static Renderer getRenderer(final String version, final org.commonmark.parser.Parser markdownParser,
            final org.commonmark.renderer.Renderer htmlRenderer) {
        if ("v0.1".equals(version)) {
            return new RendererV01(markdownParser, htmlRenderer);
        } else {
            throw new IllegalArgumentException("Unknown version: " + version);
        }
    }
}
