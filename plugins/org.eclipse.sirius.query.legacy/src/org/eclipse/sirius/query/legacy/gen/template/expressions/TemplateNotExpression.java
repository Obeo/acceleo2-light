/*******************************************************************************
 * Copyright (c) 2005-2014 Obeo
 *  
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.gen.template.expressions;

import org.eclipse.sirius.query.legacy.ecore.factories.FactoryException;
import org.eclipse.sirius.query.legacy.gen.AcceleoGenMessages;
import org.eclipse.sirius.query.legacy.gen.template.TemplateConstants;
import org.eclipse.sirius.query.legacy.gen.template.TemplateSyntaxException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeCastException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.LaunchManager;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;
import org.eclipse.sirius.query.legacy.tools.strings.Int2;
import org.eclipse.sirius.query.legacy.tools.strings.TextSearch;

/**
 * This operator is used to create negative expressions.
 * <p>
 * The general syntax is : ! expression
 * 
 * 
 */
public class TemplateNotExpression extends TemplateExpression {

    /**
     * The expression after the not operator.
     */
    protected TemplateExpression expression;

    /**
     * Constructor.
     * 
     * @param expression
     *            is the expression after the not operator
     * @param script
     *            is the script
     */
    public TemplateNotExpression(TemplateExpression expression, IScript script) {
        super(script);
        this.expression = expression;
        this.expression.setParent(this);
    }

    /**
     * @return the expression
     */
    public TemplateExpression getExpression() {
        return expression;
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException {
        final ENode test = expression.evaluate(current, script, mode);
        ENode result;
        try {
            boolean value = test.getBoolean();
            result = new ENode(!value, current);
        } catch (final ENodeCastException e1) {
            try {
                final Object boolValue = test.getAdapterValue(boolean.class);
                if (boolValue instanceof Boolean) {
                    result = new ENode(!((Boolean) boolValue).booleanValue(), current);
                } else {
                    throw new ENodeException(AcceleoGenMessages.getString("ENodeError.BooleanRequired", new Object[] { test.getType(), }), expression.getPos(), script, current, true); //$NON-NLS-1$
                }
            } catch (final ENodeCastException e2) {
                throw new ENodeException(e2.getMessage(), expression.getPos(), script, current, true);
            }
        }
        return result;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return TemplateConstants.NOT + expression.toString();
    }

    /* (non-Javadoc) */
    public static TemplateExpression fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        final Int2 trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
        if (trim.b() == -1) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
        } else {
            limits = trim;
        }
        final String text = buffer.substring(limits.b(), limits.e());
        if (text.startsWith(TemplateConstants.NOT)) {
            final TemplateExpression expression = TemplateExpression.fromString(buffer, new Int2(limits.b() + TemplateConstants.NOT.length(), limits.e()), script);
            final TemplateNotExpression notExpression = new TemplateNotExpression(expression, script);
            notExpression.setPos(limits);
            return notExpression;
        } else {
            return null;
        }
    }

}
