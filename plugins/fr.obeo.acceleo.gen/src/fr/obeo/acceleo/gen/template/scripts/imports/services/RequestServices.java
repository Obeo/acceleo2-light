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

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Request services.
 * 
 * @author www.obeo.fr
 * 
 */
public class RequestServices {

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
    public RequestServices(IScript script) {
        this.script = script;
    }

    /**
     * Evaluates the given expression on all the elements of the current node
     * and keep the elements validating the condition.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the condition
     * @return valid elements
     * @throws FactoryException
     * @throws ENodeCastException
     * @throws TemplateSyntaxException
     * @deprecated
     */
    @Deprecated
    public ENode select(ENode current, String call) throws FactoryException, ENodeCastException, TemplateSyntaxException {
        return select(current, call, new ENode(true, current));
    }

    /**
     * Evaluates the given expression on all the elements of the current node
     * and keep the elements validating the given value.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the expression to apply
     * @param value
     *            is the value of the elements to keep
     * @return valid elements
     * @throws FactoryException
     * @throws ENodeCastException
     * @throws TemplateSyntaxException
     * @deprecated
     */
    @Deprecated
    public ENode select(ENode current, String call, ENode value) throws FactoryException, ENodeCastException, TemplateSyntaxException {
        int pos = getBegin();
        call = call.replaceAll("'", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
        TemplateExpression expression = TemplateExpression.fromString(computeCall(pos, call), new Int2(pos, pos + call.length()), script);
        return select(current, expression, value);
    }

    private ENode select(ENode current, TemplateExpression call, ENode value) throws FactoryException, ENodeCastException {
        if (current.isList()) {
            ENodeList list = current.getList();
            ENodeList res = new ENodeList();
            ENodeIterator it = list.iterator();
            while (it.hasNext()) {
                res.add(select(it.next(), call, value));
            }
            return new ENode(res, current);
        } else {
            try {
                ENode result = call.evaluate(current, script, LaunchManager.create("run", true)); //$NON-NLS-1$
                if (result.equals(value)) {
                    return current;
                } else {
                    return new ENode(ENode.EMPTY, current);
                }
            } catch (ENodeException e) {
                return new ENode(ENode.EMPTY, current);
            }
        }
    }

    /**
     * Evaluates the given expression on all the elements of the current node
     * and delete the elements validating the condition.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the expression to apply
     * @return valid elements
     * @throws FactoryException
     * @throws ENodeCastException
     * @throws TemplateSyntaxException
     * @deprecated
     */
    @Deprecated
    public ENode delete(ENode current, String call) throws FactoryException, ENodeCastException, TemplateSyntaxException {
        return delete(current, call, new ENode(true, current));
    }

    /**
     * Evaluates the given expression on all the elements of the current node
     * and delete the elements validating the given value.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the expression to apply
     * @param value
     *            is the value of the elements to delete
     * @return valid elements
     * @throws FactoryException
     * @throws ENodeCastException
     * @throws TemplateSyntaxException
     * @deprecated
     */
    @Deprecated
    public ENode delete(ENode current, String call, ENode value) throws FactoryException, ENodeCastException, TemplateSyntaxException {
        int pos = getBegin();
        call = call.replaceAll("'", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
        TemplateExpression expression = TemplateExpression.fromString(computeCall(pos, call), new Int2(pos, pos + call.length()), script);
        return delete(current, expression, value);
    }

    private ENode delete(ENode current, TemplateExpression call, ENode value) throws FactoryException, ENodeCastException {
        if (current.isList()) {
            ENodeList list = current.getList();
            ENodeList res = new ENodeList();
            ENodeIterator it = list.iterator();
            while (it.hasNext()) {
                res.add(delete(it.next(), call, value));
            }
            return new ENode(res, current);
        } else {
            try {
                ENode result = call.evaluate(current, script, LaunchManager.create("run", true)); //$NON-NLS-1$
                if (!result.equals(value)) {
                    return current;
                } else {
                    return new ENode(ENode.EMPTY, current);
                }
            } catch (ENodeException e) {
                return new ENode(ENode.EMPTY, current);
            }
        }
    }

    /**
     * Evaluates the given text as an acceleo expression.
     * 
     * @param current
     *            is the current node of generation
     * @param call
     *            is the expression to evaluate
     * @return evaluation
     * @throws TemplateSyntaxException
     * @throws FactoryException
     */
    public ENode evaluate(ENode current, String call) throws TemplateSyntaxException, FactoryException {
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

}
