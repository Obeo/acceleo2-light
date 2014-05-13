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

package fr.obeo.acceleo.tools.format;

/**
 * Conventions to format text.
 * 
 * @author www.obeo.fr
 * 
 */
public class Conventions {

    /**
     * Transforms a multiline text into a literal.
     * 
     * @param value
     *            is the multiline text
     * @return the literal (single line)
     */
    public static String formatString(String value) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
            case '\t':
                result.append("\\t"); //$NON-NLS-1$
                break;
            case '\n':
                result.append("\\n"); //$NON-NLS-1$
                break;
            case '\r':
                result.append("\\r"); //$NON-NLS-1$
                break;
            case '\"':
                result.append("\\\""); //$NON-NLS-1$
                break;
            case '\\':
                result.append("\\\\"); //$NON-NLS-1$
                break;
            default:
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Transforms a literal into a multiline text.
     * 
     * @param value
     *            is the literal (single line)
     * @return the multiline text
     */
    public static String unformatString(String value) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                i++;
                c = value.charAt(i);
                switch (c) {
                case 't':
                    result.append('\t');
                    break;
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case '"':
                    result.append('"');
                    break;
                case '\\':
                    result.append('\\');
                    break;
                default:
                    result.append('\\');
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

}
