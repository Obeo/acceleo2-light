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

package fr.obeo.acceleo.gen.template.statements;

import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateNodeElement;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.log.EvalFailure;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * This is a 'feature' statement for the generation tool. It is a kind of
 * statement that contains one expression. This expression is evaluated to
 * generate text.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateFeatureStatement extends TemplateNodeElement {

    /**
     * The expression.
     */
    protected TemplateExpression expression;

    /**
     * Constructor.
     * 
     * @param expression
     *            is the expression
     * @param script
     *            is the script
     */
    public TemplateFeatureStatement(TemplateExpression expression, IScript script) {
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
    public ENode evaluate(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        try {
            ENode result = evaluateSub(object, mode);
            return result;
        } catch (ENodeException e) {
            ENode result = new ENode(ENode.EMPTY, object, this, mode.isSynchronize());
            result.log().addError(new EvalFailure(e.getMessage()));
            return result;
        }
    }

    private ENode evaluateSub(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        ENode current = new ENode(object, this, mode.isSynchronize());
        script.contextPush(IScript.CURRENT_NODE, current);
        try {
            ENode node = expression.evaluate(current, script, mode);
            if (node.isNull() && !node.isOptional()) {
                if (node.log().hasError()) {
                    return node;
                } else {
                    throw new ENodeException(AcceleoGenMessages.getString("ENodeError.EmptyEvaluation"), expression.getPos(), expression.getScript(), object, false); //$NON-NLS-1$
                }
            } else {
                return node;
            }
        } finally {
            script.contextPop(IScript.CURRENT_NODE);
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return TemplateConstants.FEATURE_BEGIN + expression.toString() + TemplateConstants.FEATURE_END;
    }

    /* (non-Javadoc) */
    @Override
    public String getOutlineText() {
        return expression.toString();
    }

    /**
     * It checks the syntax and creates a statement for the given part of the
     * text. The part of the text to be parsed is delimited by the given limits.
     * 
     * @param buffer
     *            is the textual representation of the templates
     * @param limits
     *            delimits the part of the text to be parsed for this statement
     * @param script
     *            is the generator's configuration
     * @return the new statement
     * @throws TemplateSyntaxException
     */
    public static TemplateNodeElement fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        TemplateExpression expression = TemplateExpression.fromString(buffer, new Int2(limits.b(), limits.e()), script);
        TemplateFeatureStatement element = new TemplateFeatureStatement(expression, script);
        element.setPos(limits);
        return element;
    }

}
