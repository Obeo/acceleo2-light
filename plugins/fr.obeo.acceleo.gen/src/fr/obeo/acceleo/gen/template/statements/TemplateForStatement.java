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

import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateNodeElement;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.log.EvalFailure;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * This is a 'for' statement for the generation tool. It is a kind of statement
 * that contains an expression and a template. The template is evaluated for
 * each value returned by the evaluation of the expression.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateForStatement extends TemplateNodeElement {

    /**
     * The expression.
     */
    protected TemplateExpression condition;

    /**
     * Template that is evaluated for each value returned by the evaluation of
     * the expression.
     */
    protected Template block;

    /**
     * Constructor.
     * 
     * @param condition
     *            is the conditional expression
     * @param block
     *            is the template that is evaluated for each value returned by
     *            the evaluation of the expression.
     * @param script
     *            is the script
     */
    public TemplateForStatement(TemplateExpression condition, Template block, IScript script) {
        super(script);
        this.condition = condition;
        this.condition.setParent(this);
        this.block = block;
        this.block.setParent(this);
    }

    /**
     * Gets the condition.
     */
    public TemplateExpression getCondition() {
        return condition;
    }

    /**
     * @return the block
     */
    public Template getBlock() {
        return block;
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
        final ENode all = condition.evaluate(new ENode(object, condition, mode.isSynchronize()), script, mode);
        if (mode.getMode() == LaunchManager.DEBUG_MODE) {
            node.log().getAll(all.log(), false);
        }
        if (!all.isNull()) {
            boolean containsEObject;
            ENodeIterator it;
            if (all.isList()) {
                try {
                    containsEObject = false;
                    it = all.getList().iterator();
                    while (it.hasNext() && !containsEObject) {
                        final ENode child = it.next();
                        if (child.isEObject()) {
                            containsEObject = true;
                        }
                    }
                    it = all.getList().iterator();
                } catch (final ENodeCastException e) {
                    // Never catch
                    containsEObject = false;
                    it = new ENodeList().iterator();
                }
            } else if (all.isInt()) {
                try {
                    containsEObject = true;
                    final ENodeList list = new ENodeList();
                    final int count = all.getInt();
                    for (int i = 0; i < count; i++) {
                        list.add(new ENode(object, condition, mode.isSynchronize()));
                    };
                    it = list.iterator();
                } catch (final ENodeCastException e) {
                    // Never catch
                    containsEObject = false;
                    it = new ENodeList().iterator();
                }
            } else {
                containsEObject = all.isEObject();
                final ENodeList list = new ENodeList();
                list.add(all);
                it = list.iterator();
            }
            if (it.hasNext()) {
                int iObject = 0;
                while (it.hasNext()) {
                    ENode child = it.next();
                    if (!containsEObject) {
                        if (child.isString()) {
                            try {
                                final EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
                                final String value = child.getString();
                                literal.setName(value);
                                literal.setLiteral(value);
                                child = new ENode(literal, child);
                            } catch (final ENodeCastException e) {
                                // Never catch
                            }
                        } else if (child.isInt()) {
                            try {
                                final EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
                                final String value = String.valueOf(child.getInt());
                                literal.setName(value);
                                literal.setLiteral(value);
                                child = new ENode(literal, child);
                            } catch (final ENodeCastException e) {
                                // Never catch
                            }
                        } else if (child.isDouble()) {
                            try {
                                final EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
                                final String value = String.valueOf(child.getDouble());
                                literal.setName(value);
                                literal.setLiteral(value);
                                child = new ENode(literal, child);
                            } catch (final ENodeCastException e) {
                                // Never catch
                            }
                        } else if (child.isBoolean()) {
                            try {
                                final EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
                                final String value = String.valueOf(child.getBoolean());
                                literal.setName(value);
                                literal.setLiteral(value);
                                child = new ENode(literal, child);
                            } catch (final ENodeCastException e) {
                                // Never catch
                            }
                        }
                    }
                    script.contextPush(IScript.CURRENT_NODE, child);
                    script.contextPush(IScript.WHILE_INDEX, new Integer(iObject));
                    try {
                        if (child.isEObject()) {
                            try {
                                final ENode n = block.evaluate(child.getEObject(), mode);
                                node.append(n);
                            } catch (final ENodeCastException e) {
                                // Never catch
                            }
                            ++iObject;
                        } else if (containsEObject) {
                            node.append(child);
                        }
                    } finally {
                        script.contextPop(IScript.CURRENT_NODE);
                        script.contextPop(IScript.WHILE_INDEX);
                    }
                }
            }
        }
        return node;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return TemplateConstants.FOR_BEGIN + condition.toString() + TemplateConstants.FOR_THEN + block.toString() + TemplateConstants.FOR_END;
    }

    /* (non-Javadoc) */
    @Override
    public String getOutlineText() {
        return "for " + condition.toString(); //$NON-NLS-1$
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
        Template block;
        final Int2 iThen = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.FOR_THEN, limits.b(), limits.e(), TemplateConstants.SPEC, TemplateConstants.INHIBS_EXPRESSION);
        if (iThen.b() > -1) {
            condition = TemplateExpression.fromString(buffer, new Int2(limits.b(), iThen.b()), script);
            Int2 blockPos = new Int2(iThen.e(), limits.e());
            blockPos = Template.formatTemplate(buffer, blockPos, 0);
            block = Template.read(buffer, blockPos, script);
        } else {
            condition = TemplateExpression.fromString(buffer, new Int2(limits.b(), limits.e()), script);
            block = new Template(script);
        }
        final TemplateForStatement element = new TemplateForStatement(condition, block, script);
        element.setPos(limits);
        return element;
    }

}
