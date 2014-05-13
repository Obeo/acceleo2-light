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
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * This is a variable call expression for the generation tool. It is a kind of
 * expression. A variable call can be a link of the model or a java service.
 * Between the brackets after a java service name, there is a list of arguments.
 * Each argument is an expression. The argument list is a sequence of entries,
 * each separated from the next by a comma. Syntactically, a service call
 * expression is an open parenthesis followed by one or more expressions
 * followed by a close parenthesis. The remaining expressions are all evaluated
 * and their values are the arguments to the service.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateCallExpression extends TemplateExpression {

    /**
     * The called link that is a template, a link of the model or a java service
     * name.
     */
    protected String link;

    /**
     * The prefix to filter a template, a link of the model or a java service.
     */
    protected String prefix;

    /**
     * Between the parenthesis after a java service name or a template name,
     * there is a list of arguments. Each argument is an expression. The
     * remaining expressions are all evaluated and their values are the
     * arguments to the service.
     */
    protected List arguments = new ArrayList();

    /**
     * Between the brackets after a java service name or a template name, there
     * is a filter.
     */
    protected TemplateExpression filter = null;

    /**
     * It's the next variable call expression for the current statement.
     */
    protected TemplateCallExpression nextCall = null;

    /**
     * Constructor.
     * 
     * @param link
     *            is a link of the model or a java service name
     * @param script
     *            is the script
     */
    public TemplateCallExpression(String link, IScript script) {
        super(script);
        if (link != null) {
            final int iDot = link.indexOf(TemplateConstants.LINK_PREFIX_SEPARATOR);
            if (iDot > -1) {
                this.prefix = link.substring(0, iDot).trim();
                this.link = link.substring(iDot + TemplateConstants.LINK_PREFIX_SEPARATOR.length()).trim();
            } else {
                this.prefix = ""; //$NON-NLS-1$
                this.link = link.trim();
            }
        } else {
            this.prefix = ""; //$NON-NLS-1$
            this.link = ""; //$NON-NLS-1$
        }
    }

    /**
     * @return the called link
     */
    public String getLink() {
        return link;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the next variable call expression for the current statement
     */
    public TemplateCallExpression getNextCall() {
        return nextCall;
    }

    /**
     * @param next
     *            is the next variable call expression for the current statement
     */
    public void setNextCall(TemplateCallExpression nextCall) {
        this.nextCall = nextCall;
    }

    /**
     * @return the arguments
     */
    public List getArguments() {
        return arguments;
    }

    /**
     * @return the filter
     */
    public TemplateExpression getFilter() {
        return filter;
    }

    /**
     * @param filter
     *            is the filter
     */
    public void setFilter(TemplateExpression filter) {
        this.filter = filter;
        this.filter.setParent(this);
    }

    /**
     * Adds an argument expression for the service.
     * 
     * @param expression
     *            is the new argument
     */
    public void addArgument(TemplateExpression expression) {
        arguments.add(expression);
        expression.setParent(this);
    }

    /**
     * Counts the arguments.
     * 
     * @return the number of arguments
     */
    public int countArguments() {
        return arguments.size();
    }

    /**
     * Gets the first argument.
     * 
     * @return the first argument
     */
    public TemplateExpression getFirstArgument() {
        if (arguments.size() > 0) {
            return (TemplateExpression) arguments.get(0);
        } else {
            return null;
        }
    }

    /**
     * Now, the next evaluation will be ignored.
     */
    public void ignoreNextEval() {
        ignoreNextEval = true;
    }

    private boolean ignoreNextEval = false;

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException {
        ENode result = evaluateSub(current, script, mode);
        if (filter != null) {
            result = select(result);
        }
        return result;
    }

    private ENode evaluateSub(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException {
        // Ignore?
        if (ignoreNextEval) {
            ignoreNextEval = false;
            return current;
        } else {
            ENode argCurrent = (ENode) script.contextPeek(IScript.CURRENT_NODE);
            if (argCurrent == null) {
                argCurrent = current;
            }
            if (link.length() == 0) {
                if (arguments.size() == 1) {
                    final TemplateExpression arg = (TemplateExpression) arguments.get(0);
                    return arg.evaluate(argCurrent, script, mode);
                } else if (arguments.size() > 1) {
                    final ENodeList result = new ENodeList();
                    final Iterator arguments = this.arguments.iterator();
                    while (arguments.hasNext()) {
                        final TemplateExpression arg = (TemplateExpression) arguments.next();
                        final ENode argEval = arg.evaluate(argCurrent, script, mode);
                        result.add(argEval);
                    }
                    return new ENode(result, current);
                } else {
                    return new ENode(ENode.EMPTY, current);
                }
            } else {
                // Arguments
                final List argList = new ArrayList();
                final Iterator arguments = this.arguments.iterator();
                while (arguments.hasNext()) {
                    final TemplateExpression arg = (TemplateExpression) arguments.next();
                    final ENode argEval = arg.evaluate(argCurrent, script, mode);
                    argList.add(argEval);
                }
                // Call the script or the service
                final ENode[] args = (ENode[]) argList.toArray(new ENode[argList.size()]);
                // Specific strategy
                ENode result = null;
                final IScript[] specifics = script.goToSpecifics();
                if (specifics != null && specifics.length > 0) {
                    ENodeException.disableRuntimeMarkersFor(this);
                    try {
                        for (int i = 0; result == null && i < specifics.length; i++) {
                            final IScript specific = specifics[i];
                            try {
                                result = specific.eGet(this, current, args, mode, false);
                            } catch (final ENodeException e) {
                                // result = null;
                            }
                        }
                    } finally {
                        ENodeException.enableRuntimeMarkersFor(this);
                    }
                }
                if (result == null) {
                    result = script.eGet(this, current, args, mode, true);
                }
                if (result == null) {
                    throw new ENodeException(AcceleoGenMessages.getString("TemplateCallExpression.UnresolvedCall", new Object[] { getLink(), }), pos, script, current, true); //$NON-NLS-1$
                }
                return result;
            }
        }
    }

    private ENode select(ENode current) throws FactoryException {
        // ASSERT filter != null
        if (current.isList()) {
            final ENodeList res = new ENodeList();
            try {
                final ENodeList list = current.getList();
                final ENodeIterator it = list.iterator();
                while (it.hasNext()) {
                    res.add(select(it.next()));
                }
            } catch (final ENodeCastException e) {
                // Never catch
            }
            return new ENode(res, current);
        } else {
            script.contextPush(IScript.CURRENT_NODE, current);
            script.contextPush(IScript.TEMPLATE_NODE, current);
            try {
                final ENode result = filter.evaluate(current, script, LaunchManager.create("run", true)); //$NON-NLS-1$
                final Object boolValue = result.getAdapterValue(boolean.class);
                if (boolValue instanceof Boolean && ((Boolean) boolValue).booleanValue()) {
                    return current;
                } else {
                    return new ENode(ENode.EMPTY, current);
                }
            } catch (final ENodeException e) {
                return new ENode(ENode.EMPTY, current);
            } catch (final ENodeCastException e) {
                return new ENode(ENode.EMPTY, current);
            } finally {
                script.contextPop(IScript.CURRENT_NODE);
                script.contextPop(IScript.TEMPLATE_NODE);
            }
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        if (prefix != null && prefix.length() > 0) {
            buffer.append(prefix);
            buffer.append(TemplateConstants.LINK_PREFIX_SEPARATOR);
        }
        buffer.append(link);
        if (this.arguments.size() > 0) {
            buffer.append(TemplateConstants.PARENTH[0]);
            final Iterator arguments = this.arguments.iterator();
            while (arguments.hasNext()) {
                final TemplateExpression argument = (TemplateExpression) arguments.next();
                buffer.append(argument.toString());
                if (arguments.hasNext()) {
                    buffer.append(TemplateConstants.ARG_SEP);
                }
            }
            buffer.append(TemplateConstants.PARENTH[1]);
        }
        if (this.filter != null) {
            buffer.append(TemplateConstants.BRACKETS[0]);
            buffer.append(this.filter.toString());
            buffer.append(TemplateConstants.BRACKETS[1]);
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
        TemplateExpression filter = null;
        Int2 begin = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.BRACKETS[0], limits.b(), limits.e(), TemplateConstants.SPEC,
                new String[][] { TemplateConstants.LITERAL, TemplateConstants.PARENTH });
        if (begin.b() > -1) {
            final Int2 end = TextSearch.getDefaultSearch().blockIndexEndIn(buffer, TemplateConstants.BRACKETS[0], TemplateConstants.BRACKETS[1], begin.b(), limits.e(), false, TemplateConstants.SPEC,
                    TemplateConstants.INHIBS_EXPRESSION);
            if (end.b() == -1) {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingCloseBracket"), script, begin.b()); //$NON-NLS-1$
            }
            if (buffer.substring(end.e(), limits.e()).trim().length() > 0) {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidSequence"), script, end.e()); //$NON-NLS-1$
            }
            final Int2 pos = new Int2(begin.e(), end.b());
            filter = TemplateExpression.fromString(buffer, pos, script);
            filter.setPos(pos);
            limits = new Int2(limits.b(), begin.b());
            trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
            if (trim.b() == -1) {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
            } else {
                limits = trim;
            }
        }
        TemplateCallExpression expression;
        begin = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.PARENTH[0], limits.b(), limits.e(), TemplateConstants.SPEC, new String[][] { TemplateConstants.LITERAL });
        if (begin.b() > -1) {
            final Int2 end = TextSearch.getDefaultSearch().blockIndexEndIn(buffer, TemplateConstants.PARENTH[0], TemplateConstants.PARENTH[1], begin.b(), limits.e(), false, TemplateConstants.SPEC,
                    TemplateConstants.INHIBS_EXPRESSION);
            if (end.b() == -1) {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingCloseParenthesis"), script, begin.b()); //$NON-NLS-1$
            }
            if (buffer.substring(end.e(), limits.e()).trim().length() > 0) {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidSequence"), script, end.e()); //$NON-NLS-1$
            }
            final Int2[] positions = TextSearch.getDefaultSearch().splitPositionsIn(buffer, begin.e(), end.b(), new String[] { TemplateConstants.ARG_SEP }, false, TemplateConstants.SPEC,
                    TemplateConstants.INHIBS_EXPRESSION);
            expression = new TemplateCallExpression(buffer.substring(limits.b(), begin.b()), script);
            expression.setPos(new Int2(limits.b(), begin.b()));
            for (final Int2 pos : positions) {
                expression.addArgument(TemplateExpression.fromString(buffer, pos, script));
            }
        } else {
            expression = new TemplateCallExpression(buffer.substring(limits.b(), limits.e()), script);
            expression.setPos(new Int2(limits.b(), limits.e()));
        }
        expression.setPos(limits);
        if (filter != null) {
            expression.setFilter(filter);
        }
        return expression;
    }

}
