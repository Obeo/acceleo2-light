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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * List of ENode.
 * 
 * @author www.obeo.fr
 * 
 */
public class ENodeList {

    /**
     * List of elements.
     */
    protected List list = new ArrayList();

    /**
     * Indicates that each element appears only one time in the list.
     */
    protected boolean unique;

    /**
     * Constructor.
     */
    public ENodeList() {
        this(false);
    }

    /**
     * Constructor.
     * 
     * @param unique
     *            indicates that each element appears only one time in the list
     */
    public ENodeList(boolean unique) {
        super();
        this.unique = unique;
    }

    /**
     * Appends the specified ENode to the end of this list.
     * 
     * @param node
     *            is the element to be appended to this list
     */
    public void add(ENode node) {
        if (node != null) {
            if (node.isList()) {
                try {
                    addAll(node.getList());
                } catch (ENodeCastException e) {
                    // Never catch
                }
            } else if (!node.isNull()) {
                if (unique) {
                    if (!list.contains(node)) {
                        list.add(node);
                    }
                } else {
                    list.add(node);
                }
            }
        }
    }

    /**
     * Inserts the specified ENode at the given index of this list.
     * 
     * @param index
     *            is the index
     * @param node
     *            is the element to insert
     */
    public void add(int index, ENode node) {
        if (node != null) {
            if (node.isList()) {
                try {
                    addAll(index, node.getList());
                } catch (ENodeCastException e) {
                    // Never catch
                }
            } else if (!node.isNull()) {
                if (index < 0 || index >= list.size()) {
                    if (unique) {
                        if (!list.contains(node)) {
                            list.add(node);
                        }
                    } else {
                        list.add(node);
                    }
                } else {
                    if (unique) {
                        if (!list.contains(node)) {
                            list.add(index, node);
                        }
                    } else {
                        list.add(index, node);
                    }
                }
            }
        }
    }

    /**
     * Returns the ENode at the given index, or null if the index is out of
     * bounds.
     * 
     * @param index
     *            is the index
     * @return the ENode at the given index, or null if the index is out of
     *         bounds
     */
    public ENode get(int index) {
        if (index >= 0 && index < list.size()) {
            return (ENode) list.get(index);
        } else {
            return null;
        }
    }

    /**
     * Removes the first occurrence in this list of the specified ENode.
     * 
     * @param node
     *            is the element to be removed from this list, if present
     */
    public void remove(ENode node) {
        list.remove(node);
    }

    /**
     * Appends all of the elements in the specified ENodeList to the end of this
     * list.
     * 
     * @param other
     *            is ENodeList whose elements are to be added to this list
     */
    public void addAll(ENodeList other) {
        ENodeIterator it = other.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    private void addAll(int index, ENodeList other) {
        ENodeIterator it = other.iterator();
        while (it.hasNext()) {
            add(index, it.next());
        }
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     * 
     * @return an iterator over the elements in this list in proper sequence
     */
    public ENodeIterator iterator() {
        return new ENodeIterator(this);
    }

    /**
     * Gets an array representation of the list.
     * 
     * @return an array representation of the list
     */
    public Object[] toArray() {
        return list.toArray();
    }

    /**
     * Returns true if this list contains the specified element.
     * <p>
     * More formally, returns true if and only if this list contains at least
     * <p>
     * one element e such that (o==null ? e==null : o.equals(e)).
     * 
     * @param node
     *            is element whose presence in this list is to be tested
     * @return true if this list contains the specified element
     */
    public boolean contains(ENode node) {
        return list.contains(node);
    }

    /**
     * Returns the number of elements in this list.
     * 
     * @return the number of elements in this list
     */
    public int size() {
        return list.size();
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(""); //$NON-NLS-1$
        ENodeIterator it = iterator();
        while (it.hasNext()) {
            buffer.append(it.next().toString());
        }
        return buffer.toString();
    }

    /**
     * Sorts the list using "toString" result.
     */
    public void sort() {
        Set set = new TreeSet(new Comparator() {
            public int compare(Object arg0, Object arg1) {
                return ((ENode) arg0).compareTo((arg1));
            }
        });
        set.addAll(list);
        list = new ArrayList(set);
    }

    /**
     * Gets a list representation of the ENodeList.
     * 
     * @return a list representation of the ENodeList
     */
    public List asList() {
        List result = new ArrayList();
        ENodeIterator it = iterator();
        while (it.hasNext()) {
            result.add(it.next().getValue());
        }
        return result;
    }

    /* (non-Javadoc) */
    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof ENodeList) {
            return list.equals(((ENodeList) arg0).list);
        }
        return false;
    }

}
