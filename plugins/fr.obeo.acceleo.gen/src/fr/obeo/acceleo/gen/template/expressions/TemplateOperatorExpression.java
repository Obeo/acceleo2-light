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

package fr.obeo.acceleo.gen.template.expressions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * Operators combine sub-expressions (operands) to create more complex
 * expressions.
 * <p>
 * The general syntax for using operators is :
 * <li>TemplateExpression (Operator TemplateExpression)+</li>
 * <p>
 * <p>
 * This generator has 3 types of operators:
 * <li>Arithmetic operators : + , - , / , *</li>
 * <li>Boolean operators : || , &&</li>
 * <li>Comparison operators : == , !=, > , >= , < , <=</li>
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateOperatorExpression extends TemplateExpression {

    /**
     * Operator name.
     */
    protected String operator;

    /**
     * Operands in order.
     */
    protected List children = new ArrayList();

    /**
     * Constructor.
     * 
     * @param operator
     *            is the operator name
     * @param script
     *            is the script
     */
    public TemplateOperatorExpression(String operator, IScript script) {
        super(script);
        this.operator = operator;
    }

    /**
     * @return the operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Adds an operand.
     * 
     * @param expression
     *            is the new operand
     */
    public void addChild(TemplateExpression expression) {
        children.add(expression);
        expression.setParent(this);
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException {
        try {
            final Iterator children = this.children.iterator();
            ENode last = null;
            while (children.hasNext()) {
                final TemplateExpression child = (TemplateExpression) children.next();
                ENode node = child.evaluate(current, script, mode);
                if (last == null || last.isBoolean()) {
                    if (operator.equals(TemplateConstants.OPERATOR_OR)) {
                        if (node.isBoolean() && node.getBoolean()) {
                            last = node;
                            break;
                        }
                    } else if (operator.equals(TemplateConstants.OPERATOR_AND)) {
                        if (node.isBoolean() && !node.getBoolean()) {
                            last = node;
                            break;
                        }
                    }
                }
                if (last != null) {
                    if (operator.equals(TemplateConstants.OPERATOR_OR)) {
                        node = ExpressionTools.or(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_AND)) {
                        node = ExpressionTools.and(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_EQUALS)) {
                        node = ExpressionTools.equals(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_NOT_EQUALS)) {
                        node = ExpressionTools.notEquals(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_SUP_EQUALS)) {
                        node = ExpressionTools.supE(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_INF_EQUALS)) {
                        node = ExpressionTools.infE(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_SUP)) {
                        node = ExpressionTools.sup(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_INF)) {
                        node = ExpressionTools.inf(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_ADD)) {
                        node = ExpressionTools.add(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_SUB)) {
                        node = ExpressionTools.sub(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_DIV)) {
                        node = ExpressionTools.div(last, node);
                    } else if (operator.equals(TemplateConstants.OPERATOR_MUL)) {
                        node = ExpressionTools.mul(last, node);
                    }
                }
                last = node;
            }
            ENode result;
            if (last != null) {
                result = last;
            } else {
                result = new ENode(ENode.EMPTY, current);
            }
            return result;
        } catch (final ENodeCastException e) {
            throw new ENodeException(e.getMessage(), pos, script, current, true);
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer(""); //$NON-NLS-1$
        final Iterator children = this.children.iterator();
        while (children.hasNext()) {
            final TemplateExpression child = (TemplateExpression) children.next();
            buffer.append(child.toString());
            if (children.hasNext()) {
                buffer.append(' ' + operator + ' ');
            }
        }
        return buffer.toString();
    }

    /* (non-Javadoc) */
    public static TemplateExpression fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        final Int2 trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
        if (trim.b() == -1) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
        } else {
            limits = trim;
        }
        for (String element : TemplateConstants.OPERATORS) {
            final Int2[] positions = TextSearch.getDefaultSearch().splitPositionsIn(buffer, limits.b(), limits.e(), new String[] { element }, false, TemplateConstants.SPEC,
                    TemplateConstants.INHIBS_EXPRESSION);
            if (positions.length > 1) {
                final TemplateOperatorExpression expression = new TemplateOperatorExpression(element, script);
                expression.setPos(limits);
                for (final Int2 pos : positions) {
                    expression.addChild(TemplateExpression.fromString(buffer, pos, script));
                }
                return expression;
            }
        }
        return null;
    }

}
