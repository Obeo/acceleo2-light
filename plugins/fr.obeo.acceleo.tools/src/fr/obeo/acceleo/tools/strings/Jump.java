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
 * It's a scanner that ignores a sequence of characters.
 * 
 * @author www.obeo.fr
 * 
 */
public interface Jump {

    /**
     * Returns the index within the buffer of the first occurrence of this jump,
     * starting at the specified index.
     * 
     * @param buffer
     *            is the text to be explored
     * @param posBegin
     *            is the beginning index
     * @param posEnd
     *            is the ending index
     * @return the index within the buffer of the first occurrence
     */
    public Int2 begin(String buffer, int posBegin, int posEnd);

    /**
     * Returns the index within the buffer of the end of this jump.
     * 
     * @param buffer
     *            is the text to be explored
     * @param begin
     *            is the beginning index of this jump
     * @param posEnd
     *            is the ending index
     * @param spec
     *            is the pattern's escape character
     * @param inhibs
     *            are the ignored blocks
     * @return the last ignored index
     */
    public int end(String buffer, Int2 begin, int posEnd, String spec, String[][] inhibs);

}
