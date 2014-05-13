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

package fr.obeo.acceleo.gen.template.scripts.imports.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import fr.obeo.acceleo.ecore.factories.EFactory;
import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.ecore.tools.ETools;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.tools.resources.FileContentMap;

/**
 * System services for EObject elements.
 * 
 * @author www.obeo.fr
 * 
 */
public class EObjectServices {

    /**
     * Constructor. For non eAllContents operations.
     * 
     */
    public EObjectServices() {
        // nothing to do here
    }

    /**
     * The script.
     */
    protected IScript script;

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public EObjectServices(IScript script) {
        this.script = script;
        if (script instanceof SpecificScript) {
            addMetamodel(((SpecificScript) script).getMetamodel());
        }
    }

    /**
     * Initialize eAllContents maps for the given metamodel root package.
     * 
     * @param metamodel
     *            the root package of a metamodel
     */
    protected void addMetamodel(EPackage metamodel) {
        if (!eRootPackages.contains(metamodel)) {
            listClassifiers(metamodel);
            eRootPackages.add(metamodel);
        }
    }

    /**
     * Gets the container of an EObject.
     * 
     * @param current
     *            is the object
     * @return the container
     */
    public EObject eContainer(EObject current) {
        return current.eContainer();
    }

    /**
     * Gets recursively the container of an EObject. The recursivity is stopped
     * when an element of the given type is found.
     * 
     * @throws FactoryException
     */
    public EObject eContainer(EObject current, String type) throws FactoryException {
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
    public EObject getRootContainer(EObject eObject) {
        return EcoreUtil.getRootContainer(eObject);
    }

    /**
     * Gets the metamodel class of an EObject.
     * 
     * @param current
     *            is the object
     * @return the metamodel class
     */
    public EObject eClass(EObject current) {
        return current.eClass();
    }

    /**
     * Gets the children of an EObject.
     * 
     * @param current
     *            is the object
     * @return the children
     */
    public EList eContents(EObject current) {
        return current.eContents();
    }

    /**
     * Gets all the direct contents and indirect contents of this object.
     * 
     * @param current
     *            is the object
     * @return the contents
     */
    public ENodeList eAllContents(ENode current) {
        ENodeList result = new ENodeList();
        try {
            if (current.isList()) {
                ENodeIterator it = current.getList().iterator();
                while (it.hasNext()) {
                    result.addAll(eAllContents(it.next()));
                }
            } else if (current.isEObject()) {
                TreeIterator it = current.getEObject().eAllContents();
                while (it.hasNext()) {
                    result.add(ENode.createTry(it.next(), current));
                }
            }
        } catch (ENodeCastException e) {
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
     */
    public ENodeList eAllContents(ENode current, String type) throws FactoryException {
        ENodeList result = new ENodeList();
        try {
            if (current.isList()) {
                ENodeIterator it = current.getList().iterator();
                while (it.hasNext()) {
                    result.addAll(eAllContents(it.next(), type));
                }
            } else if (current.isEObject()) {
                StringBuffer buffer = new StringBuffer();
                Resource eResource = current.getEObject().eResource();
                if (eResource != null) {
                    buffer.append(eResource.getURI().path());
                    buffer.append(':');
                    buffer.append(eResource.getURIFragment(current.getEObject()));
                } else {
                    buffer.append(':');
                    buffer.append(ETools.getURI(current.getEObject()));
                }
                buffer.append(':');
                buffer.append(type);
                String uri = buffer.toString();
                List children = (List) eAllContents.get(uri);
                if (children == null) {
                    int i = type.lastIndexOf("."); //$NON-NLS-1$
                    String typeName = i > -1 ? type.substring(i + 1) : type;
                    children = eAllContents(current.getEObject(), type, typeName);
                    eAllContents.put(uri, children);
                }
                Iterator it = children.iterator();
                while (it.hasNext()) {
                    result.add(ENode.createTry(it.next(), current));
                }
            }
        } catch (ENodeCastException e) {
            // Never catch
        }
        return result;
    }

    private Map eAllContents = new WeakHashMap();

    private List eRootPackages = new ArrayList();

    private Map eClass2containments = new HashMap();

    private Map eClass2subTypes = null;

    private List eAllContents(EObject object, String type, String typeName) {
        List result = new ArrayList();
        List containmentTypeNames = getContainmentNames(object);
        if (containmentTypeNames.contains(typeName)) {
            Iterator eContents = object.eContents().iterator();
            while (eContents.hasNext()) {
                EObject eContent = (EObject) eContents.next();
                if (EFactory.eInstanceOf(eContent, type)) {
                    result.add(eContent);
                }
                result.addAll(eAllContents(eContent, type, typeName));
            }
        }
        return result;
    }

    private List getContainmentNames(EObject object) {
        EClass eClass = object.eClass();
        EPackage ePackage = (EPackage) getRootContainer(eClass);
        addMetamodel(ePackage);
        String eClassName = eClass.getName();
        List containmentNames = (List) eClass2containments.get(eClassName);
        if (containmentNames == null) {
            containmentNames = new ArrayList();
            computeContainments(new ArrayList(), containmentNames, ePackage, eClass.getEAllReferences());
            eClass2containments.put(eClassName, containmentNames);
        }
        return containmentNames;
    }

    private void computeContainments(List containmentTypes, List containmentNames, EPackage ePackage, List eReferences) {
        Iterator references = eReferences.iterator();
        while (references.hasNext()) {
            EReference eReference = (EReference) references.next();
            if (eReference.isContainment()) {
                Set types = new HashSet();
                types.add(eReference.getEType());
                if (eReference.getEType() instanceof EClass) {
                    types.addAll(((EClass) eReference.getEType()).getEAllSuperTypes());
                }
                Iterator subTypes = eClass2subTypes(ePackage, eReference.getEType()).iterator();
                while (subTypes.hasNext()) {
                    EClassifier subType = (EClassifier) subTypes.next();
                    types.add(subType);
                    if (subType instanceof EClass) {
                        Iterator it = ((EClass) subType).getEAllSuperTypes().iterator();
                        while (it.hasNext()) {
                            EClassifier superType = (EClassifier) it.next();
                            String name = superType.getName();
                            if (!containmentNames.contains(name)) {
                                containmentNames.add(name);
                            }
                        }
                    }
                }
                Iterator it = types.iterator();
                while (it.hasNext()) {
                    EClassifier type = (EClassifier) it.next();
                    if (type instanceof EClass) {
                        String name = type.getName();
                        if (!containmentTypes.contains(type)) {
                            containmentTypes.add(type);
                            if (!containmentNames.contains(name)) {
                                containmentNames.add(name);
                            }
                            computeContainments(containmentTypes, containmentNames, ePackage, ((EClass) type).getEReferences());
                        }
                    }
                }
            }
        }
    }

    private List eClass2subTypes(EPackage ePackage, EClassifier eClass) {
        if (eClass2subTypes == null) {
            eClass2subTypes = new HashMap();
        }
        List types = (List) eClass2subTypes.get(eClass);
        if (types == null) {
            types = new ArrayList();
        }
        return types;
    }

    private void listClassifiers(EPackage ePackage) {
        Iterator classifiers = ETools.computeAllClassifiersList(ePackage, false).iterator();

        if (eClass2subTypes == null) {
            eClass2subTypes = new HashMap();
        }
        while (classifiers.hasNext()) {
            Object next = classifiers.next();
            if (next instanceof EClass) {
                EClass c = (EClass) next;
                Iterator superTypes = c.getEAllSuperTypes().iterator();
                while (superTypes.hasNext()) {
                    EClass superType = (EClass) superTypes.next();
                    List subTypes = (List) eClass2subTypes.get(superType);
                    if (subTypes == null) {
                        subTypes = new ArrayList();
                        eClass2subTypes.put(superType, subTypes);
                    }
                    if (!subTypes.contains(c)) {
                        subTypes.add(c);
                    }
                }
            }
        }
    }

    /**
     * Gets the containing feature of an EObject.
     * 
     * @param current
     *            is the object
     * @return the containing feature
     */
    public EStructuralFeature eContainingFeature(EObject current) {
        return current.eContainingFeature();
    }

    /**
     * Gets the containment feature of an EObject.
     * 
     * @param current
     *            is the object
     * @return the containment feature
     */
    public EReference eContainmentFeature(EObject current) {
        return current.eContainmentFeature();
    }

    /**
     * Gets the cross referenced objects.
     * 
     * @param current
     *            is the object
     * @return the cross referenced objects
     */
    public List eCrossReferences(EObject current) {
        return current.eCrossReferences();
    }

    /**
     * Gets the containing resource, or null.
     * 
     * @param current
     *            is the object
     * @return the resource
     */
    public String eResource(EObject current) {
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
    public String eResourceName(EObject current) {
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
     * @param path
     *            is the path of the model to load
     * @return the root element of the model
     */
    public EObject load(ENode node, String path) {
        if (path.startsWith("/resource")) { //$NON-NLS-1$
            path = path.substring("/resource".length()); //$NON-NLS-1$
        }
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
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

    private FileContentMap load = new FileContentMap();

}
