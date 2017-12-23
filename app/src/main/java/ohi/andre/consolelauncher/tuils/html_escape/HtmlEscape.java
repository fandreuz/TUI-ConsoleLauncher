/*
 * =============================================================================
 * 
 *   Copyright (c) 2014-2017, The UNBESCAPE team (http://www.unbescape.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package ohi.andre.consolelauncher.tuils.html_escape;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * <p>
 *   Utility class for performing HTML escape/unescape operations.
 * </p>
 *
 * <strong><u>Configuration of escape/unescape operations</u></strong>
 *
 * <p>
 *   <strong>Escape</strong> operations can be (optionally) configured by means of:
 * </p>
 * <ul>
 *   <li><em>Level</em>, which defines how deep the escape operation must be (what
 *       chars are to be considered eligible for escaping, depending on the specific
 *       needs of the scenario). Its values are defined by the {@link org.unbescape.html.HtmlEscapeLevel}
 *       enum.</li>
 *   <li><em>Type</em>, which defines whether escaping should be performed by means of NCRs
 *       (Named Character References), by means of decimal/hexadecimal numerical references,
 *       using the HTML5 or the HTML 4 NCR set, etc. Its values are defined by the
 *       {@link org.unbescape.html.HtmlEscapeType} enum.</li>
 * </ul>
 * <p>
 *   <strong>Unescape</strong> operations need no configuration parameters. Unescape operations
 *   will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
 *   and hexadecimal references.
 * </p>
 *
 * <strong><u>Features</u></strong>
 *
 * <p>
 *   Specific features of the HTML escape/unescape operations performed by means of this class:
 * </p>
 * <ul>
 *   <li>Whole HTML5 NCR (Named Character Reference) set supported, if required:
 *       <tt>&amp;rsqb;</tt>,<tt>&amp;NewLine;</tt>, etc. (HTML 4 set available too).</li>
 *   <li>Mixed named and numerical (decimal or hexa) character references supported.</li>
 *   <li>Ability to default to numerical (decimal or hexa) references when an applicable NCR does not exist
 *       (depending on the selected operation <em>level</em>).</li>
 *   <li>Support for the whole Unicode character set: <tt>&#92;u0000</tt> to <tt>&#92;u10FFFF</tt>, including
 *       characters not representable by only one <tt>char</tt> in Java (<tt>&gt;&#92;uFFFF</tt>).</li>
 *   <li>Support for unescape of double-char NCRs in HTML5: <tt>'&amp;fjlig;'</tt> &rarr; <tt>'fj'</tt>.</li>
 *   <li>Support for a set of HTML5 unescape <em>tweaks</em> included in the HTML5 specification:
 *       <ul>
 *         <li>Unescape of numerical character references not ending in semi-colon
 *             (e.g. <tt>'&amp;#x23ac'</tt>).</li>
 *         <li>Unescape of specific NCRs not ending in semi-colon (e.g. <tt>'&amp;aacute'</tt>).</li>
 *         <li>Unescape of specific numerical character references wrongly specified by their Windows-1252
 *             codepage code instead of the Unicode one (e.g. <tt>'&amp;#x80;'</tt> for '&euro;'
 *             (<tt>'&amp;euro;'</tt>) instead of <tt>'&amp;#x20ac;'</tt>).</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <strong><u>Input/Output</u></strong>
 *
 * <p>
 *   There are four different input/output modes that can be used in escape/unescape operations:
 * </p>
 * <ul>
 *   <li><em><tt>String</tt> input, <tt>String</tt> output</em>: Input is specified as a <tt>String</tt> object
 *       and output is returned as another. In order to improve memory performance, all escape and unescape
 *       operations <u>will return the exact same input object as output if no escape/unescape modifications
 *       are required</u>.</li>
 *   <li><em><tt>String</tt> input, <tt>java.io.Writer</tt> output</em>: Input will be read from a String
 *       and output will be written into the specified <tt>java.io.Writer</tt>.</li>
 *   <li><em><tt>java.io.Reader</tt> input, <tt>java.io.Writer</tt> output</em>: Input will be read from a Reader
 *       and output will be written into the specified <tt>java.io.Writer</tt>.</li>
 *   <li><em><tt>char[]</tt> input, <tt>java.io.Writer</tt> output</em>: Input will be read from a char array
 *       (<tt>char[]</tt>) and output will be written into the specified <tt>java.io.Writer</tt>.
 *       Two <tt>int</tt> arguments called <tt>offset</tt> and <tt>len</tt> will be
 *       used for specifying the part of the <tt>char[]</tt> that should be escaped/unescaped. These methods
 *       should be called with <tt>offset = 0</tt> and <tt>len = text.length</tt> in order to process
 *       the whole <tt>char[]</tt>.</li>
 * </ul>
 *
 * <strong><u>Glossary</u></strong>
 *
 * <dl>
 *   <dt>NCR</dt>
 *     <dd>Named Character Reference or <em>Character Entity Reference</em>: textual
 *         representation of an Unicode codepoint: <tt>&amp;aacute;</tt></dd>
 *   <dt>DCR</dt>
 *     <dd>Decimal Character Reference: base-10 numerical representation of an Unicode codepoint:
 *         <tt>&amp;#225;</tt></dd>
 *   <dt>HCR</dt>
 *     <dd>Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint:
 *         <tt>&amp;#xE1;</tt></dd>
 *   <dt>Unicode Codepoint</dt>
 *     <dd>Each of the <tt>int</tt> values conforming the Unicode code space.
 *         Normally corresponding to a Java <tt>char</tt> primitive value (codepoint &lt;= <tt>&#92;uFFFF</tt>),
 *         but might be two <tt>char</tt>s for codepoints <tt>&#92;u10000</tt> to <tt>&#92;u10FFFF</tt> if the
 *         first <tt>char</tt> is a high surrogate (<tt>&#92;uD800</tt> to <tt>&#92;uDBFF</tt>) and the
 *         second is a low surrogate (<tt>&#92;uDC00</tt> to <tt>&#92;uDFFF</tt>).</dd>
 * </dl>
 *
 * <strong><u>References</u></strong>
 *
 * <p>
 *   The following references apply:
 * </p>
 * <ul>
 *   <li><a href="http://www.w3.org/International/questions/qa-escapes" target="_blank">Using character escapes in
 *       markup and CSS</a> [w3.org]</li>
 *   <li><a href="http://www.w3.org/TR/html4/sgml/entities.html" target="_blank">Named Character References (or
 *       <em>Character entity references</em>) in HTML 4</a> [w3.org]</li>
 *   <li><a href="http://www.w3.org/TR/html5/syntax.html#named-character-references" target="_blank">Named Character
 *       References (or <em>Character entity references</em>) in HTML5</a> [w3.org]</li>
 *   <li><a href="http://www.w3.org/TR/html51/syntax.html#named-character-references" target="_blank">Named Character
 *       References (or <em>Character entity references</em>) in HTML 5.1</a> [w3.org]</li>
 *   <li><a href="http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference" target="_blank">How to consume a
 *       character reference (HTML5 specification)</a> [w3.org]</li>
 *   <li><a href="https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet"
 *       target="_blank">OWASP XSS (Cross Site Scripting) Prevention Cheat Sheet</a> [owasp.org]</li>
 *   <li><a href="http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html"
 *       target="_blank">Supplementary characters in the Java Platform</a> [oracle.com]</li>
 * </ul>
 *
 *
 * @author Daniel Fern&aacute;ndez
 * 
 * @since 1.0.0
 *
 */
public final class HtmlEscape {




    /**
     * <p>
     *   Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml5(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml4Xml(String)} because
     *  it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     *  (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml5Xml(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml4(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml5Xml(String)} because
     *  it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     *  such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml4Xml(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   This method will perform an escape operation according to the specified
     *   {@link org.unbescape.html.HtmlEscapeType} and {@link org.unbescape.html.HtmlEscapeLevel}
     *   argument values.
     * </p>
     * <p>
     *   All other <tt>String</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     *   <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param type the type of escape operation to be performed, see {@link org.unbescape.html.HtmlEscapeType}.
     * @param level the escape level to be applied, see {@link org.unbescape.html.HtmlEscapeLevel}.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml(final String text, final HtmlEscapeType type, final HtmlEscapeLevel level) {

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        return HtmlEscapeUtil.escape(text, type, level);

    }







    /**
     * <p>
     *   Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml5(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml4Xml(String, Writer)} because
     *  it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     *  (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml5Xml(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml4(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml5Xml(String, Writer)} because
     *  it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     *  such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml4Xml(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>String</tt> input, writing
     *   results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   This method will perform an escape operation according to the specified
     *   {@link org.unbescape.html.HtmlEscapeType} and {@link org.unbescape.html.HtmlEscapeLevel}
     *   argument values.
     * </p>
     * <p>
     *   All other <tt>String</tt>/<tt>Writer</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     *   <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @param type the type of escape operation to be performed, see {@link org.unbescape.html.HtmlEscapeType}.
     * @param level the escape level to be applied, see {@link org.unbescape.html.HtmlEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml(final String text, final Writer writer, final HtmlEscapeType type, final HtmlEscapeLevel level)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        HtmlEscapeUtil.escape(new InternalStringReader(text), writer, type, level);

    }







    /**
     * <p>
     *   Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml5(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml4Xml(Reader, Writer)} because
     *  it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     *  (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml5Xml(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml4(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     *   writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml5Xml(Reader, Writer)} because
     *  it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     *  such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml4Xml(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>Reader</tt> input, writing
     *   results to a <tt>Writer</tt>.
     * </p>
     * <p>
     *   This method will perform an escape operation according to the specified
     *   {@link org.unbescape.html.HtmlEscapeType} and {@link org.unbescape.html.HtmlEscapeLevel}
     *   argument values.
     * </p>
     * <p>
     *   All other <tt>Reader</tt>/<tt>Writer</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     *   <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @param type the type of escape operation to be performed, see {@link org.unbescape.html.HtmlEscapeType}.
     * @param level the escape level to be applied, see {@link org.unbescape.html.HtmlEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void escapeHtml(final Reader reader, final Writer writer, final HtmlEscapeType type, final HtmlEscapeLevel level)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        HtmlEscapeUtil.escape(reader, writer, type, level);

    }







    /**
     * <p>
     *   Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(char[], int, int, Writer, HtmlEscapeType, HtmlEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml5(final char[] text, final int offset, final int len, final Writer writer)
                                   throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as
     *  {@link #escapeHtml4Xml(char[], int, int, Writer)} because
     *  it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     *  (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(char[], int, int, Writer, HtmlEscapeType, HtmlEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml5Xml(final char[] text, final int offset, final int len, final Writer writer)
                                      throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(char[], int, int, Writer, HtmlEscapeType, HtmlEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml4(final char[] text, final int offset, final int len, final Writer writer)
                                   throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the five markup-significant characters:
     *   <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     *   <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     *   <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     *  Note this method may <strong>not</strong> produce the same results as
     *  {@link #escapeHtml5Xml(char[], int, int, Writer)}  because it will escape the apostrophe as
     *  <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(char[], int, int, Writer, HtmlEscapeType, HtmlEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link org.unbescape.html.HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link org.unbescape.html.HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml4Xml(final char[] text, final int offset, final int len, final Writer writer)
                                      throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }


    /**
     * <p>
     *   Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   This method will perform an escape operation according to the specified
     *   {@link org.unbescape.html.HtmlEscapeType} and {@link org.unbescape.html.HtmlEscapeLevel}
     *   argument values.
     * </p>
     * <p>
     *   All other <tt>char[]</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     *   <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @param type the type of escape operation to be performed, see {@link org.unbescape.html.HtmlEscapeType}.
     * @param level the escape level to be applied, see {@link org.unbescape.html.HtmlEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml(final char[] text, final int offset, final int len, final Writer writer,
                                  final HtmlEscapeType type, final HtmlEscapeLevel level)
                                  throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        final int textLen = (text == null? 0 : text.length);

        if (offset < 0 || offset > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        if (len < 0 || (offset + len) > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        HtmlEscapeUtil.escape(text, offset, len, writer, type, level);

    }






    /**
     * <p>
     *   Perform an HTML <strong>unescape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   No additional configuration arguments are required. Unescape operations
     *   will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     *   and hexadecimal references.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be unescaped.
     * @return The unescaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no unescaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String unescapeHtml(final String text) {
        if (text == null) {
            return null;
        }
        if (text.indexOf('&') < 0) {
            // Fail fast, avoid more complex (and less JIT-table) method to execute if not needed
            return text;
        }
        return HtmlEscapeUtil.unescape(text);
    }



    /**
     * <p>
     *   Perform an HTML <strong>unescape</strong> operation on a <tt>String</tt> input, writing results to
     *   a <tt>Writer</tt>.
     * </p>
     * <p>
     *   No additional configuration arguments are required. Unescape operations
     *   will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     *   and hexadecimal references.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void unescapeHtml(final String text, final Writer writer)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }
        if (text == null) {
            return;
        }
        if (text.indexOf('&') < 0) {
            // Fail fast, avoid more complex (and less JIT-table) method to execute if not needed
            writer.write(text);
            return;
        }

        HtmlEscapeUtil.unescape(new InternalStringReader(text), writer);

    }



    /**
     * <p>
     *   Perform an HTML <strong>unescape</strong> operation on a <tt>Reader</tt> input, writing results to
     *   a <tt>Writer</tt>.
     * </p>
     * <p>
     *   No additional configuration arguments are required. Unescape operations
     *   will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     *   and hexadecimal references.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     *
     * @since 1.1.2
     */
    public static void unescapeHtml(final Reader reader, final Writer writer)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        HtmlEscapeUtil.unescape(reader, writer);

    }



    /**
     * <p>
     *   Perform an HTML <strong>unescape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   No additional configuration arguments are required. Unescape operations
     *   will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     *   and hexadecimal references.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be unescaped.
     * @param offset the position in <tt>text</tt> at which the unescape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void unescapeHtml(final char[] text, final int offset, final int len, final Writer writer)
                                    throws IOException{

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        final int textLen = (text == null? 0 : text.length);

        if (offset < 0 || offset > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        if (len < 0 || (offset + len) > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        HtmlEscapeUtil.unescape(text, offset, len, writer);

    }




    private HtmlEscape() {
        super();
    }



    /*
     * This is basically a very simplified, thread-unsafe version of StringReader that should
     * perform better than the original StringReader by removing all synchronization structures.
     *
     * Note the only implemented methods are those that we know are really used from within the
     * stream-based escape/unescape operations.
     */
    private static final class InternalStringReader extends Reader {

        private String str;
        private int length;
        private int next = 0;

        public InternalStringReader(final String s) {
            super();
            this.str = s;
            this.length = s.length();
        }

        @Override
        public int read() throws IOException {
            if (this.next >= length) {
                return -1;
            }
            return this.str.charAt(this.next++);
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                    ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (this.next >= this.length) {
                return -1;
            }
            int n = Math.min(this.length - this.next, len);
            this.str.getChars(this.next, this.next + n, cbuf, off);
            this.next += n;
            return n;
        }

        @Override
        public void close() throws IOException {
            this.str = null; // Just set the reference to null, help the GC
        }

    }


}

