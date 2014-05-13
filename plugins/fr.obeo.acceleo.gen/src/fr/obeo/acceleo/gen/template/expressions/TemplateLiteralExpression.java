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

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.ecore.tools.ETools;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.tools.format.Conventions;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * The simplest expression is a literal expression. Literal expressions are also
 * known as self-evaluating data, because the value of a literal expression is
 * the corresponding datum. Strings <b>"</b>...<b>"</b>, numbers
 * <b>0</b>..<b>*</b>, <b>null</b>, and booleans <b>true</b> or <b>false</b> are
 * all valid literal expressions. However, for strings, the first and the last
 * '"' are ignored to create the real value.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateLiteralExpression extends TemplateExpression {

    /**
     * Boolean literal : true.
     */
    protected static final String LITERAL_TRUE = "true"; //$NON-NLS-1$

    /**
     * Boolean literal : false.
     */
    protected static final String LITERAL_FALSE = "false"; //$NON-NLS-1$

    /**
     * Langage literal : null.
     */
    protected static final String LITERAL_NULL = "null"; //$NON-NLS-1$

    /**
     * The value of the litteral expression. It's a String, a Boolean, an
     * Integer, or null.
     */
    protected Object value;

    /**
     * Constructor.
     * 
     * @param value
     *            is the string value of the expression
     * @param script
     *            is the script
     */
    public TemplateLiteralExpression(String value, IScript script) {
        super(script);
        this.value = value;
    }

    /**
     * Constructor.
     * 
     * @param value
     *            is the boolean value of the expression
     * @param script
     *            is the script
     */
    public TemplateLiteralExpression(boolean value, IScript script) {
        super(script);
        this.value = new Boolean(value);
    }

    /**
     * Constructor.
     * 
     * @param value
     *            is the numeric value of the expression
     * @param script
     *            is the script
     */
    public TemplateLiteralExpression(int value, IScript script) {
        super(script);
        this.value = new Integer(value);
    }

    /**
     * Constructor.
     * 
     * @param value
     *            is the double value of the expression
     * @param script
     *            is the script
     */
    public TemplateLiteralExpression(double value, IScript script) {
        super(script);
        this.value = new Double(value);
    }

    /**
     * Constructor. The value is null.
     * 
     * @param script
     *            is the script
     */
    public TemplateLiteralExpression(IScript script) {
        super(script);
        this.value = null;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(ENode current, IScript script, LaunchManager mode) throws ENodeException, FactoryException {
        ENode result;
        if (value == null) {
            result = new ENode(ENode.EMPTY, current);
        } else if (value instanceof String) {
            result = new ENode((String) value, current);
        } else if (value instanceof Boolean) {
            result = new ENode(((Boolean) value).booleanValue(), current);
        } else if (value instanceof Integer) {
            result = new ENode(((Integer) value).intValue(), current);
        } else if (value instanceof Double) {
            result = new ENode(((Double) value).doubleValue(), current);
        } else {
            result = new ENode(ENode.EMPTY, current);
        }
        return result;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        if (value == null) {
            return TemplateLiteralExpression.LITERAL_NULL;
        } else if (value instanceof String) {
            return TemplateConstants.LITERAL[0] + Conventions.formatString((String) value) + TemplateConstants.LITERAL[1];
        } else {
            return value.toString();
        }
    }

    /**
     * Resolves this literal as an EClassifier link.
     * 
     * @return an EClassifier or null if not found
     */
    public EClassifier resolveAsEClassifier() {
        if (value != null && value instanceof String && script != null && script instanceof SpecificScript && ((SpecificScript) script).getMetamodel() != null) {
            String id = (String) value;
            EClassifier result = ETools.getEClassifier(((SpecificScript) script).getMetamodel(), id);
            if (result == null) {
                result = ETools.getEClassifier(EcorePackage.eINSTANCE, id);
            }
            return result;
        } else {
            return null;
        }
    }

    /* (non-Javadoc) */
    public static TemplateExpression fromString(String buffer, Int2 limits, IScript script) throws TemplateSyntaxException {
        Int2 trim = TextSearch.getDefaultSearch().trim(buffer, limits.b(), limits.e());
        if (trim.b() == -1) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingElement"), script, limits); //$NON-NLS-1$
        } else {
            limits = trim;
        }
        TemplateExpression expression;
        String text = buffer.substring(limits.b(), limits.e());
        if (text.length() >= TemplateConstants.LITERAL[0].length() + TemplateConstants.LITERAL[1].length() && text.startsWith(TemplateConstants.LITERAL[0])) {
            if (!text.endsWith(TemplateConstants.LITERAL[1])) {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidLitteral"), script, limits); //$NON-NLS-1$
            }
            String value = text.substring(TemplateConstants.LITERAL[0].length(), text.length() - TemplateConstants.LITERAL[1].length());
            expression = new TemplateLiteralExpression(Conventions.unformatString(value), script);
        } else {
            if (text.equals(TemplateLiteralExpression.LITERAL_TRUE)) {
                expression = new TemplateLiteralExpression(true, script);
            } else if (text.equals(TemplateLiteralExpression.LITERAL_FALSE)) {
                expression = new TemplateLiteralExpression(false, script);
            } else if (text.equals(TemplateLiteralExpression.LITERAL_NULL)) {
                expression = new TemplateLiteralExpression(script);
                /*
                 * If first char is a digit, then it might be an integer or
                 * double.
                 */
            } else if (text.length() > 0 && (text.charAt(0) == '-' || Character.isDigit(text.charAt(0)))) {
                if (text.indexOf(".") > -1) { //$NON-NLS-1$
                    try {
                        double value = Double.parseDouble(text);
                        expression = new TemplateLiteralExpression(value, script);
                    } catch (NumberFormatException e) {
                        expression = null;
                    }
                } else {
                    try {
                        int value = Integer.parseInt(text);
                        expression = new TemplateLiteralExpression(value, script);
                    } catch (NumberFormatException e) {
                        expression = null;
                    }
                }
            } else {
                expression = null;
            }
        }
        if (expression != null) {
            expression.setPos(limits);
        }
        return expression;
    }
}
