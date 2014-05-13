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

package fr.obeo.acceleo.gen.template.eval;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over an ENodeList
 * 
 * @author www.obeo.fr
 * 
 */
public class ENodeIterator {

    /**
     * The iterator.
     */
    protected Iterator iterator;

    /**
     * Creates an iterator over the elements in the given list.
     * 
     * @param list
     *            is the list
     */
    protected ENodeIterator(ENodeList list) {
        iterator = list.list.iterator();
    }

    /**
     * Returns true if the iteration has more elements.
     * 
     * @return true if the iterator has more elements
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Returns the next ENode in the iteration.
     * 
     * @return the next ENode in the iteration
     * @throws NoSuchElementException
     *             - iteration has no more elements
     */
    public ENode next() {
        return (ENode) iterator.next();
    }

}
