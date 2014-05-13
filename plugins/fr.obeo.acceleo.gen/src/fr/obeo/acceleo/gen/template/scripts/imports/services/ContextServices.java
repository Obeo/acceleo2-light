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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeList;

/**
 * System services for ENode elements.
 * 
 * @author www.obeo.fr
 * 
 */
public class ContextServices {

    /**
     * Constructor.
     */
    public ContextServices() {
    }

    /**
     * Puts the current node in the context for the given key, and return an
     * empty string.
     * 
     * @param node
     *            is the current node
     * @param key
     *            is the key in the context
     * @deprecated
     */
    @Deprecated
    public void nPut(ENode node, String key) {
        put(node, key);
    }

    /**
     * Puts the current node in the context for the given key, and return an
     * empty string.
     * 
     * @param node
     *            is the current node
     * @param key
     *            is the key in the context
     */
    public void put(ENode node, String key) {
        context.put(key, node);
    }

    /**
     * Gets the node for the given key in the context.
     * 
     * @param node
     *            is the current node
     * @param key
     *            is the key in the context
     * @return the node of the context
     * @deprecated
     */
    @Deprecated
    public ENode nGet(ENode node, ENode key) throws ENodeCastException, FactoryException {
        if (key.isInt()) {
            if (node.isList()) {
                ENodeList list = node.getList();
                ENode result = list.get(key.getInt());
                if (result != null) {
                    return result;
                } else {
                    return new ENode(ENode.EMPTY, node);
                }
            } else if (key.getInt() == 0) {
                return node;
            } else {
                return new ENode(ENode.EMPTY, node);
            }
        } else if (key.isString()) {
            // deprecated nGet(ENode node, String key)
            return get(node, key.getString());
        } else {
            // deprecated nGet(ENode node, String key)
            return get(node, (String) key.getAdapterValue(String.class));
        }
    }

    /**
     * Gets the node for the given key in the context.
     * 
     * @param node
     *            is the current node
     * @param key
     *            is the key in the context
     * @return the node of the context
     */
    public ENode get(ENode node, String key) {
        ENode result = (ENode) context.get(key);
        if (result != null) {
            return result.copy();
        } else {
            return new ENode(ENode.EMPTY, node);
        }
    }

    private Map context = new HashMap();

    /**
     * Pushes the current node into the context, and returns an empty string.
     * 
     * @param node
     *            is the current node
     * @deprecated
     */
    @Deprecated
    public void nPush(ENode node) {
        push(node);
    }

    /**
     * Pushes the current node into the context, and returns an empty string.
     * 
     * @param node
     *            is the current node
     */
    public void push(ENode node) {
        stack.push(node);
    }

    /**
     * Pops the context, and returns an empty string.
     * 
     * @param node
     *            is the current node
     * @deprecated
     */
    @Deprecated
    public void nPop(ENode node) {
        pop(node);
    }

    /**
     * Pops the context, and returns an empty string.
     * 
     * @param node
     *            is the current node
     */
    public void pop(ENode node) {
        stack.pop();
    }

    /**
     * Return the last node pushed into the context, and returns an empty
     * string.
     * 
     * @param node
     *            is the current node
     * @return an empty string
     * @deprecated
     */
    @Deprecated
    public ENode nPeek(ENode node) {
        return peek(node);
    }

    /**
     * Return the last node pushed into the context, and returns an empty
     * string.
     * 
     * @param node
     *            is the current node
     * @return an empty string
     */
    public ENode peek(ENode node) {
        if (!stack.isEmpty()) {
            return ((ENode) stack.peek()).copy();
        } else {
            return new ENode("", node); //$NON-NLS-1$
        }
    }

    private Stack stack = new Stack();

}
