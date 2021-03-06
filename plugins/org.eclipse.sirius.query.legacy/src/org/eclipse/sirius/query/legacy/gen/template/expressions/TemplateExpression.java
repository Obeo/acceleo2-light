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
import org.eclipse.sirius.query.legacy.gen.template.TemplateElement;
import org.eclipse.sirius.query.legacy.gen.template.TemplateSyntaxException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.LaunchManager;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;
import org.eclipse.sirius.query.legacy.tools.strings.Int2;
import org.eclipse.sirius.query.legacy.tools.strings.TextSearch;

/**
 * Expressions are defined recursively, as is usual for programming language
 * specifications. Each expression is included in a statement.
 * 
 * 
 */
public abstract class TemplateExpression extends TemplateElement {

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public TemplateExpression(IScript script) {
        super(script);
    }

    /**
     * Evaluates this template expression on an ENode. The result node of
     * generation is also an ENode. Expressions are defined recursively, so the
     * result is transmitted from an expression to another.
     * 
     * @param current
     *            is the current node of generation
     * @param script
     *            is the generator's configuration
     * @param mode
     *            is the mode in which to launch, one of the mode constants
     *            defined - RUN_MODE or DEBUG_MODE
     * @return the result node of generation
     * @throws ENodeException
     * @throws FactoryException
     * @see ENode
     */
    public abstract ENode evaluate(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException;

    /**
     * It checks the syntax and creates an expression for the given part of the
     * text. The part of the text to be parsed is delimited by the given limits.
     * 
     * @param buffer
     *            is the textual representation of the templates
     * @param limits
     *            delimits the part of the text to be parsed for this expression
     * @param script
     *            is the generator's configuration
     * @return the new expression
     * @throws TemplateSyntaxException
     */
    public static TemplateExpression fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        Int2 trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
        if (trim.b() == -1) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
        } else {
            limits = trim;
        }
        TemplateExpression expression = TemplateOperatorExpression.fromString(buffer, limits, script);
        if (expression != null) {
            return expression;
        }
        expression = TemplateNotExpression.fromString(buffer, limits, script);
        if (expression != null) {
            return expression;
        }
        expression = TemplateParenthesisExpression.fromString(buffer, limits, script);
        if (expression != null) {
            return expression;
        }
        expression = TemplateLiteralExpression.fromString(buffer, limits, script);
        if (expression != null) {
            return expression;
        }
        expression = TemplateCallSetExpression.fromString(buffer, limits, script);
        return expression;
    }

}
