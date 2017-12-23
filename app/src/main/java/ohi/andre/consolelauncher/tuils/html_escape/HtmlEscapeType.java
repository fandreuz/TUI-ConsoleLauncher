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

/**
 * <p>
 *   Types of escape operations to be performed on HTML text:
 * </p>
 *
 * <ul>
 *     <li><tt><strong>HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL</strong></tt>: Replace escaped characters
 *         with HTML 4 <em>Named Character References</em> (<em>Character Entity References</em>) whenever
 *         possible (depending on the specified {@link org.unbescape.html.HtmlEscapeLevel}), and default to
 *         using <em>Decimal Character References</em> for escaped characters that do not have an associated
 *         NCR.</li>
 *     <li><tt><strong>HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA</strong></tt>: Replace escaped characters
 *         with HTML 4 <em>Named Character References</em> (<em>Character Entity References</em>) whenever
 *         possible (depending on the specified {@link org.unbescape.html.HtmlEscapeLevel}), and default to
 *         using <em>Hexadecimal Character References</em> for escaped characters that do not have an associated
 *         NCR.</li>
 *     <li><tt><strong>HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL</strong></tt>: Replace escaped characters
 *         with HTML5 <em>Named Character References</em> whenever
 *         possible (depending on the specified {@link org.unbescape.html.HtmlEscapeLevel}), and default to
 *         using <em>Decimal Character References</em> for escaped characters that do not have an associated
 *         NCR.</li>
 *     <li><tt><strong>HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA</strong></tt>: Replace escaped characters
 *         with HTML5 <em>Named Character References</em> whenever
 *         possible (depending on the specified {@link org.unbescape.html.HtmlEscapeLevel}), and default to
 *         using <em>Hexadecimal Character References</em> for escaped characters that do not have an associated
 *         NCR.</li>
 *     <li><tt><strong>DECIMAL_REFERENCES</strong></tt>: Replace escaped characters with
 *         <em>Decimal Character References</em> (will never use NCRs).</li>
 *     <li><tt><strong>HEXADECIMAL_REFERENCES</strong></tt>: Replace escaped characters with
 *         <em>Hexadecimal Character References</em> (will never use NCRs).</li>
 * </ul>
 *
 * <p>
 *   For further information, see the <em>Glossary</em> and the <em>References</em> sections at the
 *   documentation for the {@link org.unbescape.html.HtmlEscape} class.
 * </p>
 *
 * @author Daniel Fern&aacute;ndez
 *
 * @since 1.0.0
 *
 */
public enum HtmlEscapeType {

    /**
     * Use HTML 4 NCRs if possible, default to Decimal Character References.
     */
    HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false, false),

    /**
     * Use HTML 4 NCRs if possible, default to Hexadecimal Character References.
     */
    HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true, false),

    /**
     * Use HTML5 NCRs if possible, default to Decimal Character References.
     */
    HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false, true),

    /**
     * Use HTML5 NCRs if possible, default to Hexadecimal Character References.
     */
    HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true, true),

    /**
     * Always use Decimal Character References (no NCRs will be used).
     */
    DECIMAL_REFERENCES(false, false, false),

    /**
     * Always use Hexadecimal Character References (no NCRs will be used).
     */
    HEXADECIMAL_REFERENCES(false, true, false);


    private final boolean useNCRs;
    private final boolean useHexa;
    private final boolean useHtml5;

    HtmlEscapeType(final boolean useNCRs, final boolean useHexa, final boolean useHtml5) {
        this.useNCRs = useNCRs;
        this.useHexa = useHexa;
        this.useHtml5 = useHtml5;
    }

    boolean getUseNCRs() {
        return this.useNCRs;
    }

    boolean getUseHexa() {
        return this.useHexa;
    }

    boolean getUseHtml5() {
        return this.useHtml5;
    }
}

