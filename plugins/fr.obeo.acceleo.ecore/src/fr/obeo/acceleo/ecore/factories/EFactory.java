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

package fr.obeo.acceleo.ecore.factories;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import fr.obeo.acceleo.ecore.AcceleoEcoreMessages;
import fr.obeo.acceleo.ecore.AcceleoEcorePlugin;
import fr.obeo.acceleo.ecore.tools.ETools;

/**
 * This is a factory for an ecore metamodel. There is a factory by package. Each
 * factory is used to create instances of classifiers.
 * 
 * @author www.obeo.fr
 * 
 */
public class EFactory {

    /**
     * Ecore factory
     */
    protected Object factoryImpl = null;

    /**
     * The identifier of the factory.
     */
    protected String id = ""; //$NON-NLS-1$

    /**
     * The class loader.
     */
    protected ClassLoader loader;

    /**
     * @return the identifier of the factory
     */
    protected String getId() {
        return id;
    }

    /**
     * Constructor.
     * 
     * @param factoryId
     *            is the identifier of the factory
     * @param ePackage
     *            is the package
     * @param loader
     *            is the class loader
     * @throws FactoryException
     */
    public EFactory(String factoryId, EPackage ePackage, ClassLoader loader) {
        factoryImpl = ePackage.getEFactoryInstance();
        id = factoryId;
        this.loader = loader;
    }

    /**
     * Constructor.
     * <p>
     * Sample : new Factory("java.resources","Resources") creates an instance of
     * the factory java.resources.ResourcesFactory
     * 
     * @param factoryId
     *            is the identifier of the factory
     * @param factoryShortName
     *            is the factory short name
     * @param loader
     *            is the class loader
     * @throws FactoryException
     */
    public EFactory(String factoryId, String factoryShortName, ClassLoader loader) throws FactoryException {
        this.loader = loader;
        init(factoryId, factoryShortName, loader); // throws FactoryException
        // when error
    }

    private void init(String factoryId, String factoryShortName, ClassLoader loader) throws FactoryException {
        if (factoryId != null && factoryShortName != null && factoryId.length() > 0 && factoryShortName.length() > 0) {
            // Class name
            String rPackageImplClassName = factoryId + '.' + factoryShortName + "Package"; //$NON-NLS-1$
            // Class loader
            try {
                // Class
                Class rPackageImplClass = Class.forName(rPackageImplClassName, true, loader);
                // Method
                Field rPackageImplField = rPackageImplClass.getField("eINSTANCE"); //$NON-NLS-1$
                Method rPackageImplGetRessourcesFactoryMethod = rPackageImplClass.getMethod("get" + factoryShortName + "Factory", new Class[] {}); //$NON-NLS-1$ //$NON-NLS-2$
                // Instances
                Object packageImpl = rPackageImplField.get(null);
                factoryImpl = rPackageImplGetRessourcesFactoryMethod.invoke(packageImpl, new Object[] {});
                id = factoryId;
            } catch (Exception e) {
                throw new FactoryException(AcceleoEcoreMessages.getString("EFactory.UnexpectedException", new Object[] { e.getMessage(), })); //$NON-NLS-1$
            }
        } else {
            final String factoryName = factoryId + ".impl." + factoryShortName + "FactoryImpl"; //$NON-NLS-1$ //$NON-NLS-2$
            throw new FactoryException(AcceleoEcoreMessages.getString("EFactory.UnresolvedFactory", new Object[] { factoryName, })); //$NON-NLS-1$
        }
    }

    /**
     * Returns the factory path.
     * 
     * @return the factory path
     */
    public String getPath() {
        return id;
    }

    /**
     * Creates an instance of the classifier whose name is given.
     * <p>
     * Sample : Creates an instance of java.resources.Folder if name equals
     * "Folder" or "resources.Folder".
     * 
     * @param name
     *            is the name of the classifier to be created
     * @return the new EObject
     * @throws FactoryException
     */
    public EObject eCreate(String name) throws FactoryException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            String createName = "create" + name.substring(0, 1).toUpperCase() + name.substring(1); //$NON-NLS-1$
            return (EObject) EFactory.eCall(factoryImpl, createName, null);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private static Object eCall(Object object, String name, Object arg) throws FactoryException {
        try {
            Method method = object.getClass().getMethod(name, (arg != null) ? new Class[] { arg.getClass() } : new Class[] {});
            return method.invoke(object, (arg != null) ? new Object[] { arg } : new Object[] {});
        } catch (Exception e) {
            throw new FactoryException(e.getMessage());
        }
    }

    /**
     * Sets the value of the given feature of the object to the new value.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name of the value to set
     * @param arg
     *            is the new value
     * @throws FactoryException
     */
    public static void eSet(EObject object, String name, Object arg) throws FactoryException {
        EStructuralFeature feature = EFactory.eStructuralFeature(object, name);
        EFactory.eSet(object, feature, arg);
    }

    private static void eSet(EObject object, EStructuralFeature feature, Object arg) throws FactoryException {
        if (!EFactory.eSetEnum(object, feature, arg)) {
            object.eSet(feature, arg);
        }
    }

    private static boolean eSetEnum(EObject object, EStructuralFeature feature, Object arg) {
        if (feature != null && feature.getEType() instanceof EEnum && arg instanceof String) {
            try {
                arg = EFactory.eEnumValue(object, feature, arg);
                object.eSet(feature, arg);
            } catch (Exception e) {
                AcceleoEcorePlugin.getDefault().log(e, false);
            }
            return true;
        } else {
            return false;
        }
    }

    private static Object eEnumValue(EObject object, EStructuralFeature feature, Object arg) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        String className = ETools.getEClassifierPath(feature.getEType());
        Class c;
        try {
            c = object.getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            c = Class.forName(className);
        }
        Method m = c.getMethod("get", new Class[] { String.class }); //$NON-NLS-1$
        return m.invoke(c, new Object[] { arg });
    }

    /**
     * Sets the value of the given feature of the object to the new value.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name of the value to set
     * @param arg
     *            is the new value
     * @param loader
     *            is the specific classloader use to set the value
     * @throws FactoryException
     */
    public static void eSet(EObject object, String name, Object arg, ClassLoader loader) throws FactoryException {
        EStructuralFeature feature = EFactory.eStructuralFeature(object, name);
        if (feature != null && feature.getEType() instanceof EEnum && arg instanceof String) {
            try {
                Class c = loader.loadClass(ETools.getEClassifierPath(feature.getEType()));
                Method m = c.getMethod("get", new Class[] { String.class }); //$NON-NLS-1$
                arg = m.invoke(c, new Object[] { arg });
                object.eSet(feature, arg);
            } catch (Exception e) {
                AcceleoEcorePlugin.getDefault().log(e, false);
            }
        } else {
            object.eSet(feature, arg);
        }
    }

    /**
     * Adds the new value of the given feature of the object. If the structural
     * feature isn't a list, it behaves like eSet.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name of the new value
     * @param arg
     *            is the new value
     * @throws FactoryException
     */
    public static void eAdd(EObject object, String name, Object arg) throws FactoryException {
        EStructuralFeature feature = EFactory.eStructuralFeature(object, name);
        Object list = object.eGet(feature);
        if (list != null && list instanceof List) {
            if (arg != null) {
                if (feature != null && feature.getEType() instanceof EEnum && arg instanceof String) {
                    try {
                        arg = EFactory.eEnumValue(object, feature, arg);
                        ((List) list).add(arg);
                    } catch (Exception e) {
                        AcceleoEcorePlugin.getDefault().log(e, false);
                    }
                } else {
                    ((List) list).add(arg);
                }
            }
        } else {
            EFactory.eSet(object, feature, arg);
        }
    }

    /**
     * Removes the value of the given feature of the object. If the structural
     * feature isn't a list, it behaves like eSet(object,name,null).
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name of the value
     * @param arg
     *            is the value to remove, null is allowed
     * @throws FactoryException
     */
    public static void eRemove(EObject object, String name, Object arg) throws FactoryException {
        EStructuralFeature feature = EFactory.eStructuralFeature(object, name);
        Object list = object.eGet(feature);
        if (list != null && list instanceof List) {
            if (arg != null) {
                if (feature != null && feature.getEType() instanceof EEnum && arg instanceof String) {
                    try {
                        arg = EFactory.eEnumValue(object, feature, arg);
                        ((List) list).remove(arg);
                    } catch (Exception e) {
                        AcceleoEcorePlugin.getDefault().log(e, false);
                    }
                } else {
                    ((List) list).remove(arg);
                }
            }
        } else {
            EFactory.eSet(object, name, null);
        }
    }

    /**
     * Gets the value of the given feature of the object.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name, or a method defined on EObject like
     *            'eClass', 'eResource', 'eContainer', 'eContainingFeature',
     *            'eContainmentFeature', 'eContents', 'eAllContents',
     *            'eCrossReferences'
     * @return the value of the given feature of the object
     * @throws FactoryException
     */
    public static Object eGet(EObject object, String name) throws FactoryException {
        return EFactory.eGet(object, name, true);
    }

    /**
     * Gets the value of the given feature of the object.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name, or a method defined on EObject like
     *            'eClass', 'eResource', 'eContainer', 'eContainingFeature',
     *            'eContainmentFeature', 'eContents', 'eAllContents',
     *            'eCrossReferences'
     * @param adaptEnum
     *            to adapt automatically an enumerator
     * @return the value of the given feature of the object
     * @throws FactoryException
     */
    public static Object eGet(EObject object, String name, boolean adpatEnum) throws FactoryException {
        Object result;
        EStructuralFeature feature = EFactory.doGetFeature(object, name);
        if (feature != null) {
            result = object.eGet(feature);
        } else {
            try {
                result = EFactory.eCall(object, name, null);
            } catch (FactoryException eCall) {
                throw new FactoryException(AcceleoEcoreMessages.getString("EFactory.UnresolvedLink", new Object[] { name, object.eClass().getName(), })); //$NON-NLS-1$
            }
        }
        if (result != null && result instanceof Enumerator) {
            if (adpatEnum) {
                return ((Enumerator) result).getLiteral();
            } else {
                return result;
            }
        } else if (result != null && result instanceof BasicEList) {
            List list = new ArrayList();
            Iterator enums = ((BasicEList) result).iterator();
            while (enums.hasNext()) {
                Object next = enums.next();
                if (adpatEnum && next instanceof Enumerator) {
                    list.add(((Enumerator) next).getLiteral());
                } else {
                    list.add(next);
                }
            }
            return list;
        } else {
            return result;
        }
    }

    /**
     * Gets the structural feature of the given feature name of the object.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return the structural feature
     * @throws FactoryException
     */
    public static EStructuralFeature eStructuralFeature(EObject object, String name) throws FactoryException {
        EStructuralFeature structuralFeature = object.eClass().getEStructuralFeature(name);
        if (structuralFeature != null) {
            return structuralFeature;
        } else {
            throw new FactoryException(AcceleoEcoreMessages.getString("EFactory.UnresolvedLink", new Object[] { name, object.eClass().getName(), })); //$NON-NLS-1$
        }
    }

    /**
     * Gets the value of the given feature of the object, as an EObject.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return the value or null if it isn't an EObject
     * @throws FactoryException
     */
    public static EObject eGetAsEObject(EObject object, String name) throws FactoryException {
        Object eGet = EFactory.eGet(object, name);
        if (eGet != null && eGet instanceof EObject) {
            return (EObject) eGet;
        } else {
            return null;
        }
    }

    /**
     * Gets the value of the given feature of the object, as a String.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return the value or null if it isn't a String
     * @throws FactoryException
     */
    public static String eGetAsString(EObject object, String name) throws FactoryException {
        Object eGet = EFactory.eGet(object, name);
        if (eGet != null) {
            return eGet.toString();
        } else {
            return null;
        }
    }

    /**
     * Gets the value of the given feature of the object, as a Boolean.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return the value or null if it isn't a Boolean
     * @throws FactoryException
     */
    public static Boolean eGetAsBoolean(EObject object, String name) throws FactoryException {
        Object eGet = EFactory.eGet(object, name);
        if (eGet != null && eGet instanceof Boolean) {
            return (Boolean) eGet;
        } else {
            return null;
        }
    }

    /**
     * Gets the value of the given feature of the object, as an Integer.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return the value or null if it isn't an Integer
     * @throws FactoryException
     */
    public static Integer eGetAsInteger(EObject object, String name) throws FactoryException {
        Object eGet = EFactory.eGet(object, name);
        if (eGet != null && eGet instanceof Integer) {
            return (Integer) eGet;
        } else {
            return null;
        }
    }

    /**
     * Gets the value of the given feature of the object, as a List.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return the value, or a new List with a single element if it isn't a
     *         List, or null if it doesn't exist
     * @throws FactoryException
     */
    public static List eGetAsList(EObject object, String name) throws FactoryException {
        Object eGet = EFactory.eGet(object, name);
        if (eGet != null) {
            if (eGet instanceof List) {
                return (List) eGet;
            } else {
                List list = new BasicEList(1);
                list.add(eGet);
                return list;
            }
        } else {
            return null;
        }
    }

    /**
     * Indicates if the object is instance of the class whose name is given.
     * <p>
     * Samples :
     * <p>
     * An instance of java.resources.Folder return true if name equals "Folder"
     * or "resources.Folder".
     * <p>
     * An instance of java.resources.Folder return true if name equals "File"
     * and Folder inherits File.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the class name
     * @return true if the object is instance of the class whose name is given
     */
    public static boolean eInstanceOf(EObject object, String name) {
        if (object == null) {
            return (name == null);
        }
        if ("EObject".equals(name) || "ecore.EObject".equals(name)) { //$NON-NLS-1$//$NON-NLS-2$
            return true;
        }
        return EFactory.eInstanceOf(object.eClass(), name);
    }

    private static boolean eInstanceOf(EClass eClass, String name) {
        if (name.indexOf('.') == -1 && name.equals(eClass.getName())) {
            return true;
        } else {
            String instanceClassName = '.' + eClass.getInstanceClassName();
            String endsWith = '.' + name;
            if (instanceClassName.endsWith(endsWith)) {
                return true;
            } else {
                Iterator superTypes = eClass.getESuperTypes().iterator();
                while (superTypes.hasNext()) {
                    EClass eSuperClass = (EClass) superTypes.next();
                    if (EFactory.eInstanceOf(eSuperClass, name)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * Indicates if the feature name given is valid for the object.
     * 
     * @param object
     *            is the object
     * @param name
     *            is the feature name
     * @return true if the feature is defined, false if not
     */
    public static boolean eValid(EObject object, String name) {
        try {
            EFactory.eGet(object, name);
            return true;
        } catch (FactoryException e) {
            return false;
        }
    }

    private static EStructuralFeature doGetFeature(EObject object, String name) {
        return object.eClass().getEStructuralFeature(name);
    }

}
