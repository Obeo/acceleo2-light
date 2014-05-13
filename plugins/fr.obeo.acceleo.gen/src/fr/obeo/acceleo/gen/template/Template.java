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

package fr.obeo.acceleo.gen.template;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.EFactory;
import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.TextModelMapping;
import fr.obeo.acceleo.gen.template.expressions.TemplateExpression;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.scripts.ScriptDescriptor;
import fr.obeo.acceleo.gen.template.statements.TemplateCommentStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateFeatureStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateForStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateIfStatement;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * This is a template for the generation tool. This class provides services such
 * as :
 * <ul>
 * <li>Simple template initialization methods.
 * <li>Functions to apply the template to model's objects to allow dynamic
 * generation.
 * </ul>
 * 
 * @author www.obeo.fr
 * 
 */
public class Template extends TemplateNodeElement {

    /**
     * Empty template reference.
     */
    public static final Template EMPTY = new Template(null);

    /**
     * The descriptor of this template.
     */
    protected ScriptDescriptor descriptor = null;

    /**
     * The post expression.
     */
    protected TemplateExpression postExpression = null;

    /**
     * Indicates if the last evaluation returned an empty node.
     */
    protected boolean emptyEvaluation = true;

    /**
     * Nodes of the template.
     */
    protected List elements = new ArrayList();

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public Template(IScript script) {
        super(script);
    }

    /**
     * @return the descriptor of this template
     */
    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * @param descriptor
     *            is the descriptor of this template
     */
    public void setDescriptor(ScriptDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * @return the post expression
     */
    public TemplateExpression getPostExpression() {
        return postExpression;
    }

    /**
     * @param postExpression
     *            is the post expression
     */
    public void setPostExpression(TemplateExpression postExpression) {
        this.postExpression = postExpression;
    }

    /**
     * Indicates if the last evaluation returned an empty node.
     * 
     * @return true if the last evaluation returned an empty node
     */
    public boolean isEmptyEvaluation() {
        return emptyEvaluation;
    }

    /**
     * It saves template's state.
     * 
     * @return the template's state
     */
    public Object copyState() {
        return new ArrayList(elements);
    }

    /**
     * It restores template's state.
     * 
     * @param state
     *            is the template's state to be restored
     */
    public void fromState(Object state) {
        elements = (List) state;
    }

    /**
     * Removes all of the elements from this template.
     */
    public void clear() {
        elements.clear();
    }

    /**
     * Appends the specified element to the end of this template.
     * 
     * @param element
     *            is the element to be added
     */
    public void append(TemplateNodeElement element) {
        if (element instanceof TemplateText) {
            if (((TemplateText) element).text.length() > 0) {
                elements.add(element);
                element.setParent(this);
            }
        } else if (element != null) {
            elements.add(element);
            element.setParent(this);
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        final StringBuffer text = new StringBuffer(""); //$NON-NLS-1$
        final Iterator it = elements.iterator();
        while (it.hasNext()) {
            final TemplateNodeElement element = (TemplateNodeElement) it.next();
            text.append(element.toString());
        }
        return text.toString();
    }

    /* (non-Javadoc) */
    @Override
    public String getOutlineText() {
        return ""; //$NON-NLS-1$
    }

    /**
     * It creates a dynamic template for the given text.
     * 
     * @param buffer
     *            is the textual representation of the new template
     * @param script
     *            is the generator's configuration used to apply the new
     *            template to model's objects
     * @param object
     *            is the object to select when an error is detected
     * @return the new template
     * @throws ENodeException
     */
    public static Template from(String buffer, IScript script, EObject object) throws ENodeException {
        try {
            return Template.read(buffer, script);
        } catch (final TemplateSyntaxException e) {
            throw new ENodeException(e.getMessage(), e.getPos(), script, object, true);
        }
    }

    /**
     * It checks the syntax of the new template text and creates a template for
     * the given text.
     * 
     * @param buffer
     *            is the textual representation of the new template
     * @param script
     *            is the generator's configuration used to apply the new
     *            template to model's objects
     * @return the new template
     * @throws TemplateSyntaxException
     */
    private static Template read(String buffer, IScript script) throws TemplateSyntaxException {
        return Template.read(buffer, new Int2(0, buffer.length()), script);
    }

    /**
     * It checks the syntax and creates a template for the given part of the
     * text. The part of the text to be parsed is delimited by the given limits.
     * 
     * @param buffer
     *            is the textual representation of the script that contains
     *            templates
     * @param limits
     *            delimits the part of the text to be parsed for this template
     * @param script
     *            is the generator's configuration used to apply the new
     *            template to model's objects
     * @return the new template
     * @throws TemplateSyntaxException
     */
    public static Template read(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        Template template = (Template) Template.readPreferred("read", buffer, limits, script); //$NON-NLS-1$
        if (template == null) {
            template = new Template(script);
            template.setPos(limits);
            if (buffer != null) {
                final int[] pos = new int[4];
                for (int i = 0; i < pos.length; i++) {
                    pos[i] = -2;
                }
                int i = limits.b();
                while (i < limits.e()) {
                    if (pos[0] != -1 && i > pos[0]) {
                        pos[0] = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.COMMENT_BEGIN, i, limits.e()).b();
                    }
                    if (pos[1] != -1 && i > pos[1]) {
                        pos[1] = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.IF_BEGIN, i, limits.e()).b();
                    }
                    if (pos[2] != -1 && i > pos[2]) {
                        pos[2] = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.FOR_BEGIN, i, limits.e()).b();
                    }
                    if (pos[3] != -1 && i > pos[3]) {
                        pos[3] = TextSearch.getDefaultSearch().indexIn(buffer, TemplateConstants.FEATURE_BEGIN, i, limits.e()).b();
                    }
                    final int iTab = Template.indexOfMin(pos);
                    if (iTab == 0) {
                        i = Template.readStatement(buffer, TemplateConstants.COMMENT_BEGIN, TemplateConstants.COMMENT_END, new Int2(i, limits.e()), template, script);
                    } else if (iTab == 1) {
                        i = Template.readStatement(buffer, TemplateConstants.IF_BEGIN, TemplateConstants.IF_END, new Int2(i, limits.e()), template, script);
                    } else if (iTab == 2) {
                        i = Template.readStatement(buffer, TemplateConstants.FOR_BEGIN, TemplateConstants.FOR_END, new Int2(i, limits.e()), template, script);
                    } else if (iTab == 3) {
                        i = Template.readStatement(buffer, TemplateConstants.FEATURE_BEGIN, TemplateConstants.FEATURE_END, new Int2(i, limits.e()), template, script);
                    } else { // -1
                        final Int2 posText = new Int2(i, limits.e());
                        template.append(Template.readMiddleElement(buffer, posText, script));
                        i = limits.e();
                    }
                }
            }
        }
        return template;
    }

    /**
     * It checks the syntax and creates a "middle" template element for the
     * given part of the text. The part of the text to be parsed is delimited by
     * the given limits. A middle template element is a textual representation
     * that doesn't contain "<%...%>".
     * 
     * @param buffer
     *            is the textual representation of the script that contains
     *            templates
     * @param limits
     *            delimits the part of the text to be parsed for this "middle"
     *            template element
     * @param script
     *            is the generator's configuration used to apply the new
     *            template element to model's objects
     * @return the new "middle" template element
     * @throws TemplateSyntaxException
     */
    public static TemplateNodeElement readMiddleElement(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        TemplateNodeElement element = Template.readPreferred("readMiddleElement", buffer, limits, script); //$NON-NLS-1$
        if (element == null) {
            element = new TemplateText(buffer.substring(limits.b(), limits.e()), script);
            element.setPos(limits);
        }
        return element;
    }

    /**
     * It checks the syntax and creates a template element for the given part of
     * the text. The part of the text to be parsed is delimited by the given
     * limits. It tries to call a static method on the preferred template
     * before. The static attribute 'preferredTemplate' has to be put before.
     * 
     * @param method
     *            is the method name
     * @param buffer
     *            is the textual representation of the script that contains
     *            templates
     * @param limits
     *            delimits the part of the text to be parsed
     * @param script
     *            is the generator's configuration used to apply the new
     *            template element to model's objects
     * @return the new template element
     * @throws TemplateSyntaxException
     */
    private static TemplateNodeElement readPreferred(String method, String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        if (Template.preferredTemplate != Template.class) {
            try {
                return (TemplateNodeElement) Template.preferredTemplate.getMethod(method, new Class[] { String.class, Int2.class, IScript.class }).invoke(Template.preferredTemplate,
                        new Object[] { buffer, limits, script });
            } catch (final IllegalArgumentException e) {
                AcceleoEcoreGenPlugin.getDefault().log(e, true);
            } catch (final SecurityException e) {
                AcceleoEcoreGenPlugin.getDefault().log(e, true);
            } catch (final IllegalAccessException e) {
                AcceleoEcoreGenPlugin.getDefault().log(e, true);
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof TemplateSyntaxException) {
                    throw (TemplateSyntaxException) e.getTargetException();
                } else {
                    AcceleoEcoreGenPlugin.getDefault().log(e, true);
                }
            } catch (final NoSuchMethodException e) {
                AcceleoEcoreGenPlugin.getDefault().log(e, true);
            }
        }
        return null;
    }

    /**
     * The preferred template is the class to use to create the templates in the
     * sub statements.
     */
    protected static Class preferredTemplate = Template.class;

    /**
     * Gets the index of the minimum integer in the given table.
     * 
     * @param pos
     *            is the table
     * @return the index of the minimum integer
     */
    protected static int indexOfMin(int[] pos) {
        int index = -1;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < pos.length; i++) {
            if (pos[i] > -1 && pos[i] < min) {
                index = i;
                min = pos[i];
            }
        }
        return index;
    }

    /**
     * Reads the statements in the buffer, starting at a specified position, and
     * returns the ending index.
     * 
     * @param buffer
     *            is the textual representation of the script that contains
     *            templates
     * @param tagBegin
     *            is the begin tag of the statement
     * @param tagEnd
     *            is the end tag of the statement
     * @param limits
     *            delimits the part of the text to be parsed
     * @param template
     *            is the current template
     * @param script
     *            is the generator's configuration used to apply the new
     *            statement to model's objects
     * @return the ending index, or -1
     * @throws TemplateSyntaxException
     */
    protected static int readStatement(String buffer, String tagBegin, String tagEnd, Int2 limits, Template template, IScript script) throws TemplateSyntaxException {
        Int2 end = Int2.NOT_FOUND;
        final Int2 begin = TextSearch.getDefaultSearch().indexIn(buffer, tagBegin, limits.b(), limits.e());
        // ASSERT (begin.b > -1)
        if (tagBegin == TemplateConstants.COMMENT_BEGIN) {
            end = TextSearch.getDefaultSearch().blockIndexEndIn(buffer, tagBegin, tagEnd, begin.b(), limits.e(), false);
        } else {
            end = TextSearch.getDefaultSearch().blockIndexEndIn(buffer, tagBegin, tagEnd, begin.b(), limits.e(), true, null, TemplateConstants.INHIBS_STATEMENT);
        }
        if (end.b() > -1) {
            final boolean untab = tagBegin != TemplateConstants.FEATURE_BEGIN && Template.isFirstSignificantOfLine(buffer, begin.b()) && Template.isLastSignificantOfLine(buffer, end.e());
            if (begin.b() > limits.b()) {
                int iEndText = begin.b();
                if (untab) {
                    // Delete (\s|\t)* before <%if..., <%for...
                    int iPrevLine = TextSearch.getDefaultSearch().lastIndexIn(buffer, "\n", limits.b(), begin.b()).e(); //$NON-NLS-1$
                    if (iPrevLine == -1) {
                        iPrevLine = limits.b();
                    }
                    if (buffer.substring(iPrevLine, begin.b()).trim().length() == 0) {
                        iEndText = iPrevLine;
                    }
                }
                final Int2 posText = new Int2(limits.b(), iEndText);
                template.append(Template.readMiddleElement(buffer, posText, script));
            }
            if (tagBegin == TemplateConstants.IF_BEGIN) {
                template.append(TemplateIfStatement.fromString(buffer, new Int2(begin.e(), end.b()), script));
            }
            if (tagBegin == TemplateConstants.FOR_BEGIN) {
                template.append(TemplateForStatement.fromString(buffer, new Int2(begin.e(), end.b()), script));
            }
            if (tagBegin == TemplateConstants.FEATURE_BEGIN) {
                template.append(TemplateFeatureStatement.fromString(buffer, new Int2(begin.e(), end.b()), script));
            }
            if (tagBegin == TemplateConstants.COMMENT_BEGIN) {
                template.append(TemplateCommentStatement.fromString(buffer, new Int2(begin.e(), end.b()), script));
            }
            if (untab) {
                // Delete (\s|\t)*\n after <%}%>
                int iNextLine = TextSearch.getDefaultSearch().indexIn(buffer, "\n", end.e(), limits.e()).e(); //$NON-NLS-1$
                if (iNextLine == -1) {
                    iNextLine = limits.e();
                }
                if (buffer.substring(end.e(), iNextLine).trim().length() == 0) {
                    return iNextLine;
                }
            }
            return end.e();
        } else {
            template.append(Template.readMiddleElement(buffer, limits, script));
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("Template.UnclosedTag", new Object[] { tagEnd, tagBegin, }), script, new Int2(begin.b(), limits.e())); //$NON-NLS-1$
        }
    }

    private static boolean isFirstSignificantOfLine(String buffer, int index) {
        if (index > 0 && index < buffer.length()) {
            index--;
            while (index >= 0) {
                final char c = buffer.charAt(index);
                if (c == '\n') {
                    return true;
                } else if (c == ' ' || c == '\t' || c == '\r') {
                    index--;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isLastSignificantOfLine(String buffer, int index) {
        if (index >= 0) {
            while (index < buffer.length()) {
                final char c = buffer.charAt(index);
                if (c == '\n') {
                    return true;
                } else if (c == ' ' || c == '\t' || c == '\r') {
                    index++;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Formats the text of a template in a script.
     * 
     * @param buffer
     *            is the full text of a script
     * @param limits
     *            delimits the part of the text to parse for this template
     * @param nbReturn
     *            is the number of return character to ignore at the end of the
     *            template
     * @return is the new limits that delimits the part of the text to parse for
     *         this template
     */
    public static Int2 formatTemplate(String buffer, Int2 limits, int nbReturn) {
        if (limits.b() == -1 || limits.b() > limits.e()) {
            return Int2.NOT_FOUND;
        }
        final String text = buffer.substring(limits.b(), limits.e());
        // Search first valid char
        int posBegin = text.indexOf("\n"); //$NON-NLS-1$
        if (posBegin == -1 || text.substring(0, posBegin).trim().length() > 0) {
            posBegin = 0;
        } else {
            posBegin++;
        }
        // Search last valid char
        if (text.length() > 0) {
            int n = 0;
            int endLine = text.length();
            int posEnd = text.length() - 1;
            boolean stop = false;
            while (!stop) {
                final char c = text.charAt(posEnd);
                if (nbReturn == 0 && c == '\n') {
                    posEnd++;
                    stop = true;
                } else if (c == '\n' && n < nbReturn) {
                    n++;
                    endLine = posEnd;
                } else if (c == '\r') {
                    endLine = posEnd;
                } else if (c == ' ' || c == '\t') {
                } else {
                    // Keep space at end
                    posEnd = endLine;
                    stop = true;
                }
                if (!stop) {
                    if (posEnd > posBegin) {
                        posEnd--;
                    } else {
                        if (nbReturn != 0) {
                            posEnd = endLine;
                        }
                        stop = true;
                    }
                }
            }
            if (posEnd >= posBegin) {
                boolean formatWithComment = false;
                String sub = text.substring(posBegin, posEnd);
                if (sub.startsWith(TemplateConstants.COMMENT_BEGIN) && sub.indexOf(TemplateConstants.COMMENT_END) > -1) {
                    posBegin = text.indexOf(TemplateConstants.COMMENT_END, posBegin) + TemplateConstants.COMMENT_END.length();
                    formatWithComment = true;
                    if (posEnd > posBegin) {
                        sub = sub.substring(posBegin);
                    } else {
                        sub = ""; //$NON-NLS-1$
                    }
                }
                if (sub.endsWith(TemplateConstants.COMMENT_END) && sub.lastIndexOf(TemplateConstants.COMMENT_BEGIN, posEnd) > -1) {
                    posEnd = text.lastIndexOf(TemplateConstants.COMMENT_BEGIN, posEnd);
                    formatWithComment = true;
                }
                if (formatWithComment) {
                    return Template.formatTemplate(buffer, new Int2(limits.b() + posBegin, limits.b() + posEnd), nbReturn);
                } else {
                    return new Int2(limits.b() + posBegin, limits.b() + posEnd);
                }
            }
        }
        return limits;
    }

    /**
     * Evaluates this template on an EObject, and converts the result to a
     * string.
     * 
     * @param object
     *            is the model's object
     * @param mode
     *            is the mode in which to launch, one of the mode constants
     *            defined - RUN_MODE or DEBUG_MODE
     * @return the textual representation of the EObject for this template
     * @throws ENodeException
     * @throws FactoryException
     */
    public String evaluateAsString(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        final String text = evaluate(object, mode).asString();
        return text;
    }

    /**
     * Evaluates this template on an EObject. The result node of generation is
     * an ENode. The given comment is generated before the object if it isn't
     * null or empty.
     * 
     * @param object
     *            is the model's object
     * @param comment
     *            that is generated before the object if it isn't null or empty
     * @param mode
     *            is the mode in which to launch, one of the mode constants
     *            defined - RUN_MODE or DEBUG_MODE
     * @return the result node of generation
     * @throws ENodeException
     * @throws FactoryException
     * @see ENode
     */
    public ENode evaluateWithComment(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        final ENode node = new ENode(ENode.EMPTY, object, this, mode.isSynchronize());
        String comment = eGetAsString(object, "comment"); //$NON-NLS-1$
        if (comment.length() > 0) {
            node.append(comment, TextModelMapping.HIGHLIGHTED_COMMENT);
        }
        node.append(evaluate(object, mode));
        comment = eGetAsString(object, "endLineComment"); //$NON-NLS-1$
        if (comment.length() > 0) {
            node.append(comment, TextModelMapping.HIGHLIGHTED_COMMENT);
        }
        return node;
    }

    private String eGetAsString(EObject object, String feature) {
        try {
            String result = EFactory.eGetAsString(object, feature);
            if (result == null) {
                result = ""; //$NON-NLS-1$
            }
            return result;
        } catch (final FactoryException e) {
            return ""; //$NON-NLS-1$
        }
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        if (mode.getMonitor() != null && mode.getMonitor().isCanceled()) {
            throw new OperationCanceledException();
        }

        ENode node = new ENode(ENode.EMPTY, object, this, mode.isSynchronize());
        boolean parentIsStatement = getParent() instanceof TemplateForStatement || getParent() instanceof TemplateIfStatement;
        if (!parentIsStatement && currentEval.contains(object)) {
            emptyEvaluation = true;
            throw new ENodeException(AcceleoGenMessages.getString("Template.RecursiveCall", new Object[] { toString(), }), pos, script, object, true); //$NON-NLS-1$
        } else {
            if (script != null) {
                final ENode tmp = new ENode(object, this, mode.isSynchronize());
                script.contextPush(IScript.CURRENT_NODE, tmp);
                script.contextPush(IScript.TEMPLATE_NODE, tmp);
            }
            try {
                if (!parentIsStatement) {
                    currentEval.add(object);
                }
                TemplateNodeElement precedingElement = null;
                ENode precedingChild = null;
                final Iterator elements = this.elements.iterator();
                while (elements.hasNext()) {
                    final TemplateNodeElement element = (TemplateNodeElement) elements.next();
                    try {
                        if (!elements.hasNext() && element instanceof TemplateText && (node.isEObject() || node.isList() && node.getList().size() > 0 && node.getList().get(0).isEObject())
                                && ((TemplateText) element).getText().trim().length() == 0) {
                            break;
                        }
                    } catch (final ENodeCastException e) {
                        // never catch
                    }
                    final ENode child = element.evaluate(object, mode);
                    try {
                        if (element.getScript() != null && element.getScript().getFile() != null && element.getScript().getFile().getName().endsWith(".mt") //$NON-NLS-1$
                                && element instanceof TemplateFeatureStatement && precedingElement instanceof TemplateText && precedingChild.isString() && precedingChild.getString().length() > 0) {
                            final String text = child.asString();
                            if (text.indexOf("\n") > -1) { //$NON-NLS-1$
                                final String precedingText = precedingChild.getString();
                                final int iLine = precedingText.lastIndexOf("\n"); //$NON-NLS-1$
                                if (iLine > -1 && iLine + 1 < precedingText.length()) {
                                    final String indent = precedingText.substring(iLine + 1);
                                    if (indent.trim().length() == 0) {
                                        child.stringCall("internalIndent:" + indent, 0, 0); //$NON-NLS-1$
                                    }
                                }
                            }
                        }
                    } catch (final ENodeCastException e) {
                        // never catch
                    }
                    node.append(child);
                    precedingElement = element;
                    precedingChild = child;
                }
            } finally {
                if (script != null) {
                    script.contextPop(IScript.CURRENT_NODE);
                    script.contextPop(IScript.TEMPLATE_NODE);
                }
                if (!parentIsStatement) {
                    currentEval.remove(object);
                }
            }
        }
        if (postExpression != null) {
            node = postExpression.evaluate(node, script, mode);
        }
        emptyEvaluation = node.size() == 0;
        return node;
    }

    /**
     * The current evaluations.
     */
    protected List currentEval = new ArrayList();

    /**
     * Tells whether or not this template contains the given text, ignoring case
     * or not.
     * 
     * @param text
     *            is the string to be searched
     * @param ignoreCase
     *            indicates if case differences have been eliminated
     * @return true if template contains the given text
     */
    public boolean containsText(String text, boolean ignoreCase) {
        if (ignoreCase) {
            return toString().toUpperCase().indexOf(text.toUpperCase()) > -1;
        } else {
            return toString().indexOf(text) > -1;
        }
    }

    /* (non-Javadoc) */
    @Override
    public int hashCode() {
        if (pos.b() >= 1) {
            return pos.b();
        } else {
            return super.hashCode();
        }
    }

    /**
     * Gets the significant statements of this template : it ignores comment and
     * simple text.
     * 
     * @return the significant statements of this template.
     */
    public List getSignificantStatements() {
        final List result = new ArrayList();
        final Iterator it = children.iterator();
        while (it.hasNext()) {
            final TemplateElement element = (TemplateElement) it.next();
            if (element instanceof TemplateNodeElement && !(element instanceof TemplateCommentStatement) && !(element instanceof TemplateText)) {
                result.add(element);
            }
        }
        return result;
    }

}
