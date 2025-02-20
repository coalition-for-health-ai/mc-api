package org.chai.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

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

public class CommonMarkUtil {
    public static final List<Extension> COMMONMARK_EXTENSIONS = Arrays.asList(AutolinkExtension.create(),
            StrikethroughExtension.create());
    public static final Parser MARKDOWN_PARSER = Parser.builder().extensions(COMMONMARK_EXTENSIONS).build();
    public static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().extensions(COMMONMARK_EXTENSIONS)
            .omitSingleParagraphP(true).attributeProviderFactory(context -> new FirstTagInlineAttributeProvider())
            .build();

    public static String markdownToHtml(final String markdown) {
        return CommonMarkUtil.HTML_RENDERER.render(CommonMarkUtil.MARKDOWN_PARSER.parse(markdown));
    }
}
