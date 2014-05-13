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
				throw new FactoryException(AcceleoEcoreMessages.getString(
						"Factories.UnresolvedFactoryId", new Object[] { id, })); //$NON-NLS-1$
			}
		}
		return factory;
	}

}
