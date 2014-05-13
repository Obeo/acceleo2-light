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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import fr.obeo.acceleo.ecore.AcceleoEcoreMessages;
import fr.obeo.acceleo.ecore.tools.ETools;

/**
 * This contains all factories for a metamodel. There is a factory by package.
 * Each factory is used to create instances of classifiers.
 * 
 * @author www.obeo.fr
 * 
 */
public class Factories {

	/**
	 * Root package of the metamodel.
	 */
	protected EPackage root;

	/**
	 * All factories for the metamodel.
	 */
	protected Map factories;

	/**
	 * @return root package of the metamodel.
	 */
	public EPackage getRoot() {
		return root;
	}

	/**
	 * Creates all the factories for the root package.
	 * <p>
	 * Remark : Discouraged acces.
	 * <p>
	 * Sample : new Factories(EcoreFactory.eINSTANCE.getEPackage())
	 * 
	 * @param root
	 *            is the root package
	 * @param loader
	 *            is the class loader
	 * @throws FactoryException
	 */
	public Factories(EPackage root, ClassLoader loader) throws FactoryException {
		this.root = root;
		factories = createFactories(root, false, loader);
	}

	/**
	 * Creates all the factories for the metamodel nsURI or the metamodel file
	 * path.
	 * 
	 * @param path
	 *            is the metamodel nsURI or the metamodel file path
	 * @param loader
	 *            is the class loader
	 * @throws FactoryException
	 */
	public Factories(String path, ClassLoader loader) throws FactoryException {
		EPackage regValue = EPackage.Registry.INSTANCE.getEPackage(path);
		if (regValue != null) {
			root = regValue;
			factories = createFactories(root, false, loader);
		} else {
			root = ETools.uri2EPackage(path);
			factories = createFactories(root, true, loader);
		}
	}

	/**
	 * Creates all factories for the root package of the metamodel.
	 * 
	 * @param ePackage
	 *            is the root package
	 * @param loadingFromFile
	 *            indicates that the factories have been created from a file
	 * @param loader
	 *            is the class loader
	 * @return a map that contains all factories
	 * @throws FactoryException
	 */
	protected Map createFactories(EPackage ePackage, boolean loadingFromFile, ClassLoader loader) throws FactoryException {
		Map factories = new HashMap();
		Iterator classifiers = ETools.computeAllClassifiersList(ePackage).iterator();
		while (classifiers.hasNext()) {
			EClassifier eClassifier = (EClassifier) classifiers.next();
			if (eClassifier instanceof EClass) {
				String id = ETools.getEClassifierFactoryID(eClassifier);
				String name = ETools.getEClassifierFactoryShortName(eClassifier);
				EFactory factory = (EFactory) factories.get(id);
				if (factory == null) {
					if (loadingFromFile) {
						if (EPackage.Registry.INSTANCE.getEPackage(eClassifier.getEPackage().getNsURI()) == null) {
							factory = new EFactory(id, name, loader);
						} else {
							factory = new EFactory(id, eClassifier.getEPackage(), loader);
						}
					} else {
						factory = new EFactory(id, eClassifier.getEPackage(), loader);
					}
					factories.put(id, factory);
				}
			}
		}
		return factories;
	}

	/**
	 * Gets a factory with an identifier of factory.
	 * 
	 * @param id
	 *            is the identifier of the factory
	 * @return the factory
	 * @throws FactoryException
	 */
	public EFactory get(String id) throws FactoryException {
		EFactory factory = (EFactory) factories.get(id);
		if (factory == null) {
			Iterator it = factories.entrySet().iterator();
			while (factory == null && it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String key = '.' + entry.getKey().toString();
				if (key.endsWith('.' + id)) {
					factory = (EFactory) entry.getValue();
				}
			}
			if (factory != null) {
				factories.put(id, factory);
			} else {
				throw new FactoryException(AcceleoEcoreMessages.getString("Factories.UnresolvedFactoryId", new Object[] { id, })); //$NON-NLS-1$
			}
		}
		return factory;
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
	public EObject create(String name) throws FactoryException {
		EFactory factory = (EFactory) name2Factory.get(name);
		if (factory == null) {
			EClassifier eClassifier = ETools.getEClassifier(root, name);
			if (eClassifier != null) {
				String id = ETools.getEClassifierFactoryID(eClassifier);
				factory = get(id);
				name2Factory.put(name, factory);
			} else {
				throw new FactoryException(AcceleoEcoreMessages.getString("Factories.CreationFailed", new Object[] { name, })); //$NON-NLS-1$
			}
		}
		int i = name.lastIndexOf('.');
		if (i > -1)
			name = name.substring(i + 1);
		return factory.eCreate(name);
	}

	private Map name2Factory = new HashMap();

}
