package org.chai.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.ParseException;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.bibtex.BibTeXItemDataProvider;

public class BibTeXUtil {
    public static final BibTeXConverter BIBTEX_CONVERTER = new BibTeXConverter();

    public static void throwIfInvalidBibTeX(final String bibtex) throws ParseException {
        try {
            final InputStream stream = new ByteArrayInputStream(bibtex.getBytes(StandardCharsets.UTF_8));
            BIBTEX_CONVERTER.loadDatabase(stream);
            stream.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String bibtexToHtml(final String bibtex) throws ParseException, IOException {
        final InputStream stream = new ByteArrayInputStream(bibtex.getBytes(StandardCharsets.UTF_8));
        final BibTeXDatabase db = BIBTEX_CONVERTER.loadDatabase(stream);
        stream.close();
        final BibTeXItemDataProvider provider = new BibTeXItemDataProvider();
        provider.addDatabase(db);
        final CSL citeproc = new CSL(provider, "apa");
        provider.registerCitationItems(citeproc);
        citeproc.setOutputFormat("html");
        return citeproc.makeBibliography().makeString();
    }
}
