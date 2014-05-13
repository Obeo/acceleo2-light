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
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateElement;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.merge.MergeTools;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.statements.TemplateFeatureStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateForStatement;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * This is a sequence of variable call expression (TemplateCallExpression).
 * <p>
 * For example, "a.b.c(d,e).f" is a sequence with four variables call
 * expressions : "a", "b", "c(d,e)", and "f".
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateCallSetExpression extends TemplateExpression {

    /**
     * Variables call expressions.
     */
    protected List calls = new ArrayList();

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public TemplateCallSetExpression(IScript script) {
        super(script);
    }

    /**
     * Adds a call expression.
     * 
     * @param call
     *            is the new call
     */
    public void addCall(TemplateCallExpression call) {
        if (calls.size() > 0) {
            TemplateCallExpression last = (TemplateCallExpression) calls.get(calls.size() - 1);
            last.setNextCall(call);
        }
        calls.add(call);
        call.setParent(this);
    }

    /**
     * Gets the first call of the set, or null is the set is empty.
     * 
     * @return the first call of the set, or null
     */
    public TemplateCallExpression getFirst() {
        if (calls.size() > 0) {
            return (TemplateCallExpression) calls.get(0);
        } else {
            return null;
        }
    }

    /**
     * Gets an iterator for the calls.
     * 
     * @return an iterator for the calls
     */
    public Iterator iterator() {
        return calls.iterator();
    }

    /**
     * Indicates if this set is a predefined link.
     * <p>
     * Samples:
     * <li>i()</li>
     * <li>args(?)</li>
     * <li>startUserCode</li>
     * <li>endUserCode</li>
     * 
     * @return true if this set is a predefined link
     */
    public boolean isPredefined() {
        boolean predefined = false;
        if (this.calls.size() > 0) {
            TemplateCallExpression call = (TemplateCallExpression) calls.get(0);
            if (call.link.equals(TemplateConstants.LINK_NAME_INDEX) && call.arguments.size() == 0 && "".equals(call.getPrefix())) {
                return true;
            } else if (call.link.equals(TemplateConstants.LINK_NAME_ARGS) && call.arguments.size() == 1 && "".equals(call.getPrefix())) {
                predefined = true;
            } else if (call.link.equals(TemplateConstants.USER_BEGIN_NAME) && call.arguments.size() == 0) {
                if (parent instanceof TemplateFeatureStatement) {
                    predefined = true;
                }
            } else if (call.link.equals(TemplateConstants.USER_END_NAME) && call.arguments.size() == 0) {
                if (parent instanceof TemplateFeatureStatement) {
                    predefined = true;
                }
            }
        }
        return predefined;
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException {
        script.contextPush(IScript.CURRENT_NODE, current);
        try {
            Iterator calls = this.calls.iterator();
            if (calls.hasNext()) {
                TemplateCallExpression call = (TemplateCallExpression) calls.next();
                // Predefined links
                boolean predefined = false;
                if (call.link.equals(TemplateConstants.LINK_NAME_INDEX) && call.arguments.size() == 0 && "".equals(call.getPrefix())) {
                    Integer index = (Integer) script.contextPeek(IScript.WHILE_INDEX);
                    if (index == null) {
                        index = new Integer(0);
                    }
                    current = new ENode(index.intValue(), current);
                    predefined = true;
                } else if (call.link.equals(TemplateConstants.LINK_NAME_ARGS) && call.arguments.size() == 1 && "".equals(call.getPrefix())) {
                    ENode[] templateArgs = (ENode[]) script.contextPeek(IScript.TEMPLATE_ARGS);
                    if (templateArgs == null) {
                        templateArgs = new ENode[] {};
                    }
                    ENode index = ((TemplateExpression) call.arguments.get(0)).evaluate(current, script, mode);
                    try {
                        int i = index.getInt();
                        if (i < templateArgs.length) {
                            current = templateArgs[i];
                            predefined = true;
                        } else {
                            throw new ENodeException(
                                    AcceleoGenMessages.getString("TemplateCallSetExpression.UnresolvedArgument", new Object[] { Integer.toString(i), }), call.getPos(), script, current, true); //$NON-NLS-1$
                        }
                    } catch (ENodeCastException e) {
                        throw new ENodeException(AcceleoGenMessages.getString("TemplateCallSetExpression.InvalidArgument"), call.getPos(), script, current, true); //$NON-NLS-1$
                    }
                } else if (call.link.equals(TemplateConstants.USER_BEGIN_NAME) && call.arguments.size() == 0) {
                    current = new ENode(MergeTools.DEFAULT_USER_BEGIN, current);
                    predefined = true;
                } else if (call.link.equals(TemplateConstants.USER_END_NAME) && call.arguments.size() == 0) {
                    current = new ENode(MergeTools.DEFAULT_USER_END, current);
                    predefined = true;
                }
                if (predefined) {
                    if (calls.hasNext()) {
                        call = (TemplateCallExpression) calls.next();
                    } else {
                        return current;
                    }
                }
                // Dynamic links
                ENode first = call.evaluate(current, script, mode);
                current = first;
                while (calls.hasNext()) {
                    call = (TemplateCallExpression) calls.next();
                    current = call.evaluate(current, script, mode);
                }
                // Put linked object
                if (!predefined) {
                    if (current.isString() && first.isEObject() && !first.isContainment()) {
                        try {
                            ENode node = new ENode("", first); //$NON-NLS-1$
                            if (node.getTextModelMapping() != null) {
                                node.getTextModelMapping().linkBegin(first.getEObject());
                            }
                            node.append(current);
                            if (node.getTextModelMapping() != null) {
                                node.getTextModelMapping().linkEnd();
                            }
                            current = node;
                        } catch (ENodeCastException e) {
                            // Never catch
                        }
                    }
                    if (first.isOptional()) {
                        current.setOptional(true);
                    }
                }
            }
            return current;
        } finally {
            script.contextPop(IScript.CURRENT_NODE);
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(""); //$NON-NLS-1$
        Iterator calls = this.calls.iterator();
        while (calls.hasNext()) {
            TemplateCallExpression call = (TemplateCallExpression) calls.next();
            buffer.append(call.toString());
            if (calls.hasNext()) {
                buffer.append(TemplateConstants.CALL_SEP);
            }
        }
        return buffer.toString();
    }

    /* (non-Javadoc) */
    public static TemplateExpression fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        Int2 trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
        if (trim.b() == -1) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
        } else {
            limits = trim;
        }
        Int2[] positions = TextSearch.getDefaultSearch().splitPositionsIn(buffer, limits.b(), limits.e(), new String[] { TemplateConstants.CALL_SEP }, false, TemplateConstants.SPEC,
                TemplateConstants.INHIBS_EXPRESSION);
        TemplateCallSetExpression expression = new TemplateCallSetExpression(script);
        expression.setPos(limits);
        for (Int2 pos : positions) {
            expression.addCall((TemplateCallExpression) TemplateCallExpression.fromString(buffer, pos, script));
        }
        return expression;
    }

    /**
     * Gets the root resolver for this link : EClassifier for the block
     * container like "for" and "script".
     * 
     * @param defaultRoot
     *            is the default container for the current template
     * @param gen
     *            is the script
     * @return the root resolver for this link
     */
    public Object getRootResolver(Object defaultRoot, IScript gen) {
        TemplateElement parent = getParent();
        TemplateElement current = this;
        boolean inForBlock = false;
        while (parent != null) {
            if (parent instanceof Template && parent.getParent() instanceof TemplateForStatement) {
                inForBlock = true;
            } else if (parent instanceof TemplateForStatement) {
                if (inForBlock) {
                    TemplateExpression forCondition = ((TemplateForStatement) parent).getCondition();
                    List testElements = forCondition.getAllElements(TemplateCallSetExpression.class);
                    if (testElements.size() > 0) {
                        TemplateCallSetExpression firstCallSet = (TemplateCallSetExpression) testElements.get(0);
                        if (firstCallSet != null) {
                            Object resolvedType = firstCallSet.getRootResolver(defaultRoot, gen);
                            Iterator calls = firstCallSet.iterator();
                            while (resolvedType != null && calls.hasNext()) {
                                TemplateCallExpression call = (TemplateCallExpression) calls.next();
                                resolvedType = gen.resolveType(resolvedType, call, 0);
                            }
                            return resolvedType;
                        }
                    }
                }
            } else if (parent instanceof TemplateCallExpression && ((TemplateCallExpression) parent).filter == current && parent.getParent() instanceof TemplateCallSetExpression) {
                TemplateCallSetExpression callSet = (TemplateCallSetExpression) parent.getParent();
                Object resolvedType = callSet.getRootResolver(defaultRoot, gen);
                Iterator calls = callSet.iterator();
                while (resolvedType != null && calls.hasNext()) {
                    TemplateCallExpression call = (TemplateCallExpression) calls.next();
                    resolvedType = gen.resolveType(resolvedType, call, 0);
                    if (call == parent) {
                        break;
                    }
                }
                return resolvedType;
            }
            current = parent;
            parent = parent.getParent();
        }
        return defaultRoot;
    }

}
