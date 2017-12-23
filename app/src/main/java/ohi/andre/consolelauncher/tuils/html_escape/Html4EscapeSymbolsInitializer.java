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

import java.util.Arrays;

/**
 * <p>
 *   This class initializes the {@link org.unbescape.html.HtmlEscapeSymbols#HTML4_SYMBOLS} structure.
 * </p>
 * 
 * @author Daniel Fern&aacute;ndez
 * 
 * @since 1.0.0
 *
 */
final class Html4EscapeSymbolsInitializer {


    static HtmlEscapeSymbols initializeHtml4() {

        final HtmlEscapeSymbols.References html4References = new HtmlEscapeSymbols.References();

        /*
         * -----------------------------------------------------------------
         *   HTML4 NAMED CHARACTER REFERENCES (CHARACTER ENTITY REFERENCES)
         *   See: http://www.w3.org/TR/html4/sgml/entities.html
         * -----------------------------------------------------------------
         */

        /* HTML NCRs FOR MARKUP-SIGNIFICANT CHARACTERS */
        // (Note HTML 4 does not include &apos; as a valid NCR)
        html4References.addReference('"', "&quot;");
        html4References.addReference('&', "&amp;");
        html4References.addReference('<', "&lt;");
        html4References.addReference('>', "&gt;");
        /* HTML NCRs FOR ISO-8859-1 CHARACTERS */
        html4References.addReference('\u00A0', "&nbsp;");
        html4References.addReference('\u00A1', "&iexcl;");
        html4References.addReference('\u00A2', "&cent;");
        html4References.addReference('\u00A3', "&pound;");
        html4References.addReference('\u00A4', "&curren;");
        html4References.addReference('\u00A5', "&yen;");
        html4References.addReference('\u00A6', "&brvbar;");
        html4References.addReference('\u00A7', "&sect;");
        html4References.addReference('\u00A8', "&uml;");
        html4References.addReference('\u00A9', "&copy;");
        html4References.addReference('\u00AA', "&ordf;");
        html4References.addReference('\u00AB', "&laquo;");
        html4References.addReference('\u00AC', "&not;");
        html4References.addReference('\u00AD', "&shy;");
        html4References.addReference('\u00AE', "&reg;");
        html4References.addReference('\u00AF', "&macr;");
        html4References.addReference('\u00B0', "&deg;");
        html4References.addReference('\u00B1', "&plusmn;");
        html4References.addReference('\u00B2', "&sup2;");
        html4References.addReference('\u00B3', "&sup3;");
        html4References.addReference('\u00B4', "&acute;");
        html4References.addReference('\u00B5', "&micro;");
        html4References.addReference('\u00B6', "&para;");
        html4References.addReference('\u00B7', "&middot;");
        html4References.addReference('\u00B8', "&cedil;");
        html4References.addReference('\u00B9', "&sup1;");
        html4References.addReference('\u00BA', "&ordm;");
        html4References.addReference('\u00BB', "&raquo;");
        html4References.addReference('\u00BC', "&frac14;");
        html4References.addReference('\u00BD', "&frac12;");
        html4References.addReference('\u00BE', "&frac34;");
        html4References.addReference('\u00BF', "&iquest;");
        html4References.addReference('\u00C0', "&Agrave;");
        html4References.addReference('\u00C1', "&Aacute;");
        html4References.addReference('\u00C2', "&Acirc;");
        html4References.addReference('\u00C3', "&Atilde;");
        html4References.addReference('\u00C4', "&Auml;");
        html4References.addReference('\u00C5', "&Aring;");
        html4References.addReference('\u00C6', "&AElig;");
        html4References.addReference('\u00C7', "&Ccedil;");
        html4References.addReference('\u00C8', "&Egrave;");
        html4References.addReference('\u00C9', "&Eacute;");
        html4References.addReference('\u00CA', "&Ecirc;");
        html4References.addReference('\u00CB', "&Euml;");
        html4References.addReference('\u00CC', "&Igrave;");
        html4References.addReference('\u00CD', "&Iacute;");
        html4References.addReference('\u00CE', "&Icirc;");
        html4References.addReference('\u00CF', "&Iuml;");
        html4References.addReference('\u00D0', "&ETH;");
        html4References.addReference('\u00D1', "&Ntilde;");
        html4References.addReference('\u00D2', "&Ograve;");
        html4References.addReference('\u00D3', "&Oacute;");
        html4References.addReference('\u00D4', "&Ocirc;");
        html4References.addReference('\u00D5', "&Otilde;");
        html4References.addReference('\u00D6', "&Ouml;");
        html4References.addReference('\u00D7', "&times;");
        html4References.addReference('\u00D8', "&Oslash;");
        html4References.addReference('\u00D9', "&Ugrave;");
        html4References.addReference('\u00DA', "&Uacute;");
        html4References.addReference('\u00DB', "&Ucirc;");
        html4References.addReference('\u00DC', "&Uuml;");
        html4References.addReference('\u00DD', "&Yacute;");
        html4References.addReference('\u00DE', "&THORN;");
        html4References.addReference('\u00DF', "&szlig;");
        html4References.addReference('\u00E0', "&agrave;");
        html4References.addReference('\u00E1', "&aacute;");
        html4References.addReference('\u00E2', "&acirc;");
        html4References.addReference('\u00E3', "&atilde;");
        html4References.addReference('\u00E4', "&auml;");
        html4References.addReference('\u00E5', "&aring;");
        html4References.addReference('\u00E6', "&aelig;");
        html4References.addReference('\u00E7', "&ccedil;");
        html4References.addReference('\u00E8', "&egrave;");
        html4References.addReference('\u00E9', "&eacute;");
        html4References.addReference('\u00EA', "&ecirc;");
        html4References.addReference('\u00EB', "&euml;");
        html4References.addReference('\u00EC', "&igrave;");
        html4References.addReference('\u00ED', "&iacute;");
        html4References.addReference('\u00EE', "&icirc;");
        html4References.addReference('\u00EF', "&iuml;");
        html4References.addReference('\u00F0', "&eth;");
        html4References.addReference('\u00F1', "&ntilde;");
        html4References.addReference('\u00F2', "&ograve;");
        html4References.addReference('\u00F3', "&oacute;");
        html4References.addReference('\u00F4', "&ocirc;");
        html4References.addReference('\u00F5', "&otilde;");
        html4References.addReference('\u00F6', "&ouml;");
        html4References.addReference('\u00F7', "&divide;");
        html4References.addReference('\u00F8', "&oslash;");
        html4References.addReference('\u00F9', "&ugrave;");
        html4References.addReference('\u00FA', "&uacute;");
        html4References.addReference('\u00FB', "&ucirc;");
        html4References.addReference('\u00FC', "&uuml;");
        html4References.addReference('\u00FD', "&yacute;");
        html4References.addReference('\u00FE', "&thorn;");
        html4References.addReference('\u00FF', "&yuml;");
        /* HTML NCRs FOR SYMBOLS, MATHEMATICAL SYMBOLS AND GREEK LETTERS */
        /* - Greek */
        html4References.addReference('\u0192', "&fnof;");
        html4References.addReference('\u0391', "&Alpha;");
        html4References.addReference('\u0392', "&Beta;");
        html4References.addReference('\u0393', "&Gamma;");
        html4References.addReference('\u0394', "&Delta;");
        html4References.addReference('\u0395', "&Epsilon;");
        html4References.addReference('\u0396', "&Zeta;");
        html4References.addReference('\u0397', "&Eta;");
        html4References.addReference('\u0398', "&Theta;");
        html4References.addReference('\u0399', "&Iota;");
        html4References.addReference('\u039A', "&Kappa;");
        html4References.addReference('\u039B', "&Lambda;");
        html4References.addReference('\u039C', "&Mu;");
        html4References.addReference('\u039D', "&Nu;");
        html4References.addReference('\u039E', "&Xi;");
        html4References.addReference('\u039F', "&Omicron;");
        html4References.addReference('\u03A0', "&Pi;");
        html4References.addReference('\u03A1', "&Rho;");
        html4References.addReference('\u03A3', "&Sigma;");
        html4References.addReference('\u03A4', "&Tau;");
        html4References.addReference('\u03A5', "&Upsilon;");
        html4References.addReference('\u03A6', "&Phi;");
        html4References.addReference('\u03A7', "&Chi;");
        html4References.addReference('\u03A8', "&Psi;");
        html4References.addReference('\u03A9', "&Omega;");
        html4References.addReference('\u03B1', "&alpha;");
        html4References.addReference('\u03B2', "&beta;");
        html4References.addReference('\u03B3', "&gamma;");
        html4References.addReference('\u03B4', "&delta;");
        html4References.addReference('\u03B5', "&epsilon;");
        html4References.addReference('\u03B6', "&zeta;");
        html4References.addReference('\u03B7', "&eta;");
        html4References.addReference('\u03B8', "&theta;");
        html4References.addReference('\u03B9', "&iota;");
        html4References.addReference('\u03BA', "&kappa;");
        html4References.addReference('\u03BB', "&lambda;");
        html4References.addReference('\u03BC', "&mu;");
        html4References.addReference('\u03BD', "&nu;");
        html4References.addReference('\u03BE', "&xi;");
        html4References.addReference('\u03BF', "&omicron;");
        html4References.addReference('\u03C0', "&pi;");
        html4References.addReference('\u03C1', "&rho;");
        html4References.addReference('\u03C2', "&sigmaf;");
        html4References.addReference('\u03C3', "&sigma;");
        html4References.addReference('\u03C4', "&tau;");
        html4References.addReference('\u03C5', "&upsilon;");
        html4References.addReference('\u03C6', "&phi;");
        html4References.addReference('\u03C7', "&chi;");
        html4References.addReference('\u03C8', "&psi;");
        html4References.addReference('\u03C9', "&omega;");
        html4References.addReference('\u03D1', "&thetasym;");
        html4References.addReference('\u03D2', "&upsih;");
        html4References.addReference('\u03D6', "&piv;");
        /* - General punctuation */
        html4References.addReference('\u2022', "&bull;");
        html4References.addReference('\u2026', "&hellip;");
        html4References.addReference('\u2032', "&prime;");
        html4References.addReference('\u2033', "&Prime;");
        html4References.addReference('\u203E', "&oline;");
        html4References.addReference('\u2044', "&frasl;");
        /* - Letter-like symbols */
        html4References.addReference('\u2118', "&weierp;");
        html4References.addReference('\u2111', "&image;");
        html4References.addReference('\u211C', "&real;");
        html4References.addReference('\u2122', "&trade;");
        html4References.addReference('\u2135', "&alefsym;");
        /* - Arrows */
        html4References.addReference('\u2190', "&larr;");
        html4References.addReference('\u2191', "&uarr;");
        html4References.addReference('\u2192', "&rarr;");
        html4References.addReference('\u2193', "&darr;");
        html4References.addReference('\u2194', "&harr;");
        html4References.addReference('\u21B5', "&crarr;");
        html4References.addReference('\u21D0', "&lArr;");
        html4References.addReference('\u21D1', "&uArr;");
        html4References.addReference('\u21D2', "&rArr;");
        html4References.addReference('\u21D3', "&dArr;");
        html4References.addReference('\u21D4', "&hArr;");
        /* - Mathematical operators */
        html4References.addReference('\u2200', "&forall;");
        html4References.addReference('\u2202', "&part;");
        html4References.addReference('\u2203', "&exist;");
        html4References.addReference('\u2205', "&empty;");
        html4References.addReference('\u2207', "&nabla;");
        html4References.addReference('\u2208', "&isin;");
        html4References.addReference('\u2209', "&notin;");
        html4References.addReference('\u220B', "&ni;");
        html4References.addReference('\u220F', "&prod;");
        html4References.addReference('\u2211', "&sum;");
        html4References.addReference('\u2212', "&minus;");
        html4References.addReference('\u2217', "&lowast;");
        html4References.addReference('\u221A', "&radic;");
        html4References.addReference('\u221D', "&prop;");
        html4References.addReference('\u221E', "&infin;");
        html4References.addReference('\u2220', "&ang;");
        html4References.addReference('\u2227', "&and;");
        html4References.addReference('\u2228', "&or;");
        html4References.addReference('\u2229', "&cap;");
        html4References.addReference('\u222A', "&cup;");
        html4References.addReference('\u222B', "&int;");
        html4References.addReference('\u2234', "&there4;");
        html4References.addReference('\u223C', "&sim;");
        html4References.addReference('\u2245', "&cong;");
        html4References.addReference('\u2248', "&asymp;");
        html4References.addReference('\u2260', "&ne;");
        html4References.addReference('\u2261', "&equiv;");
        html4References.addReference('\u2264', "&le;");
        html4References.addReference('\u2265', "&ge;");
        html4References.addReference('\u2282', "&sub;");
        html4References.addReference('\u2283', "&sup;");
        html4References.addReference('\u2284', "&nsub;");
        html4References.addReference('\u2286', "&sube;");
        html4References.addReference('\u2287', "&supe;");
        html4References.addReference('\u2295', "&oplus;");
        html4References.addReference('\u2297', "&otimes;");
        html4References.addReference('\u22A5', "&perp;");
        html4References.addReference('\u22C5', "&sdot;");
        /* - Miscellaneous technical */
        html4References.addReference('\u2308', "&lceil;");
        html4References.addReference('\u2309', "&rceil;");
        html4References.addReference('\u230A', "&lfloor;");
        html4References.addReference('\u230B', "&rfloor;");
        html4References.addReference('\u2329', "&lang;");
        html4References.addReference('\u232A', "&rang;");
        /* - Geometric shapes */
        html4References.addReference('\u25CA', "&loz;");
        html4References.addReference('\u2660', "&spades;");
        html4References.addReference('\u2663', "&clubs;");
        html4References.addReference('\u2665', "&hearts;");
        html4References.addReference('\u2666', "&diams;");
        /* HTML NCRs FOR INTERNATIONALIZATION CHARACTERS */
        /* - Latin Extended-A */
        html4References.addReference('\u0152', "&OElig;");
        html4References.addReference('\u0153', "&oelig;");
        html4References.addReference('\u0160', "&Scaron;");
        html4References.addReference('\u0161', "&scaron;");
        html4References.addReference('\u0178', "&Yuml;");
        /* - Spacing modifier letters */
        html4References.addReference('\u02C6', "&circ;");
        html4References.addReference('\u02DC', "&tilde;");
        /* - General punctuation */
        html4References.addReference('\u2002', "&ensp;");
        html4References.addReference('\u2003', "&emsp;");
        html4References.addReference('\u2009', "&thinsp;");
        html4References.addReference('\u200C', "&zwnj;");
        html4References.addReference('\u200D', "&zwj;");
        html4References.addReference('\u200E', "&lrm;");
        html4References.addReference('\u200F', "&rlm;");
        html4References.addReference('\u2013', "&ndash;");
        html4References.addReference('\u2014', "&mdash;");
        html4References.addReference('\u2018', "&lsquo;");
        html4References.addReference('\u2019', "&rsquo;");
        html4References.addReference('\u201A', "&sbquo;");
        html4References.addReference('\u201C', "&ldquo;");
        html4References.addReference('\u201D', "&rdquo;");
        html4References.addReference('\u201E', "&bdquo;");
        html4References.addReference('\u2020', "&dagger;");
        html4References.addReference('\u2021', "&Dagger;");
        html4References.addReference('\u2030', "&permil;");
        html4References.addReference('\u2039', "&lsaquo;");
        html4References.addReference('\u203A', "&rsaquo;");
        html4References.addReference('\u20AC', "&euro;");


        /*
         * Initialization of escape levels.
         * Defined levels :
         *
         *    - Level 0 : Only markup-significant characters except the apostrophe (')
         *    - Level 1 : Only markup-significant characters (including the apostrophe)
         *    - Level 2 : Markup-significant characters plus all non-ASCII
         *    - Level 3 : All non-alphanumeric characters
         *    - Level 4 : All characters
         */
        final byte[] escapeLevels = new byte[0x7f + 2];
        Arrays.fill(escapeLevels, (byte)3);
        for (char c = 'A'; c <= 'Z'; c++) {
            escapeLevels[c] = 4;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            escapeLevels[c] = 4;
        }
        for (char c = '0'; c <= '9'; c++) {
            escapeLevels[c] = 4;
        }
        escapeLevels['\''] = 1;
        escapeLevels['"'] = 0;
        escapeLevels['<'] = 0;
        escapeLevels['>'] = 0;
        escapeLevels['&'] = 0;
        escapeLevels[0x7f + 1] = 2;


        return new HtmlEscapeSymbols(html4References, escapeLevels);

    }


    private Html4EscapeSymbolsInitializer() {
        super();
    }

}

