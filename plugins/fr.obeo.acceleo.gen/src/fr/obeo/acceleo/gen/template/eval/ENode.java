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

package fr.obeo.acceleo.gen.template.eval;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

import fr.obeo.acceleo.ecore.factories.EFactory;
import fr.obeo.acceleo.ecore.tools.ETools;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateElement;
import fr.obeo.acceleo.gen.template.eval.log.EvalLog;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * Node of generation. Result of a template evaluation.
 * <p>
 * Encapsulates a generation value :
 * <li>EObject</li>
 * <li>ENodeList</li>
 * <li>String</li>
 * <li>boolean</li>
 * <li>int</li>
 * <li>double</li>
 * <li>null</li>
 * 
 * @author www.obeo.fr
 * 
 */
public class ENode implements Comparable {

    /**
     * Value of this node.
     * <p>
     * It's an EObject, an ENodeList, a String, a Boolean, an Integer, or null.
     */
    protected Object value;

    /**
     * Type of this node.
     * <p>
     * <li>type == T_EObject : the value is an EObject</li>
     * <li>type == T_ENodeList : the value is an ENodeList</li>
     * <li>type == T_String : the value is a String</li>
     * <li>type == T_boolean : the value is a Boolean</li>
     * <li>type == T_int : the value is an Integer</li>
     * <li>type == T_double : the value is a Double</li>
     * <li>type == T_null : the value is null</li>
     */
    protected String type;

    protected static final String T_EObject = "EObject"; //$NON-NLS-1$

    protected static final String T_ENodeList = "ENodeList"; //$NON-NLS-1$

    protected static final String T_String = "String"; //$NON-NLS-1$

    protected static final String T_boolean = "boolean"; //$NON-NLS-1$

    protected static final String T_int = "int"; //$NON-NLS-1$

    protected static final String T_double = "double"; //$NON-NLS-1$

    protected static final String T_null = "null"; //$NON-NLS-1$

    /**
     * Class of this node.
     * <p>
     * <li>type == T_EObject : the class is EObject.class</li>
     * <li>type == T_ENodeList : the class is ENodeList.class</li>
     * <li>type == T_String : the class is String.class</li>
     * <li>type == T_boolean : the class is boolean.class</li>
     * <li>type == T_int : the class is int.class</li>
     * <li>type == T_double : the class is double.class</li>
     * <li>type == T_null : the class is null</li>
     */
    protected Class typeClass;

    /**
     * It's the model element used to generate this node.
     */
    protected EObject containerEObject;

    /**
     * It's the template element used to generate this node.
     */
    protected TemplateElement containerTemplateElement;

    /**
     * Correspondences between the model and the text value, can be null.
     */
    protected TextModelMapping textModelMapping;

    /**
     * Correspondences between the template element and the text value, can be
     * null.
     */
    protected TextTemplateElementMapping textTemplateElementMapping;

    /**
     * Evaluation errors and warnings.
     */
    protected EvalLog log = new EvalLog();

    /**
     * Activates model/text mapping.
     */
    private boolean synchronize = true;

    /**
     * @return the value of this node : an EObject, an ENodeList, a String, a
     *         Boolean, an Integer, or null.
     */
    public Object getValue() {
        return value;
    }

    /**
     * @return the type of this node : "EObject", "ENodeList", "String",
     *         "boolean", "int", "double", or null.
     */
    public String getType() {
        return type;
    }

    /**
     * @return the class of this node : EObject.class, ENodeList.class,
     *         String.class, boolean.class, int.class, double.class, or null.
     */
    public Class getTypeClass() {
        return typeClass;
    }

    /**
     * @return the model element used to generate this node
     */
    public EObject getContainerEObject() {
        return containerEObject;
    }

    /**
     * @return the template element used to generate this node
     */
    public TemplateElement getContainerTemplateElement() {
        return containerTemplateElement;
    }

    /**
     * @return correspondences between the model and the text value, can be null
     */
    public TextModelMapping getTextModelMapping() {
        return textModelMapping;
    }

    /**
     * @return correspondences between the template element and the text value,
     *         can be null
     */
    public TextTemplateElementMapping getTextTemplateElementMapping() {
        return textTemplateElementMapping;
    }

    /**
     * @return evaluation errors and warnings
     */
    public EvalLog log() {
        return log;
    }

    /**
     * Creates a root node. The value is an EObject. The model element used to
     * generate this node is the element itself because the parent node is
     * unknown.
     * <p>
     * Precondition : object != EMPTY && object != null
     * <p>
     * Remark : eContainer() == getValue().
     * 
     * @param object
     *            is the value
     * @deprecated
     */
    @Deprecated
    public ENode(EObject object) {
        this(object, (TemplateElement) null, true);
    }

    /**
     * Creates a root node. The value is an EObject. The model element used to
     * generate this node is the element itself because the parent node is
     * unknown.
     * <p>
     * Precondition : object != EMPTY && object != null
     * <p>
     * Remark : eContainer() == getValue().
     * 
     * @param object
     *            is the value
     * @param element
     *            is the template element
     * @param synchronize
     *            activates model/text mapping
     */
    public ENode(EObject object, TemplateElement element, boolean synchronize) {
        containerEObject = object;
        containerTemplateElement = element;
        this.synchronize = synchronize;
        init(object, ENode.T_EObject, EObject.class);
    }

    /**
     * Creates a child node. The value is an EObject. The model element used to
     * generate this node is the same element as for the parent.
     * <p>
     * Remark : eContainer() == parent.eContainer().
     * 
     * @param object
     *            is the value
     * @param parent
     *            is the parent node
     */
    public ENode(EObject object, ENode parent) {
        init(parent);
        init(object, ENode.T_EObject, EObject.class);
    }

    /**
     * Creates a child node. The value is an EObject. The model element used to
     * generate this node is the parent.
     * <p>
     * Precondition : parent != EMPTY && parent != null
     * <p>
     * Equivalent : new ENode(object, new ENode(parent)).
     * 
     * @param object
     *            is the value
     * @param parent
     *            is the parent object
     * @param element
     *            is the template element
     * @param synchronize
     *            activates model/text mapping
     */
    public ENode(EObject object, EObject parent, TemplateElement element, boolean synchronize) {
        this(object, new ENode(parent, element, synchronize));
    }

    /**
     * Used to create empty ENode.
     */
    public static final EObject EMPTY = null;

    /**
     * Creates a child node. The value is an ENodeList. The model element used
     * to generate this node is the same element as for the parent.
     * <p>
     * Remark : eContainer() == parent.eContainer().
     * 
     * @param l
     *            is the value
     * @param parent
     *            is the parent node
     */
    public ENode(ENodeList l, ENode parent) {
        init(parent);
        init(l, ENode.T_ENodeList, ENodeList.class);
    }

    /**
     * Creates a child node. The value is a String. The model element used to
     * generate this node is the same element as for the parent.
     * <p>
     * Remark : eContainer() == parent.eContainer().
     * 
     * @param s
     *            is the value
     * @param parent
     *            is the parent node
     */
    public ENode(String s, ENode parent) {
        init(parent);
        init(s, ENode.T_String, String.class);
    }

    /**
     * Creates a child node. The value is a boolean. The model element used to
     * generate this node is the same element as for the parent.
     * <p>
     * Remark : eContainer() == parent.eContainer().
     * 
     * @param b
     *            is the value
     * @param parent
     *            is the parent node
     */
    public ENode(boolean b, ENode parent) {
        init(parent);
        init(new Boolean(b), ENode.T_boolean, boolean.class);
    }

    /**
     * Creates a child node. The value is an int. The model element used to
     * generate this node is the same element as for the parent.
     * <p>
     * Remark : eContainer() == parent.eContainer().
     * 
     * @param i
     *            is the value
     * @param parent
     *            is the parent node
     */
    public ENode(int i, ENode parent) {
        init(parent);
        init(new Integer(i), ENode.T_int, int.class);
    }

    /**
     * Creates a child node. The value is a double. The model element used to
     * generate this node is the same element as for the parent.
     * <p>
     * Remark : eContainer() == parent.eContainer().
     * 
     * @param i
     *            is the value
     * @param parent
     *            is the parent node
     */
    public ENode(double i, ENode parent) {
        init(parent);
        init(new Double(i), ENode.T_double, double.class);
    }

    /**
     * Updates container and log.
     * 
     * @param parent
     *            is the parent node
     */
    protected void init(ENode parent) {
        if (parent != null) {
            synchronize = parent.synchronize;
            containerEObject = parent.containerEObject;
            containerTemplateElement = parent.containerTemplateElement;
            log.getAll(parent.log, false);
        }
    }

    /**
     * Updates value, type, and class.
     * 
     * @param value
     *            is the value
     * @param type
     *            is the type of the value
     * @param typeClass
     *            is the class of the value
     */
    protected void init(Object value, String type, Class typeClass) {
        if (value != null) {
            this.value = value;
            this.type = type;
            this.typeClass = typeClass;
        } else {
            this.value = null;
            this.type = ENode.T_null;
            this.typeClass = null;
        }
        this.textModelMapping = createTextModelMapping();
        this.textTemplateElementMapping = createTextTemplateElementMapping();
    }

    /**
     * Updates text-model mapping, for String only.
     * 
     * @return a new mapping if the value is a String, null if not
     */
    protected TextModelMapping createTextModelMapping() {
        if (isString() && containerEObject != null) {
            TextModelMapping mapping = new TextModelMapping(containerEObject, !synchronize);
            mapping.shift(((String) value).length());
            return mapping;
        } else {
            return null;
        }
    }

    /**
     * Updates text-template mapping, for String only.
     * 
     * @return a new mapping
     */
    protected TextTemplateElementMapping createTextTemplateElementMapping() {
        if (isString() && containerTemplateElement != null) {
            TextTemplateElementMapping mapping = new TextTemplateElementMapping(containerTemplateElement, !synchronize);
            mapping.shift(((String) value).length());
            return mapping;
        } else {
            return null;
        }
    }

    /**
     * @return true if it's an EObject, false if not
     */
    public boolean isEObject() {
        return type == ENode.T_EObject;
    }

    /**
     * @return true if it's an ENodeList, false if not
     */
    public boolean isList() {
        return type == ENode.T_ENodeList;
    }

    /**
     * @return true if it's a String, false if not
     */
    public boolean isString() {
        return type == ENode.T_String;
    }

    /**
     * @return true if it's a boolean, false if not
     */
    public boolean isBoolean() {
        return type == ENode.T_boolean;
    }

    /**
     * @return true if it's an int, false if not
     */
    public boolean isInt() {
        return type == ENode.T_int;
    }

    /**
     * @return true if it's a double, false if not
     */
    public boolean isDouble() {
        return type == ENode.T_double;
    }

    /**
     * @return true if it's null, false if not
     */
    public boolean isNull() {
        return type == ENode.T_null;
    }

    /**
     * Returns the adaptive type for the given class.
     * 
     * @param c
     *            is the class
     * @return the adaptive type
     */
    public static Class getAdapterType(Class c) {
        if (c == ENode.class || c == EObject.class || c == ENodeList.class || c == String.class || c == boolean.class || c == int.class || c == double.class || c == null) {
            return c;
        } else if (EObject.class.isAssignableFrom(c)) {
            return EObject.class;
        } else if (c == List.class) { // List fix
            return List.class;
        } else if (c == Collection.class) { // Collection fix
            return List.class;
        } else {
            return null;
        }
    }

    /**
     * Returns the adaptive value for the given class.
     * 
     * @param c
     *            is the class
     * @return the adaptive value
     */
    public Object getAdapterValue(Class c) throws ENodeCastException {
        if (c == EObject.class) {
            return toEObject_().getValue();
        } else if (c == ENodeList.class) {
            return toList_().getValue();
        } else if (c == String.class) {
            return toString_().getValue();
        } else if (c == boolean.class) {
            return toBoolean_().getValue();
        } else if (c == int.class) {
            return toInt_().getValue();
        } else if (c == double.class) {
            return toDouble_().getValue();
        } else if (c == null) {
            return toNull_().getValue();
        } else if (c == List.class) { // List fix
            return toList_().getList().asList();
        } else if (c == Collection.class) { // Collection fix
            return toList_().getList().asList();
        } else if (c == ENode.class) {
            return this;
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.InvalidAdapterType", new Object[] { c.getClass().getName(), })); //$NON-NLS-1$
        }
    }

    private ENode toEObject_() throws ENodeCastException {
        if (typeClass == EObject.class) {
            return this;
        } else if (typeClass == ENodeList.class) {
            if (getList().size() > 0) {
                return getList().get(0);
            } else {
                return new ENode(ENode.EMPTY, this);
            }
        } else if (typeClass == String.class) {
            Object result = EcoreFactory.eINSTANCE.createFromString(EcorePackage.eINSTANCE.getEString(), getString());
            if (result instanceof EObject) {
                return new ENode((EObject) result, this);
            } else {
                return new ENode(ENode.EMPTY, this);
            }
        } else if (typeClass == boolean.class) {
            Object result = EcoreFactory.eINSTANCE.createFromString(EcorePackage.eINSTANCE.getEBoolean(), toString());
            if (result instanceof EObject) {
                return new ENode((EObject) result, this);
            } else {
                return new ENode(ENode.EMPTY, this);
            }
        } else if (typeClass == int.class) {
            Object result = EcoreFactory.eINSTANCE.createFromString(EcorePackage.eINSTANCE.getEInt(), toString());
            if (result instanceof EObject) {
                return new ENode((EObject) result, this);
            } else {
                return new ENode(ENode.EMPTY, this);
            }
        } else if (typeClass == double.class) {
            Object result = EcoreFactory.eINSTANCE.createFromString(EcorePackage.eINSTANCE.getEDouble(), toString());
            if (result instanceof EObject) {
                return new ENode((EObject) result, this);
            } else {
                return new ENode(ENode.EMPTY, this);
            }
        } else {
            return new ENode(ENode.EMPTY, this);
        }
    }

    private ENode toList_() {
        if (typeClass == ENodeList.class) {
            return this;
        } else {
            ENodeList result = new ENodeList();
            result.add(this);
            return new ENode(result, this);
        }
    }

    private ENode toString_() throws ENodeCastException {
        if (typeClass == String.class) {
            return this;
        } else if (typeClass == ENodeList.class) {
            ENode result = new ENode("", this); //$NON-NLS-1$
            ENodeIterator it = getList().iterator();
            while (it.hasNext()) {
                result.append(it.next());
            }
            return result;
        } else {
            return new ENode(toString(), this);
        }
    }

    private ENode toBoolean_() throws ENodeCastException {
        if (typeClass == EObject.class) {
            return new ENode(true, this);
        } else if (typeClass == ENodeList.class) {
            return new ENode(getList().size() > 0, this);
        } else if (typeClass == String.class) {
            return new ENode("true".equalsIgnoreCase(getString().trim()), this); //$NON-NLS-1$
        } else if (typeClass == boolean.class) {
            return this;
        } else if (typeClass == int.class) {
            if (getInt() > 0) {
                return new ENode(true, this);
            } else {
                return new ENode(false, this);
            }
        } else if (typeClass == double.class) {
            if (getDouble() > 0) {
                return new ENode(true, this);
            } else {
                return new ENode(false, this);
            }
        } else {
            return new ENode(false, this);
        }
    }

    private ENode toInt_() throws ENodeCastException {
        if (typeClass == EObject.class) {
            return new ENode(1, this);
        } else if (typeClass == ENodeList.class) {
            return new ENode(getList().size(), this);
        } else if (typeClass == String.class) {
            try {
                return new ENode(Integer.parseInt(getString().trim()), this);
            } catch (NumberFormatException e) {
                throw new ENodeCastException(AcceleoGenMessages.getString("ENode.AdapterNotFound", new Object[] { "Integer", toString() + " [" + getType() + ']', })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        } else if (typeClass == boolean.class) {
            if (getBoolean()) {
                return new ENode(1, this);
            } else {
                return new ENode(0, this);
            }
        } else if (typeClass == int.class) {
            return this;
        } else if (typeClass == double.class) {
            return new ENode(((Double) value).intValue(), this);
        } else {
            return new ENode(0, this);
        }
    }

    private ENode toDouble_() throws ENodeCastException {
        if (typeClass == EObject.class) {
            return new ENode(1.0, this);
        } else if (typeClass == ENodeList.class) {
            return new ENode(new Double(getList().size()).doubleValue(), this);
        } else if (typeClass == String.class) {
            try {
                return new ENode(Double.parseDouble(getString().trim()), this);
            } catch (NumberFormatException e) {
                throw new ENodeCastException(AcceleoGenMessages.getString("ENode.AdapterNotFound", new Object[] { "Double", toString() + " [" + getType() + ']', })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        } else if (typeClass == boolean.class) {
            if (getBoolean()) {
                return new ENode(1.0, this);
            } else {
                return new ENode(0.0, this);
            }
        } else if (typeClass == int.class) {
            return new ENode(new Double(getInt()).doubleValue(), this);
        } else if (typeClass == double.class) {
            return this;
        } else {
            return new ENode(0.0, this);
        }
    }

    private ENode toNull_() {
        return new ENode(ENode.EMPTY, this);
    }

    /**
     * @return the EObject value if it's an EObject
     * @throws ENodeCastException
     *             if it isn't an EObject
     */
    public EObject getEObject() throws ENodeCastException {
        if (isEObject()) {
            return (EObject) value;
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.ENodeCastExceptionMessage", new Object[] { type, ENode.T_EObject, })); //$NON-NLS-1$
        }
    }

    /**
     * @return the ENodeList value if it's an ENodeList
     * @throws ENodeCastException
     *             if it isn't an ENodeList
     */
    public ENodeList getList() throws ENodeCastException {
        if (isList()) {
            return (ENodeList) value;
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.ENodeCastExceptionMessage", new Object[] { type, ENode.T_ENodeList, })); //$NON-NLS-1$
        }
    }

    /**
     * @return the String value if it's a String
     * @throws ENodeCastException
     *             if it isn't a String
     */
    public String getString() throws ENodeCastException {
        if (isString()) {
            return (String) value;
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.ENodeCastExceptionMessage", new Object[] { type, ENode.T_String, })); //$NON-NLS-1$
        }
    }

    /**
     * @return the boolean value if it's a boolean
     * @throws ENodeCastException
     *             if it isn't a boolean
     */
    public boolean getBoolean() throws ENodeCastException {
        if (isBoolean()) {
            return ((Boolean) value).booleanValue();
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.ENodeCastExceptionMessage", new Object[] { type, ENode.T_boolean, })); //$NON-NLS-1$
        }
    }

    /**
     * @return the int value if it's an int
     * @throws ENodeCastException
     *             if it isn't an int
     */
    public int getInt() throws ENodeCastException {
        if (isInt()) {
            return ((Integer) value).intValue();
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.ENodeCastExceptionMessage", new Object[] { type, ENode.T_int, })); //$NON-NLS-1$
        }
    }

    /**
     * @return the double value if it's a double
     * @throws ENodeCastException
     *             if it isn't a double
     */
    public double getDouble() throws ENodeCastException {
        if (isDouble()) {
            return ((Double) value).doubleValue();
        } else {
            throw new ENodeCastException(AcceleoGenMessages.getString("ENode.ENodeCastExceptionMessage", new Object[] { type, ENode.T_double, })); //$NON-NLS-1$
        }
    }

    /**
     * It transforms this node into String.
     * 
     * @return the new String value.
     */
    public String asString() {
        if (isList()) {
            try {
                ENodeIterator it = getList().iterator();
                init("", ENode.T_String, String.class); //$NON-NLS-1$
                while (it.hasNext()) {
                    ENode child = it.next();
                    append(child);
                }
            } catch (ENodeCastException e) {
                // Never catch
            }
        } else if (!isString()) {
            String v = toString();
            init(v, ENode.T_String, String.class);
        }
        return (String) value;
    }

    /**
     * Appends an other node to this current node.
     * <p>
     * If current is null, it updates current with other node settings.
     * <p>
     * If current isn't null, it transforms current into String, it transforms
     * other into String, and it appends the two node.
     * 
     * @param other
     *            is the other node
     */
    public void append(ENode other) {
        if (isNull() && !other.isString()) {
            init(other.value, other.type, other.typeClass);
            log.getAll(other.log, false);
        } else if (other.isList()) {
            try {
                ENodeIterator it = other.getList().iterator();
                while (it.hasNext()) {
                    ENode child = it.next();
                    append(child);
                }
            } catch (ENodeCastException e) {
                // Never catch
            }
        } else {
            String buffer = other.asString();
            if (other.textModelMapping != null) {
                other.textModelMapping.commit();
            }
            if (other.textTemplateElementMapping != null) {
                other.textTemplateElementMapping.commit();
            }
            asString();
            int size = size();
            value = ((String) value) + buffer;
            if (textModelMapping != null && other.textModelMapping != null) {
                textModelMapping.from(other.textModelMapping);
            }
            if (textTemplateElementMapping != null && other.textTemplateElementMapping != null) {
                textTemplateElementMapping.from(other.textTemplateElementMapping);
            }
            other.log().shiftPosition(size);
            log.getAll(other.log, false);
        }
    }

    /**
     * Appends text to this node.
     * <p>
     * The text will have the default highlight
     * TextModelMapping.HIGHLIGHTED_DEFAULT.
     * 
     * @param text
     *            is the text to be added
     */
    public void append(String text) {
        append(text, TextModelMapping.HIGHLIGHTED_DEFAULT);
    }

    /**
     * Appends text to this node.
     * <p>
     * The text will have the given highlight.
     * 
     * @param text
     *            is the text to be added
     * @param highlightedType
     *            is the highlight type.
     */
    public void append(String text, int highlightedType) {
        asString();
        value = value + text;
        if (textModelMapping != null) {
            textModelMapping.shift(text.length(), highlightedType);
        }
        if (textTemplateElementMapping != null) {
            textTemplateElementMapping.shift(text.length(), highlightedType);
        }
    }

    /* (non-Javadoc) */
    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof ENode) {
            try {
                return equals((ENode) other);
            } catch (ENodeCastException e) {
                return false;
            }
        } else {
            if (value == null && other == null) {
                return true;
            } else if (value == null || other == null) {
                return false;
            } else {
                return value.equals(other);
            }
        }
    }

    /**
     * Indicates if nodes are equals.
     * 
     * @param other
     *            is the other node
     * @return true if nodes are equals
     * @throws ENodeCastException
     *             if the types of the nodes are not compatible
     */
    public boolean equals(ENode other) throws ENodeCastException {
        if (isNull() && other.isNull()) {
            return true;
        } else if (isNull() || other.isNull()) {
            return toString().equals(other.toString());
        } else if (type == other.type) {
            return value.equals(other.value);
        } else if (other.isBoolean()) {
            return other.value.equals(getAdapterValue(other.getTypeClass()));
        } else {
            if (value.equals(other.getAdapterValue(getTypeClass()))) {
                return true;
            } else {
                return toString().equals(other.toString());
            }
        }
        // It never throws : new ENodeCastException(type + " can't be equals
        // with " + other.type);
    }

    /* (non-Javadoc) */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns the size of the node. <li>isEObject() : return 1</li> <li>
     * isList() : return getList().size()</li> <li>isString() : return
     * getString().length()</li> <li>isBoolean() : return 1</li> <li>isInt() :
     * return 1</li> <li>isDouble() : return 1</li> <li>isNull() : return 0</li>
     * 
     * @return value size
     */
    public int size() {
        if (isEObject()) {
            return 1;
        } else if (isList()) {
            return ((ENodeList) value).size();
        } else if (isString()) {
            return ((String) value).length();
        } else if (isBoolean()) {
            return 1;
        } else if (isInt()) {
            return 1;
        } else if (isDouble()) {
            return 1;
        } else {
            // ASSERT isNull()
            return 0;
        }
    }

    /* (non-Javadoc) */
    public int compareTo(Object other) {
        int result = 0;
        if (other instanceof ENode) {
            if (getValue() instanceof Comparable && ((ENode) other).getValue() instanceof Comparable) {
                result = ((Comparable) getValue()).compareTo(((ENode) other).getValue());
            } else {
                result = toString().compareTo(other.toString());
            }
        }
        if (result != 0) {
            return result;
        } else {
            return 1;
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        if (isNull()) {
            return ""; //$NON-NLS-1$
        } else if (isEObject() && !(value instanceof EEnumLiteral)) {
            EObject object = (EObject) value;
            String buffer = object.toString();
            int iProperties = buffer.indexOf("("); //$NON-NLS-1$
            if (iProperties > -1) {
                return ETools.getEClassifierShortPath(object.eClass()) + ' ' + buffer.substring(iProperties);
            } else {
                return value.toString();
            }
        } else if (isDouble()) {
            return NumberFormat.getInstance().format(((Double) value).doubleValue());
        } else {
            return value.toString();
        }
    }

    /**
     * Indicates if it's a containment node.
     */
    protected boolean containment = true;

    /**
     * @return true if it's a containment node, false if not
     */
    public boolean isContainment() {
        return containment;
    }

    /**
     * @param containment
     *            indicates if it's a containment node
     */
    public void setContainment(boolean containment) {
        this.containment = containment;
    }

    /**
     * Indicates if it's an optional node.
     */
    protected boolean optional = false;

    /**
     * @return true if it's an optional node, false if not
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * @param optional
     *            indicates if it's an optional node
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Indicates if this node is instance of the class whose name is given. <li>
     * getType() == name => true</li> <li>isEObject() =>
     * EFactory.eInstanceOf(getEObject(),name)</li>
     * <p>
     * Samples :
     * <li>Return true if isString() and name == "String"</li>
     * <li>An instance of the EObject java.resources.Folder</li>
     * return true if name equals "Folder" or "resources.Folder".
     * <li>An instance of the EObject java.resources.Folder return true if name
     * equals "File" and Folder inherits File.</li>
     * 
     * @param name
     *            is the class name
     * @return true if this node is instance of the class whose name is given
     */
    public boolean eInstanceof(String name) {
        if (getType().equals(name)) {
            return true;
        } else {
            try {
                if (EFactory.eInstanceOf(getEObject(), name)) {
                    return true;
                } else {
                    return false;
                }
            } catch (ENodeCastException e) {
                return false;
            }
        }
    }

    /**
     * It returns a new ENode if value is valid (EObject, ENodeList, String,
     * Boolean, Integer, or null), else it returns null.
     * 
     * @param value
     *            is the value
     * @param parent
     *            is the parent node
     * @return the new ENode or null
     */
    public static ENode createTry(Object value, ENode parent) {
        if (value == null) {
            return new ENode(ENode.EMPTY, parent);
        } else if (value instanceof ENode) {
            ENode node = (ENode) value;
            if (node.containerEObject == null && parent.containerEObject != null) {
                ENode result = new ENode(ENode.EMPTY, parent);
                result.append(node);
                return result;
            } else {
                return node;
            }
        } else if (value instanceof EObject) {
            if (value instanceof Enumerator) {
                return new ENode(((Enumerator) value).getLiteral(), parent);
            } else {
                return new ENode((EObject) value, parent);
            }
        } else if (value instanceof String) {
            return new ENode((String) value, parent);
        } else if (value instanceof ENodeList) {
            return new ENode((ENodeList) value, parent);
        } else if (value instanceof Collection) {
            ENodeList result = new ENodeList();
            Iterator it = ((Collection) value).iterator();
            while (it.hasNext()) {
                ENode element = ENode.createTry(it.next(), parent);
                if (element != null) {
                    result.add(element);
                }
            }
            return new ENode(result, parent);
        } else if (value instanceof Object[]) {
            ENodeList result = new ENodeList();
            Object[] values = (Object[]) value;
            for (Object value2 : values) {
                ENode element = ENode.createTry(value2, parent);
                if (element != null) {
                    result.add(element);
                }
            }
            return new ENode(result, parent);
        } else if (value instanceof Boolean) {
            return new ENode(((Boolean) value).booleanValue(), parent);
        } else if (value instanceof Integer) {
            return new ENode(((Integer) value).intValue(), parent);
        } else if (value instanceof Short) {
            return new ENode(((Short) value).intValue(), parent);
        } else if (value instanceof Long) {
            return new ENode(((Long) value).intValue(), parent);
        } else if (value instanceof Double) {
            return new ENode(((Double) value).doubleValue(), parent);
        } else if (value instanceof Float) {
            return new ENode(((Float) value).doubleValue(), parent);
        } else if (value instanceof Character) {
            return new ENode(String.valueOf(value), parent);
        } else {
            return new ENode(value.toString(), parent);
        }
    }

    /**
     * String method call.
     * 
     * @param name
     *            is the name of the method to call
     * @param begin
     *            is the beginning index, or -1
     * @param end
     *            is the ending index, or -1
     */
    public void stringCall(String name, int begin, int end) {
        if (isList()) {
            try {
                ENodeIterator it = getList().iterator();
                init(new ENodeList(), ENode.T_ENodeList, ENodeList.class);
                while (it.hasNext()) {
                    ENode child = it.next();
                    child.stringCall(name, begin, end);
                    getList().add(child);
                }
            } catch (ENodeCastException e) {
                // Never catch
            }
        } else {
            if (!isString()) {
                String v = toString();
                init(v, ENode.T_String, String.class);
            }
            // isString
            String text = (String) value;
            if (text.length() > 0) {
                if ("trim".equals(name)) { //$NON-NLS-1$
                    Int2 range = TextSearch.getDefaultSearch().trim(text, 0, text.length());
                    if (range.b() > 0 || range.e() < text.length()) {
                        range(range);
                        value = substring(text, range.b(), range.e());
                    }
                } else if ("substring".equals(name)) { //$NON-NLS-1$
                    Int2 range;
                    if (end > -1) {
                        range = new Int2(begin, end);
                    } else {
                        range = new Int2(begin, text.length());
                    }
                    if (range.b() > 0 || range.e() < text.length()) {
                        range(range);
                        value = substring(text, range.b(), range.e());
                    }
                } else if ("toLowerCase".equals(name)) { //$NON-NLS-1$
                    value = text.toLowerCase();
                } else if ("toUpperCase".equals(name)) { //$NON-NLS-1$
                    value = text.toUpperCase();
                } else if ("toU1Case".equals(name)) { //$NON-NLS-1$
                    value = Character.toUpperCase(text.charAt(0)) + text.substring(1);
                } else if ("toL1Case".equals(name)) { //$NON-NLS-1$
                    value = Character.toLowerCase(text.charAt(0)) + text.substring(1);
                } else if ("indentSpace".equals(name) || "indentTab".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
                    char c = ("indentSpace".equals(name)) ? ' ' : '\t'; //$NON-NLS-1$
                    StringBuffer buffer = new StringBuffer(text);
                    Int2[] lines = TextSearch.getDefaultSearch().splitPositionsOf(text, new String[] { "\n" }, false); //$NON-NLS-1$
                    indent(lines);
                    for (int i = lines.length - 1; i >= 0; i--) {
                        buffer.insert(lines[i].b(), c);
                    }
                    value = buffer.toString();
                } else if (name != null && name.startsWith("internalIndent:")) { //$NON-NLS-1$
                    String indent = name.substring("internalIndent:".length()); //$NON-NLS-1$
                    StringBuffer buffer = new StringBuffer(text);
                    Int2[] lines = TextSearch.getDefaultSearch().splitPositionsOf(text, new String[] { "\n" }, false); //$NON-NLS-1$
                    for (int i = 0; i < indent.length(); i++) {
                        indent(lines);
                    }
                    for (int i = lines.length - 1; i >= 0; i--) {
                        buffer.insert(lines[i].b(), indent);
                    }
                    value = buffer.toString();
                    stringCall("substring", indent.length(), -1); //$NON-NLS-1$
                }
            }
        }
    }

    private void range(Int2 range) {
        if (textModelMapping != null) {
            textModelMapping.range(range);
        }
        if (textTemplateElementMapping != null) {
            textTemplateElementMapping.range(range);
        }
        log.range(range);
    }

    private String substring(String s, int begin, int end) {
        if (begin < 0 || end < 0 || begin >= s.length() || end <= begin) {
            return ""; //$NON-NLS-1$
        } else if (end >= s.length()) {
            return s.substring(begin);
        } else {
            return s.substring(begin, end);
        }
    }

    private void indent(Int2[] lines) {
        if (textModelMapping != null) {
            textModelMapping.indent(lines);
        }
        if (textTemplateElementMapping != null) {
            textTemplateElementMapping.indent(lines);
        }
        log.indent(lines);
    }

    /**
     * Creates a copy.
     * 
     * @return the copy
     */
    public ENode copy() {
        if (value == null) {
            return new ENode(ENode.EMPTY, this);
        } else if (value instanceof EObject) {
            return new ENode((EObject) value, this);
        } else if (value instanceof String) {
            return new ENode((String) value, this);
        } else if (value instanceof ENodeList) {
            return new ENode((ENodeList) value, this);
        } else if (value instanceof Boolean) {
            return new ENode(((Boolean) value).booleanValue(), this);
        } else if (value instanceof Integer) {
            return new ENode(((Integer) value).intValue(), this);
        } else if (value instanceof Double) {
            return new ENode(((Double) value).doubleValue(), this);
        } else {
            return new ENode(value.toString(), this);
        }
    }

}
