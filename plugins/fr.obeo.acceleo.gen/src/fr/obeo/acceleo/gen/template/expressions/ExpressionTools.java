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

import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * Expression evaluation tools. It evaluates ENode expressions.
 * <p>
 * Samples : || , && , == , != , + , - , / , * , > , >= , < , <=
 * 
 * @author www.obeo.fr
 * @see ENode
 */
public class ExpressionTools {

    /**
     * Evaluates an expression '||' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode or(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isList()) {
            ENodeList res = new ENodeList();
            if (e2.isList()) {
                res.addAll(e1.getList());
                res.addAll(e2.getList());
            } else {
                res.addAll(e1.getList());
                res.add(e1);
            }
            return new ENode(res, e1);
        } else if (e1.isBoolean() && e2.isBoolean()) {
            return new ENode(e1.getBoolean() || e2.getBoolean(), e1);
        } else {
            return new ENode(((Boolean) e1.getAdapterValue(boolean.class)).booleanValue() || ((Boolean) e2.getAdapterValue(boolean.class)).booleanValue(), e1);
        }
    }

    /**
     * Evaluates an expression '&&' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode and(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isList()) {
            ENodeList res = new ENodeList();
            if (e2.isList()) {
                ENodeIterator it = e1.getList().iterator();
                while (it.hasNext()) {
                    ENode n1 = it.next();
                    if (e2.getList().contains(n1)) {
                        res.add(n1);
                    }
                }
            } else {
                ENodeIterator it = e1.getList().iterator();
                while (it.hasNext()) {
                    ENode n1 = it.next();
                    if (e2.equals(n1)) {
                        res.add(n1);
                    }
                }
            }
            return new ENode(res, e1);
        } else if (e1.isBoolean() && e2.isBoolean()) {
            return new ENode(e1.getBoolean() && e2.getBoolean(), e1);
        } else {
            return new ENode(((Boolean) e1.getAdapterValue(boolean.class)).booleanValue() && ((Boolean) e2.getAdapterValue(boolean.class)).booleanValue(), e1);
        }
    }

    /**
     * Evaluates an expression '==' between two nodes.
     * <p>
     * Returns e1.equals(e2)
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     *             if the types of the nodes are not compatible
     */
    public static ENode equals(ENode e1, ENode e2) throws ENodeCastException {
        return new ENode(e1.equals(e2), e1);
    }

    /**
     * Evaluates an expression '!=' between two nodes.
     * <p>
     * Returns !e1.equals(e2)
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     *             if the types of the nodes are not compatible
     */
    public static ENode notEquals(ENode e1, ENode e2) throws ENodeCastException {
        return new ENode(!e1.equals(e2), e1);
    }

    /**
     * Evaluates an expression '+' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode add(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isList()) {
            ENodeList res = new ENodeList();
            if (e2.isList()) {
                res.addAll(e1.getList());
                res.addAll(e2.getList());
            } else {
                res.addAll(e1.getList());
                res.add(e2);
            }
            return new ENode(res, e1);
        } else if (e2.isList()) {
            ENodeList res = new ENodeList();
            res.add(e1);
            res.addAll(e2.getList());
            return new ENode(res, e1);
        } else if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() + e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() + e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() + e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() + e2.getDouble(), e1);
        } else if (e1.isString() || e2.isString()) {
            ENode result = new ENode("", e1); //$NON-NLS-1$
            result.append(e1);
            result.append(e2);
            return result;
        } else if (e1.isNull()) {
            return e2;
        } else if (e2.isNull()) {
            return e1;
        } else {
            ENodeList res = new ENodeList();
            res.add(e1);
            res.add(e2);
            return new ENode(res, e1);
        }
    }

    /**
     * Evaluates an expression '-' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode sub(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isList()) {
            ENodeList res = new ENodeList();
            res.addAll(e1.getList());
            if (e2.isList()) {
                ENodeIterator it = e2.getList().iterator();
                while (it.hasNext()) {
                    ENode n2 = it.next();
                    res.remove(n2);
                }
            } else {
                res.remove(e2);
            }
            return new ENode(res, e1);
        } else if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() - e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() - e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() - e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() - e2.getDouble(), e1);
        } else if (e1.isNull()) {
            return e1;
        } else if (e2.isNull()) {
            return e1;
        } else if (e1.isString()) {
            Int2[] positions = TextSearch.getDefaultSearch().allIndexOf(e1.getString(), e2.toString());
            if (positions.length == 0) {
                return e1;
            } else {
                String s1 = e1.getString();
                StringBuffer result = new StringBuffer(s1.substring(0, positions[0].b()));
                for (int i = 1; i < positions.length; i++) {
                    result.append(s1.substring(positions[i - 1].e(), positions[i].b()));
                }
                result.append(s1.substring(positions[positions.length - 1].e(), s1.length()));
                return new ENode(result.toString(), e1);
            }
        } else if (e1.isEObject()) {
            if (e1.equals(e2)) {
                return new ENode(ENode.EMPTY, e1);
            } else {
                return e1;
            }
        }
        final String expression = e1.getType() + ' ' + TemplateConstants.OPERATOR_SUB + ' ' + e2.getType();
        throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.InvalidExpression", new Object[] { expression, })); //$NON-NLS-1$
    }

    /**
     * Evaluates an expression '/' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode div(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() / ExpressionTools.div0(e2.getInt()), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() / ExpressionTools.div0(e2.getDouble()), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() / ExpressionTools.div0(e2.getInt()), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() / ExpressionTools.div0(e2.getDouble()), e1);
        } else {
            try {
                // adapter "int / int"
                if (e2.isInt() || (e2.isString() && e2.getString().indexOf(".") == -1)) { //$NON-NLS-1$
                    return new ENode(((Integer) e1.getAdapterValue(int.class)).intValue() / ExpressionTools.div0(((Integer) e2.getAdapterValue(int.class)).intValue()), e1);
                }
            } catch (ENodeCastException ex1) {
                // step
            }
            try {
                // adapter "double / double" (it includes "int / double" and
                // "double / int")
                if (e2.isDouble() || e2.isString()) {
                    return new ENode(((Double) e1.getAdapterValue(double.class)).doubleValue() / ExpressionTools.div0(((Double) e2.getAdapterValue(double.class)).doubleValue()), e1);
                }
            } catch (ENodeCastException ex2) {
                // step
            }
            final String expression = e1.getType() + ' ' + TemplateConstants.OPERATOR_DIV + ' ' + e2.getType();
            throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.InvalidExpression", new Object[] { expression, })); //$NON-NLS-1$
        }
    }

    private static int div0(int i) throws ENodeCastException {
        if (i == 0) {
            throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.ZeroDivide")); //$NON-NLS-1$
        }
        return i;
    }

    private static double div0(double d) throws ENodeCastException {
        if (d == 0) {
            throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.ZeroDivide")); //$NON-NLS-1$
        }
        return d;
    }

    /**
     * Evaluates an expression '*' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode mul(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() * e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() * e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() * e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() * e2.getDouble(), e1);
        } else {
            try {
                // adapter "int * int"
                if ((e1.isInt() || (e1.isString() && e1.getString().indexOf(".") == -1)) && (e2.isInt() || (e2.isString() && e2.getString().indexOf(".") == -1))) { //$NON-NLS-1$ //$NON-NLS-2$
                    return new ENode(((Integer) e1.getAdapterValue(int.class)).intValue() * ((Integer) e2.getAdapterValue(int.class)).intValue(), e1);
                }
            } catch (ENodeCastException ex1) {
                // step
            }
            try {
                // adapter "double * double" (it includes "int * double" and
                // "double * int")
                if ((e1.isDouble() || e1.isString()) && (e2.isDouble() || e2.isString())) {
                    return new ENode(((Double) e1.getAdapterValue(double.class)).doubleValue() * ((Double) e2.getAdapterValue(double.class)).doubleValue(), e1);
                }
            } catch (ENodeCastException ex2) {
                // step
            }
            final String expression = e1.getType() + ' ' + TemplateConstants.OPERATOR_MUL + ' ' + e2.getType();
            throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.InvalidExpression", new Object[] { expression, })); //$NON-NLS-1$
        }
    }

    /**
     * Evaluates an expression '>' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode sup(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() > e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() > e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() > e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() > e2.getDouble(), e1);
        } else if (e1.isString() && e2.isString()) {
            return new ENode(e1.getString().compareTo(e2.getString()) > 0, e1);
        } else {
            try {
                // adapter "double > double" (it includes "int > double" and
                // "double > int")
                return new ENode(((Double) e1.getAdapterValue(double.class)).doubleValue() > ((Double) e2.getAdapterValue(double.class)).doubleValue(), e1);
            } catch (ENodeCastException ex) {
                final String expression = e1.toString() + '>' + e2.toString() + " [" + e1.getType() + '>' + e2.getType() + ']'; //$NON-NLS-1$
                throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.AdapterNotFound", new Object[] { ">", expression, })); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Evaluates an expression '>=' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode supE(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() >= e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() >= e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() >= e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() >= e2.getDouble(), e1);
        } else if (e1.isString() && e2.isString()) {
            return new ENode(e1.getString().compareTo(e2.getString()) >= 0, e1);
        } else {
            try {
                // adapter "double >= double" (it includes "int >= double"
                // and "double >= int")
                return new ENode(((Double) e1.getAdapterValue(double.class)).doubleValue() >= ((Double) e2.getAdapterValue(double.class)).doubleValue(), e1);
            } catch (ENodeCastException ex) {
                final String expression = e1.toString() + ">=" + e2.toString() + " [" + e1.getType() + ">=" + e2.getType() + ']'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.AdapterNotFound", new Object[] { ">=", expression, })); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

/**
	 * Evaluates an expression '<' between two nodes.
	 * <p>
	 * 
	 * @param e1
	 *            is the first node
	 * @param e2
	 *            is the second node
	 * @return the result node
	 * @throws ENodeCastException
	 */
    public static ENode inf(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() < e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() < e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() < e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() < e2.getDouble(), e1);
        } else if (e1.isString() && e2.isString()) {
            return new ENode(e1.getString().compareTo(e2.getString()) < 0, e1);
        } else {
            try {
                // adapter "double < double" (it includes "int < double" and
                // "double < int")
                return new ENode(((Double) e1.getAdapterValue(double.class)).doubleValue() < ((Double) e2.getAdapterValue(double.class)).doubleValue(), e1);
            } catch (ENodeCastException ex) {
                final String expression = e1.toString() + '<' + e2.toString() + " [" + e1.getType() + '<' + e2.getType() + ']'; //$NON-NLS-1$
                throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.AdapterNotFound", new Object[] { "<", expression, })); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Evaluates an expression '<=' between two nodes.
     * <p>
     * 
     * @param e1
     *            is the first node
     * @param e2
     *            is the second node
     * @return the result node
     * @throws ENodeCastException
     */
    public static ENode infE(ENode e1, ENode e2) throws ENodeCastException {
        if (e1.isInt() && e2.isInt()) {
            return new ENode(e1.getInt() <= e2.getInt(), e1);
        } else if (e1.isDouble() && e2.isDouble()) {
            return new ENode(e1.getDouble() <= e2.getDouble(), e1);
        } else if (e1.isDouble() && e2.isInt()) {
            return new ENode(e1.getDouble() <= e2.getInt(), e1);
        } else if (e1.isInt() && e2.isDouble()) {
            return new ENode(e1.getInt() <= e2.getDouble(), e1);
        } else if (e1.isString() && e2.isString()) {
            return new ENode(e1.getString().compareTo(e2.getString()) <= 0, e1);
        } else {
            try {
                // adapter "double <= double" (it includes "int <= double"
                // and "double <= int")
                return new ENode(((Double) e1.getAdapterValue(double.class)).doubleValue() <= ((Double) e2.getAdapterValue(double.class)).doubleValue(), e1);
            } catch (ENodeCastException ex) {
                final String expression = e1.toString() + "<=" + e2.toString() + " [" + e1.getType() + "<=" + e2.getType() + ']'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                throw new ENodeCastException(AcceleoGenMessages.getString("ExpressionTools.AdapterNotFound", new Object[] { "<=", expression, })); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

}
