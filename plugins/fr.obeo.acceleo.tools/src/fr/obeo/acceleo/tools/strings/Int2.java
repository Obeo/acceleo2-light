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

/**
 * Complex index in a string.
 * <p>
 * Attributes are public to simplify their uses.
 * 
 * @author www.obeo.fr
 * 
 */
public class Int2 {

    /**
     * Index not found.
     */
    public static final Int2 NOT_FOUND = new Int2(-1, -1);

    /**
     * The begin index of a substring.
     */
    private int b;

    /**
     * The end index of a substring.
     */
    private int e;

    /**
     * Constructor.
     * 
     * @param b
     *            is the begin index of a substring
     * @param e
     *            is the end index of a substring
     */
    public Int2(int b, int e) {
        this.b = b;
        this.e = e;
    }

    /**
     * @return the begin index.
     */
    public int b() {
        return b;
    }

    /**
     * @return the end index.
     */
    public int e() {
        return e;
    }

    /* (non-Javadoc) */
    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof Int2) {
            Int2 other = (Int2) arg0;
            return (b == other.b) && (e == other.e);
        } else {
            return false;
        }
    }

    /* (non-Javadoc) */
    @Override
    public int hashCode() {
        return b;
    }

    /**
     * Shift bounds.
     * 
     * @param i
     *            is the value to add
     */
    public void range(Int2 range) {
        if (range.b > -1 && range.e > -1 && b > -1 && e > -1) {
            if (e > range.e) {
                e = range.e;
            }
            if (b < range.b) {
                b = range.b;
            }
            if (b < e) {
                b -= range.b;
                e -= range.b;
            } else {
                b = -1;
                e = -1;
            }
        } else {
            b = -1;
            e = -1;
        }
    }

    /**
     * Applies the indent strategy (each line adds one character).
     * 
     * @param lines
     *            are the positions of the lines
     */
    public void indent(Int2[] lines) {
        if (b > -1 && e > -1) {
            int newB = b;
            int newE = e;
            for (Int2 line : lines) {
                if (line.b() < b) {
                    newB++;
                }
                if (line.b() < e) {
                    newE++;
                } else {
                    break;
                }
            }
            b = newB;
            e = newE;
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return '[' + Integer.toString(b) + ',' + Integer.toString(e) + ']';
    }

}
