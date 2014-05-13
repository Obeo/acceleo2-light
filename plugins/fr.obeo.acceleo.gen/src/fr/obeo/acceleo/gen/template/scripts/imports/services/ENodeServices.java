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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.EFactory;
import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.format.Conventions;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * System services for ENode elements.
 * 
 * @author www.obeo.fr
 * 
 */
public class ENodeServices {

    /**
     * The script.
     */
    protected IScript script;

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public ENodeServices(IScript script) {
        this.script = script;
    }

    /**
     * Gets all the nodes whose type is the given type.
     * <p>
     * An element is kept only if it's an EObject whose type is the given type
     * or if it isn't an EObject.
     * <p>
     * Sample :
     * <p>
     * <li>a is an instance of class A</li>
     * <li>b is an instance of class B</li>
     * <li>c is an instance of class C</li>
     * <li>B extends A</li>
     * <p>
     * If type equals "A" and node is a list {a, "\n", b, c}
     * <p>
     * the result is a list {a, "\n", b}.
     * 
     * @param node
     *            is the list
     * @param type
     *            is the type kept
     * @return a list that contains the nodes whose type is the given type.
     * @throws ENodeCastException
     *             if the given node isn't a list
     * @throws FactoryException
     */
    public ENode filter(ENode node, String type) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList res = new ENodeList();
            ENodeIterator it = node.getList().iterator();
            while (it.hasNext()) {
                ENode child = filter(it.next(), type);
                if (!child.isNull()) {
                    res.add(child);
                }
            }
            return new ENode(res, node);
        } else if (node.isEObject()) {
            if (EFactory.eInstanceOf(node.getEObject(), type)) {
                return node;
            } else {
                return new ENode(ENode.EMPTY, node);
            }
        } else {
            return node;
        }
    }

    /**
     * Gets all the nodes whose type is the given type.
     * <p>
     * An element is kept only if it's an EObject whose type is the given type
     * or if it isn't an EObject.
     * <p>
     * Sample :
     * <p>
     * <li>a is an instance of class A</li>
     * <li>b is an instance of class B</li>
     * <li>c is an instance of class C</li>
     * <li>B extends A</li>
     * <p>
     * If type equals "A" and node is a list {a, "\n", b, c}
     * <p>
     * the result is a list {a, "\n", b}.
     * 
     * @param node
     *            is the list
     * @param type
     *            is the type kept
     * @return a list that contains the nodes whose type is the given type.
     * @throws ENodeCastException
     *             if the given node isn't a list
     * @throws FactoryException
     * @deprecated
     */
    @Deprecated
    public ENode cast(ENode node, String type) throws ENodeCastException, FactoryException {
        return filter(node, type);
    }

    /**
     * Add a separator between each element of the list. It returns the given
     * node if it isn't a list.
     * <p>
     * Sample :
     * <p>
     * If separator equals "\t" and node is a list {a, b, c} the result is a
     * list {a, "\t", b, "\t", c}.
     * 
     * @param node
     *            is the list
     * @param separator
     *            is the separator
     * @return a list that contains a separator between each element.
     * @throws FactoryException
     */
    public ENode sep(ENode node, String separator) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList list = node.getList();
            ENodeList res = new ENodeList();
            ENodeIterator it = list.iterator();
            if (it.hasNext()) {
                ENode element = it.next();
                boolean needSeparator = element.size() > 0;
                res.add(element);
                while (it.hasNext()) {
                    element = it.next();
                    if (needSeparator && element.size() > 0) {
                        res.add(new ENode(separator, node));
                        needSeparator = true;
                    } else if (!needSeparator) {
                        needSeparator = element.size() > 0;
                    }
                    res.add(element);
                }
            }
            return new ENode(res, node);
        } else {
            return node;
        }
    }

    /**
     * Add a separator between each element of the list and serialize the list.
     * It returns node.toString() if it isn't a list.
     * <p>
     * Sample :
     * <p>
     * If separator equals "\t" and node is a list {a, b, c} the result is a
     * string : a + "\t" + b + "\t" + c.
     * 
     * @param node
     *            is the list
     * @param separator
     *            is the separator
     * @return a string that contains a separator between each element.
     * @throws FactoryException
     * @deprecated
     */
    @Deprecated
    public ENode sepStr(ENode node, String separator) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList list = node.getList();
            ENodeList res = new ENodeList();
            ENodeIterator it = list.iterator();
            if (it.hasNext()) {
                ENode element = it.next();
                boolean needSeparator = element.size() > 0;
                res.add(element);
                while (it.hasNext()) {
                    element = it.next();
                    if (needSeparator && element.size() > 0) {
                        res.add(new ENode(separator, node));
                        needSeparator = true;
                    } else if (!needSeparator) {
                        needSeparator = element.size() > 0;
                    }
                    res.add(element);
                }
            }
            ENode result = new ENode(res, node);
            result.asString();
            return result;
        } else {
            node.asString();
            return node;
        }
    }

    /**
     * Get recursively the feature value of an object. The recursivity is
     * stopped when an element of the given type is found.
     * <p>
     * The given node must be an EObject.
     * <p>
     * Sample :
     * <p>
     * <li>a is an instance of class A</li>
     * <li>b is an instance of class B</li>
     * <li>c is an instance of class C</li>
     * <li>a contains b</li>
     * <li>b contains c</li>
     * <li>'container' feature is defined on a, b, and c</li>
     * <li>c.container returns b</li>
     * <li>b.container returns a</li>
     * <li>a.container returns null</li>
     * <p>
     * until(c,"container","A") returns a
     * <p>
     * until(b,"container","A") returns a
     * <p>
     * until(a,"container","A") returns a
     * <p>
     * until(b,"container","C") returns null
     * <p>
     * 
     * @param node
     *            is an EObject
     * @param link
     *            is the feature name
     * @param type
     *            is the type which stops the recursivity
     * @return the found object or null
     * @throws ENodeCastException
     *             if the given node isn't an EObject
     * @throws FactoryException
     * @deprecated
     */
    @Deprecated
    public ENode until(ENode node, String link, String type) throws ENodeCastException, FactoryException {
        EObject object = node.getEObject();
        while (object != null) {
            if (EFactory.eInstanceOf(object, type)) {
                return new ENode(object, node);
            } else {
                object = EFactory.eGetAsEObject(object, link);
            }
        }
        return null;
    }

    /**
     * Returns the current node.
     * 
     * @param node
     *            is the current node
     * @return the current node
     */
    public ENode current(ENode node) {
        return node;
    }

    /**
     * Returns the current node.
     * 
     * @param node
     *            is the current node
     * @param level
     *            is the level in the parent hierarchy
     * @return the current node
     * @throws ENodeCastException
     */
    public ENode current(ENode node, ENode arg) throws ENodeCastException {
        if (arg.isString()) {
            String type = arg.getString();
            int i = 0;
            Object value = script.contextAt(IScript.TEMPLATE_NODE, i);
            while (value != null) {
                ENode current = ENode.createTry(value, node);
                if (current != null && current.isEObject()) {
                    if (EFactory.eInstanceOf(current.getEObject(), type)) {
                        return current;
                    }
                }
                i++;
                value = script.contextAt(IScript.TEMPLATE_NODE, i);
            }
        } else if (arg.isInt()) {
            Object value = script.contextAt(IScript.TEMPLATE_NODE, arg.getInt());
            return ENode.createTry(value, node);
        }
        return new ENode(ENode.EMPTY, node);
    }

    /**
     * Transforms the node into a string.
     * 
     * @param node
     *            is the current node
     * @return the string node
     */
    public ENode toString(ENode node) {
        node.asString();
        return node;
    }

    /**
     * Returns the adaptive value for the given type short name.
     * 
     * @param node
     *            is the node
     * @param type
     *            is the type name : "EObject", "ENodeList", "String",
     *            "boolean", "int", "double", "List", "ENode"
     * @return the adaptive value
     * @throws ENodeCastException
     */
    public ENode adapt(ENode node, String type) throws ENodeCastException {
        Class c;
        if ("EObject".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = EObject.class;
        } else if ("ENodeList".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = ENodeList.class;
        } else if ("String".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = String.class;
        } else if ("boolean".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = boolean.class;
        } else if ("int".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = int.class;
        } else if ("double".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = double.class;
        } else if ("List".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = List.class;
        } else if ("ENode".equalsIgnoreCase(type)) { //$NON-NLS-1$
            c = ENode.class;
        } else {
            c = null;
        }
        ENode result = ENode.createTry(node.getAdapterValue(c), node);
        if (result != null) {
            return result;
        } else {
            return new ENode(ENode.EMPTY, node);
        }
    }

    /**
     * It prints current node.
     * 
     * @param node
     *            is the current node
     * @return the current node
     */
    public ENode debug(ENode node) {
        return trace(node);
    }

    /**
     * It prints current node.
     * 
     * @param node
     *            is the current node
     * @return the current node
     */
    public ENode trace(ENode node) {
        return trace(node, ""); //$NON-NLS-1$
    }

    /**
     * It prints current node.
     * 
     * @param node
     *            is the current node
     * @param template
     *            is the template to evaluate for each node
     * @return the current node
     */
    public ENode trace(ENode node, String template) {
        trace++;
        String prefix;
        if (template != null && template.length() > 0) {
            try {
                prefix = Conventions.formatString(template) + '=' + Conventions.formatString(evaluate(node, template).toString());
            } catch (TemplateSyntaxException e) {
                prefix = ""; //$NON-NLS-1$
            } catch (FactoryException e) {
                prefix = ""; //$NON-NLS-1$
            }
        } else {
            prefix = ""; //$NON-NLS-1$
        }
        String marker = ('[' + trace + "]          ").substring(0, 10); //$NON-NLS-1$
        System.out.println(marker + prefix + ' ' + AcceleoGenMessages.getString("ENodeServices.TraceMessage", new Object[] { node.getType(), Conventions.formatString(node.toString()), })); //$NON-NLS-1$
        return node;
    }

    private int trace = 0;

    private ENode evaluate(ENode current, String call) throws TemplateSyntaxException, FactoryException {
        int pos = getBegin();
        call = call.replaceAll("'", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
        TemplateExpression expression = TemplateExpression.fromString(computeCall(pos, call), new Int2(pos, pos + call.length()), script);
        try {
            return expression.evaluate(current, script, LaunchManager.create("run", true)); //$NON-NLS-1$
        } catch (ENodeException e) {
            return new ENode(ENode.EMPTY, current);
        }
    }

    private int getBegin() {
        Int2 pos = (Int2) script.contextPeek(IScript.ARGUMENT_POSITION);
        if (pos != null) {
            return pos.b();
        } else {
            return 0;
        }
    }

    private String computeCall(int begin, String call) {
        if (begin > 0) {
            StringBuffer result = new StringBuffer(""); //$NON-NLS-1$
            for (int i = 0; i < begin; i++) {
                result.append(' ');
            }
            result.append(call);
            return result.toString();
        } else {
            return call;
        }
    }

    /***************************************************************************
     * 
     * List Services
     * 
     **************************************************************************/

    /**
     * Returns the size of the node.
     * 
     * @param node
     *            is the current node <li>isEObject() : return 1</li> <li>
     *            isList() : return getList().size()</li> <li>isString() :
     *            return getString().length()</li> <li>isBoolean() : return 1</li>
     *            <li>isInt() : return 1</li> <li>isNull() : return 0</li>
     * @return value size
     */
    public int nSize(ENode node) {
        return node.size();
    }

    /**
     * Returns the child node at the given index in the current node.
     * 
     * @param node
     *            is the current node
     * @param index
     *            is the index of the child
     * @return the child node
     * @throws ENodeCastException
     * @throws FactoryException
     */
    public ENode nGet(ENode node, int index) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList list = node.getList();
            ENode result = list.get(index);
            if (result != null) {
                return result;
            } else {
                return new ENode(ENode.EMPTY, node);
            }
        } else if (index == 0) {
            return node;
        } else {
            return new ENode(ENode.EMPTY, node);
        }
    }

    /**
     * Returns the children in the given range.
     * 
     * @param node
     *            is the current node
     * @param begin
     *            is the beginning index
     * @param begin
     *            is the ending index
     * @return the children in the given range
     * @throws ENodeCastException
     * @throws FactoryException
     */
    public ENode nGet(ENode node, int begin, int end) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList list = node.getList();
            ENodeList result = new ENodeList();
            for (int i = begin; i < list.size() && (i < end || end == -1); i++) {
                ENode child = list.get(i);
                result.add(child);
            }
            return new ENode(result, node);
        } else if (begin == 0 && (end > 0 || end == -1)) {
            return node;
        } else {
            return new ENode(ENode.EMPTY, node);
        }
    }

    /**
     * Returns the first child of the current node.
     * 
     * @param node
     *            is the current node
     * @return the first child node
     * @throws ENodeCastException
     * @throws FactoryException
     */
    public ENode nFirst(ENode node) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList list = node.getList();
            ENode result = list.get(0);
            if (result != null) {
                return result;
            } else {
                return new ENode(ENode.EMPTY, node);
            }
        } else {
            return node;
        }
    }

    /**
     * Returns the last child of the current node.
     * 
     * @param node
     *            is the current node
     * @return the last child node
     * @throws ENodeCastException
     * @throws FactoryException
     */
    public ENode nLast(ENode node) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList list = node.getList();
            ENode result = list.get(list.size() - 1);
            if (result != null) {
                return result;
            } else {
                return new ENode(ENode.EMPTY, node);
            }
        } else {
            return node;
        }
    }

    /**
     * Removes all duplicated nodes.
     * 
     * @param node
     *            is the current node
     * @return the minimized node
     * @throws ENodeCastException
     * @deprecated
     */
    @Deprecated
    public ENode minimize(ENode node) throws ENodeCastException {
        return nMinimize(node);
    }

    /**
     * Removes all duplicated nodes.
     * 
     * @param node
     *            is the current node
     * @return the minimized node
     * @throws ENodeCastException
     */
    public ENode nMinimize(ENode node) throws ENodeCastException {
        if (node.isList()) {
            ENodeList result = new ENodeList(true);
            result.addAll(node.getList());
            return new ENode(result, node);
        } else {
            return node;
        }
    }

    /**
     * Reverses the order of the elements of the specified node.
     * 
     * @param node
     *            is the node whose elements are to be reversed
     * @return the new node
     * @throws ENodeCastException
     * @throws FactoryException
     * @deprecated
     */
    @Deprecated
    public ENode reverse(ENode node) throws ENodeCastException, FactoryException {
        return nReverse(node);
    }

    /**
     * Reverses the order of the elements of the specified node.
     * 
     * @param node
     *            is the node whose elements are to be reversed
     * @return the new node
     * @throws ENodeCastException
     * @throws FactoryException
     */
    public ENode nReverse(ENode node) throws ENodeCastException, FactoryException {
        if (node.isList()) {
            ENodeList result = new ENodeList();
            ENodeIterator it = node.getList().iterator();
            while (it.hasNext()) {
                ENode child = it.next();
                if (!child.isNull()) {
                    result.add(0, child);
                }
            }
            return new ENode(result, node);
        } else {
            return node;
        }
    }

    /**
     * Returns true if this list contains the specified element.
     * <p>
     * More formally, returns true if and only if this list contains at least
     * <p>
     * one element e such that (o==null ? e==null : o.equals(e)).
     * 
     * @param node
     *            is the current node
     * @param element
     *            is element whose presence in this list is to be tested
     * @return true if this list contains the specified element
     * @throws ENodeCastException
     */
    public boolean nContains(ENode node, ENode element) throws ENodeCastException {
        if (node.isList()) {
            return node.getList().contains(element);
        } else {
            return node.equals(element);
        }
    }

    /**
     * Sorts the node and its children.
     * 
     * @param node
     *            is the current node
     * @return the sorted node
     * @throws ENodeCastException
     * @deprecated
     */
    @Deprecated
    public ENode sort(ENode node) throws ENodeCastException {
        return nMinimize(nSort(node));
    }

    /**
     * Sorts the node and its children.
     * 
     * @param node
     *            is the current node
     * @return the sorted node
     * @throws ENodeCastException
     */
    public ENode nSort(ENode node) throws ENodeCastException {
        if (node.isList()) {
            node.getList().sort();
        }
        return node;
    }

    /**
     * Sorts the node and its children.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the expression to evaluate for each node
     * @return the sorted node
     * @throws ENodeCastException
     * @deprecated
     */
    @Deprecated
    public ENode sort(ENode current, String call) throws ENodeCastException {
        return nMinimize(nSort(current, call));
    }

    /**
     * Sorts the node and its children.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the expression to evaluate for each node
     * @return the sorted node
     * @throws ENodeCastException
     */
    public ENode nSort(ENode current, String call) throws ENodeCastException {
        final String template = call;
        if (current.isList()) {
            Set set = new TreeSet(new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    ENode n0;
                    try {
                        n0 = evaluate((ENode) arg0, template);
                    } catch (TemplateSyntaxException e) {
                        n0 = new ENode(ENode.EMPTY, (ENode) arg0);
                    } catch (FactoryException e) {
                        n0 = new ENode(ENode.EMPTY, (ENode) arg0);
                    }
                    ENode n1;
                    try {
                        n1 = evaluate((ENode) arg1, template);
                    } catch (TemplateSyntaxException e) {
                        n1 = new ENode(ENode.EMPTY, (ENode) arg1);
                    } catch (FactoryException e) {
                        n1 = new ENode(ENode.EMPTY, (ENode) arg1);
                    }
                    return n0.compareTo(n1);
                }
            });
            ENodeIterator it = current.getList().iterator();
            while (it.hasNext()) {
                set.add(it.next());
            }
            ENodeList result = new ENodeList();
            Iterator elements = set.iterator();
            while (elements.hasNext()) {
                result.add((ENode) elements.next());
            }
            return new ENode(result, current);
        } else {
            return current;
        }
    }

}
