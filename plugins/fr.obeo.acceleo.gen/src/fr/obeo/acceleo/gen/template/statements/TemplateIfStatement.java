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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateNodeElement;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.log.EvalFailure;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * This is a 'if' statement for the generation tool. It is a kind of statement
 * that contains a conditional expression and two templates. If the conditional
 * expression is validated then the first template is evaluated, else the second
 * template is evaluated.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateIfStatement extends TemplateNodeElement {

    /**
     * The conditional expression.
     */
    protected TemplateExpression condition;

    /**
     * Template that is evaluated if the conditional expression is validated.
     */
    protected Template ifTemplate;

    /**
     * The expressions for all the "else if" conditions.
     */
    protected TemplateExpression[] elseIfConditions;

    /**
     * The "else if" templates.
     */
    protected Template[] elseIfTemplates;

    /**
     * Template that is evaluated if the conditional expression is not
     * validated.
     */
    protected Template elseTemplate;

    /**
     * Constructor.
     * 
     * @param condition
     *            is the conditional expression
     * @param ifTemplate
     *            is the template that is evaluated if the conditional
     *            expression is validated
     * @param elseTemplate
     *            is the template that is evaluated if the conditional
     *            expression is not validated.
     * @param script
     *            is the script
     */
    public TemplateIfStatement(TemplateExpression condition, Template ifTemplate, Template elseTemplate, IScript script) {
        this(condition, ifTemplate, new TemplateExpression[] {}, new Template[] {}, elseTemplate, script);
    }

    /**
     * Constructor.
     * 
     * @param condition
     *            is the conditional expression
     * @param ifTemplate
     *            is the template that is evaluated if the conditional
     *            expression is validated
     * @param elseIfConditions
     *            are "else if" conditions
     * @param elseIfTemplates
     *            are "else if" templates
     * @param elseTemplate
     *            is the template that is evaluated if the conditional
     *            expression is not validated.
     * @param script
     *            is the script
     */
    public TemplateIfStatement(TemplateExpression condition, Template ifTemplate, TemplateExpression[] elseIfConditions, Template[] elseIfTemplates, Template elseTemplate, IScript script) {
        super(script);
        this.condition = condition;
        this.condition.setParent(this);
        this.ifTemplate = ifTemplate;
        this.ifTemplate.setParent(this);
        this.elseIfConditions = elseIfConditions;
        for (final TemplateExpression elseIfCondition : elseIfConditions) {
            elseIfCondition.setParent(this);
        }
        this.elseIfTemplates = elseIfTemplates;
        for (final Template elseIfTemplate : elseIfTemplates) {
            elseIfTemplate.setParent(this);
        }
        this.elseTemplate = elseTemplate;
        this.elseTemplate.setParent(this);
    }

    /**
     * @return the conditional expression
     */
    public TemplateExpression getCondition() {
        return condition;
    }

    /**
     * @return the elseIf conditions
     */
    public TemplateExpression[] getElseIfConditions() {
        return elseIfConditions;
    }

    /**
     * @return the elseIf templates
     */
    public Template[] getElseIfTemplates() {
        return elseIfTemplates;
    }

    /**
     * @return the else template
     */
    public Template getElseTemplate() {
        return elseTemplate;
    }

    /**
     * @return the if template
     */
    public Template getIfTemplate() {
        return ifTemplate;
    }

    /**
     * Returns the conditional expression for the given template.
     * 
     * @return the conditional expression for the given template or null
     */
    public String getConditionText(Template template) {
        if (template == ifTemplate) {
            return condition.toString();
        } else if (template == elseTemplate) {
            return "else"; //$NON-NLS-1$
        } else {
            for (int i = 0; i < elseIfTemplates.length && i < elseIfConditions.length; i++) {
                if (template == elseIfTemplates[i]) {
                    return elseIfConditions[i].toString();
                }
            }
            return ""; //$NON-NLS-1$
        }
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        try {
            final ENode result = evaluateSub(object, mode);
            return result;
        } catch (final ENodeException e) {
            final ENode result = new ENode(ENode.EMPTY, object, this, mode.isSynchronize());
            result.log().addError(new EvalFailure(e.getMessage()));
            return result;
        }
    }

    private ENode evaluateSub(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        final ENode node = new ENode(ENode.EMPTY, object, this, mode.isSynchronize());
        boolean testOK = false;
        ENode test = condition.evaluate(new ENode(object, condition, mode.isSynchronize()), script, mode);
        if (mode.getMode() == LaunchManager.DEBUG_MODE) {
            node.log().getAll(test.log(), false);
        }
        try {
            testOK = test.getBoolean();
        } catch (final ENodeCastException e) {
            try {
                final Object boolValue = test.getAdapterValue(boolean.class);
                if (boolValue instanceof Boolean) {
                    testOK = ((Boolean) boolValue).booleanValue();
                } else {
                    throw new ENodeException(AcceleoGenMessages.getString("ENodeError.BooleanRequired", new Object[] { test.getType(), }), condition.getPos(), script, object, true); //$NON-NLS-1$
                }
            } catch (final ENodeCastException ex) {
                throw new ENodeException(ex.getMessage(), condition.getPos(), script, object, true);
            }
        }
        if (testOK) {
            node.append(ifTemplate.evaluate(object, mode));
        } else {
            for (int i = 0; !testOK && i < elseIfConditions.length; i++) {
                final TemplateExpression elseIfCondition = elseIfConditions[i];
                test = elseIfCondition.evaluate(new ENode(object, elseIfCondition, mode.isSynchronize()), script, mode);
                if (mode.getMode() == LaunchManager.DEBUG_MODE) {
                    node.log().getAll(test.log(), false);
                }
                try {
                    testOK = test.getBoolean();
                } catch (final ENodeCastException e) {
                    try {
                        final Object boolValue = test.getAdapterValue(boolean.class);
                        if (boolValue instanceof Boolean) {
                            testOK = ((Boolean) boolValue).booleanValue();
                        } else {
                            throw new ENodeException(AcceleoGenMessages.getString("ENodeError.BooleanRequired", new Object[] { test.getType(), }), elseIfCondition.getPos(), script, object, true); //$NON-NLS-1$
                        }
                    } catch (final ENodeCastException ex) {
                        throw new ENodeException(ex.getMessage(), elseIfCondition.getPos(), script, object, true);
                    }
                }
                if (testOK) {
                    node.append(elseIfTemplates[i].evaluate(object, mode));
                }
            }
            if (!testOK) {
                node.append(elseTemplate.evaluate(object, mode));
            }
        }
        return node;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        String optElse = elseTemplate.toString();
        if (optElse.length() > 0) {
            optElse = TemplateConstants.IF_ELSE + optElse;
        }
        final StringBuffer optElseIf = new StringBuffer(""); //$NON-NLS-1$
        for (int i = 0; i < elseIfConditions.length; i++) {
            optElseIf.append(TemplateConstants.IF_ELSE_IF);
            optElseIf.append(elseIfConditions[i].toString());
            optElseIf.append(TemplateConstants.IF_THEN);
            optElseIf.append(elseIfTemplates[i].toString());
        }
        return TemplateConstants.IF_BEGIN + condition.toString() + TemplateConstants.IF_THEN + ifTemplate.toString() + optElseIf.toString() + optElse + TemplateConstants.IF_END;
    }

    /* (non-Javadoc) */
    @Override
    public String getOutlineText() {
        return "if " + condition.toString(); //$NON-NLS-1$
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
        TemplateExpression condition;
        Template ifTemplate;
        final List elseIfConditions = new ArrayList();
        final List elseIfTemplates = new ArrayList();
        Template elseTemplate;
        // If -> Then
        final Int2 iThen = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.IF_THEN, limits.b(), limits.e(), TemplateConstants.SPEC, TemplateConstants.INHIBS_EXPRESSION);
        if (iThen.b() > -1) {
            condition = TemplateExpression.fromString(buffer, new Int2(limits.b(), iThen.b()), script);
            int iThenEnd;
            // Else -> End
            final Int2 iElse = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.IF_ELSE, iThen.e(), limits.e(), null, TemplateConstants.INHIBS_STATEMENT);
            if (iElse.b() > -1) {
                iThenEnd = iElse.b();
                Int2 elsePos = new Int2(iElse.e(), limits.e());
                elsePos = Template.formatTemplate(buffer, elsePos, 0);
                elseTemplate = Template.read(buffer, elsePos, script);
            } else {
                iThenEnd = limits.e();
                elseTemplate = new Template(script);
            }
            // ElseIf -> ElseIf|Else|End
            final int iElseIfEndMax = iThenEnd;
            Int2 iElseIf = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.IF_ELSE_IF, iThen.e(), iElseIfEndMax, null, TemplateConstants.INHIBS_STATEMENT);
            if (iElseIf.b() > -1) {
                iThenEnd = iElseIf.b();
                while (iElseIf.b() > -1) {
                    // ElseIf test
                    TemplateExpression elseIfTest;
                    final Int2 iTestEnd = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.IF_THEN, iElseIf.e(), iElseIfEndMax, TemplateConstants.SPEC,
                            TemplateConstants.INHIBS_EXPRESSION);
                    if (iTestEnd.b() > -1) {
                        elseIfTest = TemplateExpression.fromString(buffer, new Int2(iElseIf.e(), iTestEnd.b()), script);
                    } else {
                        throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingCloseIf"), script, new Int2(iElseIf.e(), iElseIfEndMax)); //$NON-NLS-1$
                    }
                    iElseIf = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.IF_ELSE_IF, iTestEnd.e(), iElseIfEndMax, null, TemplateConstants.INHIBS_STATEMENT);
                    Int2 elseIfPos;
                    if (iElseIf.b() > -1) {
                        elseIfPos = new Int2(iTestEnd.e(), iElseIf.b());
                    } else {
                        elseIfPos = new Int2(iTestEnd.e(), iElseIfEndMax);
                    }
                    elseIfPos = Template.formatTemplate(buffer, elseIfPos, 0);
                    final Template elseIfTemplate = Template.read(buffer, elseIfPos, script);
                    elseIfConditions.add(elseIfTest);
                    elseIfTemplates.add(elseIfTemplate);
                }
            }
            // Then -> ElseIf|Else|End
            Int2 ifPos = new Int2(iThen.e(), iThenEnd);
            ifPos = Template.formatTemplate(buffer, ifPos, 0);
            ifTemplate = Template.read(buffer, ifPos, script);
        } else {
            condition = TemplateExpression.fromString(buffer, new Int2(limits.b(), limits.e()), script);
            ifTemplate = new Template(script);
            elseTemplate = new Template(script);
        }
        final TemplateIfStatement element = new TemplateIfStatement(condition, ifTemplate, (TemplateExpression[]) elseIfConditions.toArray(new TemplateExpression[elseIfConditions.size()]),
                (Template[]) elseIfTemplates.toArray(new Template[elseIfTemplates.size()]), elseTemplate, script);
        element.setPos(limits);
        return element;
    }

}
