/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.business.internal.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.query.legacy.ecore.factories.EFactory;
import org.eclipse.sirius.query.legacy.ecore.factories.FactoryException;
import org.eclipse.sirius.query.legacy.ecore.tools.ETools;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeCastException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeIterator;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeList;
import org.eclipse.sirius.query.legacy.tools.resources.FileContentMap;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * This class comes from Acceleo, it provides the basic browsing services :
 * eContainer(), eAllContents().... It is needed here for two things : handle
 * metamodel extensions, and fix issues with cache on eAllContents().
 * 
 * @author cbrun
 * 
 */
public class EObjectServices {
    private FileContentMap load = new FileContentMap();

    private EPackage eRootPackage;

    private Map<String, List<String>> eClass2containments = new HashMap<String, List<String>>();

    private Map<EClass, List<EClass>> eClass2subTypes;

    /**
     * Gets the container of an EObject.
     * 
     * @param current
     *            is the object
     * @return the container
     */
    public EObject eContainer(final EObject current) {
        return current.eContainer();
    }

    /**
     * Gets recursively the container of an EObject. The recursion is stopped
     * when an element of the given type is found.
     * 
     * @param root
     *            : current instance.
     * @param type
     *            : type name.
     * @return the parent.
     * @throws FactoryException
     *             on type error.
     */
    public EObject eContainer(final EObject root, final String type) throws FactoryException {
        EObject current = root;
        while (current != null) {
            if (EFactory.eInstanceOf(current, type)) {
                return current;
            } else {
                current = current.eContainer();
            }
        }
        return null;
    }

    /**
     * Returns the root container; it may be this object itself and it will have
     * a <code>null</code> {@link EObject#eContainer container}.
     * <p>
     * The root container must be {@link Resource#getContents directly
     * contained} in a resource for its {@link EObject#eAllContents tree} to be
     * {@link Resource#save(java.util.Map) serializable}.
     * </p>
     * 
     * @param eObject
     *            the object to get the root container for.
     * @return the root container.
     */
    public EObject getRootContainer(final EObject eObject) {
        return EcoreUtil.getRootContainer(eObject);
    }

    /**
     * Gets the metamodel class of an EObject.
     * 
     * @param current
     *            is the object
     * @return the metamodel class
     */
    public EObject eClass(final EObject current) {
        return current.eClass();
    }

    /**
     * Gets the children of an EObject.
     * 
     * @param current
     *            is the object
     * @return the children
     */
    public EList<EObject> eContents(final EObject current) {
        return current.eContents();
    }

    /**
     * Gets all the direct contents and indirect contents of this object.
     * 
     * @param current
     *            is the object
     * @return the contents
     */
    public ENodeList eAllContents(final ENode current) {
        final ENodeList result = new ENodeList();
        try {
            if (current.isList()) {
                final ENodeIterator it = current.getList().iterator();
                while (it.hasNext()) {
                    result.addAll(eAllContents(it.next()));
                }
            } else if (current.isEObject()) {
                final TreeIterator<EObject> it = current.getEObject().eAllContents();
                while (it.hasNext()) {
                    result.add(ENode.createTry(it.next(), current));
                }
            }
        } catch (final ENodeCastException e) {
            // Never catch
        }
        return result;
    }

    /**
     * Gets all the direct contents and indirect contents of the object.
     * 
     * @param current
     *            is the object
     * @param type
     *            is the type of the objects to select
     * @return the contents
     * @throws FactoryException
     *             on type error.
     */
    public ENodeList eAllContents(final ENode current, final String type) throws FactoryException {
        final ENodeList result = new ENodeList();
        try {
            if (current.isList()) {
                final ENodeIterator it = current.getList().iterator();
                while (it.hasNext()) {
                    result.addAll(eAllContents(it.next(), type));
                }
            } else if (current.isEObject()) {
                final int i = type.lastIndexOf(".");
                final String typeName = i > -1 ? type.substring(i + 1) : type;
                final List<EObject> children = eAllContents(current.getEObject(), type, typeName);
                final Iterator<EObject> it = children.iterator();
                while (it.hasNext()) {
                    result.add(ENode.createTry(it.next(), current));
                }
            }
        } catch (final ENodeCastException e) {
            // Never catch
        }
        return result;
    }

    private List<EObject> eAllContents(final EObject object, final String type, final String typeName) {
        final List<EObject> result = new ArrayList<EObject>();
        final List<String> containmentTypeNames = getContainmentNames(object);
        if (containmentTypeNames.contains(typeName)) {
            final Iterator<EObject> eContents = object.eContents().iterator();
            while (eContents.hasNext()) {
                final EObject eContent = eContents.next();
                if (EFactory.eInstanceOf(eContent, type)) {
                    result.add(eContent);
                }
                result.addAll(eAllContents(eContent, type, typeName));
            }
        }
        return result;
    }

    private List<String> getContainmentNames(final EObject object) {
        final EClass eClass = object.eClass();
        final EPackage ePackage = (EPackage) getRootContainer(eClass);
        if (ePackage != eRootPackage) {
            eRootPackage = ePackage;
            eClass2containments.clear();
            eClass2subTypes = null;
        }
        final String eClassName = eClass.getName();
        List<String> containmentNames = eClass2containments.get(eClassName);
        if (containmentNames == null) {
            containmentNames = new ArrayList<String>();
            computeContainments(new ArrayList<EClassifier>(), containmentNames, ePackage, eClass.getEAllReferences());
            eClass2containments.put(eClassName, containmentNames);
        }
        return containmentNames;
    }

    private void computeContainments(final List<EClassifier> containmentTypes, final List<String> containmentNames, final EPackage ePackage, final Iterable<EReference> eReferences) {
        final Iterator<EReference> references = eReferences.iterator();
        while (references.hasNext()) {
            final EReference eReference = references.next();
            if (eReference.isContainment()) {
                final List<EClassifier> types = new ArrayList<EClassifier>();
                types.add(eReference.getEType());
                if (eReference.getEType() instanceof EClass) {
                    types.addAll(((EClass) eReference.getEType()).getEAllSuperTypes());
                }
                types.addAll(eClass2subTypes(ePackage, eReference.getEType()));
                final Iterator<EClassifier> it = types.iterator();
                while (it.hasNext()) {
                    final EClassifier type = it.next();
                    if (type instanceof EClass) {
                        final String name = type.getName();
                        if (!containmentTypes.contains(type)) {
                            containmentTypes.add(type);
                            containmentNames.add(name);
                            computeContainments(containmentTypes, containmentNames, ePackage, getEReferences((EClass) type));
                        }
                    }
                }
            }
        }
    }

    private Iterable<EReference> getEReferences(final EClass type) {
        return Iterables.filter(type.getEStructuralFeatures(), EReference.class);
    }

    private List<EClass> eClass2subTypes(final EPackage ePackage, final EClassifier eClass) {
        if (eClass2subTypes == null) {
            eClass2subTypes = Maps.newHashMap();
            final Iterator<?> classifiers = ETools.computeAllClassifiersList(ePackage, false).iterator();
            while (classifiers.hasNext()) {
                final Object next = classifiers.next();
                if (next instanceof EClass) {
                    final EClass c = (EClass) next;
                    final Iterator<EClass> superTypes = c.getEAllSuperTypes().iterator();
                    while (superTypes.hasNext()) {
                        final EClass superType = superTypes.next();
                        List<EClass> subTypes = eClass2subTypes.get(superType);
                        if (subTypes == null) {
                            subTypes = new ArrayList<EClass>();
                            eClass2subTypes.put(superType, subTypes);
                        }
                        if (!subTypes.contains(c)) {
                            subTypes.add(c);
                        }
                    }
                }
            }
        }
        List<EClass> types = eClass2subTypes.get(eClass);
        if (types == null) {
            types = new ArrayList<EClass>();
        }
        return types;
    }

    /**
     * Gets the containing feature of an EObject.
     * 
     * @param current
     *            is the object
     * @return the containing feature
     */
    public EStructuralFeature eContainingFeature(final EObject current) {
        return current.eContainingFeature();
    }

    /**
     * Gets the containment feature of an EObject.
     * 
     * @param current
     *            is the object
     * @return the containment feature
     */
    public EReference eContainmentFeature(final EObject current) {
        return current.eContainmentFeature();
    }

    /**
     * Gets the cross referenced objects.
     * 
     * @param current
     *            is the object
     * @return the cross referenced objects
     */
    public List<EObject> eCrossReferences(final EObject current) {
        return current.eCrossReferences();
    }

    /**
     * Gets the containing resource, or null.
     * 
     * @param current
     *            is the object
     * @return the resource
     */
    public String eResource(final EObject current) {
        if (current != null && current.eResource() != null) {
            return current.eResource().getURI().path();
        } else {
            return null;
        }
    }

    /**
     * Gets the containing resource name, or null.
     * 
     * @param current
     *            is the object
     * @return the resource
     */
    public String eResourceName(final EObject current) {
        if (current != null && current.eResource() != null) {
            return current.eResource().getURI().lastSegment();
        } else {
            return null;
        }
    }

    /**
     * Loads the root element of the given model.
     * 
     * @param node
     *            is the current node
     * @param rootpath
     *            is the path of the model to load
     * @return the root element of the model
     */
    public EObject load(final ENode node, final String rootpath) {
        String path = rootpath;
        if (path.startsWith("/resource")) {
            path = path.substring("/resource".length());
        }
        final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
        if (file.exists()) {
            EObject result = (EObject) load.get(file);
            if (result == null) {
                result = ETools.loadXMI(path);
                load.put(file, result);
            }
            return result;
        } else {
            return ETools.loadXMI(path);
        }
    }

}
