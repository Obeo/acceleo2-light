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

package fr.obeo.acceleo.gen.template.eval.merge;

import org.eclipse.core.resources.IFile;

/**
 * A strategy to overdone the user code after the generation.
 * 
 * @author www.obeo.fr
 * 
 */
public interface IOverdoneCodeStrategy {

	/**
	 * Moves the overdone user code from the old buffer to the new buffer.
	 * 
	 * @param file
	 *            is the file to generate
	 * @param oldBuffer
	 *            is the old buffer
	 * @param newBuffer
	 *            is the new buffer
	 * @param lost
	 *            is the content of the lost file to create
	 */
	public void keepOverdoneCode(IFile file, StringBuffer oldBuffer, StringBuffer newBuffer, StringBuffer lost);

}
