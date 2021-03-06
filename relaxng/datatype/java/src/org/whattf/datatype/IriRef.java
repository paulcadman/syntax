/*
 * Copyright (c) 2006 Henri Sivonen
 * Copyright (c) 2007-2014 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package org.whattf.datatype;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.relaxng.datatype.DatatypeException;
import org.whattf.io.DataUri;
import org.whattf.io.DataUriException;
import org.whattf.io.Utf8PercentDecodingReader;

import io.mola.galimatias.URL;
import io.mola.galimatias.URLParsingSettings;
import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.StrictErrorHandler;

public class IriRef extends AbstractDatatype {

    /**
     * The singleton instance.
     */
    public static final IriRef THE_INSTANCE = new IriRef();

    protected IriRef() {
        super();
    }

    private final CharSequencePair splitScheme(CharSequence iri) {
        StringBuilder sb = new StringBuilder();
        Boolean atSchemeBeginning = true;
        for (int i = 0; i < iri.length(); i++) {
            char c = toAsciiLowerCase(iri.charAt(i));
            if (atSchemeBeginning) {
                // Skip past any leading characters that the HTML5 spec defines
                // as space characters: space, tab, LF, FF, CR
                if (' ' == c || '\t' == c || '\n' == c || '\f' == c
                        || '\r' == c) {
                    continue;
                }
                if ('a' <= c && 'z' >= c) {
                    atSchemeBeginning = false;
                    sb.append(c);
                } else {
                    return null;
                }
            } else {
                if (('a' <= c && 'z' >= c) || ('0' <= c && '9' >= c)
                        || c == '+' || c == '.') {
                    sb.append(c);
                    continue;
                } else if (c == ':') {
                    return new CharSequencePair(sb, iri.subSequence(i + 1,
                            iri.length()));
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public void checkValid(CharSequence literal) throws DatatypeException {
        URL url = null;
        URLParsingSettings settings = URLParsingSettings.create().withErrorHandler(
                StrictErrorHandler.getInstance());
        boolean data = false;
        try {
            CharSequencePair pair = splitScheme(literal);
            if (pair == null) {
                // no scheme or scheme is private
                if (isAbsolute()) {
                    throw newDatatypeException("The string \u201c" + literal
                            + "\u201d is not an absolute URL.");
                } else {
                    // in this case, doc's actual base URL isn't relevant,
                    // so just use http://example.org/foo/bar as base
                    url = URL.parse(settings,
                            URL.parse("http://example.org/foo/bar"),
                            literal.toString());
                }
            } else {
                CharSequence scheme = pair.getHead();
                CharSequence tail = pair.getTail();
                if (isWellKnown(scheme)) {
                    url = URL.parse(settings, literal.toString());
                } else if ("javascript".contentEquals(scheme)) {
                    // StringBuilder sb = new StringBuilder(2 +
                    // literal.length());
                    // sb.append("x-").append(literal);
                    // iri = fac.construct(sb.toString());
                    url = null; // Don't bother user with generic IRI syntax
                    Reader reader = new BufferedReader(
                            new Utf8PercentDecodingReader(new StringReader(
                                    "function(event){" + tail.toString() + "}")));
                    // XXX CharSequenceReader
                    reader.mark(1);
                    int c = reader.read();
                    if (c != 0xFEFF) {
                        reader.reset();
                    }
                    try {
                        Context context = ContextFactory.getGlobal().enterContext();
                        context.setOptimizationLevel(0);
                        context.setLanguageVersion(Context.VERSION_1_6);
                        // -1 for lineno arg prevents Rhino from appending
                        // "(unnamed script#1)" to all error messages
                        context.compileReader(reader, null, -1, null);
                    } finally {
                        Context.exit();
                    }
                } else if ("data".contentEquals(scheme)) {
                    data = true;
                    url = URL.parse(settings, literal.toString());
                } else if (isHttpAlias(scheme)) {
                    StringBuilder sb = new StringBuilder(5 + tail.length());
                    sb.append("http:").append(tail);
                    url = URL.parse(settings, sb.toString());
                } else {
                    StringBuilder sb = new StringBuilder(2 + literal.length());
                    sb.append("x-").append(literal);
                    url = URL.parse(settings, sb.toString());
                }
            }
        } catch (GalimatiasParseException e) {
            throw newDatatypeException(e.getMessage() + ".");
        } catch (IOException e) {
            throw newDatatypeException(e.getMessage());
        } catch (RhinoException e) {
            throw newDatatypeException(e.getMessage());
        }
        if (url != null) {
            if ("".equals(url.toString())) {
                    throw newDatatypeException("Must be non-empty.");
            }
            if (data) {
                try {
                    DataUri dataUri = new DataUri(url);
                    InputStream is = dataUri.getInputStream();
                    while (is.read() >= 0) {
                        // spin
                    }
                } catch (DataUriException e) {
                    throw newDatatypeException(e.getIndex(), e.getHead(),
                            e.getLiteral(), e.getTail());
                } catch (IOException e) {
                    throw newDatatypeException(e.getMessage());
                }
            }
        }
    }

    private final boolean isHttpAlias(CharSequence scheme) {
        return "feed".contentEquals(scheme) || "webcal".contentEquals(scheme);
    }

    private final boolean isWellKnown(CharSequence scheme) {
        return "http".contentEquals(scheme) || "https".contentEquals(scheme)
                || "ftp".contentEquals(scheme)
                || "mailto".contentEquals(scheme)
                || "file".contentEquals(scheme);
    }

    protected boolean isAbsolute() {
        return false;
    }

    @Override
    public String getName() {
        return "IRI reference";
    }

    private class CharSequencePair {
        private final CharSequence head;
        private final CharSequence tail;

        /**
         * @param head
         * @param tail
         */
        public CharSequencePair(final CharSequence head, final CharSequence tail) {
            this.head = head;
            this.tail = tail;
        }

        /**
         * Returns the head.
         * 
         * @return the head
         */
        public CharSequence getHead() {
            return head;
        }

        /**
         * Returns the tail.
         * 
         * @return the tail
         */
        public CharSequence getTail() {
            return tail;
        }
    }
}
