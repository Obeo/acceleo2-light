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
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * The valuation of a parenthesis expression is the valuation of the expression
 * inside the parenthesis. The type of a parenthesis expression is the same as
 * the type of the expression inside the parenthesis.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateParenthesisExpression extends TemplateExpression {

    /**
     * The expression inside the parenthesis.
     */
    protected TemplateExpression expression;

    /**
     * Constructor.
     * 
     * @param expression
     *            is the expression inside the parenthesis
     * @param script
     *            is the script
     */
    public TemplateParenthesisExpression(TemplateExpression expression, IScript script) {
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
        ENode result = expression.evaluate(current, script, mode);
        return result;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return TemplateConstants.PARENTH[0] + expression.toString() + TemplateConstants.PARENTH[1];
    }

    /* (non-Javadoc) */
    public static TemplateExpression fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        Int2 trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
        if (trim.b() == -1) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
        } else {
            limits = trim;
        }
        Int2 begin = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.PARENTH[0], limits.b(), limits.e());
        if (begin.b() == limits.b()) {
            Int2 end = TextSearch.getDefaultSearch().blockIndexEndIn(buffer, TemplateConstants.PARENTH[0], TemplateConstants.PARENTH[1], begin.b(), limits.e(), true, TemplateConstants.SPEC,
                    TemplateConstants.INHIBS_EXPRESSION);
            if (end.e() == limits.e()) {
                TemplateExpression expression = TemplateExpression.fromString(buffer, new Int2(limits.b() + TemplateConstants.PARENTH[0].length(), limits.e() - TemplateConstants.PARENTH[1].length()),
                        script);
                TemplateParenthesisExpression parenthesis = new TemplateParenthesisExpression(expression, script);
                parenthesis.setPos(limits);
                return parenthesis;
            }
        }
        return null;
    }

}
