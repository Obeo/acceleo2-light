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
 * This operator is used to create negative expressions.
 * <p>
 * The general syntax is : ! expression
 * 
 * @author www.obeo.fr
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
