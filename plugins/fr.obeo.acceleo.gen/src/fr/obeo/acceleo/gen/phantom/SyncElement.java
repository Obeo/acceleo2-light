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

package fr.obeo.acceleo.gen.phantom;

import java.util.Map;

/**
 * An element that can be synchronized.
 * 
 * @author www.obeo.fr
 * 
 */
public interface SyncElement {

	/**
	 * @return correspondences between the input and the output
	 */
	public Map inOutMapping();

	/**
	 * @return correspondences between the template element and the output
	 */
	public Map templateOutMapping();

	/**
	 * @return correspondences between the output and the input
	 */
	public Map outInMapping();

	/**
	 * @return correspondences between the output and the template element
	 */
	public Map outTemplateMapping();

	/**
	 * @return the content of this element
	 */
	public Object getContent();

}
