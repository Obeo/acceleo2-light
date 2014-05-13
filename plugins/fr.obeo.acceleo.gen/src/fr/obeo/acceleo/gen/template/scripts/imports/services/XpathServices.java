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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;

/**
 * Xpath axes.
 * 
 * @author www.obeo.fr
 * 
 */
public class XpathServices {

    /**
     * Same meaning as the "ancestor" axe in Xpath : selects all ancestors
     * (parent, grandparent, etc.) of the current node
     * 
     * @param current
     *            is the current node of generation
     * @return all ancestors (parent, grandparent, etc.) of the current node
     */
    public List ancestor(EObject object) {
        List result = new ArrayList();
        object = object.eContainer();
        while (object != null) {
            result.add(object);
            object = object.eContainer();
        }
        return result;
    }

    /**
     * Same meaning as the "parent" axe in Xpath : selects the parent of the
     * current node
     * 
     * @param current
     *            is the current node of generation
     * @return the parent of the current node
     */
    public EObject parent(EObject object) {
        return object.eContainer();
    }

    /**
     * Same meaning as the "self" axe in Xpath : selects the current node
     * 
     * @param current
     *            is the current node of generation
     * @return the current node
     */
    public EObject self(EObject object) {
        return object;
    }

    /**
     * Same meaning as the "child" axe in Xpath : selects all children of the
     * current node
     * 
     * @param current
     *            is the current node of generation
     * @return all children of the current node
     */
    public List child(EObject object) {
        return object.eContents();
    }

    /**
     * Same meaning as the "descendant" axe in Xpath: Gets all the descendants
     * (children, grandchildren, etc.) of the current node.
     * 
     * @param object
     *            is the current node
     * @return the descendants of the current node
     */
    public List descendant(EObject object) {
        List result = new ArrayList();
        TreeIterator it = object.eAllContents();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    /**
     * Same meaning as the "preceding-sibling" axe in Xpath: selects all
     * siblings before the current node.
     * 
     * @param object
     *            is the current node
     * @return all siblings before the current node
     */
    public List precedingSibling(EObject object) {
        List result = new ArrayList();
        EObject parent = object.eContainer();
        if (parent != null) {
            Iterator it = parent.eContents().iterator();
            while (it.hasNext()) {
                Object child = it.next();
                if (child == object) {
                    break;
                } else {
                    result.add(child);
                }
            }
        }
        return result;
    }

    /**
     * Same meaning as the "preceding" axe in Xpath: selects everything in the
     * model that is before the current node
     * 
     * @param object
     *            is the current node
     * @return everything in the model that is before the current node
     */
    public List preceding(EObject object) {
        List result = new ArrayList();
        List ancestors = ancestor(object);
        ancestors.add(object);
        // now return all preceding siblings and their respective children...
        Iterator it = ancestors.iterator();
        while (it.hasNext()) {
            EObject parent = (EObject) it.next();
            if (parent != object) {
                result.add(parent);
            }
            result.addAll(precedingSibling(parent));
            Iterator it2 = precedingSibling(parent).iterator();
            while (it2.hasNext()) {
                result.addAll(descendant((EObject) it2.next()));
            }
        }
        return result;
    }

    /**
     * Same meaning as the "following-sibling" axe in Xpath: selects all
     * siblings after the current node
     * 
     * @param object
     *            is the current node
     * @return all siblings after the current node
     */
    public List followingSibling(EObject object) {
        List result = new ArrayList();
        EObject parent = object.eContainer();
        Iterator it = parent.eContents().iterator();
        while (it.hasNext()) {
            Object child = it.next();
            if (child == object) {
                break;
            }
        }
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    /**
     * Same meaning as the "following" axe in Xpath: selects everything in the
     * model after the current node
     * 
     * @param object
     *            is the current node
     * @return everything after the current node
     */
    public List following(EObject object) {
        List result = new ArrayList();
        Iterator it = followingSibling(object).iterator();
        while (it.hasNext()) {
            EObject following = (EObject) it.next();
            result.add(following);
            result.addAll(descendant(following));
        }
        return result;
    }

}
