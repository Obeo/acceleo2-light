/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.business.internal.interpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.common.tools.api.interpreter.EvaluationException;
import org.eclipse.sirius.common.tools.api.util.StringUtil;
import org.eclipse.sirius.query.legacy.AcceleoInterpreterPlugin;
import org.eclipse.sirius.query.legacy.gen.template.TemplateElement;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeCastException;

/**
 * Utility class to handle ENodes.
 * 
 * @author cbrun
 * 
 */
public final class ENodeHelper {
    /**
     * Utility class: no instantiation
     */
    private ENodeHelper() {
    }

    /**
     * Transform the given {@link ENode} to a list of EObject. All elements not
     * being {@link EObject} in the result will be ignored.
     * 
     * @param enode
     *            any ENode.
     * @return a list of {@link EObject}.
     */
    @SuppressWarnings("unchecked")
    public static Collection<EObject> getAsListOfEObjects(final ENode enode) {
        Collection<EObject> result = null;
        try {
            result = (Collection<EObject>) enode.getAdapterValue(Collection.class);
        } catch (final ENodeCastException e) {
            // silent catch
        }
        if (result != null) {
            final Iterator<?> it = result.iterator();
            while (it.hasNext()) {
                final Object cur = it.next();
                if (!(cur instanceof EObject)) {
                    it.remove();
                }
            }
        } else {
            result = new ArrayList<EObject>();
        }
        return result;
    }

    /**
     * Convert an {@link ENode} instance.
     * 
     * @param node
     *            the {@link ENode} instance to convert
     * @param s
     *            the error message in case of {@link ENodeCastException}
     * @return the converted object or null.
     */
    public static Object getValue(final ENode node, final String s) {
        Object value = null;
        if (!node.isNull()) {
            try {
                if (node.isEObject()) {
                    value = node.getEObject();
                } else if (node.isString()) {
                    value = node.getString();
                } else if (node.isBoolean()) {
                    value = Boolean.valueOf(node.getBoolean());
                } else if (node.isDouble()) {
                    value = new Double(node.getDouble());
                } else if (node.isInt()) {
                    value = Integer.valueOf(node.getInt());
                } else if (node.isList() && node.getList().size() > 0) {
                    // FIXME specification for list in SetValue.
                    value = node.getList().asList();
                }
            } catch (final ENodeCastException e) {
                AcceleoInterpreterPlugin.getDefault().error(s, e);
            }
        }
        return value;
    }

    /**
     * Wrapper method to evaluate an expression.
     * 
     * @param interpreter
     *            an expression interpreter.
     * @param target
     *            the EObject instance to evaluate on.
     * @param expression
     *            the expression to evaluate.
     * @return an ENode with the evaluation result.
     * @throws EvaluationException
     *             if the evaluation was not successful.
     */
    public static ENode evaluate(final AcceleoInterpreter interpreter, final EObject target, final String expression) throws EvaluationException {
        ENode result = new ENode(null, (TemplateElement) null, true);
        try {
            if (!StringUtil.isEmpty(expression)) {
                result = interpreter.evaluateENode(target, expression);
            }

        } catch (final EvaluationException e) {
            throw new EvaluationException(e);
        }
        return result;
    }

    /**
     * adapt an ENode to a boolean.
     * 
     * @param evaluate
     *            ENode to adapt.
     * @return the boolean value, false if adaptation fails.
     */
    public static boolean getAsBoolean(final ENode evaluate) {
        boolean adaptedBool = false;
        try {
            if (evaluate.isBoolean()) {
                return evaluate.getBoolean();
            }
            final Boolean value = (Boolean) evaluate.getAdapterValue(Boolean.class);
            if (value != null) {
                adaptedBool = value.booleanValue();
            }
        } catch (final ENodeCastException e) {
            // silent catch
        }
        return adaptedBool;
    }

    /**
     * adapt an ENode to an EObject.
     * 
     * @param result
     *            ENode to adapt.
     * @return the EObject value, null if no adaptation is possible.
     */
    public static EObject getAsEObject(final ENode result) {
        EObject adaptedEObject = null;
        try {
            if (result.isEObject()) {
                return result.getEObject();
            } else {
                adaptedEObject = (EObject) result.getAdapterValue(EObject.class);
            }
        } catch (final ENodeCastException e) {
            // silent catch
        }
        return adaptedEObject;
    }

    /**
     * adapt an ENode to an String.
     * 
     * @param result
     *            ENode to adapt.
     * @return the String value, null if no adaptation is possible.
     */
    public static String getAsString(final ENode result) {
        String adaptedEObject = null;
        try {
            if (result.isString()) {
                return result.getString();
            } else {
                adaptedEObject = (String) result.getAdapterValue(String.class);
            }
        } catch (final ENodeCastException e) {
            // silent catch
        }
        return adaptedEObject;
    }

    /**
     * adapt an ENode to an Integer.
     * 
     * @param result
     *            ENode to adapt.
     * @return the Integer value, null if no adaptation is possible.
     */
    public static Integer getAsInteger(final ENode result) {
        Integer adaptedEObject = null;
        try {
            if (result.isInt()) {
                return result.getInt();
            } else {
                adaptedEObject = (Integer) result.getAdapterValue(int.class);
            }
        } catch (final ENodeCastException e) {
            // silent catch
        }
        return adaptedEObject;
    }
}
