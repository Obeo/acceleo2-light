/*
 * Copyright (c) 2005-2008 Obeo
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 */

package fr.obeo.acceleo.tools.strings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fr.obeo.acceleo.tools.AcceleoToolsMessages;
import fr.obeo.acceleo.tools.AcceleoToolsPlugin;
import fr.obeo.acceleo.tools.resources.FileContentMap;
import fr.obeo.acceleo.tools.resources.Resources;

/**
 * Java language supports pattern matching. This class includes methods for
 * examining sequences of characters, for searching strings with or without the
 * regular expression library. The public static attribute 'settings' specifies
 * the pattern matching configuration for all the methods of this class : <li>
 * regular expression</li> <li>ignore case</li> <li>escape characters</li> <li>
 * recursive blocks</li>
 * 
 * @author www.obeo.fr
 * 
 */
public class TextSearch {

    /**
     * This property is used to disable the blocks recursivity.
     */
    public static final String FORCE_NOT_RECURSIVE = "__FORCE_NOT_RECURSIVE__"; //$NON-NLS-1$

    /**
     * @return the default search
     */
    public static TextSearch getDefaultSearch() {
        if (TextSearch.defaultSearch == null) {
            TextSearch.defaultSearch = new TextSearch(false, false, true);
        }
        return TextSearch.defaultSearch;
    }

    private static TextSearch defaultSearch = null;

    /**
     * @return the regex search
     */
    public static TextSearch getRegexSearch() {
        if (TextSearch.regexSearch == null) {
            TextSearch.regexSearch = new TextSearch(true, false, true);
        }
        return TextSearch.regexSearch;
    }

    private static TextSearch regexSearch = null;

    /**
     * @return the ignore case search
     */
    public static TextSearch getIgnoreCaseSearch() {
        if (TextSearch.ignoreCaseSearch == null) {
            TextSearch.ignoreCaseSearch = new TextSearch(false, true, true);
        }
        return TextSearch.ignoreCaseSearch;
    }

    private static TextSearch ignoreCaseSearch = null;

    /**
     * The sequences of characters are read with (true) or without (false) the
     * regular expression library.
     */
    private boolean regex;

    /**
     * Case is ignored if regex is false and IGNORE_CASE_IF_NOT_REGEX is true.
     */
    private boolean ignoreCase;

    /**
     * Blocks are recursively ignored.
     * <p>
     * Sample :
     * <li>"{1 .. [2 .. {3 .. }4 .. ]5 .. }6" is the string to be parsed</li>
     * <li>"[","]" and "{","}" are defined blocks</li>
     * <li>if RECURSIVE_INHIBS is true then block {1 .. }6 is detected, else
     * block {1 .. }4</li>
     */
    private boolean recursiveInhibs;

    /**
     * Constructor.
     * 
     * @param regex
     *            indicates if the regular expression library is used
     * @param ignoreCase
     *            indicates if the case is ignored
     * @param recursiveInhibs
     *            indicates if the blocks are recursively ignored
     */
    private TextSearch(boolean regex, boolean ignoreCase, boolean recursiveInhibs) {
        this.regex = regex;
        this.ignoreCase = ignoreCase;
        this.recursiveInhibs = recursiveInhibs;
    }

    /**
     * Compiles the given regular expression into a pattern.
     * 
     * @param regex
     *            is the expression to be compiled
     * @return a compiled representation of a regular expression
     * @throws PatternSyntaxException
     *             if the expression's syntax is invalid
     */
    protected Pattern compile(String regex) {
        Pattern pattern = (Pattern) patternMap.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            patternMap.put(regex, pattern);
        }
        return pattern;
    }

    private Map patternMap = new HashMap();

    /**
     * Indicates if the strings are matching.
     * <p>
     * The pattern matching configuration (Strings.settings) has to be verified
     * before.
     * 
     * @param s1
     *            is the first string
     * @param s2
     *            is the second string
     * @return true if the strings are matching
     */
    public boolean matches(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        if (regex) {
            Pattern pattern = compile(s2);
            Matcher m = pattern.matcher(s1);
            return m.matches();
        } else {
            if (ignoreCase) {
                return s1.equalsIgnoreCase(s2);
            } else {
                return s1.equals(s2);
            }
        }
    }

    /**
     * Returns the index within the buffer of the first occurrence of the
     * specified substring.
     * <p>
     * The pattern matching configuration (Strings.settings) has to be verified
     * before.
     * 
     * @param buffer
     *            is the text to be explored
     * @param tag
     *            is the substring to search for
     * @return The index within the buffer of the first occurrence of the
     *         specified substring. If it does not occur as a substring,
     *         Int2(-1,-1) is returned.
     */
    public Int2 indexOf(final String buffer, final String tag) {
        return indexOf(buffer, tag, 0);
    }

    /**
     * Returns the index within the buffer of the first occurrence of the
     * specified substring, starting at the specified index. If no such
     * substring occurs in this string at or after position pos, then
     * Int2(-1,-1) is returned.
     * <p>
     * The pattern matching configuration (Strings.settings) has to be verified
     * before.
     * 
     * @param buffer
     *            is the text to be explored
     * @param tag
     *            is the substring to search for
     * @param pos
     *            is the index to start the search from
     * @return The index within the buffer of the first occurrence of the
     *         specified substring, starting at the specified index. If it does
     *         not occur as a substring, or pos < 0, or buffer.length() <= pos,
     *         Int2(-1,-1) is returned.
     */
    public Int2 indexOf(final String buffer, final String tag, int pos) {
        return indexIn(buffer, tag, pos, buffer.length());
    }

    /**
     * Returns the index within the buffer of the first occurrence of the
     * specified substring. If no such substring occurs in this string, then
     * Int2(-1,-1) is returned.
     * 
     * @param buffer
     *            is the text to be explored
     * @param tag
     *            is the substring to search for
     * @param spec
     *            is the pattern's escape character
     * @param inhibs
     *            are the ignored sub-blocks
     * @return the index within the buffer of the first occurrence of the
     *         specified substring. If it does not occur as a substring,
     *         Int2(-1,-1) is returned. It returns 0 if (tag.length() == 0).
     */
    public Int2 indexOf(final String buffer, final String tag, String spec, String[][] inhibs) {
        return indexOf(buffer, tag, 0, spec, inhibs);
    }

    /**
     * Returns the index within the buffer of the first occurrence of the
     * specified substring, starting at the specified index. If no such
     * substring occurs in this string at or after position pos, then
     * Int2(-1,-1) is returned.
     * <p>
     * An inhibs element sample : String[] = { 1,2 (,3)? }
     * <li>1:begin tag "</li>
     * <li>2:end tag "</li>
     * <li>3:blocks properties FORCE_NOT_RECURSIVE</li>
     * 
     * @param buffer
     *            is the text to be explored
     * @param tag
     *            is the substring to search for
     * @param pos
     *            is the beginning index
     * @param spec
     *            is the pattern's escape character
     * @param inhibs
     *            are the ignored sub-blocks
     * @return the index within the buffer of the first occurrence of the
     *         specified substring, starting at the specified index. If it does
     *         not occur as a substring, or pos < 0, or buffer.length() <= pos,
     *         Int2(-1,-1) is returned. It returns 0 if (tag.length() == 0).
     */
    public Int2 indexOf(final String buffer, final String tag, int pos, String spec, String[][] inhibs) {
        return indexOf(buffer, tag, pos, spec, null, inhibs);
    }

    /**
     * Returns the index within the buffer of the first occurrence of the
     * specified substring, starting at the specified index. If no such
     * substring occurs in this string at or after position pos, then
     * Int2(-1,-1) is returned.
     * <p>
     * An inhibs element sample : String[] = { 1,2 (,3)? }
     * <li>1:begin tag "</li>
     * <li>2:end tag "</li>
     * <li>3:blocks properties FORCE_NOT_RECURSIVE</li>
     * 
     * @param buffer
     *            is the text to be explored
     * @param tag
     *            is the substring to search for
     * @param pos
     *            is the beginning index
     * @param spec
     *            is the pattern's escape character
     * @param jump
     *            is a scanner that ignores a sequence of characters
     * @param inhibs
     *            are the ignored sub-blocks
     * @return the index within the buffer of the first occurrence of the
     *         specified substring, starting at the specified index. If it does
     *         not occur as a substring, or pos < 0, or buffer.length() <= pos,
     *         Int2(-1,-1) is returned. It returns 0 if (tag.length() == 0).
     */
    public Int2 indexOf(final String buffer, final String tag, int pos, String spec, Jump jump, String[][] inhibs) {
        return indexIn(buffer, tag, pos, buffer.length(), spec, jump, inhibs);
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || posEnd >
     * buffer.length()) => -1
     */
    public Int2 indexIn(final String buffer, final String tag, int posBegin, int posEnd) {
        if (buffer == null || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return Int2.NOT_FOUND;
        }
        if (tag == null || tag.length() == 0) {
            return Int2.NOT_FOUND;
        }
        String substring = buffer.substring(posBegin, posEnd);
        if (regex) {
            Pattern pattern = compile(tag);
            Matcher m = pattern.matcher(substring);
            if (m.find()) {
                return new Int2(posBegin + m.start(), posBegin + m.end());
            } else {
                return Int2.NOT_FOUND;
            }
        } else {
            int i;
            if (ignoreCase) {
                i = substring.toLowerCase().indexOf(tag.toLowerCase());
            } else {
                i = substring.indexOf(tag);
            }
            if (i > -1) {
                int b = posBegin + i;
                return new Int2(b, b + tag.length());
            } else {
                return Int2.NOT_FOUND;
            }
        }
    }

    public Int2 indexIn(final String buffer, final String tag, int posBegin, int posEnd, String spec, String[][] inhibs) {
        return indexIn(buffer, tag, posBegin, posEnd, spec, null, inhibs);
    }

    public Int2 indexIn(final String buffer, final String tag, int posBegin, int posEnd, String spec, Jump jump, String[][] inhibs) {
        if (buffer == null || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return Int2.NOT_FOUND;
        }
        if (spec == null && inhibs == null) {
            return indexIn(buffer, tag, posBegin, posEnd);
        }
        if (tag == null) {
            return Int2.NOT_FOUND;
        }
        int inhibs_size = 0;
        if (inhibs != null) {
            inhibs_size = inhibs.length;
        }
        Int2[] positions = new Int2[3 + inhibs_size];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = new Int2(-2, -2);
        }
        while (posBegin > -1 && posBegin < posEnd) {
            // Positions for tags and inhibs
            if (positions[0].b() != -1 && spec != null && posBegin > positions[0].b()) {
                positions[0] = indexIn(buffer, spec, posBegin, posEnd); // spec
            }
            if (positions[1].b() != -1 && posBegin > positions[1].b()) {
                positions[1] = indexIn(buffer, tag, posBegin, posEnd); // tag
            }
            if (positions[2].b() != -1 && jump != null && posBegin > positions[2].b()) {
                positions[2] = jump.begin(buffer, posBegin, posEnd); // jump
            }
            for (int i = 3; i < positions.length; i++) { // inhibsTag
                if (positions[i].b() != -1 && posBegin > positions[i].b()) {
                    positions[i] = indexIn(buffer, inhibs[i - 3][0], posBegin, posEnd);
                }
            }
            // Get next position
            int positionMin = posEnd;
            int iPositionMin = -1;
            for (int i = 0; i < positions.length; i++) {
                if ((positions[i].b() > -1) && (positions[i].b() < positionMin)) {
                    iPositionMin = i;
                    positionMin = positions[i].b();
                }
            }
            if (iPositionMin == -1 /* NOT FOUND */) {
                return Int2.NOT_FOUND;
            }
            // Get the next element
            if (iPositionMin == 0 /* spec */) {
                posBegin = positions[iPositionMin].e();
            } else if (iPositionMin == 1 /* tag */) {
                return positions[iPositionMin];
            } else if (iPositionMin == 2 /* jump */) {
                posBegin = jump.end(buffer, positions[iPositionMin], posEnd, spec, inhibs);
            } else if (iPositionMin >= 3 /* inhibsTag */) {
                boolean forceNotRecursive;
                if (inhibs[iPositionMin - 3].length >= 3 && inhibs[iPositionMin - 3][2] != null) {
                    forceNotRecursive = inhibs[iPositionMin - 3][2].indexOf(TextSearch.FORCE_NOT_RECURSIVE) > -1;
                } else {
                    forceNotRecursive = false;
                }
                posBegin = blockIndexEndIn(buffer, inhibs[iPositionMin - 3][0], inhibs[iPositionMin - 3][1], positions[iPositionMin].b(), posEnd,
                        (recursiveInhibs && !forceNotRecursive) ? true : false, spec, jump, (recursiveInhibs && !forceNotRecursive) ? inhibs : null).e();
            }
        };
        return Int2.NOT_FOUND;
    }

    /*
     * (endTag.length()==0) => buffer.length() (endTag not found || posBegin ne
     * correspond pas a beginTag) => -1 (posBegin < 0 ||
     * (buffer.length()-posBegin) <= 0) => -1
     */
    public Int2 blockIndexEndOf(final String buffer, String beginTag, String endTag, int posBegin, boolean recursive) {
        return blockIndexEndOf(buffer, beginTag, endTag, posBegin, recursive, null, null);
    }

    public Int2 blockIndexEndOf(final String buffer, String beginTag, String endTag, int posBegin, boolean recursive, String spec, String[][] inhibs) {
        return blockIndexEndOf(buffer, beginTag, endTag, posBegin, recursive, spec, null, inhibs);
    }

    public Int2 blockIndexEndOf(final String buffer, String beginTag, String endTag, int posBegin, boolean recursive, String spec, Jump jump, String[][] inhibs) {
        return blockIndexEndIn(buffer, beginTag, endTag, posBegin, buffer.length(), recursive, spec, jump, inhibs);
    }

    /*
     * (endTag.length()==0) => buffer.length() (endTag not found || posBegin ne
     * correspond pas a beginTag) => -1 (posBegin < 0 || posEnd < 0 || posEnd <=
     * posBegin || posEnd > buffer.length()) => -1
     */
    public Int2 blockIndexEndIn(final String buffer, String beginTag, String endTag, int posBegin, int posEnd, boolean recursive) {
        return blockIndexEndIn(buffer, beginTag, endTag, posBegin, posEnd, recursive, null, null);
    }

    public Int2 blockIndexEndIn(final String buffer, String beginTag, String endTag, int posBegin, int posEnd, boolean recursive, String spec, String[][] inhibs) {
        return blockIndexEndIn(buffer, beginTag, endTag, posBegin, posEnd, recursive, spec, null, inhibs);
    }

    public Int2 blockIndexEndIn(final String buffer, String beginTag, String endTag, int posBegin, int posEnd, boolean recursive, String spec, Jump jump, String[][] inhibs) {
        if (buffer == null || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return Int2.NOT_FOUND;
        }
        // Error cases
        if (beginTag == null) {
            return Int2.NOT_FOUND;
        }
        if (endTag == null) {
            return Int2.NOT_FOUND;
        }
        Int2 posBeginInt2 = indexIn(buffer, beginTag, posBegin, posEnd);
        if (beginTag.length() == 0) {
            recursive = false;
        } else if (posBeginInt2.b() != posBegin) {
            return Int2.NOT_FOUND;
        }
        if (endTag.length() == 0) {
            return new Int2(posEnd, posEnd);
        }
        // Block search
        int nbBeginTagOuvert = 1;
        int pos = posBeginInt2.e();
        int inhibs_size = 0;
        if (inhibs != null) {
            inhibs_size = inhibs.length;
        }
        Int2[] positions = new Int2[4 + inhibs_size];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = new Int2(-2, -2);
        }
        while (pos > -1 && pos < posEnd) {
            // Positions for end, begin, and inhibs
            if (positions[0].b() != -1 && spec != null && pos > positions[0].b()) {
                positions[0] = indexIn(buffer, spec, pos, posEnd); // spec
            }
            if (positions[1].b() != -1 && pos > positions[1].b()) {
                positions[1] = indexIn(buffer, endTag, pos, posEnd); // endTag
            }
            if (positions[2].b() != -1 && recursive && pos > positions[2].b()) {
                positions[2] = indexIn(buffer, beginTag, pos, posEnd); // beginTag
            }
            if (positions[3].b() != -1 && jump != null && pos > positions[3].b()) {
                positions[3] = jump.begin(buffer, pos, posEnd); // jump
            }
            for (int i = 4; i < positions.length; i++) { // inhibsTag
                if (positions[i].b() != -1 && pos > positions[i].b()) {
                    positions[i] = indexIn(buffer, inhibs[i - 4][0], pos, posEnd);
                }
            }
            // Get next position
            int positionMin = posEnd;
            int iPositionMin = -1;
            for (int i = 0; i < positions.length; i++) {
                if ((positions[i].b() > -1) && (positions[i].b() < positionMin)) {
                    iPositionMin = i;
                    positionMin = positions[i].b();
                }
            }
            if (iPositionMin == -1 /* NOT FOUND */) {
                return Int2.NOT_FOUND;
            }
            // Get the next element
            if (iPositionMin == 0 /* spec */) {
                pos = positions[iPositionMin].e();
            } else if (iPositionMin == 1 /* endTag */) {
                nbBeginTagOuvert--;
                pos = positions[iPositionMin].e();
                if (!recursive) {
                    return positions[iPositionMin];
                }
            } else if (iPositionMin == 2 /* beginTag */) {
                nbBeginTagOuvert++;
                pos = positions[iPositionMin].e();
            } else if (iPositionMin == 3 /* jump */) {
                pos = jump.end(buffer, positions[iPositionMin], posEnd, spec, inhibs);
            } else if (iPositionMin >= 4 /* inhibsTag */) {
                boolean forceNotRecursive;
                if (inhibs[iPositionMin - 4].length >= 3 && inhibs[iPositionMin - 4][2] != null) {
                    forceNotRecursive = inhibs[iPositionMin - 4][2].indexOf(TextSearch.FORCE_NOT_RECURSIVE) > -1;
                } else {
                    forceNotRecursive = false;
                }
                pos = blockIndexEndIn(buffer, inhibs[iPositionMin - 4][0], inhibs[iPositionMin - 4][1], positions[iPositionMin].b(), posEnd, (recursiveInhibs && !forceNotRecursive) ? true : false,
                        spec, jump, (recursiveInhibs && !forceNotRecursive) ? inhibs : null).e();
            }
            if (nbBeginTagOuvert == 0) {
                return positions[iPositionMin];
            }
        };
        return Int2.NOT_FOUND;
    }

    /*
     * (pos < 0 || (buffer.length()-pos) <= 0) => -1
     */
    public Int2 lastIndexOf(final String buffer, final String tag) {
        return lastIndexOf(buffer, tag, 0);
    }

    public Int2 lastIndexOf(final String buffer, final String tag, int pos) {
        return lastIndexOf(buffer, tag, pos, null, null);
    }

    public Int2 lastIndexOf(final String buffer, final String tag, String spec, String[][] inhibs) {
        return lastIndexOf(buffer, tag, 0, spec, inhibs);
    }

    public Int2 lastIndexOf(final String buffer, final String tag, int pos, String spec, String[][] inhibs) {
        return lastIndexIn(buffer, tag, pos, buffer.length(), spec, inhibs);
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || posEnd >
     * buffer.length()) => -1
     */
    public Int2 lastIndexIn(final String buffer, final String tag, int posBegin, int posEnd) {
        return lastIndexIn(buffer, tag, posBegin, posEnd, null, null);
    }

    public Int2 lastIndexIn(final String buffer, final String tag, int posBegin, int posEnd, String spec, String[][] inhibs) {
        if (buffer == null || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return Int2.NOT_FOUND;
        }
        Int2 i = indexIn(buffer, tag, posBegin, posEnd, spec, inhibs);
        Int2 last_i = i;
        while (i.b() > -1) {
            i = indexIn(buffer, tag, i.e(), posEnd, spec, inhibs);
            if (i.b() > -1) {
                last_i = i;
            }
        }
        return last_i;
    }

    /*
     * (pos < 0) => {}
     */
    public Int2[] allIndexOf(final String buffer, final String tag) {
        return allIndexOf(buffer, tag, 0);
    }

    public Int2[] allIndexOf(final String buffer, final String tag, int pos) {
        return allIndexOf(buffer, tag, pos, null, null);
    }

    public Int2[] allIndexOf(final String buffer, final String tag, String spec, String[][] inhibs) {
        return allIndexOf(buffer, tag, 0, spec, inhibs);
    }

    public Int2[] allIndexOf(final String buffer, final String tag, int pos, String spec, String[][] inhibs) {
        return allIndexIn(buffer, tag, pos, buffer.length(), spec, inhibs);
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || (posEnd-posBegin) <=
     * 0 || posEnd > buffer.length()) => {}
     */
    public Int2[] allIndexIn(final String buffer, final String tag, int posBegin, int posEnd) {
        return allIndexIn(buffer, tag, posBegin, posEnd, null, null);
    }

    public Int2[] allIndexIn(final String buffer, final String tag, int posBegin, int posEnd, String spec, String[][] inhibs) {
        if (buffer == null || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return new Int2[] {};
        }
        List lst = new ArrayList();
        Int2 i = indexIn(buffer, tag, posBegin, posEnd, spec, inhibs);
        while (i.b() > -1) {
            lst.add(i);
            i = indexIn(buffer, tag, i.e(), posEnd, spec, inhibs);
        }
        return (Int2[]) lst.toArray(new Int2[lst.size()]);
    }

    public String[] splitOf(final String buffer, String[] separators, boolean keepSeparators) {
        return splitOf(buffer, separators, keepSeparators, null, null);
    }

    public String[] splitOf(final String buffer, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        return splitOf(buffer, 0, separators, keepSeparators, spec, inhibs);
    }

    /*
     * (posBegin < 0 || (buffer.length()-posBegin) <= 0) => {}
     */
    public String[] splitOf(final String buffer, int posBegin, String[] separators, boolean keepSeparators) {
        return splitOf(buffer, posBegin, separators, keepSeparators, null, null);
    }

    public String[] splitOf(final String buffer, int posBegin, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        return splitIn(buffer, posBegin, buffer.length(), separators, keepSeparators, spec, inhibs);
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || posEnd >
     * buffer.length()) => {}
     */
    public String[] splitIn(final String buffer, int posBegin, int posEnd, String[] separators, boolean keepSeparators) {
        return splitIn(buffer, posBegin, posEnd, separators, keepSeparators, null, null);
    }

    public String[] splitIn(final String buffer, int posBegin, int posEnd, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        List list = split(buffer, posBegin, posEnd, separators, keepSeparators, spec, inhibs);
        return (String[]) list.toArray(new String[list.size()]);
    }

    private List split(final String buffer, int posBegin, int posEnd, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        List result = new LinkedList();
        if (buffer == null || buffer.length() == 0 || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return result;
        }
        if (separators == null) {
            return result;
        }
        for (String separator : separators) {
            if (separator != null && separator.length() > 0) {
                Int2 index = indexIn(buffer, separator, posBegin, posEnd, spec, inhibs);
                if (keepSeparators) {
                    if (index.b() == posBegin) {
                        return concat(buffer.substring(index.b(), index.e()), split(buffer, index.e(), posEnd, separators, keepSeparators, spec, inhibs));
                    } else if (index.e() == buffer.length()) {
                        return concat(split(buffer, posBegin, index.b(), separators, keepSeparators, spec, inhibs), buffer.substring(index.b(), index.e()));
                    } else if (index.b() > -1) {
                        return concat(split(buffer, posBegin, index.b(), separators, keepSeparators, spec, inhibs), buffer.substring(index.b(), index.e()),
                                split(buffer, index.e(), posEnd, separators, keepSeparators, spec, inhibs));
                    }
                } else {
                    if (index.b() == 0) {
                        return split(buffer, index.e(), posEnd, separators, keepSeparators, spec, inhibs);
                    } else if (index.e() == buffer.length()) {
                        return split(buffer, posBegin, index.b(), separators, keepSeparators, spec, inhibs);
                    } else if (index.b() > -1) {
                        return concat(split(buffer, posBegin, index.b(), separators, keepSeparators, spec, inhibs), split(buffer, index.e(), posEnd, separators, keepSeparators, spec, inhibs));
                    }
                }
            }
        }
        result.add(buffer.substring(posBegin, posEnd));
        return result;
    }

    public Int2[] splitPositionsOf(final String buffer, String[] separators, boolean keepSeparators) {
        return splitPositionsOf(buffer, separators, keepSeparators, null, null);
    }

    public Int2[] splitPositionsOf(final String buffer, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        return splitPositionsOf(buffer, 0, separators, keepSeparators, spec, inhibs);
    }

    /*
     * (posBegin < 0 || (buffer.length()-posBegin) <= 0) => {}
     */
    public Int2[] splitPositionsOf(final String buffer, int posBegin, String[] separators, boolean keepSeparators) {
        return splitPositionsOf(buffer, posBegin, separators, keepSeparators, null, null);
    }

    public Int2[] splitPositionsOf(final String buffer, int posBegin, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        return splitPositionsIn(buffer, posBegin, buffer.length(), separators, keepSeparators, spec, inhibs);
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || posEnd >
     * buffer.length()) => {}
     */
    public Int2[] splitPositionsIn(final String buffer, int posBegin, int posEnd, String[] separators, boolean keepSeparators) {
        return splitPositionsIn(buffer, posBegin, posEnd, separators, keepSeparators, null, null);
    }

    public Int2[] splitPositionsIn(final String buffer, int posBegin, int posEnd, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        List list = splitPositions(buffer, posBegin, posEnd, separators, keepSeparators, spec, inhibs);
        return (Int2[]) list.toArray(new Int2[list.size()]);
    }

    private List splitPositions(final String buffer, int posBegin, int posEnd, String[] separators, boolean keepSeparators, String spec, String[][] inhibs) {
        List result = new LinkedList();
        if (buffer == null || buffer.length() == 0 || posBegin < 0 || posEnd <= 0 || posEnd <= posBegin || posEnd > buffer.length()) {
            return result;
        }
        if (separators == null) {
            return result;
        }
        Int2[] positions = new Int2[separators.length];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = new Int2(-2, -2);
        }
        while (posBegin > -1 && posBegin < posEnd) {
            for (int i = 0; i < positions.length; i++) {
                if (positions[i].b() != -1 && posBegin > positions[i].b()) {
                    if (separators[i] != null && separators[i].length() > 0) {
                        positions[i] = indexIn(buffer, separators[i], posBegin, posEnd, spec, inhibs);
                    } else {
                        positions[i] = Int2.NOT_FOUND;
                    }
                }
            }
            // Get next position
            int positionMin = posEnd;
            int iPositionMin = -1;
            for (int i = 0; i < positions.length; i++) {
                if ((positions[i].b() > -1) && (positions[i].b() < positionMin)) {
                    iPositionMin = i;
                    positionMin = positions[i].b();
                }
            }
            if (iPositionMin == -1 /* NOT FOUND */) {
                if (posEnd > posBegin) {
                    result.add(new Int2(posBegin, posEnd));
                }
                break;
            } else {
                if (positions[iPositionMin].b() > posBegin) {
                    result.add(new Int2(posBegin, positions[iPositionMin].b()));
                }
                if (keepSeparators) {
                    result.add(positions[iPositionMin]);
                }
                posBegin = positions[iPositionMin].e();
            }
        }
        return result;
    }

    private List concat(List t1, Object o, List t2) {
        t1.add(o);
        t1.addAll(t2);
        return t1;
    }

    private List concat(List t1, List t2) {
        t1.addAll(t2);
        return t1;
    }

    private List concat(List t1, Object o) {
        t1.add(o);
        return t1;
    }

    private List concat(Object o, List t2) {
        t2.add(0, o);
        return t2;
    }

    /*
     * Sample : <li> buffer =1:aaa.bbb </li> <li> sRegex = \[0-9]:ID.ID </li>
     * <li> arg = ID </li> <li> argRegex = [a-zA-Z]+ </li> <p> => {aaa,bbb}
     */
    public String[] extractValues(final String buffer, String sRegex, String arg, String argRegex) {
        if (buffer == null || sRegex == null || arg == null || argRegex == null) {
            return new String[] {};
        }
        List res = new ArrayList();
        String[] regex = splitOf(sRegex, new String[] { arg }, true/* keepSeparators */, null, null);
        boolean posOK = true;
        int iPosOK = 0;
        for (int i = 0; posOK && i < regex.length; i++) {
            String ex = regex[i];
            boolean isArg = ex.equals(arg);
            if (isArg) {
                ex = argRegex;
            }
            Int2 pos = indexOf(buffer, ex, iPosOK);
            if (isArg) {
                res.add(buffer.substring(pos.b(), pos.e()));
            }
            posOK = (pos.b() == iPosOK);
            iPosOK = pos.e();
        }
        posOK = (iPosOK == buffer.length());
        if (posOK) {
            return (String[]) res.toArray(new String[res.size()]);
        } else {
            return new String[] {};
        }
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || posEnd >
     * buffer.length()) => -1
     */
    public Int2 trim(final String buffer, int posBegin, int posEnd) {
        if (buffer == null) {
            return Int2.NOT_FOUND;
        }
        if (posBegin < 0 || posBegin >= buffer.length()) {
            return Int2.NOT_FOUND;
        }
        if (posEnd < 0 || posEnd > buffer.length()) {
            return Int2.NOT_FOUND;
        }
        if (posBegin > posEnd) {
            return Int2.NOT_FOUND;
        }
        while (posBegin < posEnd) {
            char c = buffer.charAt(posBegin);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                posBegin++;
            } else {
                break;
            }
        }
        while (posEnd > posBegin) {
            char c = buffer.charAt(posEnd - 1);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                posEnd--;
            } else {
                break;
            }
        }
        if (posBegin < posEnd) {
            return new Int2(posBegin, posEnd);
        } else {
            return Int2.NOT_FOUND;
        }
    }

    /*
     * (pos < 0) => 0
     */
    public int countOf(final String buffer, final String tag) {
        return countOf(buffer, tag, 0);
    }

    public int countOf(final String buffer, final String tag, int pos) {
        return countOf(buffer, tag, pos, null, null);
    }

    public int countOf(final String buffer, final String tag, String spec, String[][] inhibs) {
        return countOf(buffer, tag, 0, spec, inhibs);
    }

    public int countOf(final String buffer, final String tag, int pos, String spec, String[][] inhibs) {
        return countIn(buffer, tag, pos, buffer.length(), spec, inhibs);
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || (posEnd-posBegin) <=
     * 0 || posEnd > buffer.length()) => 0
     */
    public int countIn(final String buffer, final String tag, int posBegin, int posEnd) {
        return countIn(buffer, tag, posBegin, posEnd, null, null);
    }

    public int countIn(final String buffer, final String tag, int posBegin, int posEnd, String spec, String[][] inhibs) {
        return allIndexIn(buffer, tag, posBegin, posEnd, spec, inhibs).length;
    }

    /*
     * (buffer == null || index < 0 || index > buffer.length()) => 0
     */
    public int lineNumber(File file, int index) {
        Object[] object = (Object[]) lineNumberFileContentMap.get(file);
        if (object == null) {
            object = new Object[2];
            object[0] = new HashMap();
            ((Map) object[0]).put(new Integer(0), new Integer(1));
            String buffer = Resources.getFileContent(file).toString();
            object[1] = buffer;
            Int2[] endLines = allIndexOf(buffer, "\n"); //$NON-NLS-1$
            for (int i = 0; i < endLines.length; i++) {
                ((Map) object[0]).put(new Integer(endLines[i].e()), new Integer(i + 2));
            }
            lineNumberFileContentMap.put(file, object);
        }
        Map firstCharToLine = (Map) object[0];
        String buffer = (String) object[1];
        int firstChar = index;
        while (firstChar > 0 && buffer.charAt(firstChar - 1) != '\n') {
            firstChar--;
        }
        Integer lineNumber = (Integer) firstCharToLine.get(new Integer(firstChar));
        if (lineNumber != null) {
            return lineNumber.intValue();
        } else {
            AcceleoToolsPlugin.getDefault().log(AcceleoToolsMessages.getString("TextSearch.CharacterLineNotFound", new Object[] { Integer.toString(index), file.getAbsolutePath(), }), true); //$NON-NLS-1$
            return 0;
        }
    }

    private FileContentMap lineNumberFileContentMap = new FileContentMap(5, true);

    /*
     * (buffer == null || index < 0 || index > buffer.length()) => 0
     */
    public int lineNumber(final String buffer, int index) {
        if (buffer != null && index >= 0 && index < buffer.length()) {
            if (refBuffer.equals(buffer) && index >= refIndex) {
                for (int i = refIndex; i < index; i++) {
                    if (buffer.charAt(i) == '\n') {
                        refLineNumber++;
                    }
                }
            } else {
                refLineNumber = 1;
                for (int i = 0; i < index; i++) {
                    if (buffer.charAt(i) == '\n') {
                        refLineNumber++;
                    }
                }
            }
            refBuffer = buffer;
            refIndex = index;
            return refLineNumber;
        } else {
            return 0;
        }
    }

    private String refBuffer = ""; //$NON-NLS-1$

    private int refIndex = 0;

    private int refLineNumber = 1;

    /*
     * (buffer == null || index <= 0 || index > buffer.length()) => 0
     */
    public int columnNumber(final String buffer, int index) {
        if (buffer != null && index > 0 && index < buffer.length()) {
            int column = 0;
            for (int i = index - 1; i >= 0; i--) {
                if (buffer.charAt(i) != '\n') {
                    column++;
                } else {
                    break;
                }
            }
            return column;
        } else {
            return 0;
        }
    }

    /*
     * (posBegin < 0 || posEnd < 0 || posEnd <= posBegin || posEnd >
     * buffer.length()) => buffer
     */
    public String replaceAllIn(final String buffer, String string, String replacementString, int posBegin, int posEnd, String[][] inhibs) {
        if (buffer == null) {
            return null;
        }
        StringBuffer result = new StringBuffer(buffer);
        Int2[] positions = allIndexIn(buffer, string, posBegin, posEnd, null, inhibs);
        if (positions.length > 0) {
            for (int i = positions.length - 1; i >= 0; i--) {
                Int2 pos = positions[i];
                result.replace(pos.b(), pos.e(), replacementString);
            }
        }
        return result.toString();
    }

}
