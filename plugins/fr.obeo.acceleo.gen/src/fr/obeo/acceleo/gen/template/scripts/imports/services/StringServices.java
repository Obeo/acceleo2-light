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

package fr.obeo.acceleo.gen.template.scripts.imports.services;

import java.util.Arrays;
import java.util.List;

import fr.obeo.acceleo.gen.template.eval.ENode;

/**
 * System services for String elements.
 * 
 * @author www.obeo.fr
 * 
 */
public class StringServices {

    /**
     * Returns the string's length
     * 
     * @param s
     *            is the string
     * @return the string's length
     */
    public int length(String s) {
        return s.length();
    }

    /**
     * Returns an upper case representation of a string.
     * 
     * @param node
     *            is the current node
     * @return the upper case representation
     */
    public ENode toUpperCase(ENode node) {
        node.stringCall("toUpperCase", -1, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Returns a lower case representation of a string.
     * 
     * @param node
     *            is the current node
     * @return the lower case representation
     */
    public ENode toLowerCase(ENode node) {
        node.stringCall("toLowerCase", -1, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Translates the first character of the given string to upper case.
     * 
     * @param node
     *            is the current node
     * @return the upper case representation
     */
    public ENode toU1Case(ENode node) {
        node.stringCall("toU1Case", -1, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Translates the first character of the given string to lower case.
     * 
     * @param node
     *            is the current node
     * @return the lower case representation
     */
    public ENode toL1Case(ENode node) {
        node.stringCall("toL1Case", -1, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Returns a substring of a string that begins at a specified index and ends
     * at end - 1.
     * 
     * @param node
     *            is the current node
     * @param begin
     *            is the beginning index, inclusive
     * @param end
     *            is the ending index, exclusive
     * @return the substring
     */
    public ENode substring(ENode node, int begin, int end) {
        node.stringCall("substring", begin, end); //$NON-NLS-1$
        return node;
    }

    /**
     * Gets a substring of a string that begins at a specified index and ends at
     * the end of the string.
     * 
     * @param node
     *            is the current node
     * @param begin
     *            is the beginning index, inclusive
     * @return the substring
     */
    public ENode substring(ENode node, int begin) {
        node.stringCall("substring", begin, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Replaces all occurences of substring by another substring in a string.
     * 
     * @param buffer
     *            is the buffer
     * @param s1
     *            is the substring to replace
     * @param s2
     *            is the new substring
     * @return the new string
     */
    public String replaceAll(String buffer, String s1, String s2) {
        return buffer.replaceAll(s1, s2);
    }

    /**
     * Replaces the first occurence of a substring by another substring in a
     * string.
     * 
     * @param buffer
     *            is the buffer
     * @param s1
     *            is the substring to replace
     * @param s2
     *            is the new substring
     * @return the new string
     */
    public String replaceFirst(String buffer, String s1, String s2) {
        return buffer.replaceFirst(s1, s2);
    }

    /**
     * Removes leading and trailing spaces of a string.
     * 
     * @param node
     *            is the current node
     * @return the string without leading and trailing spaces
     */
    public ENode trim(ENode node) {
        node.stringCall("trim", -1, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Indicates if a string starts with a given substring.
     * 
     * @param s
     *            is the string
     * @param arg
     *            is the substring
     * @return true if the string starts with the given substring
     */
    public boolean startsWith(String s, String arg) {
        return s.startsWith(arg);
    }

    /**
     * Indicates if a string ends with a given substring.
     * 
     * @param s
     *            is the string
     * @param arg
     *            is the substring
     * @return true if the string ends with the given substring
     */
    public boolean endsWith(String s, String arg) {
        return s.endsWith(arg);
    }

    /**
     * Indicates if two strings are equal, ignoring case.
     * 
     * @param s
     *            is the first string
     * @param arg
     *            is the second string
     * @return true if the two strings are equal
     */
    public boolean equalsIgnoreCase(String s, String arg) {
        return s.equalsIgnoreCase(arg);
    }

    /**
     * Indicates if a string matches a regex.
     * 
     * @param s
     *            is the string
     * @param regex
     *            is the regex
     * @return true if the string matches with the given regex
     */
    public boolean matches(String s, String regex) {
        return s.matches(regex);
    }

    /**
     * Gets the char at a given position.
     * 
     * @param s
     *            is the string
     * @param index
     *            is the position
     * @return the char at the given position
     */
    public String charAt(String s, int index) {
        return String.valueOf(s.charAt(index));
    }

    /**
     * Returns the position of a substring in a string.
     * 
     * @param buffer
     *            is the string
     * @param arg
     *            is the substring
     * @return the position of the substring
     */
    public int indexOf(String buffer, String arg) {
        return buffer.indexOf(arg);
    }

    /**
     * Returns the position of a substring a the string, starting at a specified
     * position.
     * 
     * @param buffer
     *            is the string
     * @param arg
     *            is the substring
     * @param index
     *            is the position where the research starts form
     * @return the position of the substring
     */
    public int indexOf(String buffer, String arg, int index) {
        return buffer.indexOf(arg, index);
    }

    /**
     * Returns the last position of a substring in a string.
     * 
     * @param buffer
     *            is the string
     * @param arg
     *            is the substring
     * @return the last position of the substring
     */
    public int lastIndexOf(String buffer, String arg) {
        return buffer.lastIndexOf(arg);
    }

    /**
     * Returns the last position of a substring in a string, starting at a
     * specified position.
     * 
     * @param buffer
     *            is the string
     * @param arg
     *            is the substring
     * @param index
     *            is the position where the research starts form
     * @return the last position of the substring
     */
    public int lastIndexOf(String buffer, String arg, int index) {
        return buffer.lastIndexOf(arg, index);
    }

    /**
     * Splits a string with a substring.
     * 
     * @param buffer
     *            is the string
     * @param arg
     *            is the substring
     * @return a list that contains the splitted string
     */
    public List split(String buffer, String arg) {
        String[] result = buffer.split(arg);
        if (result == null) {
            result = new String[0];
        }
        return Arrays.asList(result);
    }

    /**
     * Indents a string with ' '.
     * 
     * @param node
     *            is the current node
     * @return the indented string
     */
    public ENode indentSpace(ENode node) {
        node.stringCall("indentSpace", -1, -1); //$NON-NLS-1$
        return node;
    }

    /**
     * Indents a string with '\t'.
     * 
     * @param node
     *            is the current node
     * @return the indented string
     */
    public ENode indentTab(ENode node) {
        node.stringCall("indentTab", -1, -1); //$NON-NLS-1$
        return node;
    }

}
