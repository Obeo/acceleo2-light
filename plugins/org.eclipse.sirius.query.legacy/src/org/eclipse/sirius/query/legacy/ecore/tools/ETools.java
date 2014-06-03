/*******************************************************************************
 * Copyright (c) 2005-2014 Obeo
 *  
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.ecore.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EcorePackageImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.sirius.query.legacy.ecore.AcceleoEcoreMessages;
import org.eclipse.sirius.query.legacy.ecore.AcceleoEcorePlugin;
import org.eclipse.sirius.query.legacy.tools.plugins.AcceleoMetamodelProvider;
import org.eclipse.sirius.query.legacy.tools.resources.Resources;

/**
 * This contains general support for ecore browsing.
 * 
 * 
 */
public class ETools {

    /**
     * Returns a URI for the eObject, i.e., either the eProxyURI, the URI of the
     * eResource with the fragment produced by the eResource, or the URI
     * consisting of just the fragment that would be produced by a default
     * Resource with the eObject as its only contents.
     * 
     * @param eObject
     *            the object for which to get the URI.
     * @return the URI for the object.
     */
    public static String getURI(EObject object) {
        if (object == null) {
            return ""; //$NON-NLS-1$
        } else if (object.eResource() != null) {
            return object.eResource().getURIFragment(object);
        } else {
            // inspired from EMF sources
            StringBuffer result = new StringBuffer("//"); //$NON-NLS-1$
            List uriFragmentPath = new ArrayList();
            for (EObject container = object.eContainer(); container != null; container = object.eContainer()) {
                uriFragmentPath.add(((InternalEObject) container).eURIFragmentSegment(object.eContainmentFeature(), object));
                object = container;
            }
            int size = uriFragmentPath.size();
            if (size > 0) {
                for (int i = size - 1;; --i) {
                    result.append((String) uriFragmentPath.get(i));
                    if (i == 0) {
                        break;
                    } else {
                        result.append('/');
                    }
                }
            }
            return result.toString();
        }
    }

    /**
     * It validates the given EMF object.
     * <p>
     * An error is put in the acceleo log if the validation failed.
     * <p>
     * <li>!blocker || Diagnostic.OK => root</li>
     * <li>blocker && !Diagnostic.OK => null</li>
     * <li>root == null => null</li>
     * 
     * @param root
     *            is the object to validate
     * @param blocker
     *            indicates if the result must be Diagnostic.OK
     * @param message
     *            is the error message to put in the acceleo log
     * @return the given object or null
     */
    public static EObject validate(EObject root, boolean blocker, String message) {
        if (root != null && !(root.getClass().getName().startsWith("org.eclipse.uml2"))) { //$NON-NLS-1$
            if (Diagnostician.INSTANCE.validate(root).getSeverity() != Diagnostic.OK) {
                AcceleoEcorePlugin.getDefault().log(message, blocker);
                if (blocker) {
                    return null;
                }
            }
        }
        return root;
    }

    /**
     * Creates root package for the metamodel nsURI or the metamodel file path.
     * <p>
     * uri2EPackage(path,true)
     * 
     * @param path
     *            is the metamodel nsURI or the metamodel file path
     * @return the root package of the metamodel
     */
    public static EPackage uri2EPackage(String path) {
        return ETools.uri2EPackage(path, true);
    }

    /**
     * Creates root package for the metamodel nsURI or the metamodel file path.
     * 
     * @param path
     *            is the metamodel nsURI or the metamodel file path
     * @param forceReload
     *            indicates that the reload of the metamodel is forced, if
     *            forceReload is false then file modification stamp is used for
     *            optimization
     * @return the root package of the metamodel
     */
    public static EPackage uri2EPackage(String path, boolean forceReload) {
        if (path != null && path.length() > 0) {
            path = path.trim();
            EPackage regValue = EPackage.Registry.INSTANCE.getEPackage(path);
            if (regValue != null) {
                return regValue;
            } else {
                return ETools.ecore2EPackage(path, forceReload);
            }
        } else {
            return null;
        }
    }

    /**
     * Creates root package for the ecore file path.
     * 
     * @param path
     *            is the ecore file path
     * @param forceReload
     *            indicates that the reload of the metamodel is forced, if
     *            forceReload is false then file modification stamp is used
     * @return the root package of the metamodel
     */
    private static EPackage ecore2EPackage(String path, boolean forceReload) {
        IPath ecorePath = new Path(path);
        if (ecorePath.segmentCount() >= 2) {
            path = ecorePath.removeFileExtension().addFileExtension("ecore").toString(); //$NON-NLS-1$
            IFile ecoreFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
            if (ecoreFile != null && ecoreFile.exists()) {
                if (forceReload) {
                    return ETools.subEcore2EPackage(path, false);
                } else {
                    EPackage ePackage = (EPackage) ETools.ecore2EPackage.get(path);
                    Double newModificationStamp = new Double(ecoreFile.getModificationStamp());
                    Double oldModificationStamp = (Double) ETools.ecore2OldModificationStamp.get(path);
                    if (ePackage == null || oldModificationStamp == null || oldModificationStamp.doubleValue() != newModificationStamp.doubleValue()) {
                        ePackage = ETools.subEcore2EPackage(path, false);
                        ETools.ecore2EPackage.put(path, ePackage);
                        ETools.ecore2OldModificationStamp.put(path, newModificationStamp);
                    }
                    return ePackage;
                }
            } else {
                File file = AcceleoMetamodelProvider.getDefault().getFile(new Path(path));
                if (file != null && file.exists()) {
                    path = file.getAbsolutePath();
                    if (forceReload) {
                        return ETools.subEcore2EPackage(path, true);
                    } else {
                        EPackage ePackage = (EPackage) ETools.ecore2EPackage.get(path);
                        if (ePackage == null) {
                            ePackage = ETools.subEcore2EPackage(path, true);
                            ETools.ecore2EPackage.put(path, ePackage);
                            ETools.ecore2OldModificationStamp.put(path, null);
                        }
                        return ePackage;
                    }
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private static Map ecore2EPackage = new WeakHashMap();

    private static Map ecore2OldModificationStamp = new HashMap();

    private static EPackage subEcore2EPackage(String path, boolean isExternal) {
        URI ecoreURI;
        if (isExternal) {
            ecoreURI = URI.createFileURI(path);
        } else {
            ecoreURI = Resources.createPlatformResourceURI(path);
        }
        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        reg.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl()); //$NON-NLS-1$
        ResourceSet ecoreResourceSet = new ResourceSetImpl();
        Resource ecoreResource = ecoreResourceSet.getResource(ecoreURI, true);
        Object result = (ecoreResource.getContents().size() > 0) ? ecoreResource.getContents().get(0) : null;
        if (result instanceof EPackage) {
            String nsURI = ETools.getNsURI((EPackage) result);
            if (nsURI != null) {
                EPackage regValue = EPackage.Registry.INSTANCE.getEPackage(nsURI);
                if (regValue != null) {
                    return regValue;
                }
            }
            return (EPackage) ETools.validate((EPackage) result, false, AcceleoEcoreMessages.getString("ETools.ModelValidationNeeded", new Object[] { path, })); //$NON-NLS-1$
        } else {
            return null;
        }
    }

    private static String getNsURI(EPackage p) {
        String pNsURI = p.getNsURI();
        if (pNsURI != null && pNsURI.length() > 0) {
            return pNsURI;
        }
        if (p.getESubpackages().size() == 1) {
            return ETools.getNsURI(p.getESubpackages().get(0));
        } else {
            return null;
        }
    }

    /**
     * Search all the classifiers recursively in a package.
     * 
     * @param ePackage
     *            is the container
     * @return table of classifiers
     */
    public static EClassifier[] computeAllClassifiers(EPackage ePackage) {
        List classifiers = ETools.computeAllClassifiersList(ePackage);
        return (EClassifier[]) classifiers.toArray(new EClassifier[] {});
    }

    /**
     * Search all the classifiers recursively in a package.
     * 
     * @param ePackage
     *            is the container
     * @return list of classifiers
     */
    public static List computeAllClassifiersList(EPackage ePackage) {
        return ETools.computeAllClassifiersList(ePackage, false);
    }

    /**
     * Search all the classifiers recursively in a package.
     * 
     * @param ePackage
     *            is the container
     * @param classOnly
     *            indicates that only the classes are kept
     * @return list of classifiers
     */
    public static List computeAllClassifiersList(EPackage ePackage, boolean classOnly) {
        List classifiers = new BasicEList();
        if (ePackage != null) {
            ETools.computeAllClassifiersList(ePackage, classifiers, classOnly);
        }
        return classifiers;
    }

    private static void computeAllClassifiersList(EPackage ePackage, List all, boolean classOnly) {
        Iterator classifiers = ePackage.getEClassifiers().iterator();
        while (classifiers.hasNext()) {
            EClassifier classifier = (EClassifier) classifiers.next();
            if (!classOnly) {
                all.add(classifier);
            } else {
                if (classifier instanceof EClass && !((EClass) classifier).isAbstract() && !((EClass) classifier).isInterface()) {
                    all.add(classifier);
                }
            }
        }
        Iterator packages = ePackage.getESubpackages().iterator();
        while (packages.hasNext()) {
            ETools.computeAllClassifiersList((EPackage) packages.next(), all, classOnly);
        }
    }

    /**
     * Search a classifier recursively in a package.
     * <p>
     * Remarks :
     * <li>It never returns classifier java.resources.Folder if name = "File"</li>
     * <li>It never returns classifier java.resources.Folder if name = "older"</li>
     * <li>It returns classifier java.resources.Folder for "Folder" or
     * "resources.Folder"</li>
     * 
     * @param ePackage
     *            is the container
     * @param name
     *            is the classifier identifier
     * @return classifier or null if not found
     */
    public static EClassifier getEClassifier(EPackage ePackage, String name) {
        if (ePackage == null || name == null) {
            return null;
        }
        name = name.trim();
        EClassifier get = null;
        /*
         * Special case for EObject as we want a generic type for all the
         * elements.
         */
        if (name.equals("EObject") || name.equals("ecore.EObject")) { //$NON-NLS-1$ //$NON-NLS-2$
            get = EcorePackage.eINSTANCE.getEObject();
        }
        Iterator classifiers = ePackage.getEClassifiers().iterator();
        while (get == null && classifiers.hasNext()) {
            EClassifier classifier = (EClassifier) classifiers.next();
            String instanceClassName = '.' + ETools.getEClassifierPath(classifier);
            String endsWith = '.' + name;
            if (instanceClassName.endsWith(endsWith)) {
                get = classifier;
            }
        }
        Iterator packages = ePackage.getESubpackages().iterator();
        while (get == null && packages.hasNext()) {
            EClassifier classifier = ETools.getEClassifier((EPackage) packages.next(), name);
            if (classifier != null) {
                get = classifier;
            }
        }
        return get;
    }

    /**
     * Returns the feature with this classifier and this name.
     * 
     * @param currentEClassifier
     *            is the classifier
     * @param name
     *            is the feature name
     * @return the feature
     */
    public static EStructuralFeature getEStructuralFeature(EClassifier currentEClassifier, String name) {
        if (currentEClassifier != null && currentEClassifier instanceof EClass) {
            return ETools.getEStructuralFeature((EClass) currentEClassifier, name);
        } else {
            return null;
        }
    }

    /**
     * Returns the feature with this class and this name.
     * 
     * @param currentEClass
     *            is the class
     * @param name
     *            is the feature name
     * @return the feature
     */
    public static EStructuralFeature getEStructuralFeature(EClass currentEClass, String name) {
        name = name.trim();
        if (currentEClass != null) {
            return currentEClass.getEStructuralFeature(name);
        } else {
            return null;
        }
    }

    /**
     * Returns a factory name for a classifier. There is a factory by package.
     * The factory is used to create instances of classifiers.
     * <p>
     * Sample : "ResourcesFactory" is the name of the factory
     * java.resources.ResourcesFactory
     * 
     * @param eClassifier
     *            is the classifier
     * @return the factory name
     */
    public static String getEClassifierFactoryName(EClassifier eClassifier) {
        return ETools.getEClassifierFactoryShortName(eClassifier) + "Factory"; //$NON-NLS-1$
    }

    /**
     * Returns a factory short name for a classifier. There is a factory by
     * package. The factory is used to create instances of classifiers.
     * <p>
     * Sample : "Resources" is the short name of the factory
     * java.resources.ResourcesFactory
     * 
     * @param eClassifier
     *            is the classifier
     * @return the factory short name
     */
    public static String getEClassifierFactoryShortName(EClassifier eClassifier) {
        if (eClassifier != null) {
            EPackage p = eClassifier.getEPackage();
            String name = p.getName();
            if (name != null && name.length() > 0) {
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
        }
        return ""; //$NON-NLS-1$
    }

    /**
     * Returns the full path of the classifier.
     * <p>
     * Sample : "java.resources.JavaFile" is the full path for the classifier
     * java.resources.JavaFile
     * 
     * @param eClassifier
     *            is the classifier
     * @return full path of the classifier
     */
    public static String getEClassifierPath(EClassifier eClassifier) {
        if (eClassifier != null) {
            String instanceClassName = eClassifier.getInstanceClassName();
            String name = eClassifier.getName();
            if (eClassifier.getEPackage() != null) {
                EPackage container = eClassifier.getEPackage();
                if (container != null && instanceClassName != null && instanceClassName.endsWith(container.getName() + '.' + name)) {
                    return instanceClassName;
                }
                while (container != null) {
                    name = container.getName() + '.' + name;
                    container = container.getESuperPackage();
                }
            }
            return name;
        } else {
            return null;
        }
    }

    /**
     * Returns the short path of the classifier.
     * <p>
     * Sample : "resources.JavaFile" is the short path for the classifier
     * java.resources.JavaFile
     * 
     * @param eClassifier
     *            is the classifier
     * @return short path of the classifier
     */
    public static String getEClassifierShortPath(EClassifier eClassifier) {
        String name = eClassifier.getName();
        if (eClassifier.getEPackage() != null) {
            name = eClassifier.getEPackage().getName() + '.' + name;
        }
        return name;
    }

    /**
     * Creates a package and his children in an ecore model. The children are
     * separated by '.'
     * <p>
     * Sample : "a.b.c" is a package full path,
     * <p>
     * "a", "b", and "c" packages are created, the root package "a" is returned.
     * 
     * @param path
     *            is the package full path
     * @return the root package
     */
    public static EPackage createPackageHierarchy(String path) {
        EPackage ePackage = null;
        if (path != null && path.length() > 0) {
            EcorePackage p = EcorePackageImpl.init();
            EcoreFactory factory = p.getEcoreFactory();
            EPackage parent = null;
            StringTokenizer st = new StringTokenizer(path, "."); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                EPackage child = factory.createEPackage();
                child.setName(name);
                if (parent != null) {
                    parent.getESubpackages().add(child);
                } else {
                    ePackage = child;
                }
                parent = child;
            }
        }
        return ePackage;
    }

    /**
     * Get a package in a parent package.
     * <p>
     * Sample : "a.b" is a parent package and "c.d" is a relative path,
     * <p>
     * "a.b.c.d" package is returned.
     * 
     * @param parent
     *            is the parent package
     * @param path
     *            is the relative path of the required package
     * @return the required package, null if not found
     */
    public static EPackage getEPackage(EPackage parent, String path) {
        EPackage ePackage = parent;
        if (path != null && path.length() > 0) {
            StringTokenizer st = new StringTokenizer(path, "."); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                boolean found = false;
                Iterator subPackages = ePackage.getESubpackages().iterator();
                while (!found && subPackages.hasNext()) {
                    EPackage subPackage = (EPackage) subPackages.next();
                    if (subPackage.getName().equals(name)) {
                        found = true;
                        ePackage = subPackage;
                    }
                }
                if (!found) {
                    return null;
                }
            }
        }
        return ePackage;
    }

    /**
     * Indicates if an instance of the classifier is an instance of the type.
     * 
     * @param classifier
     *            is the classifier
     * @param type
     *            is the type
     * @return true if an instance of the classifier is an instance of the type
     */
    public static boolean ofType(EClassifier classifier, String type) {
        if (classifier instanceof EClass) {
            return ETools.ofType((EClass) classifier, type);
        } else {
            return ETools.ofClass(classifier, type);
        }
    }

    /**
     * Indicates if an instance of the class is an instance of the type.
     * 
     * @param eClass
     *            is the class
     * @param type
     *            is the type
     * @return true if an instance of the class is an instance of the type
     */
    public static boolean ofType(EClass eClass, String type) {
        if ("EObject".equalsIgnoreCase(type) || "ecore.EObject".equalsIgnoreCase(type) || ETools.ofClass(eClass, type)) {
            return true;
        }
        Iterator superTypes = eClass.getESuperTypes().iterator();
        while (superTypes.hasNext()) {
            EClassifier superType = (EClassifier) superTypes.next();
            if (ETools.ofType(superType, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates if the type corresponds to the name of the classifier.
     * 
     * @param classifier
     *            is the classifier
     * @param type
     *            is the type
     * @return true if the type corresponds to the name of the classifier
     */
    public static boolean ofClass(EClassifier classifier, String type) {
        String path = ETools.getEClassifierPath(classifier);
        return ('.' + path).endsWith('.' + type);
    }

    /**
     * Loads an EMF model.
     * 
     * @param path
     *            is the path of the model to load
     * @return the root element of the model
     */
    public static EObject loadXMI(String path) {
        return ETools.loadXMI(path, null);
    }

    /**
     * Loads an EMF model, using a specific resource factory (using UML resource
     * for an XMI file)
     * 
     * @param path
     *            is the path of the model to load
     * @param resourceFactoryExtension
     *            is the resource factory default extension, for example "uml",
     *            can be null
     * 
     * @return the root element of the model
     */
    public static EObject loadXMI(String path, String resourceFactoryExtension) {
        URI modelURI = Resources.createPlatformResourceURI(path);
        String fileExtension = modelURI.fileExtension();
        if (fileExtension == null || fileExtension.length() == 0) {
            fileExtension = Resource.Factory.Registry.DEFAULT_EXTENSION;
        }
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Object resourceFactory;
        if (resourceFactoryExtension != null) {
            resourceFactory = reg.getExtensionToFactoryMap().get(resourceFactoryExtension);
        } else {
            resourceFactory = reg.getExtensionToFactoryMap().get(fileExtension);
        }
        if (resourceFactory != null) {
            resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension, resourceFactory);
        } else {
            resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension, new XMIResourceFactoryImpl());
        }
        Resource modelResource = resourceSet.getResource(modelURI, true);
        return ((modelResource.getContents().size() > 0) ? modelResource.getContents().get(0) : null);
    }

}
