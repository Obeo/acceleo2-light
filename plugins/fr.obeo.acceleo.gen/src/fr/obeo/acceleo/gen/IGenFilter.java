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

package fr.obeo.acceleo.gen;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;

/**
 * A generation filter.
 * 
 * @author www.obeo.fr
 * 
 */
public interface IGenFilter {

	/**
	 * The method used to filter or not the generation.
	 * 
	 * @param script
	 *            is the script
	 * @param targetFile
	 *            is the file to generate
	 * @param object
	 *            is the object
	 * @return true if the generation will be executed
	 * @throws CoreException
	 */
	public boolean filter(File script, IFile targetFile, EObject object) throws CoreException;

}
